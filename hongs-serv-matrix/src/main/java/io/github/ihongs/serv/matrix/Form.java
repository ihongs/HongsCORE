package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.action.NaviMap;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Dist;
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

    public Form() throws CruxException {
        this(DB.getInstance("matrix").getTable("form"));
    }

    public Form(Table table) throws CruxException {
        super(table);
    }

    @Override
    public int put(String id, Map rd) throws CruxException {
        Map xd = table.fetchCase()
                .filter("id = ?", id)
                .select("state,name")
                .getOne();

        String stax = Synt.asString(xd.get("state"));
        String namx = Synt.asString(xd.get("name" ));
        String stat = Synt.asString(rd.get("state"));
        String name = Synt.asString(rd.get("name" ));
        List   conf = parseConf(id, rd);

        // 冻结意味着手动修改了配置
        // 再操作可能会冲掉自定配置
        if ("8".equals(stax)) {
            throw new CruxException(400, "表单冻结, 禁止操作");
        }
        if ("0".equals(stax)) {
            throw new CruxException(404, "表单缺失, 无法操作");
        }

        int n  = superPut (id , rd);
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
            if (name != null || stat != null) {
                updateFormMenu(id, stat,name);
            }
            if (conf != null || stat != null) {
                updateFormConf(id, stat,conf);
            }

            // 更新单元菜单
            if (stat != null) {
                updateUnitMenu(id);
            }
        }
        return  n;
    }

    @Override
    public int add(String id, Map rd) throws CruxException {
        String stat = Synt.asString(rd.get("state"));
        String name = Synt.asString(rd.get("name" ));
        List   conf = parseConf(id, rd);

        int n  = superAdd (id , rd);
        if (n != 0) {
            // 记录配置变更
            storeConf(id , rd.get("conf"));

            // 更新配置文件
            updateFormMenu(id, stat, name);
            updateFormConf(id, stat, conf);

            // 更新单元菜单
            updateUnitMenu(id);

            // 添加表单权限
            insertAuthRole(id);
        }

        return  n;
    }

    @Override
    public int del(String id, FetchCase fc) throws CruxException {
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

    protected final int superPut(String id, Map rd) throws CruxException {
        return super.put(id, rd);
    }

    protected final int superAdd(String id, Map rd) throws CruxException {
        return super.add(id, rd);
    }

    protected final int superDel(String id, FetchCase fc) throws CruxException {
        return super.del(id, fc);
    }

    /**
     * 保存配置
     * @param id
     * @param conf
     * @return 0 无变化, 1 有变化, 2 字段类型有变, 4 数量类型有变
     * @throws CruxException
     */
    protected int storeConf(String id, Object conf) throws CruxException {
        Object uid = ActionHelper.getInstance().getSessibute(Cnst.UID_SES);
        long   now = System.currentTimeMillis() / 1000L ;
        String tbl = DB.Q(db.getTable("data").tableName);
        String dat = DB.Q("data"); // DATA 是 MySQL 关键词
        String sql ;

        if (conf != null) {
            // 配置未改变则不增加日志
            sql = "SELECT "+dat+" FROM "+tbl+ " WHERE etime = ? AND form_id = ? AND id = ?";
            Map row  = db.fetchOne(sql, "0", "-", id);
            if (row != null && !row.isEmpty()
            &&  conf.equals(row.get("data"))) {
                return 0;
            }

            sql = "UPDATE "+tbl+" SET etime = ? WHERE etime = ? AND form_id = ? AND id = ?";
            db.updates(sql, now, "0", "-", id);

            sql = "INSERT INTO "+tbl+" (ctime, etime, form_id, id, user_id, "+dat+", state) VALUES (?, ?, ?, ?, ?, ?, ?)";
            db.updates(sql, now, "0", "-", id, uid, conf, "1");

            /**
             * 探查是否有变更字段类型
             * 变更影响存储需重建索引
             */

            /*
            int i = 1;
            Object data = row != row.get("data");
            Map<String, Map> confDict = new HashMap();
            List<Map> confData = Synt.asList(Dist.toObject((String) data));
            List<Map> confList = Synt.asList(Dist.toObject((String) conf));

            for(Map fo : confData) {
                String fn = (String)  fo.get("__name__");
                confDict.put(fn, fo);
            }
            for(Map fc : confList) {
                String fn = (String)  fc.get("__name__");
                Map fo = confDict.get(fn);
                if (fo == null) continue ;

                // 字段类型有变
                String ft0 = getDataType(fo);
                String ft1 = getDataType(fc);
                if ( ! ft0.equals(ft1)) {
                    if (2 != (2 & i)) i += 2;
                }

                // 数量类型有变
                if (Synt.declare (fo.get("__repeated__"), false)
                !=  Synt.declare (fc.get("__repeated__"), false) ) {
                    if (4 != (4 & i)) i += 4;
                }
            }
            return  i;
            */
        } else {
            sql = "UPDATE "+tbl+" SET etime = ? WHERE etime = ? AND form_id = ? AND id = ?";
            db.updates(sql, now, "0", "-", id);

            sql = "INSERT INTO "+tbl+" (ctime, etime, form_id, id, user_id, "+dat+", state) VALUES (?, ?, ?, ?, ?, ?, ?)";
            db.updates(sql, now, "0", "-", id, uid, "[]", "0");
        }

        return 1;
    }

    protected List<Map> parseConf(String id, Map rd) throws CruxException {
        List<Map> flds = null;
        String conf = (String) rd.get("conf");
        String name = (String) rd.get("name");

        if (conf != null && !"".equals(conf)) {
               flds = Synt.asList(Dist.toObject(conf));
            Set fns = new HashSet(flds.size( ));
            Map tdf = null;
            Map idf = null;
            Map fld ;

            Iterator<Map> ite = flds.iterator();
            while (ite.hasNext()) {
                fld = ite.next();
                Object fn = fld.get("__name__");
                if (!"-".equals(fn )
                &&  !"@".equals(fn)) {
                    fns.add (fn);
                }
            }

            Iterator<Map> itr = flds.iterator();
            while (itr.hasNext()) {
                fld = itr.next();
                Object fn = fld.get("__name__");
                if ( "-".equals(fn)) {
                    fn = getFieldName(fld, fns);
                    fld.put ( "__name__" , fn );
                    fns.add (fn);
                } else
                if ( "@".equals(fn)) {
                    tdf = fld;
                    itr.remove();
                } else
                if ("id".equals(fn)) {
                    idf = fld;
                    itr.remove();
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

            conf = Dist.toString(flds, true);
            rd.put("conf", conf);

            // 补全表配置项
            fld = Synt.mapOf(
                "__text__", name,
                "form_id" , id  ,
                "part_id" , id  ,
                "db-name" , "matrix/base" ,
                "db-path" , "matrix/base"
                /*
                // 已改完默认按类型判断
                // 故没有必要再作设置了
                "listable", "?" ,
                "sortable", "?" ,
                "srchable", "?" ,
                "findable", "?" ,
                "nameable", "?"
                */
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
                "__type__", "hidden",
                "deforce" , "create",
                "default" , "@id"
            );
            if (idf != null) {
                fld .putAll(idf);
                idf .putAll(fld);
            } else {
                flds.add(1, fld);
            }

            /*
            // 增加描述字段
            // 2023/09/08 不再默认设置, 避免冲掉其他 nameable,wordable 字段
            if (!fns.contains("name")) {
                flds.add(Synt.mapOf(
                    "__name__", "name",
                    "__type__", "hidden",
                    "disabled", "true",
                    "wordable", "true"
                ));
            }
            if (!fns.contains("word")) {
                flds.add(Synt.mapOf(
                    "__name__", "word",
                    "__type__", "search",
                    "disabled", "true",
                    "srchable", "true"
                ));
            }
            */

            // 增加用户字段
            if (!fns.contains("muser")) {
                flds.add(Synt.mapOf(
                    "__name__", "muser",
                    "__type__", "hidden",
                    "disabled", "true",
                    "default" , "@uid",
                    "deforce" , "always"
                ));
            }
            if (!fns.contains("cuser")) {
                flds.add(Synt.mapOf(
                    "__name__", "cuser",
                    "__type__", "hidden",
                    "disabled", "true",
                    "default" , "@uid",
                    "deforce" , "create"
                ));
            }

            // 增加时间字段
            if (!fns.contains("mtime")) {
                flds.add(Synt.mapOf(
                    "__name__", "mtime",
                    "__type__", "datetime",
                      "type"  , "timestamp",
                    "disabled", "true",
                    "default" , "@now",
                    "deforce" , "always"
                ));
            }
            if (!fns.contains("ctime")) {
                flds.add(Synt.mapOf(
                    "__name__", "ctime",
                    "__type__", "datetime",
                      "type"  , "timestamp",
                    "disabled", "true",
                    "default" , "@now",
                    "deforce" , "create"
                ));
            }
        } else {
            rd.remove("conf");
        }

        return flds;
    }

    @Override
    protected void filter(FetchCase caze, Map rd) throws CruxException {
        super.filter(caze, rd);

        // 超级管理员不做限制
        ActionHelper helper = Core.getInstance (ActionHelper.class);
        String uid = ( String ) helper.getSessibute( Cnst.UID_SES );
        if (Cnst.ADM_UID.equals(uid)) {
            return;
        }

        // 非常规动作不作限制
        String  mm = caze.getOption("MODEL_START" , "");
        if (!"search".equals(mm)
        &&  !"recite".equals(mm)
        &&  !"update".equals(mm)
        &&  !"delete".equals(mm)) {
            return;
        }
        mm = "/" + mm;

        // 从权限串中取表单ID
        NaviMap nm = NaviMap.getInstance(centra);
        String  pm = centra + "/";
        Set<String> ra = nm.getUserRoles( );
        Set<String> rs = new   HashSet  ( );
        for (String rn : ra) {
            if (rn.startsWith(pm)
            &&  rn.  endsWith(mm)) {
                rs.add(rn.substring(pm.length(), rn.length() - mm.length()));
            }
        }

        // 限制为有权限的表单
        caze.filter(DB.Q(table.name)+".id IN (?)", rs);
    }

    protected void insertAuthRole(String id) throws CruxException {
        ActionHelper helper = Core.getInstance(ActionHelper.class);
        String uid = (String) helper.getSessibute ( Cnst.UID_SES );
        String tan ;

        // 写入权限
        tan = (String) table.getParams().get("role.table");
        if (tan != null) {
            Table tab = db.getTable(tan);
            tab.insert(Synt.mapOf("user_id", uid,
                "role", centra + "/" + id + "/search"
            ));
            tab.insert(Synt.mapOf("user_id", uid,
                "role", centra + "/" + id + "/create"
            ));
            tab.insert(Synt.mapOf("user_id", uid,
                "role", centra + "/" + id + "/update"
            ));
            tab.insert(Synt.mapOf("user_id", uid,
                "role", centra + "/" + id + "/delete"
            ));
            tab.insert(Synt.mapOf("user_id", uid,
                "role", centra + "/" + id + "/reveal"
            ));
            tab.insert(Synt.mapOf("user_id", uid,
                "role", centra + "/" + id + "/revert"
            ));
        }

        // 更新缓存(通过改变权限更新时间)
        tan = (String) table.getParams().get("user.table");
        if (tan != null) {
            Table tab = db.getTable(tan);
            tab.update(Synt.mapOf(
                "rtime", System.currentTimeMillis() / 1000
            ) , "id = ?" , uid);
        }
    }

    protected void deleteAuthRole(String id) throws CruxException {
        ActionHelper helper = Core.getInstance(ActionHelper.class);
        String uid = (String) helper.getSessibute ( Cnst.UID_SES );
        String tan;

        // 删除权限
        tan = (String) table.getParams().get("role.table");
        if (tan != null) {
            Table tab = db.getTable(tan);
            tab.remove(DB.Q("role")+" IN (?)", Synt.setOf(
                centra + "/" + id + "/search",
                centra + "/" + id + "/create",
                centra + "/" + id + "/update",
                centra + "/" + id + "/delete",
                centra + "/" + id + "/reveal",
                centra + "/" + id + "/revert"
            ));
        }

        // 更新缓存(通过改变权限更新时间)
        tan = (String) table.getParams().get("user.table");
        if (tan != null) {
            Table tab = db.getTable(tan);
            tab.update(Synt.mapOf(
                "rtime", System.currentTimeMillis() / 1000
            ) , "id = ?" , uid);
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

    protected void updateFormConf(String id, String stat, List<Map> conf) throws CruxException {
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

        Set<String> ats = new HashSet(conf.size());

        for (Map fiel: conf) {
            item = docm.createElement("field");
            form.appendChild ( item );

            String n, t, y;
            n = (String) fiel.get("__name__");
            if (n != null) item.setAttribute("name", n); else n = "";
            t = (String) fiel.get("__type__");
            if (t != null) item.setAttribute("type", t); else t = "";
            y = (String) fiel.get("__text__");
            if (y != null) item.setAttribute("text", y);
            y = (String) fiel.get("__rule__");
            if (y != null && y.length() != 0) item.setAttribute("rule", y);
            y = (String) fiel.get("__required__");
            if (y != null && y.length() != 0) item.setAttribute("required", y);
            y = (String) fiel.get("__repeated__");
            if (y != null && y.length() != 0) item.setAttribute("repeated", y);

            //** 复查字段属性 **/

            y = (String) types.get(t);
            // 日期类型要指定存储格式
            if ("date".equals(y)) {
                if(!fiel.containsKey("type")) {
                    fiel.put("type", "timestamp");
                }
            } else
            // 选项表单要指定配置路径
            if ("enum".equals(y)
            ||  "form".equals(y)) {
                if(!fiel.containsKey("conf")) {
                    fiel.put("conf", centra + "/" + id);
                }
            } else
            // 文件类型要指定上传路径(含字段名)
            if ("file".equals(y)) {
                if(!fiel.containsKey("href")) {
                    fiel.put("href", upload + "/" + id);
                }
                if(!fiel.containsKey("path")) {
                    fiel.put("path", upload + "/" + id);
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
                if (k.startsWith(".") ) {
                    int    p;
                    String l;
                    p = k.indexOf(':', 1);
                    if (p < 0) throw new CruxException(400, "Wrong param name '"+k+"', must be '.name:code'");
                    l = k.substring(1+ p);
                    k = k.substring(0, p);
                    k = n.equals("@") ? id + k : n + k;
                    if (preset == null) {
                        preset =  new  LinkedHashMap();
                    }
                    Dict.put(preset, v, k, l);
                    continue;
                }
                if (k.startsWith(":") ) {
                    String l;
                    l = k.substring(1   );
                    k = n.equals("@") ? id     : n    ;
                    if (preset == null) {
                        preset =  new  LinkedHashMap();
                    }
                    Dict.put(preset, v, k, l);
                    continue;
                }
                if (k.equals("datalist")) {
                    select = Synt.asList(Dist.toObject(v) );
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

            //** 提取关联动作 **/

            if ("fork".equals(t)) {
                String  at = Synt.asString(fiel.get("data-at"));
                if (at != null && ! at.isEmpty()) {
                    // 清理代理前缀和参数后缀
                    int p  = at.indexOf  ('|');
                    if (p != -1 ) {
                        at = at.substring(1+p);
                    }   p  = at.indexOf  ('?');
                    if (p != -1 ) {
                        at = at.substring(0,p);
                    }
                    ats.add( at );
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

            /*
            // 全局性保护, 改用 VarsFilter 限制
            defs = docm.createElement("enum");
            root.appendChild ( defs );
            defs.setAttribute("name", id+".defense");
            defi = docm.createElement("value");
            defs.appendChild ( defi );
            defi.setAttribute("code", Cnst.AR_KEY);
            defi.appendChild ( docm.createTextNode("(void)"));
            defi = docm.createElement("value");
            defs.appendChild ( defi );
            defi.setAttribute("code", Cnst.NR_KEY);
            defi.appendChild ( docm.createTextNode("(void)"));
            defi = docm.createElement("value");
            defs.appendChild ( defi );
            defi.setAttribute("code", Cnst.OR_KEY);
            defi.appendChild ( docm.createTextNode("(void)"));
            */

            // 限定读范围
            defs = docm.createElement("enum");
            root.appendChild ( defs );
            defs.setAttribute("name", id+".defense");
            defi = docm.createElement("value");
            defs.appendChild ( defi );
            defi.setAttribute("code", Cnst.AR_KEY+".x.cuser");
            defi.appendChild ( docm.createTextNode("($session.uid||\"*\")"));

            // 保护写接口
            defs = docm.createElement("enum");
            root.appendChild ( defs );
            defs.setAttribute("name", id+".defence");
            defi = docm.createElement("value");
            defs.appendChild ( defi );
            defi.setAttribute("code", Cnst.AR_KEY+".x.cuser");
            defi.appendChild ( docm.createTextNode("($session.uid||\"*\")"));
        }

        saveDocument(file , docm);

        //** 关联权限 **/

        NodeList list;
        Set      acts;

        R0: {
            file = new File(Core.CONF_PATH +"/"+ centra +"/"+ id + Cnst.NAVI_EXT +".xml");
            docm = readDocument(file);
            root = docm.getDocumentElement();
            if (root == null) break R0 ;
            root = getNodeByTagNameAndAttr(root, "menu", "href", centra+"/"+id+"/");
            if (root == null) break R0 ;
            root = getNodeByTagNameAndAttr(root, "role", "name", centra+"/"+id+"/relate");
            if (root == null) break R0 ;
            list = root.getChildNodes();
            for(int i = list.getLength() - 1; i > -1; i --) {
                root.removeChild(list.item(i));
            }
            acts = NaviMap.getInstance("centra").actions;
            for(String at : ats) {
                if (at.startsWith(centre+"/")) {
                    at = centra+"/"+at.substring(centre.length()); // 更换分区前缀
                    at = at + Cnst.ACT_EXT;
                } else
                if (at.startsWith(centra+"/")) {
                    at = at + Cnst.ACT_EXT;
                } else {
                    continue;
                }
                if (acts.contains(at)) {
                    item = docm.createElement("action");
                    item.appendChild(docm.createTextNode(at));
                    root.appendChild(item);
                }
            }
            saveDocument(file, docm);
        }

        R1: {
            file = new File(Core.CONF_PATH +"/"+ centre +"/"+ id + Cnst.NAVI_EXT +".xml");
            docm = readDocument(file);
            root = docm.getDocumentElement();
            if (root == null) break R1 ;
            root = getNodeByTagNameAndAttr(root, "menu", "href", centre+"/"+id+"/");
            if (root == null) break R1 ;
            root = getNodeByTagNameAndAttr(root, "role", "name", centre+"/"+id+"/relate");
            if (root == null) break R1 ;
            list = root.getChildNodes();
            for(int i = list.getLength() - 1; i > -1; i --) {
                root.removeChild(list.item(i));
            }
            acts = NaviMap.getInstance("centre").actions;
            for(String at : ats) {
                if (at.startsWith(centra+"/")) {
                    at = centre+"/"+at.substring(centra.length()); // 更换分区前缀
                    at = at + Cnst.ACT_EXT;
                } else
                if (at.startsWith(centre+"/")) {
                    at = at + Cnst.ACT_EXT;
                } else {
                    continue;
                }
                if (acts.contains(at)) {
                    item = docm.createElement("action");
                    item.appendChild(docm.createTextNode(at));
                    root.appendChild(item);
                }
            }
            saveDocument(file, docm);
        }
    }

    protected void updateFormMenu(String id, String stat, String name) throws CruxException {
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
            role.appendChild ( docm.createTextNode("#centra") );

            // 查看
            // 2023/11/04 数值统计并入选项统计
            // 2023/11/05 列表和详情的接口分离
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", href +"search");
            role.setAttribute("text", "查看");
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"search"+ Cnst.ACT_EXT) );
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"recite"+ Cnst.ACT_EXT) );
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"recipe"+ Cnst.ACT_EXT) );
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"acount"+ Cnst.ACT_EXT) );
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"assort"+ Cnst.ACT_EXT) );
            depn = docm.createElement("depend");
            role.appendChild ( depn );
            depn.appendChild ( docm.createTextNode("centra") );

            // 添加
            // 2019/08/10 添加不再依赖查看权限
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", href +"create");
            role.setAttribute("text", "添加");
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"create"+ Cnst.ACT_EXT) );
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"recipe"+ Cnst.ACT_EXT) );
            depn = docm.createElement("depend");
            role.appendChild ( depn );
            depn.appendChild ( docm.createTextNode("centra") );
            depn = docm.createElement("depend");
            role.appendChild ( depn );
            depn.appendChild ( docm.createTextNode(href +"relate") );

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
            depn = docm.createElement("depend");
            role.appendChild ( depn );
            depn.appendChild ( docm.createTextNode(href +"relate") );

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
            // 2023/11/05 列表和详情的接口分离
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", href +"reveal");
            role.setAttribute("text", "回看");
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"reveal"+ Cnst.ACT_EXT) );
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"remind"+ Cnst.ACT_EXT) );
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
            actn.appendChild ( docm.createTextNode(href +"revert"+ Cnst.ACT_EXT) );
            depn = docm.createElement("depend");
            role.appendChild ( depn );
            depn.appendChild ( docm.createTextNode(href +"reveal") );

            // 关联
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", href +"relate");

            // 其他
            role = docm.createElement("role");
            menu.appendChild ( role );
            role.setAttribute("name", href +"reject");
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"assort"+ Cnst.ACT_EXT) );
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
                menu.setAttribute("hrel" , "!HIDE");
            } else
            if ("6".equals(stat)) {
                menu.setAttribute("hrel" , "!DENY");
            } else {
                menu.setAttribute("hrel" ,   ""   );
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
                menu.setAttribute("hrel" , "!HIDE");
            } else
            if ("6".equals(stat)) {
                menu.setAttribute("hrel" , "!DENY");
            } else {
                menu.setAttribute("hrel" ,   ""   );
            }

            // 会话
            role = docm.createElement("rsname");
            root.appendChild ( role );
            role.appendChild ( docm.createTextNode("#centre") );

            // 公共读取权限
            role = docm.createElement( "role" );
            menu.appendChild ( role );
            role.setAttribute("name", "public");

            // 增删改须登录
            role = docm.createElement( "role" );
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
            depn = docm.createElement("depend");
            role.appendChild ( depn );
            depn.appendChild ( docm.createTextNode(href +"relate") );

            // 关联查询接口, 默认登录可用 (增改依赖)
            role = docm.createElement( "role" );
            menu.appendChild ( role );
            role.setAttribute("name", href +"relate");

            // 聚合统计接口, 默认禁止访问 (较耗资源)
            role = docm.createElement( "role" );
            menu.appendChild ( role );
            role.setAttribute("name", href +"reject");
            actn = docm.createElement("action");
            role.appendChild ( actn );
            actn.appendChild ( docm.createTextNode(href +"assort"+ Cnst.ACT_EXT) );
        }

        saveDocument(file , docm);
    }

    protected void updateUnitMenu(String id) throws CruxException {
        new Furl().updateMenus( );
    }

    /**
     * 构建字段名称
     * 避免库里有太多的零散字段
     * @param fc
     * @param fs
     * @return
     * @throws CruxException
     */
    protected String getFieldName(Map fc, Set fs) throws CruxException {
        Table  tab = db.getTable ("feed");
        String sql = "SELECT fn FROM "+DB.Q(tab.tableName)+" WHERE ft = ? AND fn NOT IN (?) ORDER BY fn";

        String ft1 = Synt.declare(fc.get ( "__type__" ),"string");
        String fn1 = (String) db.fetchOne(sql, ft1, fs).get("fn");
        if ( null != fn1 ) return fn1;

        String ft2 = getFieldType(fc);
        String fn2 = (String) db.fetchOne(sql, ft2, fs).get("fn");
        if ( null != fn2 ) return fn2;

        fn2 = Core.newIdentity ( "FN" );
        tab.insert( Synt.mapOf (
            "fn", fn2,
            "ft", ft2
        ) );
        return fn2;
    }

    /**
     * 获取字段类型
     * 同 LuceneRecord.datatype
     * @param fc
     * @return
     */
    protected String getFieldType(Map fc) {
        String t = (String) fc.get("__type__");
        if (t == null) {
            return t ;
        }

        // 常规类型
        switch (t) {
            case "string":
            case "search":
            case "sorted":
            case "stored":
            case "object":
                return t ;
        }

        // 基准类型
        try {
            String k  = (String) FormSet
                  .getInstance ( /***/ )
                  .getEnum ("__types__")
                  .get (t);
            if (null != k) {
                   t  = k;
            }
        } catch ( CruxException e) {
            throw e.toExemption( );
        }

        // 扩展类型
        switch (t) {
            case "number":
                return Synt.declare(fc.get("type"), "double");
            case "hidden":
            case  "enum" :
            case  "fork" :
                return Synt.declare(fc.get("type"), "string");
            case  "file" :
                return "string";
            case  "form" :
                return "object";
            default:
                return t ;
        }
    }

    private Document makeDocument() throws CruxException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder        builder = factory.newDocumentBuilder();
            return  builder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new CruxException(e);
        }
    }

    private Document readDocument(File file) throws CruxException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder        builder = factory.newDocumentBuilder();
            try {
                /**
                 * 因写入时有换行缩进
                 * 换行和缩进会解析成 TextNode
                 * 需遍历并清理掉这些 TextNode
                 * 设置 factory.setIgnoringElementContentWhitespace(true) 无效
                 * Java 8 没发现这个问题, Java 11/17 都遇到这个问题
                 */
                Document docu;
                Element  root;
                docu = builder.parse(new FileInputStream(file));
                root = docu.getDocumentElement();
                delBlankLinesInDocument ( root );
                return docu;
            } catch (FileNotFoundException e) {
                return builder.newDocument( );
            }
        } catch (ParserConfigurationException e) {
            throw new CruxException(e);
        } catch (SAXException e) {
            throw new CruxException(e);
        } catch ( IOException e) {
            throw new CruxException(e);
        }
    }

    private void saveDocument(File file, Document docm) throws CruxException {
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
            throw new CruxException(e);
        } catch (UnsupportedEncodingException e) {
            throw new CruxException(e);
        } catch (IllegalArgumentException e) {
            throw new CruxException(e);
        } catch (FileNotFoundException e) {
            throw new CruxException(e);
        } catch ( TransformerException e) {
            throw new CruxException(e);
        }
    }

    private void delBlankLinesInDocument(Element elem) {
        NodeList list = elem.getChildNodes();
        for(int i = list.getLength() -1; i > -1; i --) {
            Node node = list.item(i);
            switch ( node.getNodeType() ) {
                case Node.ELEMENT_NODE:
                    delBlankLinesInDocument( (Element)  node  );
                    break;
                case Node.TEXT_NODE:
                    if (node.getTextContent( ).matches("\\s*")) {
                        elem.removeChild(node);
                    }
                    break;
            }
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
