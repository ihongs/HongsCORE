package io.github.ihongs.dh.search;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.lucene.LuceneRecord;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
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

    private SearchWriter WRITER = null;
    private final List<SearchTrnsct> WRITES = new LinkedList();

    public SearchEntity(Map form, String path, String name) {
        super(form , path , name);
    }

    public SearchEntity(Map form) {
        this (form , null , null);
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

        if (WRITER != null) {
            return  WRITER.conn( );
        }

        final String path = getDbPath();
        final String name = getDbName();
        final boolean[] b = new boolean[] {false};

        try {
            WRITER = Core.GLOBAL_CORE.get (SearchWriter.class.getName() + ":" + name,
            new Supplier<SearchWriter> () {
                @Override
                public SearchWriter get() {
                    IndexWriter writer;
                    b[0] = true;

                    try {
                        IndexWriterConfig iwc = new IndexWriterConfig(getAnalyzer());
                        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

                        Directory dir = FSDirectory.open(Paths.get(path));

                        writer = new IndexWriter(dir, iwc);
                    } catch (IOException x ) {
                        throw new HongsExemption.Common(x);
                    } catch (HongsException x) {
                        throw x.toExemption( );
                    }

                    return new SearchWriter(writer , name);
                }
            });
        } catch (HongsExemption x) {
            throw x.toException( );
        }

        // 首次进入无需计数
        if ( b[0] ) {
            return  WRITER.conn( );
        } else {
            return  WRITER.open( );
        }
    }

    @Override
    public void close( ) {
        super . close( );

        if (TRNSCT_MODE) {
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
                for (SearchTrnsct st : WRITES) {
                st.update(iw);
                }
                iw.commit(  );
            }
        } catch (HongsException ex) {
            throw ex.toExemption( );
        } catch (   IOException ex) {
            throw new HongsExemption(0x102d, ex);
        } finally {
            WRITES.clear();
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
            throw new HongsExemption(0x102d, ex);
        } finally {
            WRITES.clear();
        }
    }

    @Override
    public void addDoc(Document doc) throws HongsException {
        WRITES.add(new SearchTrnsct(SearchTrnsct.TYPE.ADD, null, doc));
        if (!TRNSCT_MODE) {
            commit();
        }
    }

    @Override
    public void setDoc(String id, Document doc) throws HongsException {
        WRITES.add(new SearchTrnsct(SearchTrnsct.TYPE.SET, new Term(Cnst.ID_KEY, id), doc ));
        if (!TRNSCT_MODE) {
            commit();
        }
    }

    @Override
    public void delDoc(String id) throws HongsException {
        WRITES.add(new SearchTrnsct(SearchTrnsct.TYPE.DEL, new Term(Cnst.ID_KEY, id), null));
        if (!TRNSCT_MODE) {
            commit();
        }
    }

    private static class SearchTrnsct {

        public  enum  TYPE     {ADD, SET, DEL};
        private final TYPE     type;
        private final Term     term;
        private final Document docu;

        public SearchTrnsct(TYPE type, Term term, Document dm) {
            this.type = type;
            this.term = term;
            this.docu = dm  ;
        }

        public void update (IndexWriter iw) throws IOException {
            switch( type ) {
                case ADD: iw.   addDocument (      docu); break;
                case SET: iw.updateDocument (term, docu); break;
                case DEL: iw.deleteDocuments(term      ); break;
            }
        }
    }

    private static class SearchWriter implements AutoCloseable, Core.Cleanable, Core.Singleton {

        private final IndexWriter writer;
        private final      String dbname;
        private  long             t = 0 ;
        private   int             c = 1 ;

        public SearchWriter(IndexWriter writer, String dbname) {
            this.writer = writer;
            this.dbname = dbname;

            if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                CoreLogger.trace("Start the lucene writer for " + dbname);
            }
        }

//      synchronized
        public IndexWriter conn() {
//          c += 0;
            return  writer;
        }

        synchronized
        public IndexWriter open() {
            c += 1;
            return  writer;
        }

        synchronized
        public void exit() {
            if (c >= 1) {
                c -= 1;
            }
            t = System.currentTimeMillis();
        }

        @Override
        public byte clean() {
            // 全部退出超过 1 分钟或已断开, 则可以清理了
            if (c <= 0 && t <= System.currentTimeMillis() - 60000
            ||  ! writer.isOpen( )) {
                return (byte) 1;
            } else {
                return (byte) 0;
            }
        }

        @Override
        public void close() {
            if (! writer.isOpen( )) {
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
                writer.close();
            } catch (IOException x) {
                CoreLogger.error(x);
            }

            if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                CoreLogger.trace("Close the lucene writer for " + dbname);
            }
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
