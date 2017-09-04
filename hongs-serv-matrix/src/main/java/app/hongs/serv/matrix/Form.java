package app.hongs.serv.matrix;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.db.DB;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.db.util.FetchCase;
import app.hongs.util.Data;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * 表单模型
 * @author Hongs
 */
public class Form extends Model {

    protected String prefix = "manage/data";

    public Form() throws HongsException {
        this(DB.getInstance("matrix").getTable("form"));
    }

    public Form(Table table)
    throws HongsException {
        super(table);
    }

    /**
     * 添加/修改记录
     *
     * @param rd
     * @return 记录ID
     * @throws app.hongs.HongsException
     */
    @Override
    public int add(String id, Map rd) throws HongsException {
        List<Map> flds = parseConf(rd);
        String    name = (String)  rd.get("name");

        int an = superAdd (id,  rd );

        // 建立表单配置
        if (flds != null) {
            updateFormConf(id, flds);
        }

        // 建立菜单配置
        if (name != null) {
            updateFormMenu(id, name);
        }

        // 更新频道菜单
        String unitId = (String) rd.get( "unit_id" );
        String unitNm = (String) db.getTable("unit")
                .filter ("id = ?", unitId)
                .select ("name")
                .one    ()
                .get    ("name");
        updateUnitMenu(unitId, unitNm);

        return an;
    }

    @Override
    public int put(String id, Map rd) throws HongsException {
        List<Map> flds = parseConf(rd);
        String    name = (String)  rd.get("name");

        // 冻结意味着手动修改了配置
        // 再操作可能会冲掉自定配置
        if (!rd.containsKey("state")
        &&  !fetchCase()
            .field("id")
            .where("id = ? AND state = ?", id, 2)
            .one  (    )
            .isEmpty(  )) {
            throw new HongsException(0x1100, "表单已冻结, 禁止操作");
        }

        int an = superPut (id,  rd );

        // 建立表单配置
        if (flds != null) {
            updateFormConf(id, flds);
        }

        // 建立菜单配置
        if (name != null) {
            updateFormMenu(id, name);
        }

        return an;
    }

    @Override
    public int del(String id, FetchCase fc) throws HongsException {
        String unitId = (String) db.getTable("form")
                .filter ("id = ?", id)
                .select ("unit_id")
                .one    ()
                .get    ("unit_id");
        String unitNm = (String) db.getTable("unit")
                .filter ("id = ?", unitId)
                .select ("name")
                .one    ()
                .get    ("name");

        int n = super.del(id,fc);

        // 删除配置文件
        deleteFormConf(id);
        deleteFormMenu(id);

        // 更新单元菜单
        updateUnitMenu(unitId, unitNm);

        return n;
    }

    protected final int superAdd(String id, Map rd) throws HongsException {
        return super.add(id, rd);
    }

    protected final int superPut(String id, Map rd) throws HongsException {
        return super.put(id, rd);
    }

    protected final int superDel(String id, FetchCase fc) throws HongsException {
        return super.del(id, fc);
    }

    protected List<Map> parseConf(Map rd) {
        List<Map> flds = null;
        String conf = (String) rd.get("conf");
        String name = (String) rd.get("name");

        if (conf != null && !"".equals(conf)) {
            flds = Synt.asList(Data.toObject(conf));
            Set set = Synt.setOf("name", "find", "cuid", "muid", "ctime", "mtime");
            Map tdf = null;
            Map idf = null;
            Map fld ;

            Iterator<Map> itr = flds.iterator();
            while (itr.hasNext()) {
                fld = itr.next();
                if (   "".equals(fld.get("__name__"))) {
                    fld.put("__name__", Core.newIdentity());
                } else
                if (  "@".equals(fld.get("__name__"))) {
                    tdf = fld;
                    itr. remove (  );
                } else
                if ( "id".equals(fld.get("__name__"))) {
                    idf = fld;
                    itr. remove (  );
                } else
                if (set.contains(fld.get("__name__"))) {
                    set. remove (fld.get("__name__"));
                }
            }

            if (tdf != null) {
                flds.add(0, tdf);
                // 去掉表单的基础属性
                tdf.remove("__text__");
                tdf.remove("__type__");
                tdf.remove("__rule__");
                tdf.remove("__required__");
                tdf.remove("__repeated__");
            }
            if (idf != null) {
                flds.add(1, idf);
                // 去掉主键的基础属性
                idf.remove("__required__");
                idf.remove("__repeated__");
            }

            conf = Data.toString(flds);
            rd.put("conf", conf);

            // 补全表配置项
            fld = Synt.mapOf(
                "__text__", name,
                "__name__", "@",
                "listable", "?",
                "sortable", "?",
                "siftable", "?",
                "nameable", "?"
            );
            if (tdf != null) {
                fld .putAll(tdf);
                tdf .putAll(fld);
            } else {
                flds.add(0, fld);
            }

            // 补全编号字段
            fld = Synt.mapOf(
                "__text__", "ID",
                "__name__", "id",
                "__type__", "hidden"
            );
            if (idf != null) {
                fld .putAll(idf);
                idf .putAll(fld);
            } else {
                flds.add(1, fld);
            }

            // 增加名称字段
            if (set.contains("name")) {
                flds.add(Synt.mapOf(
                    "__name__", "name",
                    "__type__", "stored",
                    "editable", "false"
                ));
            }

            // 增加搜索字段
            if (set.contains("find")) {
                flds.add(Synt.mapOf(
                    "__name__", "find",
                    "__type__", "search",
                    "editable", "false" ,
                    "unstored", "true"
                ));
            }

            // 增加用户字段
            if (set.contains("cuid")) {
                flds.add(Synt.mapOf(
                    "__name__", "cuid",
                    "__type__", "hidden",
                    "editable", "false" ,
                    "default" , "=$uid" ,
                    "default-always", "true",
                    "default-create", "true"
                ));
            }
            if (set.contains("muid")) {
                flds.add(Synt.mapOf(
                    "__name__", "muid",
                    "__type__", "hidden",
                    "editable", "false" ,
                    "default" , "=$uid" ,
                    "default-always", "true",
                    "default-create", "false"
                ));
            }

            // 增加时间字段
            if (set.contains("ctime")) {
                flds.add(Synt.mapOf(
                    "__name__", "ctime" ,
                    "__type__", "number",
                      "type"  ,  "long" ,
                    "editable", "false" ,
                    "default" , "=%now" ,
                    "default-always", "true",
                    "default-create", "true"
                ));
            }
            if (set.contains("mtime")) {
                flds.add(Synt.mapOf(
                    "__name__", "mtime" ,
                    "__type__", "number",
                      "type"  ,  "long" ,
                    "editable", "false" ,
                    "default" , "=%now" ,
                    "default-always", "true",
                    "default-create", "false"
                ));
            }
        } else {
            rd.remove("conf");
        }

        return flds;
    }

    protected void deleteFormMenu(String id) {
        File fo = new File(Core.CONF_PATH +"/"+ prefix +"/"+ id + Cnst.FORM_EXT +".xml");
        if (fo.exists()) {
            fo.delete();
        }
    }

    protected void deleteFormConf(String id) {
        File fo = new File(Core.CONF_PATH +"/"+ prefix +"/"+ id + Cnst.NAVI_EXT +".xml");
        if (fo.exists()) {
            fo.delete();
        }
    }

    protected void updateUnitMenu(String id, String name) throws HongsException {
        Unit un = new Unit();
        un.updateUnitMenu(id, name);
        un.updateRootMenu(        );
    }

    protected void updateFormMenu(String id, String name) throws HongsException {
        Document docm = makeDocument();

        Element  root = docm.createElement("root");
        docm.appendChild ( root );

        Element  menu = docm.createElement("menu");
        root.appendChild ( menu );
        menu.setAttribute("text", name);
        menu.setAttribute("href", prefix+"/"+id+"/");
        menu.setAttribute("hrel", prefix+"/"+id+"/main.html");

        Element  role, actn, depn;

        // 会话
        role = docm.createElement("rsname");
        root.appendChild ( role );
        role.appendChild ( docm.createTextNode( "@manage" ) );

        // 查看

        role = docm.createElement("role");
        menu.appendChild ( role );
        role.setAttribute("name", prefix+"/"+id+"/search");
        role.setAttribute("text", "查看"+name);

        actn = docm.createElement("action");
        role.appendChild ( actn );
        actn.appendChild ( docm.createTextNode(prefix+"/"+id+"/search.act") );

        actn = docm.createElement("action");
        role.appendChild ( actn );
        actn.appendChild ( docm.createTextNode(prefix+"/"+id+"/stream.act") );

        // 修改

        role = docm.createElement("role");
        menu.appendChild ( role );
        role.setAttribute("name", prefix+"/"+id+"/update");
        role.setAttribute("text", "修改"+name);

        actn = docm.createElement("action");
        role.appendChild ( actn );
        actn.appendChild ( docm.createTextNode(prefix+"/"+id+"/update.act") );

        depn = docm.createElement("depend");
        role.appendChild ( depn );
        depn.appendChild ( docm.createTextNode(prefix+"/"+id+"/search") );

        // 添加

        role = docm.createElement("role");
        menu.appendChild ( role );
        role.setAttribute("name", prefix+"/"+id+"/create");
        role.setAttribute("text", "添加"+name);

        actn = docm.createElement("action");
        role.appendChild ( actn );
        actn.appendChild ( docm.createTextNode(prefix+"/"+id+"/create.act") );

        depn = docm.createElement("depend");
        role.appendChild ( depn );
        depn.appendChild ( docm.createTextNode(prefix+"/"+id+"/search") );

        // 删除

        role = docm.createElement("role");
        menu.appendChild ( role );
        role.setAttribute("name", prefix+"/"+id+"/delete");
        role.setAttribute("text", "删除"+name);

        actn = docm.createElement("action");
        role.appendChild ( actn );
        actn.appendChild ( docm.createTextNode(prefix+"/"+id+"/delete.act") );

        depn = docm.createElement("depend");
        role.appendChild ( depn );
        depn.appendChild ( docm.createTextNode(prefix+"/"+id+"/search") );

        // 恢复

        role = docm.createElement("role");
        menu.appendChild ( role );
        role.setAttribute("name", prefix+"/"+id+"/revert");
        role.setAttribute("text", "恢复"+name);

        actn = docm.createElement("action");
        role.appendChild ( actn );
        actn.appendChild ( docm.createTextNode(prefix+"/"+id+"/revert/search.act") );

        actn = docm.createElement("action");
        role.appendChild ( actn );
        actn.appendChild ( docm.createTextNode(prefix+"/"+id+"/revert/update.act") );

        depn = docm.createElement("depend");
        role.appendChild ( depn );
        depn.appendChild ( docm.createTextNode(prefix+"/"+id+"/search") );

        depn = docm.createElement("depend");
        role.appendChild ( depn );
        depn.appendChild ( docm.createTextNode(prefix+"/"+id+"/update") );

        // 保存
        saveDocument(Core.CONF_PATH+"/"+prefix+"/"+id+Cnst.NAVI_EXT+".xml", docm);
    }

    protected void updateFormConf(String id, List<Map> conf) throws HongsException {
        Document docm = makeDocument();

        Element  root = docm.createElement("root");
        docm.appendChild ( root );

        Element  form = docm.createElement("form");
        root.appendChild ( form );
        form.setAttribute("name" , id);

        Map types = FormSet.getInstance().getEnum("__types__");

        for (Map fiel: conf) {
            Element  item  = docm.createElement( "field" );
            form.appendChild ( item );
            String s, n, t;
            s = (String) fiel.get("__text__");
            item.setAttribute("text", s);
            n = (String) fiel.get("__name__");
            item.setAttribute("name", n);
            t = (String) fiel.get("__type__");
            item.setAttribute("type", t);
            s = (String) fiel.get("__rule__");
            item.setAttribute("rule", s);
            s = Synt.declare(fiel.get("__required__"), "");
            item.setAttribute("required", s);
            s = Synt.declare(fiel.get("__repeated__"), "");
            item.setAttribute("repeated", s);

            // 文件类型要指定上传路径
            if ("file".equals(types.get(t) )) {
                if(!fiel.containsKey("path")
                || !fiel.containsKey("href")) {
                    fiel.put("path", "static/upload/data");
                    fiel.put("href", "static/upload/data");
                }
            } else
            // 日期类型要指定存储格式
            if ("date".equals(types.get(t) )) {
                if(!fiel.containsKey("type")) {
                    fiel.put("type", "timestamp");
                }
            }
            // 可搜索指定存为搜索类型
            if (Synt.declare(fiel.get("findable"), false)) {
                if(!fiel.containsKey("lucnene-type")) {
                    fiel.put("lucene-type", "search");
                }
            }

            Map<String,Map<String,String>> preset = null;
            List<List<String>>             select = null;

            //** 构建字段参数 **/

            for(Object ot : fiel.entrySet( )) {
                Map.Entry et = (Map.Entry) ot;
                String k = (String) et.getKey(  );
                String v = (String) et.getValue();

                if (k== null || v== null) {
                    continue;
                }
                if (k.startsWith( "__" )) {
                    continue;
                }
                if (k.startsWith( ":"  )) {
                    if (preset == null )  {
                        preset =  new  LinkedHashMap();
                    }
                    String  l;
                    if (n.equals( "@"  )) {
                        int p=k.indexOf('.' );
                        if (p == -1) continue;
                        l = k.substring(1+ p);
                        k = k.substring(0, p);
                        k = id + k ;
                    } else {
                        l = k.substring(1   );
                        k = n;
                    }
                    Dict.put(preset, v, k, l);
                    continue;
                }
                if (k.equals("datalist")) {
                    select = Synt.asList(Data.toObject(v) );
                    continue;
                }

                Element  para = docm.createElement("param");
                item.appendChild ( para );
                para.setAttribute("name" , k);
                para.appendChild ( docm.createTextNode(v) );
            }

            //** 构建枚举列表 **/

            if (select != null) {
                Element  anum = docm.createElement("enum" );
                root.appendChild ( anum );
                anum.setAttribute("name" , n);

                for(List a : select) {
                    String c = Synt.declare( a.get(0), "" );
                    String l = Synt.declare( a.get(1), "" );

                    Element  valu = docm.createElement("value");
                    anum.appendChild ( valu );
                    valu.setAttribute("code" , c);
                    valu.appendChild ( docm.createTextNode(l) );
                }
            }

            //** 构建预置数据 **/

            if (preset != null) {
                for(Map.Entry<String,Map<String,String>> et0 : preset.entrySet()) {
                    n = et0.getKey();

                    Element  anum = docm.createElement("enum" );
                    root.appendChild ( anum );
                    anum.setAttribute("name" , n);

                    for(Map.Entry<String,String> et1 : et0.getValue().entrySet()) {
                        String c = et1.getKey(  );
                        String l = et1.getValue();

                        Element  valu = docm.createElement("value");
                        anum.appendChild ( valu );
                        valu.setAttribute("code" , c);
                        valu.appendChild ( docm.createTextNode(l) );
                    }
                }
            }
        }

        saveDocument(Core.CONF_PATH+"/"+prefix+"/"+id+Cnst.FORM_EXT+".xml", docm);
    }

    private Document makeDocument() throws HongsException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder        builder = factory.newDocumentBuilder();
            return  builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new HongsException.Common ( e);
        }
    }

    private void saveDocument(String path, Document docm) throws HongsException {
        File file = new File(path);
        File fold = file.getParentFile();
        if (!fold.exists()) {
             fold.mkdirs();
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            Transformer    tr = tf.newTransformer();
            DOMSource      ds = new DOMSource(docm);
            StreamResult   sr = new StreamResult (
                                new OutputStreamWriter(
                                new FileOutputStream(file), "utf-8"));

            tr.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            tr.setOutputProperty(OutputKeys.METHOD  , "xml"  );
            tr.setOutputProperty(OutputKeys.INDENT  , "yes"  );
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            tr.transform(ds, sr);
        } catch (TransformerConfigurationException e) {
            throw new HongsException.Common(e);
        } catch (IllegalArgumentException e) {
            throw new HongsException.Common(e);
        } catch (TransformerException  e) {
            throw new HongsException.Common(e);
        } catch (FileNotFoundException e) {
            throw new HongsException.Common(e);
        } catch (UnsupportedEncodingException e) {
            throw new HongsException.Common(e);
        }
    }

}
