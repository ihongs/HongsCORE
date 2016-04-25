package app.hongs.serv.module;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.db.DB;
import app.hongs.db.Mtree;
import app.hongs.db.Table;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
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
 * 单元模型
 * @author Hongs
 */
public class Unit extends Mtree {

    public Unit() throws HongsException {
        this(DB.getInstance("module").getTable("unit"));
    }

    public Unit(Table table)
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
        String id = (String) rd.get(this.table.primaryKey);
        if (id == null || id.length() == 0) {
            id = this.add(rd);
        } else {
            this.put(id , rd);
        }

        // 建立菜单配置
        String name = (String) rd.get("name");
        if (name != null && !"".equals(name)) {
            updateOrCreateMenuSet( id, name );
            updateOrCreateMenuSet(  );
        }

        return id;
    }

    public void updateOrCreateMenuSet(String id, String name) throws HongsException {
        Document docm = makeDocument();

        Element  root = docm.createElement("root");
        docm.appendChild ( root );

        Element  menu = docm.createElement("menu");
        root.appendChild ( menu );
        menu.setAttribute("disp", name);
        menu.setAttribute("href", "manage/data/"+id+"/");

        Element  incl;

        // 会话
        incl = docm.createElement("rsname");
        root.appendChild ( incl );
        incl.appendChild ( docm.createTextNode("@manage") );

        List<Map> rows;

        // 单元下的表单
        rows = this.db.getTable("form").fetchCase()
            .select("id").where("unit_id = ?" , id)
            .all();
        for (Map row : rows) {
            String fid = row.get("id").toString();
            incl = docm.createElement( "import" );
            menu.appendChild( incl );
            incl.appendChild( docm.createTextNode("manage/data/"+fid) );
        }

        // 保存
        saveDocument(Core.CONF_PATH+"/manage/data/"+id+Cnst.NAVI_EXT+".xml", docm);
    }

    public void updateOrCreateMenuSet() throws HongsException {
        Document docm = makeDocument();

        Element  root = docm.createElement("root");
        docm.appendChild ( root );

        Element  incl;

        // 会话
        incl = docm.createElement("rsname");
        root.appendChild ( incl );
        incl.appendChild ( docm.createTextNode("@manage") );

        List<Map> rows;

        // 全部一级单元
        rows = this.table.fetchCase( )
            .select("id").where("pid  = 0")
            .all();
        for (Map row : rows) {
            String uid = row.get("id").toString();
            incl = docm.createElement( "import" );
            root.appendChild( incl );
            incl.appendChild( docm.createTextNode("manage/data/"+uid) );
        }

        Element  menu = docm.createElement("menu");
        root.appendChild ( menu );
        menu.setAttribute("disp", "");
        menu.setAttribute("href", "!manage/data/");

        // 一级以下单元
        rows = this.table.fetchCase( )
            .select("id").where("pid != 0")
            .all();
        for (Map row : rows) {
            String uid = row.get("id").toString();
            incl = docm.createElement( "import" );
            menu.appendChild( incl );
            incl.appendChild( docm.createTextNode("manage/data/"+uid) );
        }

        saveDocument(Core.CONF_PATH+"/manage/data"+Cnst.NAVI_EXT+".xml", docm);
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
