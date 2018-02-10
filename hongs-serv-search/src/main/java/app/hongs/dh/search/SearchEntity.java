package app.hongs.dh.search;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.action.FormSet;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.util.Block;

import java.io.IOException;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

/**
 * 搜索记录
 *
 * 增加写锁避免同时写入导致失败
 * 注意: 此类的对象无法使用事务
 *
 * @author Hongs
 */
public class SearchEntity extends LuceneRecord {

    public SearchEntity(Map form, String path, String name) throws HongsException {
        super(form, path, name);
    }

    public SearchEntity(Map form) throws HongsException {
        this (form, null, null);
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
    public static SearchEntity getInstance(String conf, String form) throws HongsException {
        String code = SearchEntity.class.getName() +":"+ conf +"."+ form;
        Core   core = Core.getInstance( );
        if ( ! core.containsKey( code ) ) {
            String path = conf +"/"+ form;
            String name = conf +"."+ form;
            String cxnf = FormSet.hasConfFile(path)? path : conf ;
            Map    fxrm = FormSet.getInstance(cxnf).getForm(form);

            // 表单配置中可指定数据路径
            Map c = (Map) fxrm.get("@");
            if (c!= null) {
                String p;
                p = (String) c.get("data-path");
                if (null != p && 0 < p.length()) {
                    path  = p;
                }
                p = (String) c.get("data-name");
                if (null != p && 0 < p.length()) {
                    name  = p;
                }
            }

            SearchEntity inst = new SearchEntity(fxrm, path,name);
            core.put( code, inst ) ; return inst ;
        } else {
            return  (SearchEntity) core.got(code);
        }
    }

    @Override
    public void addDoc(final Document doc) throws HongsException {
        final String key = SearchEntity.class.getName() + ":" + getDataName();
        Block.Locker loc = Block.getLocker(key);
        loc.lock();
        try {
            IndexWriter iw = this.getWriter(  );
            iw.addDocument (doc);
            iw.commit();
        } catch (IOException ex) {
            throw new HongsExpedient.Common(ex);
        } finally {
            loc.unlock();
        }
    }

    @Override
    public void setDoc(final String id, final Document doc) throws HongsException {
        final String key = SearchEntity.class.getName() + ":" + getDataName();
        Block.Locker loc = Block.getLocker(key);
        loc.lock();
        try {
            IndexWriter iw = this.getWriter(  );
            iw.updateDocument (new Term(Cnst.ID_KEY, id), doc);
            iw.commit();
        } catch (IOException ex) {
            throw new HongsExpedient.Common(ex);
        } finally {
            loc.unlock();
        }
    }

    @Override
    public void delDoc(final String id) throws HongsException {
        final String key = SearchEntity.class.getName() + ":" + getDataName();
        Block.Locker loc = Block.getLocker(key);
        loc.lock();
        try {
            IndexWriter iw = this.getWriter(  );
            iw.deleteDocuments(new Term(Cnst.ID_KEY, id) /**/);
            iw.commit();
        } catch (IOException ex) {
            throw new HongsExpedient.Common(ex);
        } finally {
            loc.unlock();
        }
    }

}
