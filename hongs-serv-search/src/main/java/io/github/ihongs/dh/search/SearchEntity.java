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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;

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

    private Writer gotWriter() {
        if (WRITER == null) {
            final String name = getDbName();
            final String path = getDbPath();

            WRITER  = Core.GLOBAL_CORE.get(
                Writer.class.getName () + ":" + name ,
                new Supplier<Writer> () {
                    @Override
                    public Writer get() {
                        return new Writer(path, name);
                    }
                }
            );
        }
        return WRITER;
    }

    @Override
    public IndexWriter getWriter() throws HongsException {
        try {
            return gotWriter().getWriter();
        }
        catch (IOException ex) {
            throw new HongsException( ex );
        }
    }

    @Override
    public IndexReader getReader() throws HongsException {
        try {
            return gotWriter().getReader();
        }
        catch (IOException ex) {
            throw new HongsException( ex );
        }
    }

    @Override
    public IndexSearcher getFinder() throws HongsException {
        try {
            return gotWriter().getFinder();
        }
        catch (IOException ex) {
            throw new HongsException( ex );
        }
    }

    public void flush( ) {
        gotWriter().flush();
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
            WRITER  = null;
        }
    }

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
    public void commit() {
        super . commit();

        if (WRITES.isEmpty()) {
            return;
        }
        try {
            gotWriter().write(WRITES);
        }
        catch (IOException e ) {
            throw new HongsExemption(e, 1055);
        }
        finally {
            WRITES.clear();
            DOCK = null;
        }
    }

    @Override
    public void revert() {
        super . revert();

        // 清空内部缓存即可
        if (WRITES.isEmpty()) {
            return;
        } else {
            WRITES.clear();
            DOCK = null;
        }
    }

    @Override
    protected void permit(Map rd, Set ids, int ern)
    throws HongsException {
        /**
         * 遇到中途关闭情况再查一遍
         * 还那么倒霉只好就这样算了
         * 下同此
         */
        try {
            super.permit(rd, ids, ern);
        } catch (Halt e) {
            super.permit(rd, ids, ern);
        }
    }

    @Override
    public Map  search(Map rd)
    throws HongsException {
        try {
            return super.search(rd);
        } catch (Halt e) {
            return super.search(rd);
        }
    }

    @Override
    public Map  getOne(Map rd)
    throws HongsException {
        try {
            return super.getOne(rd);
        } catch (Halt e) {
            return super.getOne(rd);
        }
    }

    @Override
    public List getAll(Map rd)
    throws HongsException {
        try {
            return super.getAll(rd);
        } catch (Halt e) {
            return super.getAll(rd);
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

        // 查找对应文档, 遇错重查
        Document doc;
        try {
            doc = super.getDoc(id);
        } catch (Halt e) {
            doc = super.getDoc(id);
        }
        
        DOCK = doc;
        return doc;
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
    protected void preDoc(Document doc) {
        DOCK = doc;
    }

    /**
     * 读写单例
     *
     * 几个问题, 记录备忘:
     *
     * Q: 如果需要立即存盘并立即读到应该怎么办?
     * A: 注释上面 SearchEntity 的 getReader,getFinder 方法, 让其使用默认方法; 注释下方 Writer 中 write 的锁, 启用 iw.commit() 代码.
     *
     * Q: 更新标识 vary 为何用 volatile 而不用 AtomicBoolean?
     * A: 本打算把 flush 用锁包裹, 这样 vary 读写全在锁内, 经测试发现 flush 加锁耗时长, 不锁也没事, 而更新标识在 flush 后无需很精确.
     */
    private static class Writer implements AutoCloseable {

        private final ReentrantReadWriteLock WL = new ReentrantReadWriteLock();
        private final ReentrantReadWriteLock RL = new ReentrantReadWriteLock();
        private final ScheduledFuture flushs;
        private final ScheduledFuture merges;
        private final String   dbpath;
        private final String   dbname;
        private  IndexWriter   writer = null;
        private  IndexReader   reader = null;
        private  IndexSearcher finder = null;
        private volatile boolean vary = true;

        public Writer(String dbpath, String dbname) {
            this.dbname = dbname;
            this.dbpath = dbpath;

            Chore timer = Chore.getInstance();
            this.flushs = timer.runTimed ( () -> this.flush() ); // 间隔时间提交
            this.merges = timer.runDaily ( () -> this.merge() ); // 每天合并索引
        }

        private IndexWriter getWriter() throws IOException {
            WL.readLock().lock();
            try {
                if (writer != null && writer.isOpen()) {
                    return writer;
                }
            } finally {
                WL.readLock().unlock();
            }

            WL.writeLock().lock();
            try {
                if (writer != null && writer.isOpen()) {
                    return writer;
                }

                CoreConfig cc = CoreConfig.getInstance();
                IndexWriterConfig iwc = new IndexWriterConfig();
                iwc.setMaxBufferedDocs(cc.getProperty("core.lucene.max.buf.docs", -1 ));
                iwc.setRAMBufferSizeMB(cc.getProperty("core.lucene.ram.buf.size", 16D));
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                iwc.setCommitOnClose(true);

                Directory dir = FSDirectory.open(Paths.get(dbpath));

                writer = new IndexWriter(dir, iwc);
                if  (  ! new File(dbpath).exists()) {
                    writer.commit();
                }

                CoreLogger.trace("Start the lucene writer for {}", dbname);

                return writer;
            } finally {
                WL.writeLock().unlock();
            }
        }

        private IndexReader getReader() throws IOException {
            RL.readLock().lock();
            try {
                if (!vary && 0 < reader.getRefCount()) {
                    return reader;
                }
            } finally {
                RL.readLock().unlock();
            }

            RL.writeLock().lock();
            try {
                if (!vary && 0 < reader.getRefCount()) {
                    return reader;
                }

                getWriter();

                if (reader != null) {
                  IndexReader readar, readax;
                    readar  = DirectoryReader.openIfChanged
                      ( (DirectoryReader) reader , writer);
                if (readar != null) {
                    readax  = reader;
                    reader  = readar;
                    readax.close( ) ;
                    finder  = new  IndexSearcher  (reader);
                    vary    = false ;
                }} else {
                    reader  = DirectoryReader.open(writer);
                    finder  = new  IndexSearcher  (reader);
                    vary    = false ;
                }

                CoreLogger.trace("Start the lucene reader for {}", dbname);

                return reader;
            } finally {
                RL.writeLock().unlock();
            }
        }

        public IndexSearcher getFinder() throws IOException {
            getReader();

            return finder;
        }

        public void write(Map<String, Document> WRITES) throws IOException {
            IndexWriter iw = getWriter();

            RL.writeLock().lock();
            try {
                for(Map.Entry<String, Document> et : WRITES.entrySet()) {
                    String   id = et.getKey  ();
                    Document dc = et.getValue();
                    if (dc != null) {
                        iw.updateDocument (new Term("@"+Cnst.ID_KEY, id), dc);
                    } else {
                        iw.deleteDocuments(new Term("@"+Cnst.ID_KEY, id)    );
                    }
                }
            //  iw.commit();
                vary = true;
            } finally {
                RL.writeLock().unlock();
            }
        }

        public void flush() {
            try {
                if (writer == null
                || !writer.isOpen()) {
                    return;
                }

                long tt = System.currentTimeMillis();

                writer.commit();
                vary  =  true  ;

                tt = System.currentTimeMillis() - tt;
                CoreLogger.trace("Flush lucene indexes: {}, TC: {} ms", dbname, tt);
            } catch (IOException x) {
                CoreLogger.error(x);
            }
        }

        public void merge() {
            try {
                if (writer == null
                || !writer.isOpen()) {
                    return;
                }

                long tt = System.currentTimeMillis();

                writer.maybeMerge ( /**/ );
                writer.deleteUnusedFiles();

                tt = System.currentTimeMillis() - tt;
                CoreLogger.trace("Merge lucene indexes: {}, TC: {} ms", dbname, tt);
            } catch (IOException x) {
                CoreLogger.error(x);
            }
        }

        @Override
        public void close() {
            try {
                flushs.cancel(false);
                merges.cancel(true );

                if (writer != null
                &&  writer.isOpen()) {
                    writer.close ( );
                }
                if (reader != null ) {
                    reader.close ( );
                }

                CoreLogger.trace("Close the lucene writer for {}", dbname);
            } catch (IOException x) {
                CoreLogger.error(x);
            }
        }

    }

}
