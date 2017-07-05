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
import app.hongs.util.Synt;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
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
             Map  top  = null;

            flds = Synt.declare(Data.toObject(conf), List.class);
            for (Map fld : flds) {
                if ( "".equals(fld.get("__name__"))) {
                    fld.put("__name__", Core.newIdentity());
                } else
                if ("@".equals(fld.get("__name__"))) {
                    top = fld;
                }
            }

            // 增加表配置项
            if (top == null) {
                top  = new HashMap();
            }
            flds.add(0, top);
            top.put("__name__", "@" );
            top.put("__text__", name);

            // 增加编号字段
            top = new HashMap( );
            flds.add(1, top);
            top.put("__name__", "id");
            top.put("__text__", "ID");
            top.put("__type__", "hidden");
            top.put("listable", "yes");
            top.put("sortable", "yes");
            top.put("findable", "yes");
            top.put("filtable", "yes");

            conf = Data.toString ( flds );
            rd.put("conf", conf);
        } else {
            rd.remove ( "conf" );
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
        menu.setAttribute("href", prefix+"/#"+id);
        menu.setAttribute("hrel", prefix+"/" +id+"/main.html");

        Element  role, actn, depn;

        // 会话
        role = docm.createElement("rsname");
        root.appendChild ( role );
        role.appendChild ( docm.createTextNode("@manage") );

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

        Element  item, anum, para;

        Map types = FormSet.getInstance().getEnum("__types__");

        for (Map fiel: conf) {
            item = docm.createElement("field");
            form.appendChild ( item );
            String s, t;
            s = (String) fiel.get("__text__");
            item.setAttribute("text", s);
            s = (String) fiel.get("__name__");
            item.setAttribute("name", s);
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
                    fiel.put("path", "public/upload/data");
                    fiel.put("href", "public/upload/data");
                }
            } else
            // 日期类型要指定存储格式
            if ("date".equals(types.get(t) )) {
                if(!fiel.containsKey("type")) {
                    fiel.put("type", "timestamp");
                }
            }

            for(Object ot : fiel.entrySet( )) {
                Map.Entry et = (Map.Entry) ot;
                String k = (String) et.getKey(  );
                String v = (String) et.getValue();

                // 忽略基础参数
                if (k.startsWith( "__" )) {
                    continue;
                }

                // 构建枚举列表
                if (k.equals("datalist")) {
                    anum = docm.createElement("enum");
                    root.appendChild ( anum );
                    anum.setAttribute("name", item.getAttribute("name"));
                    Object x = Data.toObject( v );
                    List de = new ArrayList(/**/);
                    List dl = Synt.declare(x, de);
                    for (Object o : dl) {
                        List di = Synt.declare(o, de);
                        Element valu = docm.createElement("value");
                        anum.appendChild ( valu );
                        valu.setAttribute("code", Synt.declare(di.get(0), ""));
                        valu.appendChild ( docm.createTextNode(Synt.declare(di.get(1), "")) );
                    }
                    continue;
                }

                // 识别搜索类型
                if (k.equals("findable") && Synt.declare(v, false)) {
                    para = docm.createElement("param");
                    item.appendChild ( para );
                    para.setAttribute("name" , "lucene-fieldtype");
                    para.appendChild ( docm.createTextNode("search") );
                }

                para = docm.createElement("param");
                item.appendChild ( para );
                para.setAttribute("name", k);
                para.appendChild ( docm.createTextNode(v) );
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
            Transformer tr = tf.newTransformer();
            DOMSource   ds = new DOMSource(docm);
            tr.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            tr.setOutputProperty(OutputKeys.METHOD  , "xml"  );
            tr.setOutputProperty(OutputKeys.INDENT  , "yes"  );
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            PrintWriter  pw = new PrintWriter (new FileOutputStream(file));
            StreamResult sr = new StreamResult( pw  );
            tr.transform(ds, sr);
        } catch (TransformerConfigurationException e) {
            throw new HongsException.Common(e);
        } catch (IllegalArgumentException e) {
            throw new HongsException.Common(e);
        } catch (TransformerException  e) {
            throw new HongsException.Common(e);
        } catch (FileNotFoundException e) {
            throw new HongsException.Common(e);
        }
    }

}
