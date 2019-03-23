package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.action.NaviMap;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.util.Data;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 表单模型
 * @author Hongs
 */
public class Form extends Model {

    protected String centra = "centra/data";
    protected String centre = "centre/data";
    protected String upload = "static/upload/data";
    protected static final  String SERI_DATA_PATH = Core.DATA_PATH + "/serial";

    public Form() throws HongsException {
        this(DB.getInstance("matrix").getTable("form"));
    }

    public Form(Table table) throws HongsException {
        super(table);
    }

    @Override
    public int put(String id, Map rd) throws HongsException {
        Map xd = table.fetchCase()
                .filter("id = ?", id)
                .select("state,name")
                .getOne();

        String stax = Synt.asString(xd.get("state"));
        String namx = Synt.asString(xd.get("name" ));
        String stat = Synt.asString(rd.get("state"));
        String name = Synt.asString(rd.get("name" ));
        List   conf = parseConf(rd);

        // 冻结意味着手动修改了配置
        // 再操作可能会冲掉自定配置
        if ("8".equals(stax)) {
            throw new HongsException(0x1100, "表单冻结, 禁止操作");
        }
        if ("0".equals(stax)) {
            throw new HongsException(0x1104, "表单缺失, 无法操作");
        }

        int n  = superPut(id, rd);
        if (n != 0) {
            // 用旧数据补全
            if (stat == null && name != null) {
                stat  = stax;
            }
            if (name == null && stat != null) {
                name  = namx;
            }

            // 备份配置数据
            if (conf != null) {
                storeConf(id, rd.get("conf"));
            }

            // 更新配置文件
            if (conf != null || stat != null) {
                updateFormConf(id, stat,conf);
            }
            if (name != null || stat != null) {
                updateFormMenu(id, stat,name);
            }

            // 更新单元菜单
            if (stat != null) {
                updateUnitMenu(id);
            }
        }
        return  n;
    }

    @Override
    public int add(String id, Map rd) throws HongsException {
        String stat = Synt.asString(rd.get("state"));
        String name = Synt.asString(rd.get("name" ));
        List   conf = parseConf(rd);

        int n  = superAdd(id, rd);
        if (n != 0) {
            // 记录配置变更
            storeConf(id, rd.get("conf") );

            // 更新配置文件
            updateFormConf(id, stat, conf);
            updateFormMenu(id, stat, name);

            // 更新单元菜单
            updateUnitMenu(id);

            // 添加表单权限
            insertAuthRole(id);
        }

        return  n;
    }

    @Override
    public int del(String id, FetchCase fc) throws HongsException {
        int n  = superDel(id, fc);
        if (n != 0) {
            // 记录配置变更
            storeConf(id,null);

            // 删除配置文件
            deleteFormConf(id);
            deleteFormMenu(id);

            // 更新单元菜单
            updateUnitMenu(id);

            // 删除表单权限
            deleteAuthRole(id);
        }
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

    protected int storeConf(String id, Object conf) throws HongsException {
        Object uid = ActionHelper.getInstance().getSessibute(Cnst.UID_SES);
        long   now = System.currentTimeMillis() / 1000;
        String tbl = db.getTable(  "data"  ).tableName;
        String sql ;

        if (conf != null) {
            // 配置未改变则不增加日志
            sql = "SELECT `data` FROM `" + tbl + "` WHERE `etime` = ? AND `form_id` = ? AND `id` = ?";
            Map row  = db.fetchOne(sql, "0", "-", id );
            if (row != null && !row.isEmpty()
            &&  conf.equals(row.get("data"))) {
                return 0;
            }

            sql = "UPDATE `"+tbl+"` SET `etime` = ? WHERE `etime` = ? AND `form_id` = ? AND `id` = ?";
            db.updates(sql, now, "0", "-", id);

            sql = "INSERT INTO `"+tbl+"` (`ctime`,`etime`,`form_id`,`id`,`user_id`,`data`,`state`) VALUES (?, ?, ?, ?, ?, ?, ?)";
            db.updates(sql, now, "0", "-", id, uid, conf, "1");
        } else {
            sql = "UPDATE `"+tbl+"` SET `etime` = ? WHERE `etime` = ? AND `form_id` = ? AND `id` = ?";
            db.updates(sql, now, "0", "-", id);

            sql = "INSERT INTO `"+tbl+"` (`ctime`,`etime`,`form_id`,`id`,`user_id`,`data`,`state`) VALUES (?, ?, ?, ?, ?, ?, ?)";
            db.updates(sql, now, "0", "-", id, uid, "{}", "0");
        }

        return 1;
    }

    protected List<Map> parseConf(Map rd) {
        List<Map> flds = null;
        String conf = (String) rd.get("conf");
        String name = (String) rd.get("name");

        if (conf != null && !"".equals(conf)) {
            flds = Synt.asList(Data.toObject(conf));
            Set set = Synt.setOf("name", "word", "cuser", "muser", "ctime", "mtime");
            Map tdf = null;
            Map idf = null;
            Map fld ;

            Iterator<Map> itr = flds.iterator();
            while (itr.hasNext()) {
                fld = itr.next();
                if (  "-".equals(fld.get("__name__"))) {
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

            conf = Data.toString(flds, true);
            rd.put("conf", conf);

            // 补全表配置项
            fld = Synt.mapOf(
                "__text__", name,
                "listable", "?" ,
                "sortable", "?" ,
                "srchable", "?" ,
                "findable", "?" ,
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
                    "__type__", "hidden",
                    "readonly", "true",
                    "wordable", "true"
                ));
            }

            // 增加用户字段
            if (set.contains("muser")) {
                flds.add(Synt.mapOf(
                    "__name__", "muser",
                    "__type__", "hidden",
                    "readonly", "true",
                    "default" , "=$uid",
                    "deforce" , "always"
                ));
            }
            if (set.contains("cuser")) {
                flds.add(Synt.mapOf(
                    "__name__", "cuser",
                    "__type__", "hidden",
                    "readonly", "true",
                    "default" , "=$uid",
                    "deforce" , "create"
                ));
            }

            // 增加时间字段
            if (set.contains("mtime")) {
                flds.add(Synt.mapOf(
                    "__name__", "mtime",
                    "__type__", "datetime",
                      "type"  , "timestamp",
                    "readonly", "true",
                    "default" , "=%now",
                    "deforce" , "always"
                ));
            }
            if (set.contains("ctime")) {
                flds.add(Synt.mapOf(
                    "__name__", "ctime",
                    "__type__", "datetime",
                      "type"  , "timestamp",
                    "readonly", "true",
                    "default" , "=%now",
                    "deforce" , "create"
                ));
            }
        } else {
            rd.remove("conf");
        }

        return flds;
    }

    @Override
    protected void filter(FetchCase caze, Map rd) throws HongsException {
        super.filter(caze, rd);

        // 超级管理员不做限制
        ActionHelper helper = Core.getInstance (ActionHelper.class);
        String uid = ( String ) helper.getSessibute( Cnst.UID_SES );
        if (Cnst.ADM_UID.equals(uid)) {
            return;
        }

        String  mm = caze.getOption("MODEL_START" , "");
        if ("getList".equals(mm)
        ||  "getInfo".equals(mm)) {
            mm = "/search";
        } else
        if ("update" .equals(mm)
        ||  "delete" .equals(mm)) {
            mm = "/" + mm ;
        } else {
            return; // 非常规动作不限制
        }

        // 从权限串中取表单ID
        NaviMap nm = NaviMap.getInstance(centra);
        String  pm = centra + "/";
        Set<String> ra = nm.getRoleSet( );
        Set<String> rs = new   HashSet( );
        for (String rn : ra) {
            if (rn.startsWith(pm)
            &&  rn.  endsWith(mm)) {
                rs.add(rn.substring(pm.length(), rn.length() - mm.length()));
            }
        }

        // 限制为有权限的表单
        caze.filter("`"+table.name+"`.`id` IN (?)", rs);
    }

    protected void insertAuthRole(String id) throws HongsException {
        ActionHelper helper = Core.getInstance(ActionHelper.class);
        String uid = (String) helper.getSessibute ( Cnst.UID_SES );
        String tan ;

        // 写入权限
        tan = (String) table.getParams().get("role.table");
        if (tan != null) {
            Table tab = db.getTable(tan);
            tab.insert(Synt.mapOf("user_id", uid,
                "role"   , centra + "/" + id + "/search"
            ));
            tab.insert(Synt.mapOf("user_id", uid,
                "role"   , centra + "/" + id + "/create"
            ));
            tab.insert(Synt.mapOf("user_id", uid,
                "role"   , centra + "/" + id + "/update"
            ));
            tab.insert(Synt.mapOf("user_id", uid,
                "role"   , centra + "/" + id + "/delete"
            ));
            tab.insert(Synt.mapOf("user_id", uid,
                "role"   , centra + "/" + id + "/revert"
            ));
        }

        // 更新缓存(通过改变权限更新时间)
        tan = (String) table.getParams().get("user.table");
        if (tan != null) {
            Table tab = db.getTable(tan);
            tab.update(Synt.mapOf(
                "rtime", System.currentTimeMillis() / 1000
            ) , "`id` = ?" , uid );
        }
    }

    protected void deleteAuthRole(String id) throws HongsException {
        ActionHelper helper = Core.getInstance(ActionHelper.class);
        String uid = (String) helper.getSessibute ( Cnst.UID_SES );
        String tan;

        // 删除权限
        tan = (String) table.getParams().get("role.table");
        if (tan != null) {
            Table tab = db.getTable(tan);
            tab.remove("`role` IN (?)", Synt.setOf(centra + "/" + id + "/search",
                centra + "/" + id + "/create",
                centra + "/" + id + "/update",
                centra + "/" + id + "/delete",
                centra + "/" + id + "/revert"
            ));
        }

        // 更新缓存(通过改变权限更新时间)
        tan = (String) table.getParams().get("user.table");
        if (tan != null) {
            Table tab = db.getTable(tan);
            tab.update(Synt.mapOf(
                "rtime", System.currentTimeMillis() / 1000
            ) , "`id` = ?" , uid );
        }
    }

    protected void deleteFormConf(String id) {
        File file;

        file = new File(Core.CONF_PATH +"/"+ centra +"/"+ id + Cnst.FORM_EXT +".xml");
        if (file.exists()) file.delete();

        file = new File(Core.CONF_PATH +"/"+ centre +"/"+ id + Cnst.FORM_EXT +".xml");
        if (file.exists()) file.delete();

        file = new File(SERI_DATA_PATH +"/"+ centra +"/"+ id + Cnst.FORM_EXT +".ser");
        if (file.exists()) file.delete();

        file = new File(SERI_DATA_PATH +"/"+ centre +"/"+ id + Cnst.FORM_EXT +".ser");
        if (file.exists()) file.delete();
    }

    protected void deleteFormMenu(String id) {
        File file;

        file = new File(Core.CONF_PATH +"/"+ centra +"/"+ id + Cnst.NAVI_EXT +".xml");
        if (file.exists()) file.delete();

        file = new File(Core.CONF_PATH +"/"+ centre +"/"+ id + Cnst.NAVI_EXT +".xml");
        if (file.exists()) file.delete();

        file = new File(SERI_DATA_PATH +"/"+ centra +"/"+ id + Cnst.NAVI_EXT +".ser");
        if (file.exists()) file.delete();

        file = new File(SERI_DATA_PATH +"/"+ centre +"/"+ id + Cnst.NAVI_EXT +".ser");
        if (file.exists()) file.delete();
    }

    protected void updateFormConf(String id, String stat, List<Map> conf) throws HongsException {
        File     file;
        Document docm;
        Element  root, form, item, defs, defi;

        file = new File(Core.CONF_PATH +"/"+ centra +"/"+ id + Cnst.FORM_EXT +".xml");
        docm = makeDocument();

        root = docm.createElement("root");
        docm.appendChild ( root );

        form = docm.createElement("form");
        root.appendChild ( form );
        form.setAttribute("name" ,  id  );

        Map types = FormSet.getInstance().getEnum("__types__");

        for (Map fiel: conf) {
            item = docm.createElement("field");
            form.appendChild ( item );

            String s, n, t;
            s = (String) fiel.get("__text__");
            if (s != null) item.setAttribute("text", s);
            n = (String) fiel.get("__name__");
            if (n != null) item.setAttribute("name", n);
            t = (String) fiel.get("__type__");
            if (t != null) item.setAttribute("type", t);
            s = (String) fiel.get("__rule__");
            if (s != null && s.length() != 0) item.setAttribute("rule", s);
            s = (String) fiel.get("__required__");
            if (s != null && s.length() != 0) item.setAttribute("required", s);
            s = (String) fiel.get("__repeated__");
            if (s != null && s.length() != 0) item.setAttribute("repeated", s);

            //** 复查字段属性 **/

            // 日期类型要指定存储格式
            if ("date".equals(types.get(t) )) {
                if(!fiel.containsKey("type")) {
                    fiel.put("type", "timestamp");
                }
            } else
            // 文件类型要指定上传路径
            if ("file".equals(types.get(t) )) {
                if(!fiel.containsKey("href")) {
                    fiel.put("href", upload +"/"+ id);
                }
                if(!fiel.containsKey("path")) {
                    fiel.put("path", upload +"/"+ id);
                }
            } else
            // 选项表单要指定配置路径
            if ("enum".equals(types.get(t) )
            ||  "form".equals(types.get(t) )) {
                if(!fiel.containsKey("conf")) {
                    fiel.put("conf", centra +"/"+ id);
                }
            } else
            // 可搜索指定存为搜索类型
            if ("search".equals(t)
            ||  Synt.declare(fiel.get("srchable"), false)) {
                if(!fiel.containsKey("lucnene-type")) {
                    fiel.put("lucene-type", "search");
                }
            } else
            // 文本禁搜索则为存储类型
            if ("stored".equals(t)
            ||"textarea".equals(t) ||"textview".equals(t)) {
                if(!fiel.containsKey("lucnene-type")) {
                    fiel.put("lucene-type", "stored");
                }
            }

            //** 构建字段参数 **/

            Map<String,Map<String,String>> preset = null;
            List<List<String>>             select = null;

            for(Object ot : fiel.entrySet( )) {
                Map.Entry et = (Map.Entry) ot;
                String k = (String) et.getKey(  );
                String v = (String) et.getValue();

                if (k==null || v==null) {
                    continue;
                }
                if (k.startsWith("__")) {
                    continue;
                }
                if (k.startsWith( "." )   // 外部预置数据
                ||  k.startsWith( ":")) { // 内部预置数据
                    if (preset == null) {
                        preset =  new  LinkedHashMap();
                    }
                    String  l;
                    if (n.equals( "@")) { // 表单预置数据
                        int p = k.indexOf('.', 1);
                        if (p == -1) continue;
                        l = k.substring(1+ p);
                        k = k.substring(0, p);
                        k = id + k ;
                    } else {              // 一般枚举选项
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

        saveDocument(file , docm);

        //** 对外开放 **/

        file = new File(Core.CONF_PATH +"/"+ centre +"/"+ id + Cnst.FORM_EXT +".xml");
        docm = readDocument(file);

        root = docm.getDocumentElement();
        if (root == null) {
            root = docm.createElement("root");
            docm.appendChild ( root );
        }

        form = getNodeByTagNameAndAttr(root, "form", "name", id);
        if (form == null) {
        form = getNodeByTagNameAndAttr(root, "xxxx", "name", id);
        }
        if (form != null) {
            /**
             * 1 为内部资源, 改名使其失效
             */
            if ("1".equals(stat)) {
                docm.renameNode(form, null, "xxxx");
            } else {
                docm.renameNode(form, null, "form");
            }
        } else {
            form = docm.createElement("form");
            root.appendChild ( form );
            form.setAttribute("name" ,  id  );

            /**
             * 1 为内部资源, 改名使其失效
             */
            if ("1".equals(stat)) {
                docm.renameNode(form, null, "xxxx");
            } else {
            //  docm.renameNode(form, null, "form");
            }

            // 全局性保护
            defs = docm.createElement("enum");
            root.appendChild ( defs );
            defs.setAttribute("name", id+":defense");
            defi = docm.createElement("value");
            defs.appendChild ( defi );
            defi.setAttribute("code", Cnst.AR_KEY);
            defi.appendChild ( docm.createTextNode("(null)"));
            defi = docm.createElement("value");
            defs.appendChild ( defi );
            defi.setAttribute("code", Cnst.OR_KEY);
            defi.appendChild ( docm.createTextNode("(null)"));

            // 保护写接口
            defs = docm.createElement("enum");
            root.appendChild ( defs );
            defs.setAttribute("name", id+":defence");
            defi = docm.createElement("value");
            defs.appendChild ( defi );
            defi.setAttribute("code", Cnst.AR_KEY+".x.cuser");
            defi.appendChild ( docm.createTextNode("($session.uid)"));

            // 我所创建的
            defs = docm.createElement("enum");
            root.appendChild ( defs );
            defs.setAttribute("name", id+".created");
            defi = docm.createElement("value");
            defs.appendChild ( defi );
            defi.setAttribute("code", Cnst.AR_KEY+".x.cuser");
            defi.appendChild ( docm.createTextNode("($session.uid)"));
        }

        saveDocument(file , docm);
    }

    protected void updateFormMenu(String id, String stat, String name) throws HongsException {
        File     file;
        String   href;
        Document docm;
        Element  root, menu, role, actn, depn;

        file = new File(Core.CONF_PATH +"/"+ centra +"/"+ id + Cnst.NAVI_EXT +".xml");
        href = centra+"/"+id+"/" ;
        docm = readDocument(file);

        root = docm.getDocumentElement();
        if (root == null) {
            root = docm.createElement("root");
            docm.appendChild ( root );
        }

        menu = getNodeByTagNameAndAttr(root, "menu", "href", href);
        if (menu != null) {
            menu.setAttribute("text" , name );
        } else {
            menu = docm.createElement("menu");
            root.appendChild ( menu );
            menu.setAttribute("text" , name );
            menu.setAttribute("href" , href );

            // 会话
            role = docm.createElement("rsname");
            root.appendChild ( role );
            role.appendChild ( docm.createTextNode("@centra"));

            // 查看
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", href +"search");
            role.setAttribute("text", "查看");
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"search"+ Cnst.ACT_EXT) );
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"statis/search"+ Cnst.ACT_EXT) );
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"statis/amount"+ Cnst.ACT_EXT) );
//          actn = docm.createElement("action");
//          role.appendChild ( actn );
//          actn.appendChild ( docm.createTextNode(href +"stream/search"+ Cnst.ACT_EXT) );
            depn = docm.createElement("depend");
            role.appendChild ( depn );
            depn.appendChild ( docm.createTextNode("centra") );

            // 添加
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", href +"create");
            role.setAttribute("text", "添加");
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"create"+ Cnst.ACT_EXT) );
            depn = docm.createElement("depend");
            role.appendChild ( depn );
            depn.appendChild ( docm.createTextNode(href +"search") );

            // 修改
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", href +"update");
            role.setAttribute("text", "修改");
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"update"+ Cnst.ACT_EXT) );
            depn = docm.createElement("depend");
            role.appendChild ( depn );
            depn.appendChild ( docm.createTextNode(href +"search") );

            // 删除
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", href +"delete");
            role.setAttribute("text", "删除");
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"delete"+ Cnst.ACT_EXT) );
            depn = docm.createElement("depend");
            role.appendChild ( depn );
            depn.appendChild ( docm.createTextNode(href +"search") );

            // 回看
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", href +"review");
            role.setAttribute("text", "回看");
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"revert/search"+ Cnst.ACT_EXT) );
            depn = docm.createElement("depend");
            role.appendChild ( depn );
            depn.appendChild ( docm.createTextNode(href +"search") );

            // 恢复
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", href +"revert");
            role.setAttribute("text", "恢复");
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"revert/update"+ Cnst.ACT_EXT) );
            depn = docm.createElement("depend");
            role.appendChild ( depn );
            depn.appendChild ( docm.createTextNode(href +"review") );
        }

        saveDocument(file , docm);

        //** 对外开放 **/

        file = new File(Core.CONF_PATH +"/"+ centre +"/"+ id + Cnst.NAVI_EXT +".xml");
        href = centre+"/"+id+"/" ;
        docm = readDocument(file);

        root = docm.getDocumentElement();
        if (root == null) {
            root = docm.createElement("root");
            docm.appendChild ( root );
        }

        menu = getNodeByTagNameAndAttr(root, "menu", "href", href);
        if (menu == null) {
        menu = getNodeByTagNameAndAttr(root, "xxxx", "href", href);
        }
        if (menu != null) {
            menu.setAttribute("text" , name );

            /**
             * 1 为内部资源, 改名使其失效
             * 4 仅提供接口, 写入特殊属性
             */
            if ("1".equals(stat)) {
                docm.renameNode(menu, null, "xxxx");
            } else {
                docm.renameNode(menu, null, "menu");
            }
            if ("4".equals(stat)) {
                menu.setAttribute( "hrel" , "HIDE");
            } else {
                menu.setAttribute( "hrel" ,   ""  );
            }
        } else {
            menu = docm.createElement("menu");
            root.appendChild ( menu );
            menu.setAttribute("text" , name );
            menu.setAttribute("href" , href );

            /**
             * 1 为内部资源, 改名使其失效
             * 4 仅提供接口, 写入特殊属性
             */
            if ("1".equals(stat)) {
                docm.renameNode(menu, null, "xxxx");
            } else {
            //  docm.renameNode(menu, null, "menu");
            }
            if ("4".equals(stat)) {
                menu.setAttribute( "hrel" , "HIDE");
            } else {
                menu.setAttribute( "hrel" ,   ""  );
            }

            // 会话
            role = docm.createElement("rsname");
            root.appendChild ( role );
            role.appendChild ( docm.createTextNode("@centre"));

            // 公共读取权限
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", "public");

            // 增删改须登录
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", "centre");
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"create"+ Cnst.ACT_EXT) );
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"update"+ Cnst.ACT_EXT) );
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"delete"+ Cnst.ACT_EXT) );
        }

        saveDocument(file , docm);
    }

    protected void updateUnitMenu(String id) throws HongsException {
        new Unit().updateMenus( );
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

    private Document readDocument(File file) throws HongsException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder        builder = factory.newDocumentBuilder();
            try {
                return  builder.parse(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                return  builder.newDocument();
            }
        } catch (ParserConfigurationException e) {
            throw new HongsException.Common ( e);
        } catch (SAXException e) {
            throw new HongsException.Common ( e);
        } catch ( IOException e) {
            throw new HongsException.Common ( e);
        }
    }

    private void saveDocument(File file, Document docm) throws HongsException {
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

    private Element getNodeByTagNameAndAttr(Element elem, String tag, String att, String val) {
        NodeList a = elem.getChildNodes();
        for (int i = 0; i < a.getLength(); i ++ ) {
            Node n = a.item(i);
            if ( n.getNodeType() != Node.ELEMENT_NODE ) {
                continue;
            }
            Element e = (Element) n;
            if (!tag.equals(e.getTagName(/***/))) {
                continue;
            }
            if (!val.equals(e.getAttribute(att))) {
                continue;
            }
            return e;
        }
        return  null;
    }

}
