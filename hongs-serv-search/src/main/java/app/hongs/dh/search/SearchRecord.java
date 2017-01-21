package app.hongs.dh.search;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.util.Synt;
import java.util.HashMap;
import java.util.Map;
import org.apache.lucene.document.Document;

/**
 * 搜索记录
 * 避免同时写入而产生异常
 * <p>
 * 此类中的 add,put,set和del 不会立即写库,
 * 而是调用 SearchQueuer.add 排队等待写入.
 * </p>
 * @author Hongs
 */
public class SearchRecord extends LuceneRecord {

    public SearchRecord(String path, Map form) throws HongsException {
        super(path, form);
    }

    public SearchRecord(String path) throws HongsException {
        super(path);
    }

    public SearchQueuer getQueuer( ) throws HongsException {
        return  SearchQueuer.getInstance(this);
    }

    /**
     * 获取实例
     * 存储为 conf/form 表单为 conf.form
     * 表单缺失则尝试获取 conf/form.form
     * 实例生命周期将交由 Core 维护
     * @param conf
     * @param form
     * @return
     * @throws HongsException
     */
    public static SearchRecord getInstance(String conf, String form) throws HongsException {
        SearchRecord  inst;
        Core   core = Core.getInstance();
        String name = SearchRecord.class.getName( ) + ":" +  conf + "." + form;
        if ( ! core.containsKey( name )) {
            String path = conf + "/" +  form;
            String canf = FormSet.hasConfFile(path) ? path : conf ;
            Map    farm = FormSet.getInstance(canf).getForm( form);
            inst =  new SearchRecord(path , farm);
            core.put( name, inst );
        } else {
            inst =  (SearchRecord) core.got(name);
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
        String id = Synt.declare(rd.get(Cnst.ID_KEY), String.class);
        if (id != null && id.length() != 0) {
            throw new HongsException.Common("Id can not set in add");
        }
        id = Core.newIdentity();
        rd.put(Cnst.ID_KEY, id);

        // 放入索引队列
        //addDoc(map2Doc(rd));
        rd = new HashMap(rd);
        rd.put("__action__", "set");
        getQueuer( ).add(rd);

        return id;
    }

    /**
     * 设置文档(无则添加)
     * @param id
     * @param rd
     * @throws HongsException
     */
    @Override
    public void set(String id, Map rd) throws HongsException {
        if (id == null || id.length() == 0) {
            throw new HongsException.Common("Id must be set in put");
        }
        Document doc = getDoc(id);
        if (doc == null) {
//          doc =  new Document();
        } else {
            /**
             * 实际运行中发现
             * 直接往取出的 doc 里设置属性, 会造成旧值的索引丢失
             * 故只好转换成 map 再重新设置, 这样才能确保索引完整
             * 但那些 Store=NO 的数据将无法设置
             */
            setView(new HashMap());
            Map  md = doc2Map(doc);
            md.putAll(rd);
            rd = md;
        }
        rd.put(Cnst.ID_KEY, id);

        // 放入索引队列
        //docAdd(doc, rd);
        //setDoc(id, doc);
        rd = new HashMap(rd);
        rd.put("__action__", "set");
        getQueuer( ).add(rd);
    }

    /**
     * 修改文档(局部更新)
     * @param id
     * @param rd
     * @throws HongsException
     */
    @Override
    public void put(String id, Map rd) throws HongsException {
        if (id == null || id.length() == 0) {
            throw new HongsException.Common("Id must be set in put");
        }
        Document doc = getDoc(id);
        if (doc == null) {
            throw new HongsException.Common("Doc#"+id+" not exists");
        } else {
            /**
             * 实际运行中发现
             * 直接往取出的 doc 里设置属性, 会造成旧值的索引丢失
             * 故只好转换成 map 再重新设置, 这样才能确保索引完整
             * 但那些 Store=NO 的数据将无法设置
             */
            setView(new HashMap());
            Map  md = doc2Map(doc);
            md.putAll(rd);
            rd = md;
        }
        rd.put(Cnst.ID_KEY, id);

        // 放入索引队列
        //docAdd(doc, rd);
        //setDoc(id, doc);
        rd = new HashMap(rd);
        rd.put("__action__", "set");
        getQueuer( ).add(rd);
    }

    /**
     * 删除文档(delDoc 的别名)
     * @param id
     * @throws HongsException
     */
    @Override
    public void del(String id) throws HongsException {
        //放入索引队列
        //delDoc(id);
        Map rd = new HashMap();
        rd.put("__action__", "set");
        rd.put(Cnst.ID_KEY ,  id  );
        getQueuer( ).add( rd );
    }

}
