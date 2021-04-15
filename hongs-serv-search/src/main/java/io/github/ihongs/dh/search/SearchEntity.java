package io.github.ihongs.dh.search;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.lucene.LuceneRecord;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import java.io.File;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * 搜索记录
 *
 * 增加写锁避免同时写入导致失败,
 * 默认退出时才会真的进行写操作.
 *
 * @author Hongs
 */
public class SearchEntity extends LuceneRecord {

    private final Map<String, Document> WRITES = new LinkedHashMap();
    private Writer WRITER = null ;
    private Document DOCK = null ;

    public SearchEntity(Map form , String path , String name) {
        super(form , path , name);
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
            Map    fxrm = FormSet.getInstance(conf).getForm(form);

            // 表单配置中可指定数据路径
            Map c = (Map) fxrm.get("@");
            if (c!= null) {
                String p;
                p = (String) c.get("db-path");
                if (null != p && p.length() != 0) {
                    path  = p;
                }
                p = (String) c.get("db-name");
                if (null != p && p.length() != 0) {
                    name  = p;
                }
            }

            // 进一步处理路径中的变量等
            Map m = new HashMap();
            m.put("SERVER_ID", Core.SERVER_ID);
            m.put("CORE_PATH", Core.CORE_PATH);
            m.put("DATA_PATH", Core.DATA_PATH);
            path = Syno.inject(path, m);
            if ( ! new File(path).isAbsolute())
            path = Core.DATA_PATH + "/lucene/" + path;

            SearchEntity inst = new SearchEntity(fxrm, path,name);
            core.put( code, inst ) ; return inst ;
        } else {
            return  (SearchEntity) core.got(code);
        }
    }

    @Override
    public IndexWriter getWriter() throws HongsException {
        /**
         * 依次检查当前对象和全部空间是否存在 SearchWriter,
         * 需从全局获取时调 SearchWriter.open 计数,
         * 当前实例退出时调 SearchWriter.exit 减掉,
         * 计数归零可被回收.
         */

        final String path = getDbPath();
        final String name = getDbName();
        final boolean[] b = new boolean[] {false};

        if (WRITER != null) {
            b[ 0 ]  = true;
        } else try {
            WRITER  = Core.GLOBAL_CORE.get(Writer.class.getName() + ":" + name,
            new Supplier<Writer> () {
                @Override
                public Writer get() {
                    b[ 0 ]  = true;

                    try {
                        IndexWriterConfig iwc = new IndexWriterConfig(getAnalyzer());
                        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

                        return new Writer(iwc, path, name);
                    } catch (HongsException x) {
                        throw x.toExemption( );
                    }
                }
            });
        } catch (HongsExemption x) {
            throw x.toException( );
        }

        // 首次进入无需计数
        if (b[0] == true) {
            return  WRITER.conn( );
        } else {
            return  WRITER.open( );
        }
    }

    @Override
    public void begin( ) {
        /**
         * 有尚未提交的变更
         * 又不在全局事务内
         */
        if (REFLUX_MODE
        && !WRITES.isEmpty()
        && !Synt.declare(Core.getInstance().got(Cnst.REFLUX_MODE), false)) {
            throw new HongsExemption(1054, "Uncommitted changes");
        }

        super . begin( );
    }

    @Override
    public void close( ) {
        super . close( );

        if (REFLUX_MODE) {
            try {
            try {
                commit();
            } catch (Throwable e) {
                revert();
                throw e ;
            }
            } catch (Throwable e) {
                CoreLogger.error(e);
            }
        }

        if (WRITER != null) {
            WRITER.exit( );
            WRITER  = null;
        }
    }

    @Override
    public void commit() {
        super . commit();

        if (WRITES.isEmpty()) {
            return;
        }
        try {
            IndexWriter iw = getWriter();
            synchronized (iw) {
                // 此处才会是真的更新文档
                for (Map.Entry<String, Document> et : WRITES.entrySet()) {
                    String   id = et.getKey  ();
                    Document dc = et.getValue();
                    if (dc != null) {
                        iw.updateDocument (new Term("@"+Cnst.ID_KEY, id), dc);
                    } else {
                        iw.deleteDocuments(new Term("@"+Cnst.ID_KEY, id)    );
                    }
                }
                iw.commit(  );
            }
        } catch (HongsException ex) {
            throw ex.toExemption( );
        } catch (   IOException ex) {
            throw new HongsExemption(1055, ex);
        } finally {
            WRITES.clear();
            DOCK = null;
        }
    }

    @Override
    public void revert() {
        super . revert();

        if (WRITES.isEmpty()) {
            return;
        }
        try {
            IndexWriter iw = getWriter();
            synchronized (iw) {
                iw.rollback();
            }
        } catch (HongsException ex) {
            throw ex.toExemption( );
        } catch (   IOException ex) {
            throw new HongsExemption(1056, ex);
        } finally {
            WRITES.clear();
            DOCK = null;
        }
    }

    @Override
    public void addDoc(Document doc)
    throws HongsException {
        String  id  =  doc . get( Cnst.ID_KEY );
        WRITES.put(id, doc);
        if (!REFLUX_MODE) {
            commit();
        }
    }

    @Override
    public void setDoc(String id, Document doc)
    throws HongsException {
        WRITES.put(id, doc);
        if (!REFLUX_MODE) {
            commit();
        }
    }

    @Override
    public void delDoc(String id)
    throws HongsException {
        WRITES.put(id,null);
        if (!REFLUX_MODE) {
            commit();
        }
    }

    @Override
    public Document getDoc(String id)
    throws HongsException {
        if (WRITES.containsKey(id)) {
            return  WRITES.get(id);
        }

        // 规避遍历更新时重复读取
        if (null != DOCK && id.equals(DOCK.get(Cnst.ID_KEY))) {
            return  DOCK ;
        }

        Document doc = super.getDoc(id);
        DOCK = doc;
        return doc;
    }

    @Override
    protected void preDoc(Document doc) {
        DOCK = doc;
    }

    private static class Writer implements AutoCloseable, Core.Cleanable, Core.Singleton {

        private final String            dbpath;
        private final String            dbname;
        private final IndexWriterConfig config;
        private       IndexWriter       writer;
        private       int               c = 1 ;

        public Writer(IndexWriterConfig config, String dbpath, String dbname) {
            this.config = config;
            this.dbpath = dbpath;
            this.dbname = dbname;

            init();

            CoreLogger.trace("Start the lucene writer for {}", dbname);
        }

        private void init() {
            try {
                Directory direct;
                direct = FSDirectory.open(Paths.get(dbpath));
                writer = new IndexWriter ( direct , config );
            } catch (IOException x) {
                throw new HongsExemption(x);
            }
        }

        synchronized public IndexWriter conn() {
            //  c += 0;
            if ( ! writer.isOpen() ) init(); // 重连
            return writer;
        }

        synchronized public IndexWriter open() {
                c += 1;
            if ( ! writer.isOpen() ) init(); // 重连
            return writer;
        }

        synchronized public void exit () {
            if (c >= 1) {
                c -= 1;
            }
        }

        @Override
        synchronized public byte clean() {
            if (c <= 0) {
                this . close( );
                return (byte) 1;
            } else {
                return (byte) 0;
            }
        }

        @Override
        synchronized public void close() {
            if (! writer.isOpen() ) {
                return;
            }

            // 退出时合并索引
            try {
                writer.maybeMerge();
            } catch (IOException x) {
                CoreLogger.error(x);
            }

            // 关闭后外部移除
            try {
                writer.close( );
            } catch (IOException x) {
                CoreLogger.error(x);
            }

            CoreLogger.trace("Close the lucene writer for {}", dbname);
        }

        @Override
        protected void finalize() throws Throwable {
            try {
                this  .   close();
            } finally {
                super .finalize();
            }
        }

    }

}
