package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.NaviMap;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Grade;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.util.Synt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * 单元模型
 * @author Hongs
 */
public class Unit extends Grade {

    protected String centra = "centra/data";
    protected String centre = "centre/data";

    public Unit() throws HongsException {
        this(DB.getInstance("matrix").getTable("unit"));
    }

    public Unit(Table table) throws HongsException {
        super(table);
    }

    @Override
    public int add(String id, Map rd) throws HongsException {
        int n = super.add(id, rd);

        // 建立菜单配置
        updateMenus();

        return n;
    }

    @Override
    public int put(String id, Map rd) throws HongsException {
        int n = super.put(id, rd);

        // 更新菜单配置
        updateMenus();

        return n;
    }

    @Override
    public int del(String id) throws HongsException {
        int n = super.del(id);

        // 更新菜单配置
        updateMenus();

        return n;
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
//          mm = "/search";
        } else
        if ("update" .equals(mm)
        ||  "delete" .equals(mm)) {
//          mm = "/" + mm ;
        } else {
            return; // 非常规动作不限制
        }

        // 从导航表中取单元ID
        Set<String> us = new HashSet();
        NaviMap     nv = NaviMap.getInstance(centra);
        getSubUnits(nv.menus, nv.getRoleSet ( ), us);

        // 限制为有权限的单元
        caze.filter("`"+table.name+"`.`id` IN (?)", us);
    }

    private static final Pattern UNIT_CODE = Pattern.compile("\\Wx=(\\w+)");

    private int getSubUnits(Map<String, Map> menus, Set<String> roles, Set<String> units) {
        /**
         * 返回值及 hasRol 和 hasSub 取值含义为:
         * 0 无角色设置
         * 1 有访问权限
         * 2 无访问权限
         */

        int cntNul = 0; // 空角色设置计数
        int cntRol = 0; // 有访问权限计数
        int l = menus.size(); // 菜单数量

        for(Map.Entry<String, Map> entry : menus.entrySet()) {
                String          href = entry.getKey  ();
            Map<String, Object> menu = entry.getValue();
            Map<String, Map>  menus2 = (Map) menu.get("menus");
            Set<String     >  roles2 = (Set) menu.get("roles");

            int hasRol = roles2 == null || roles2.isEmpty( ) ? 0 : 1;
            if (hasRol == 1) {
                hasRol = Collections.disjoint(roles2, roles) ? 2 : 1;
            }

            int hasSub = menus2 == null || menus2.isEmpty( ) ? 0 : 1;
            if (hasSub == 1) {
                hasSub = getSubUnits(menus2 , roles , units);
            }

            if (hasRol == 0 && hasSub == 0) {
                cntNul += 1;
                Matcher m = UNIT_CODE.matcher(href);
                if (m.find()) units.add(m.group(1));
            } else
            if (hasRol == 1 || hasSub == 1) {
                cntRol += 1;
                Matcher m = UNIT_CODE.matcher(href);
                if (m.find()) units.add(m.group(1));
            }
        }

        if (cntNul == l) { // 没任何角色设置
            return 0;
        }
        if (cntRol >= 1) { // 有至少一个权限
            return 1;
        }
        return 2;
    }

    public  void updateMenus()
    throws HongsException {
        Document centraDocm, centreDocm;
        Element  centraRoot, centreRoot;
        Element  importNode;

        //** 后端 */

        centraDocm = makeDocument();

        centraRoot = centraDocm.createElement("root");
        centraDocm.appendChild(centraRoot);

        importNode = centraDocm.createElement("rsname");
        importNode.appendChild(centraDocm.createTextNode("@centra"));
        centraRoot.appendChild(importNode);

        //** 前端 **/

        centreDocm = makeDocument();

        centreRoot = centreDocm.createElement("root");
        centreDocm.appendChild(centreRoot);

        importNode = centreDocm.createElement("rsname");
        importNode.appendChild(centreDocm.createTextNode("@centre"));
        centreRoot.appendChild(importNode);

        //** 填充 **/

        // 第一层表单
        insertForms(centraDocm, centraRoot, centreDocm, centreRoot, "0");

        // 第一层单元
        insertUnits(centraDocm, centraRoot, centreDocm, centreRoot, "0");

        saveDocument(new File(Core.CONF_PATH+"/"+centra+Cnst.NAVI_EXT+".xml"), centraDocm);
        saveDocument(new File(Core.CONF_PATH+"/"+centre+Cnst.NAVI_EXT+".xml"), centreDocm);
    }

    private void insertForms(
            Document centraDocm, Element centraRoot,
            Document centreDocm, Element centreRoot,
            String id)
    throws HongsException {
        Element importNode;
        List<Map> rows;
        int  cnt  = 0 ;

        rows = this.db.getTable("form")
            .fetchCase()
            .filter("unit_id = ? AND state > 0",id)
            .select("id, state" )
            .assort("boost DESC")
            .getAll( );
        for ( Map  row : rows ) {
            String fid = row.get( "id" ).toString();
            int    sta = Synt.declare(row.get("state"), 0 );
            if (sta == 1 || sta == 4 ) { // 内部表单或仅开接口
                cnt ++ ;
            }

            importNode = centraDocm.createElement("import");
            importNode.appendChild(centraDocm.createTextNode(centra+"/"+fid));
            centraRoot.appendChild(importNode);

            importNode = centreDocm.createElement("import");
            importNode.appendChild(centreDocm.createTextNode(centre+"/"+fid));
            centreRoot.appendChild(importNode);
        }

        /**
         * 非顶层而下级无可见表单
         * 则将上级菜单设置为隐藏
         */
        if (! "0".equals(id)) {
            if (rows.size() ==  0 ) {
                centraRoot.setAttribute("hrel", "HIDE");
            }
            if (rows.size() == cnt) {
                centreRoot.setAttribute("hrel", "HIDE");
            }
        }
    }

    private void insertUnits(
            Document centraDocm, Element centraRoot,
            Document centreDocm, Element centreRoot,
            String id)
    throws HongsException {
        Element centraRoo2, centreRoo2;
        List<Map> rows;

        rows = this.table
            .fetchCase()
            .filter("pid = ? AND state > 0", id)
            .select("id, name"  )
            .assort("boost DESC")
            .getAll( );

        for ( Map  row : rows ) {
            String pid = row.get( "id" ).toString();
            String nam = row.get("name").toString();

            centraRoo2 = centraDocm.createElement("menu");
            centraRoo2.setAttribute("href", "common/menu.act?m="+centra+"&x="+pid);
            centraRoo2.setAttribute("text",  nam  );
            centraRoot.appendChild (  centraRoo2  );

            centreRoo2 = centreDocm.createElement("menu");
            centreRoo2.setAttribute("href", "common/menu.act?m="+centre+"&x="+pid);
            centreRoo2.setAttribute("text",  nam  );
            centreRoot.appendChild (  centreRoo2  );

            insertForms(
                centraDocm, centraRoo2,
                centreDocm, centreRoo2,
                pid
            );

            insertHides(
                centraDocm, centraRoo2,
                centreDocm, centreRoo2,
                pid
            );
        }
    }

    private void insertHides(
            Document centraDocm, Element centraRoot,
            Document centreDocm, Element centreRoot,
            String id)
    throws HongsException {
        Element centraHid2, centreHid2;
        List<Map> rows;

        rows = this.table
            .fetchCase()
            .filter("pid = ? AND state > 0", id)
            .select("id, name"  )
            .assort("boost DESC")
            .getAll( );

        for ( Map  row : rows ) {
            String pid = row.get( "id" ).toString();
            String nam = row.get("name").toString();

            centraHid2 = centraDocm.createElement("menu");
            centraHid2.setAttribute("href", "common/menu.act?m="+centra+"&x="+pid);
            centraHid2.setAttribute("hrel", "HIDE"); // 默认隐藏
            centraHid2.setAttribute("text",  nam  );
            centraRoot.appendChild (  centraHid2  );

            centreHid2 = centreDocm.createElement("menu");
            centreHid2.setAttribute("href", "common/menu.act?m="+centre+"&x="+pid);
            centraHid2.setAttribute("hrel", "HIDE"); // 默认隐藏
            centreHid2.setAttribute("text",  nam  );
            centreRoot.appendChild (  centreHid2  );

            insertForms(
                centraDocm, centraHid2,
                centreDocm, centreHid2,
                pid
            );

            insertHides(
                centraDocm, centraHid2,
                centreDocm, centreHid2,
                pid
            );
        }
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

}
