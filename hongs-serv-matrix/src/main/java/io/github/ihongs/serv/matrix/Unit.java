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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
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

    private final Pattern UNIT_ID_RG = Pattern.compile("x=(.*)");

    public Unit() throws HongsException {
        this(DB.getInstance("matrix").getTable("unit"));
    }

    public Unit(Table table)
    throws HongsException {
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
        NaviMap navi = NaviMap.getInstance(centra);
        Map<String, Map> ms = navi.menus;
        Set<String> rs = navi.getRoleSet();
        Set<String> us = /**/new HashSet();
        getSubUnits(ms , rs , us );

        // 限制为有权限的单元
        caze.filter("`"+table.name+"`.`id` IN (?)", us);
    }

    private boolean getSubUnits(Map<String, Map> menus, Set<String> roles, Set<String> units) {
        boolean hasRol = false;
        boolean hasSub ;
        boolean hasOne ;
        Matcher keyMat ;
        for(Map.Entry<String, Map> subEnt : menus.entrySet()) {
            Map<String, Object> menu = subEnt.getValue();
            Map<String, Map> menus2 = (Map) menu.get("menus");
            Set<String/***/> roles2 = (Set) menu.get("roles");
            hasSub = hasOne = false;
            if (menus2 != null && !menus2.isEmpty() /* Check sub menus */ ) {
                hasSub  = getSubUnits (menus2, roles, units );
            } else {
                hasOne  = true ;
            }
            if (roles2 != null && !roles2.isEmpty() && (!hasSub || hasOne)) {
                hasOne  = false;
            for(String rn : roles2) {
            if (roles.contains(rn)) {
                hasOne  = true ;
                break;
            }}
            }
            if (hasSub || hasOne  ) {
                hasRol  = true ;
                keyMat  = UNIT_ID_RG.matcher(subEnt.getKey());
                if (keyMat.find()) units.add(keyMat.group(1));
            }
        }
        return  hasRol ;
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

        saveDocument(Core.CONF_PATH+"/"+centra+Cnst.NAVI_EXT+".xml", centraDocm);
        saveDocument(Core.CONF_PATH+"/"+centre+Cnst.NAVI_EXT+".xml", centreDocm);
    }

    private void insertForms(
            Document centraDocm, Element centraRoot,
            Document centreDocm, Element centreRoot,
            String id)
    throws HongsException {
        Element importNode;
        List<Map> rows;

        rows = this.db.getTable("form")
            .fetchCase()
            .filter("unit_id = ? AND state > 0",id)
            .select("id, state" )
            .assort("boost DESC")
            .getAll( );
        for ( Map  row : rows ) {
            String fid = row.get( "id"  ).toString();
            String sta = row.get("state").toString();

            importNode = centraDocm.createElement("import");
            importNode.appendChild(centraDocm.createTextNode(centra+"/"+fid));
            centraRoot.appendChild(importNode);

            if (! "2".equals(sta)) {
                continue;
            }

            importNode = centreDocm.createElement("import");
            importNode.appendChild(centreDocm.createTextNode(centre+"/"+fid));
            centreRoot.appendChild(importNode);
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
            centraHid2.setAttribute("text", nam);
            centraHid2.setAttribute("href", "!"+centra+"/"+pid);

            centreHid2 = centreDocm.createElement("menu");
            centreHid2.setAttribute("text", nam);
            centreHid2.setAttribute("href", "!"+centre+"/"+pid);

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

            if (centraHid2.hasChildNodes()) {
                centraRoot.appendChild(centraHid2);
            }

            if (centreHid2.hasChildNodes()) {
                centreRoot.appendChild(centreHid2);
            }
        }
    }

    private void insertUnits(
            Document centraDocm, Element centraRoot,
            Document centreDocm, Element centreRoot,
            String id)
    throws HongsException {
        Element centraRoo2, centraHid2;
        Element centreRoo2, centreHid2;
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
            centraRoo2.setAttribute("text", nam);
            centraRoo2.setAttribute("href", "common/menu.act?m="+centra+"&x="+pid);

            centraHid2 = centraDocm.createElement("menu");
            centraHid2.setAttribute("text", nam);
            centraHid2.setAttribute("href", "!"+centra+"/"+pid);

            centreRoo2 = centreDocm.createElement("menu");
            centreRoo2.setAttribute("text", nam);
            centreRoo2.setAttribute("href", "common/menu.act?m="+centre+"&x="+pid);

            centreHid2 = centreDocm.createElement("menu");
            centreHid2.setAttribute("text", nam);
            centreHid2.setAttribute("href", "!"+centre+"/"+pid);

            insertForms(
                centraDocm, centraRoo2,
                centreDocm, centreRoo2,
                pid
            );

            insertHides(
                centraDocm, centraHid2,
                centreDocm, centreHid2,
                pid
            );

            if (centraRoo2.hasChildNodes()) {
                centraRoot.appendChild(centraRoo2);
            }
            if (centraHid2.hasChildNodes()) {
                centraRoot.appendChild(centraHid2);
            }

            if (centreRoo2.hasChildNodes()) {
                centreRoot.appendChild(centreRoo2);
            }
            if (centreHid2.hasChildNodes()) {
                centreRoot.appendChild(centreHid2);
            }
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
