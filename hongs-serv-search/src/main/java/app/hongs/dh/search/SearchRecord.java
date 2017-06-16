package app.hongs.dh.search;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.action.FormSet;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.util.Block;
import java.io.File;

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
public class SearchRecord extends LuceneRecord {

    private String dbname = null;

    public SearchRecord(String path, Map form) throws HongsException {
        super(path, form);
    }

    public SearchRecord(String path) throws HongsException {
        this (path, null);
    }

    public SearchRecord(  Map  form) throws HongsException {
        this (null, form);
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
     * 获取仓库名称
     * 通常为路径名
     * @return 
     */
    public String getBaseName() {
        if (null != dbname) {
            return  dbname;
        }
        String p = Core.DATA_PATH + "/lucene/";
        String d = getDataPath();
        if (! "/".equals (File.separator) ) {
            d = d.replace(File.separator, "/");
        }
        if (d.endsWith("/")) {
            d = d.substring(0,d.length()-1);
        }
        if (d.startsWith(p)) {
            d = d.substring(  p.length()  );
        }
        dbname= d;
        return  d;
    }

    @Override
    public void addDoc(final Document doc) throws HongsException {
        final SearchRecord that = this;
        final String key = SearchRecord.class.getName() + ":" + getBaseName();
        Block.locker(key , new Runnable( ) {
            @Override
            public void run() {
                try {
                    IndexWriter iw = that.getWriter( );
                    iw.addDocument (doc);
                    iw.commit();
                } catch (   IOException e) {
                    throw new HongsExpedient.Common(e);
                } catch (HongsException e) {
                    throw e.toExpedient( );
                }
            }
        });
    }

    @Override
    public void setDoc(final String id, final Document doc) throws HongsException {
        final SearchRecord that = this;
        final String key = SearchRecord.class.getName() + ":" + getBaseName();
        Block.locker(key , new Runnable( ) {
            @Override
            public void run() {
                try {
                    IndexWriter iw = that.getWriter( );
                    iw.updateDocument (new Term(Cnst.ID_KEY, id), doc);
                    iw.commit();
                } catch (   IOException e) {
                    throw new HongsExpedient.Common(e);
                } catch (HongsException e) {
                    throw e.toExpedient( );
                }
            }
        });
    }

    @Override
    public void delDoc(final String id) throws HongsException {
        final SearchRecord that = this;
        final String key = SearchRecord.class.getName() + ":" + getBaseName();
        Block.locker(key , new Runnable( ) {
            @Override
            public void run() {
                try {
                    IndexWriter iw = that.getWriter( );
                    iw.deleteDocuments(new Term(Cnst.ID_KEY, id) /**/);
                    iw.commit();
                } catch (   IOException e) {
                    throw new HongsExpedient.Common(e);
                } catch (HongsException e) {
                    throw e.toExpedient( );
                }
            }
        });
    }

}
