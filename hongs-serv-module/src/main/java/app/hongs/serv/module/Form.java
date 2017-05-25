package app.hongs.serv.module;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.db.DB;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.db.util.FetchCase;
import app.hongs.util.Data;
import app.hongs.util.Synt;

import java.util.Map;
import java.util.List;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

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

    public Form() throws HongsException {
        this(DB.getInstance("module").getTable("form"));
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
    public String set(Map rd) throws HongsException {
        // 给字段加名字
        String conf = (String) rd.get("conf");
        String name = (String) rd.get("name");
        List<Map> flds = null;
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
            top.put("__disp__", name);

            // 增加编号字段
            top = new HashMap();
            flds.add(1, top);
            top.put("__name__", "id");
            top.put("__disp__", "ID");
            top.put("__type__", "hidden");
            top.put("listable","yes");
/*
            // 增加创建用户
            top = new HashMap();
            flds.add(2, top);
            top.put("__name__",  "cuid" );
            top.put("__type__", "string");
            top.put("default" , "=$"+Cnst.UID_SES);
            top.put("default-create","yes");
            top.put("default-always","yes");
            top.put("editable", "no");
            top.put("listable", "no");

            // 增加修改用户
            top = new HashMap();
            flds.add(3, top);
            top.put("__name__",  "muid" );
            top.put("__type__", "string");
            top.put("default" , "=$"+Cnst.UID_SES);
            top.put("default-create", "no");
            top.put("default-always","yes");
            top.put("editable", "no");
            top.put("listable", "no");

            // 增加创建时间
            top = new HashMap();
            flds.add(top);
            top.put("__name__", "ctime" );
            top.put("__type__", "datetime");
            top.put(  "type"  ,"timestamp");
            top.put("default" , "=%now" );
            top.put("default-create","yes");
            top.put("default-always","yes");
            top.put("editable", "no");
            top.put("listable","yes");

            // 增加修改时间
            top = new HashMap();
            flds.add(top);
            top.put("__name__", "mtime" );
            top.put("__type__", "datetime");
            top.put(  "type"  ,"timestamp");
            top.put("default" , "=%now" );
            top.put("default-create", "no");
            top.put("default-always","yes");
            top.put("editable", "no");
            top.put("listable","yes");
*/
            conf = Data.toString(flds);
            rd.put("conf", conf);
        }

        String  id = (String) rd.get(this.table.primaryKey);
        boolean ic;
        if (id == null || id.length() == 0) {
            id = this.add(rd);
            ic = true ;
        } else {
            this.put(id , rd);
            ic = false;
        }

        // 建立表单配置
        if (flds != null) {
            updateOrCreateFormSet( id, flds );
        }

        // 建立菜单配置
        if (name != null && !"".equals(name)) {
            updateOrCreateMenuSet( id, name );
        }

        // 更新频道菜单
        if (ic) {
            String unitId = (String) rd.get( "unit_id" );
            String unitNm = (String) db.getTable("unit")
                    .filter ("id = ?", unitId)
                    .select ("name")
                    .one    ()
                    .get    ("name");
            Unit   un  =  new Unit();
            un.updateOrCreateMenuSet(unitId, unitNm);
            un.updateOrCreateMenuSet(              );
        }

        return id;
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

        // 更新单元菜单
        Unit   un  =  new Unit();
        un.updateOrCreateMenuSet(unitId, unitNm);
        un.updateOrCreateMenuSet(              );

        // 删除配置文件
        File   fo;
        fo = new File(Core.CONF_PATH +"manage/data/"+ id + Cnst.FORM_EXT +".xml");
        if (fo.exists()) {
            fo.delete();
        }
        fo = new File(Core.CONF_PATH +"manage/data/"+ id + Cnst.NAVI_EXT +".xml");
        if (fo.exists()) {
            fo.delete();
        }

        return n;
    }

    protected void updateOrCreateMenuSet(String id, String name) throws HongsException {
        Document docm = makeDocument();

        Element  root = docm.createElement("root");
        docm.appendChild ( root );

        Element  menu = docm.createElement("menu");
        root.appendChild ( menu );
        menu.setAttribute("disp", name);
        menu.setAttribute("href", "manage/data/#"+id);
        menu.setAttribute("hrel", "manage/data/" +id+"/main.html");

        Element  role, actn, depn;

        // 会话
        role = docm.createElement("rsname");
        root.appendChild ( role );
        role.appendChild ( docm.createTextNode("@manage") );

        // 查看

        role = docm.createElement("role");
        menu.appendChild ( role );
        role.setAttribute("name", "manage/data/" +id+"/retrieve");
        role.setAttribute("disp", "查看"+name);

        actn = docm.createElement("action");
        role.appendChild ( actn );
        actn.appendChild ( docm.createTextNode("manage/data/"+id+"/retrieve.act") );

        // 添加

        role = docm.createElement("role");
        menu.appendChild ( role );
        role.setAttribute("name", "manage/data/" +id+"/create");
        role.setAttribute("disp", "添加"+name);

        actn = docm.createElement("action");
        role.appendChild ( actn );
        actn.appendChild ( docm.createTextNode("manage/data/"+id+"/create.act") );

        depn = docm.createElement("depend");
        role.appendChild ( depn );
        depn.appendChild ( docm.createTextNode("manage/data/"+id+"/retrieve"  ) );

        // 修改

        role = docm.createElement("role");
        menu.appendChild ( role );
        role.setAttribute("name", "manage/data/" +id+"/update");
        role.setAttribute("disp", "修改"+name);

        actn = docm.createElement("action");
        role.appendChild ( actn );
        actn.appendChild ( docm.createTextNode("manage/data/"+id+"/update.act") );

        depn = docm.createElement("depend");
        role.appendChild ( depn );
        depn.appendChild ( docm.createTextNode("manage/data/"+id+"/retrieve"  ) );

        // 删除

        role = docm.createElement("role");
        menu.appendChild ( role );
        role.setAttribute("name", "manage/data/" +id+"/delete");
        role.setAttribute("disp", "删除"+name);

        actn = docm.createElement("action");
        role.appendChild ( actn );
        actn.appendChild ( docm.createTextNode("manage/data/"+id+"/delete.act") );

        depn = docm.createElement("depend");
        role.appendChild ( depn );
        depn.appendChild ( docm.createTextNode("manage/data/"+id+"/retrieve"  ) );

        // 保存
        saveDocument(Core.CONF_PATH+"/manage/data/"+id+Cnst.NAVI_EXT+".xml", docm);
    }

    protected void updateOrCreateFormSet(String id, List<Map> conf) throws HongsException {
        Document docm = makeDocument();

        Element  root = docm.createElement("root");
        docm.appendChild ( root );

        Element  form = docm.createElement("form");
        root.appendChild ( form );
        form.setAttribute("name", id);

        Element  item, anum, para;

        for (  Map  fiel : conf ) {
            item = docm.createElement("field");
            form.appendChild ( item );
            String s;
            s = (String) fiel.get("__disp__");
            item.setAttribute("disp", s);
            s = (String) fiel.get("__name__");
            item.setAttribute("name", s);
            s = (String) fiel.get("__type__");
            item.setAttribute("type", s);
            s = Synt.declare(fiel.get("__required__"), "");
            item.setAttribute("required", s);
            s = Synt.declare(fiel.get("__repeated__"), "");
            item.setAttribute("repeated", s);

            for (Object   ot : fiel.entrySet( )) {
                Map.Entry et = (Map.Entry) ot;
                String k = (String) et.getKey(  );
                String v = (String) et.getValue();

                // 忽略基础参数
                if (k.startsWith("__")) {
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
                    para.setAttribute("name", "lucene-fieldtype");
                    para.appendChild ( docm.createTextNode("search") );
                }

                para = docm.createElement("param");
                item.appendChild ( para );
                para.setAttribute("name", k);
                para.appendChild ( docm.createTextNode(v) );
            }
        }

        saveDocument(Core.CONF_PATH+"/manage/data/"+id+Cnst.FORM_EXT+".xml", docm);
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
