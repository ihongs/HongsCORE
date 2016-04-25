package app.hongs.serv.module;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.db.DB;
import app.hongs.db.Model;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.util.Synt;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.document.Document;

/**
 * 数据存储模型
 * @author Hongs
 */
public class Data extends LuceneRecord {

    String formId = null;
    public String[] findCols = new String[] {};
    public String[] listCols = new String[] {};

    public Data(String conf, String form) throws HongsException {
        super(
            conf.replaceFirst("^(handle|manage)/", "")+"/"+form,
            FormSet.getInstance(conf + "/" + form).getForm(form)
        );
        formId = form;
    }

    /**
     * 获取实例
     * 生命周期将交由 Core 维护
     * @param conf
     * @param form
     * @return
     * @throws HongsException
     */
    public static Data getInstance(String conf, String form) throws HongsException {
        Data   inst;
        Core   core = Core.getInstance();
        String name = Data.class.getName() +":"+ conf +":"+ form;
        if (core.containsKey(name)) {
            inst = (Data) core.got(name);
        } else {
            inst = new Data( conf, form);
            core.put ( name, inst);
        }
        return inst;
    }

    /**
     * 添加文档
     * @param rd
     * @return ID
     * @throws HongsException
     */
    @Override
    public String add(Map rd) throws HongsException {
        String id = Core.getUniqueId();
        save(id, rd);
        return id;
    }

    /**
     * 修改文档(局部更新)
     * @param id
     * @param rd
     * @throws HongsException
     */
    @Override
    public void put(String id, Map rd) throws HongsException {
        save(id, rd);
    }

    /**
     * 设置文档(无则添加)
     * @param id
     * @param rd
     * @throws HongsException
     */
    @Override
    public void set(String id, Map rd) throws HongsException {
        save(id, rd);
    }

    /**
     * 删除文档
     * @param id
     * @throws HongsException
     */
    @Override
    public void del(String id) throws HongsException {
        save(id, null);
    }

    public void save(String id, Map rd) throws HongsException {
        Model model = DB.getInstance("module").getModel("data");
        Map   dd,od = new HashMap();
        od.put("etime", System.currentTimeMillis());
        String   where = "`id` = ? AND `etime` = ?";
        Object[] param = new String[ ] { id , "0" };

        // 删除当前数据
        if (rd == null) {
            od.put("state", 0);
            model.table.update(od, where, param);
            super.del(id);
            return;
        }

        // 获取旧的数据
        dd = model.table.fetchCase().select("data").where(where, param).one( );
        if(!dd.isEmpty()) {
            dd = (Map) app.hongs.util.Data.toObject(dd.get("data").toString());
        }

        // 合并新旧数据
        int i = 0;
        Map<String,Map> fields = getFields (  );
        for(String fn : fields.keySet()) {
            String fr = Synt.declare(rd.get(fn), "");
            String fo = Synt.declare(dd.get(fn), "");
            if (   rd.containsKey( fn )) {
                dd.put( fn , fr );
            } else
            if ( "id".equals(fn)) {
                dd.put( fn , id );
            }
            if ( ! fr.equals(fo)) {
                i += 1;
            }
        }
        if (i == 0) {
            // 什么也没改变
            return;
        }

        // 拼接展示字段
        StringBuilder nm = new StringBuilder();
        for (String fn : listCols) {
            nm.append(dd.get(fn).toString()).append(' ');
        }

        // 保存到数据库
        Map nd = new HashMap();
        nd.put( "id" , id);
        nd.put("form_id", formId);
        nd.put("name", nm.toString( ).trim( ));
        nd.put("data", app.hongs.util.Data.toString(dd));
        nd.put("etime", 0);
        model.table.update(od , where , param);
        model.table.insert(nd);

        // 保存到索引库
        Document doc = new Document();
        dd.put(Cnst.ID_KEY, id);
        docAdd(doc, dd);
        setDoc(id, doc);
    }

    public void redo(String id, String ct) throws HongsException {
        Model model = DB.getInstance("module").getModel("data");
        Map   dd,od ;
        String   where = "`id` = ? AND `etime` = ?";
        Object[] param = new String[ ] { id , "0" };

        // 获取旧的数据
        od = model.table.fetchCase().select("data").where(where, param).one( );
        if(!od.isEmpty()) {
            dd = (Map) app.hongs.util.Data.toObject(od.get("data").toString());
        } else {
            super.del(id);
            return;
        }
        ct = String.valueOf(System.currentTimeMillis());

        // 保存到数据库
        Map nd = new HashMap();
        nd.put( "id" , id);
        nd.put("form_id", formId);
        nd.put("name", od.get("name" ));
        nd.put("data", od.get("data" ));
        nd.put("rtime",od.get("mtime"));
        nd.put("ctime",ct);
        nd.put("etime", 0);
        model.table.insert(nd);

        // 保存到索引库
        Document doc = new Document(  );
        dd.put(Cnst.ID_KEY, id);
        docAdd(doc, dd);
        setDoc(id, doc);
    }

}
