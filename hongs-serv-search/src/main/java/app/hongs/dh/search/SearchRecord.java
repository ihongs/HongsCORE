package app.hongs.dh.search;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.action.FormSet;
import app.hongs.dh.lucene.LuceneRecord;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
public class SearchRecord extends LuceneRecord {

    public SearchRecord(String path) throws HongsException {
        super(path);
    }

    public SearchRecord(String path, Map form) throws HongsException {
        super(path, form);
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

    @Override
    public void addDoc(final Document doc) throws HongsException {
        String key = SearchRecord.class.getName() + ":" + getDbName();
        ReentrantLock loc = lock(key);
        loc.lock();
        try {
            IndexWriter iw = this.getWriter( );
            iw.addDocument (doc);
            iw.commit();
        } catch (IOException ex) {
            throw new HongsException.Common(ex);
        } finally {
            loc.unlock();
        }
    }

    @Override
    public void setDoc(final String id, final Document doc) throws HongsException {
        String key = SearchRecord.class.getName() + ":" + getDbName();
        ReentrantLock loc = lock(key);
        loc.lock();
        try {
            IndexWriter iw = this.getWriter( );
            iw.updateDocument (new Term(Cnst.ID_KEY, id), doc);
            iw.commit();
        } catch (IOException ex) {
            throw new HongsException.Common(ex);
        } finally {
            loc.unlock();
        }
    }

    @Override
    public void delDoc(final String id) throws HongsException {
        String key = SearchRecord.class.getName() + ":" + getDbName();
        ReentrantLock loc = lock(key);
        loc.lock();
        try {
            IndexWriter iw = this.getWriter( );
            iw.deleteDocuments(new Term(Cnst.ID_KEY, id) /**/);
            iw.commit();
        } catch (IOException ex) {
            throw new HongsException.Common(ex);
        } finally {
            loc.unlock();
        }
    }

      /** 私有方法 **/

      private ReentrantLock lock(String flag)
      {
          ReentrantLock rwlock;
          Lock lock;

          lock = lockr. readLock();
          lock.lock();
          try {
              rwlock = locks.get(flag);
              if (rwlock != null) {
                  return rwlock;
              }
          } finally {
              lock.unlock();
          }

          lock = lockr.writeLock();
          lock.lock();
          try {
              rwlock = new ReentrantLock();
              locks.put(flag, rwlock);
              return rwlock;
          } finally {
              lock.unlock();
          }
      }

      private static Map<String, ReentrantLock> locks = new HashMap(  );
      private static ReadWriteLock lockr = new ReentrantReadWriteLock();

}
