package io.github.ihongs.dh.search;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.lucene.LuceneRecord;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.daemon.Chore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Supplier;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
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
    private Reader READER = null ;
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
        String code = SearchEntity.class.getName() +":"+ conf +":"+ form;
        Core   core = Core.getInstance( );
        SearchEntity  inst = (SearchEntity) core.get(code);
        if (inst == null) {
            String path = conf +"/"+ form;
            String name = conf +":"+ form;
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
            path = Core.DATA_PATH+ "/lucene/" + path ;

            inst = new SearchEntity(fxrm, path, name);
            core.set(code , inst);
        }
        return inst;
    }

    @Override
    public IndexSearcher getFinder() throws HongsException {
        getReader();
        return READER.find();
    }

    @Override
    public IndexReader getReader() throws HongsException {
        /**
         * 依次检查当前对象和全部空间是否存在 Reader,
         * 需从全局获取时调 Reader.open 计数,
         * 当前实例退出时调 Reader.exit 减掉,
         * 计数归零可被回收.
         */

        final SearchEntity  that = this;
        final String path = getDbPath();
        final String name = getDbName();
        final boolean[] b = new boolean[] {false};

        if (READER != null) {
            b[ 0 ]  = true;
        } else try {
            READER  = Core.GLOBAL_CORE.get(
                Reader.class.getName () + ":" + name ,
                new Supplier<Reader> () {
                    @Override
                    public Reader get() {
                        b[ 0 ]  = true;

                        // 目录不存在需开写并提交从而建立索引
                        // 否则会抛出: IndexNotFoundException
                        try {
                            if (! new File(path).exists()) {
                                that.getWriter().commit();
                            }
                        } catch (HongsException e) {
                            throw e.toExemption( );
                        } catch (   IOException e) {
                            throw new HongsExemption( e );
                        }

                        return new Reader(path, name);
                    }
                }
            );
        } catch (HongsExemption x) {
            throw x.toException( );
        }

        try {
            // 首次或重复调用无需计数
            if (b[0] == true) {
                return  READER.conn( );
            } else {
                return  READER.open( );
            }
        } catch (HongsExemption x) {
            throw x.toException( );
        }
    }

    @Override
    public IndexWriter getWriter() throws HongsException {
        /**
         * 依次检查当前对象和全部空间是否存在 Writer,
         * 需从全局获取时调 Writer.open 计数,
         * 当前实例退出时调 Writer.exit 减掉,
         * 计数归零可被回收.
         */

        final String path = getDbPath();
        final String name = getDbName();
        final boolean[] b = new boolean[] {false};

        if (WRITER != null) {
            b[ 0 ]  = true;
        } else try {
            WRITER  = Core.GLOBAL_CORE.get(
                Writer.class.getName () + ":" + name ,
                new Supplier<Writer> () {
                    @Override
                    public Writer get() {
                        b[ 0 ]  = true;

                        return new Writer(path, name);
                    }
                }
            );
        } catch (HongsExemption x) {
            throw x.toException( );
        }

        try {
            // 首次或重复调用无需计数
            if (b[0] == true) {
                return  WRITER.conn( );
            } else {
                return  WRITER.open( );
            }
        } catch (HongsExemption x) {
            throw x.toException( );
        }
    }

    /*
    public Gate.Locker getLocker() {
        if (WRITER != null) {
            return WRITER.lock( );
        }
        return Gate.getLocker (Writer.class.getName() +":"+ getDbName());
    }
    */

    @Override
    public void begin( ) {
        /**
         * 有尚未提交的变更
         * 又不在全局事务内
         */
        if (REFLUX_MODE
        && !WRITES.isEmpty()) {
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

        if (READER != null) {
            READER.exit( );
            READER  = null;
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
        //  Gate.Locker lk = getLocker();

        //  long tt = CoreConfig.getInstance().getProperty("core.try.lock.save.timeout" , 0L);
        //  if ( tt > 0L ) {
        //      if (! lk.tryLock(tt , TimeUnit.MILLISECONDS)) {
        //          throw new HongsExemption(861, "Lucene try to commit timeout(${0}ms)", tt);
        //      }
        //  } else {
        //      lk.lockInterruptibly();
        //  }

            // 此处才是真更新文档
        //  try {
                for (Map.Entry<String, Document> et : WRITES.entrySet()) {
                    String   id = et.getKey  ();
                    Document dc = et.getValue();
                    if (dc != null) {
                        iw.updateDocument (new Term("@"+Cnst.ID_KEY, id), dc);
                    } else {
                        iw.deleteDocuments(new Term("@"+Cnst.ID_KEY, id)    );
                    }
                }
                iw.commit();
        //  } finally {
        //      lk.unlock();
        //  }
        }
        catch (HongsException e) {
            throw e.toExemption( );
        }
        catch ( IOException e) {
            throw new HongsExemption(e, 1055);
        }/*
        catch ( InterruptedException e) {
            throw new HongsExemption(e, 860 );
        }*/
        finally {
            WRITES.clear();
            DOCK = null;
        }
    }

    @Override
    public void revert() {
        super . revert();

        if (WRITES.isEmpty()) {
            return;
        } else {
            // 此为缓存, 清空即可
            WRITES.clear();
            DOCK = null;
        }
    }

    @Override
    public void addDoc(String id, Document doc)
    throws HongsException {
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

    private static class Reader implements AutoCloseable, Core.Singleton {

        private final String dbpath;
        private final String dbname;
        private  IndexReader reader;
        private  IndexSearcher finder;
        private volatile int  c = 1;
        private volatile long t = 0;

        public Reader(String dbpath, String dbname) {
            this.dbname = dbname;
            this.dbpath = dbpath;

            this.reader = null;
            this.finder = null;

            init();
        }

        private void init() {
            if (reader != null) {
                try {
                    // 如果有更新数据则会重新打开查询接口
                    // 这可以规避提交更新后却查不到的问题
                    IndexReader  nred = DirectoryReader.openIfChanged((DirectoryReader) reader);
                    if ( null != nred) {
                    //  reader.close(); // 不要关, 其他线程可能在用, 其内引用计数, 关不关无所谓
                        reader = nred ;
                        finder = null ;
                    }
                } catch (IOException x) {
                    throw new HongsExemption(x);
                }
            } else {
                try {
                    Directory dir = FSDirectory.open(Paths.get(dbpath));

                    reader = DirectoryReader.open(dir);
                } catch (IOException x) {
                    throw new HongsExemption(x);
                }

                CoreLogger.trace("Start the lucene reader for {}", dbname);
            }
        }

        synchronized public IndexSearcher find() {
            if (null != finder) {
                return  finder;
            }
        //  c += 1;
            init();
            finder = new IndexSearcher(reader);
            return finder;
        }

        synchronized public IndexReader conn() {
        //  c += 1;
            init();
            return reader;
        }

        synchronized public IndexReader open() {
            c += 1;
            init();
            return reader;
        }

        synchronized public void exit () {
            if (c >= 1) {
                c -= 1;
                t  = System.currentTimeMillis();
            }
        }

        synchronized public void clean() {
            if (c >= 1 || t > System.currentTimeMillis() - 3600000) { //  // 一小时内有用则不关闭
                return;
            }
            c  =  0;
            close();
        }

        @Override
        public void close() {
            if (reader != null ) {
                try {
                    reader.close();
                } catch (IOException x) {
                    CoreLogger.error(x);
                } finally {
                    reader = null ;
                    finder = null ;
                }
            }
            CoreLogger.trace("Close the lucene reader for {}", dbname);
        }

    }

    private static class Writer implements AutoCloseable, Core.Singleton {

        private final ScheduledFuture cleans;
        private final ScheduledFuture merges;
        private final String dbpath;
        private final String dbname;
    //  private final String lkname;
        private  IndexWriter writer;
        private volatile int  c = 1;
        private volatile long t = 0;

        public Writer(String dbpath, String dbname) {
            Chore timer = Chore.getInstance();
            this.cleans = timer.runTimed ( () -> this.clean() );
            this.merges = timer.runDaily ( () -> this.merge() );

        //  this.lkname = Writer.class.getName() + ":" + dbname;
            this.dbname = dbname;
            this.dbpath = dbpath;

            init();
        }

        private void init() {
            try {
                CoreConfig cc = CoreConfig.getInstance();
                IndexWriterConfig iwc = new IndexWriterConfig();
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                iwc.setMaxBufferedDocs(cc.getProperty("core.lucene.max.buf.docs", -1 ));
                iwc.setRAMBufferSizeMB(cc.getProperty("core.lucene.ram.buf.size", 16D));

                Directory dir = FSDirectory.open(Paths.get(dbpath));

                writer = new IndexWriter(dir, iwc);
            } catch (IOException x) {
                throw new HongsExemption(x);
            }

            CoreLogger.trace("Start the lucene writer for {}", dbname);
        }

        /*
        public Gate.Locker lock() {
            return Gate.getLocker( lkname );
        }
        */

        synchronized public IndexWriter conn() {
            //  c += 1;
            if ( ! writer.isOpen() ) {
                init();
            }
            return writer;
        }

        synchronized public IndexWriter open() {
                c += 1;
            if ( ! writer.isOpen() ) {
                init();
            }
            return writer;
        }

        synchronized public void exit () {
            if (c >= 1) {
                c -= 1;
                t  = System.currentTimeMillis();
            }
        }

        synchronized public void clean() {
            if (c >= 1 || t > System.currentTimeMillis() - 3600000) { // 一小时内有用则不关闭
                return;
            }
            c  =  0;
            cloze();
        }

        @Override
        public void close() {
            cloze();
            cleans.cancel(false);
            merges.cancel(true );
        }

        public void cloze() {
        //  Gate.Locker lk = lock();
            try {
        //      lk.lockInterruptibly();
                if (! writer.isOpen()) {
                    return;
                }
                writer.close( );
            } catch (IOException x) {
                CoreLogger.error(x);
        //  } catch (InterruptedException x) {
        //      CoreLogger.error(x);
        //  } finally {
        //      lk.unlock();
            }

            CoreLogger.trace("Close the lucene writer for {}", dbname);
        }

        public void merge() {
            long t = System.currentTimeMillis();

        //  Gate.Locker lk = lock();
            try {
        //      lk.lockInterruptibly();
                if (! writer.isOpen()) {
                    init();
                }
                writer.maybeMerge();
                writer.deleteUnusedFiles();
            } catch (IOException x) {
                CoreLogger.error(x);
        //  } catch (InterruptedException x) {
        //      CoreLogger.error(x);
        //  } finally {
        //      lk.unlock();
            }

            t = System.currentTimeMillis() - t ;
            CoreLogger.trace("Merge lucene indexes: {} {}", dbname, t);
        }

    }

}
