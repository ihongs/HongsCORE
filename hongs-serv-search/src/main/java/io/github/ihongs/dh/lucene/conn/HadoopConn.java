package io.github.ihongs.dh.lucene.conn;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.util.daemon.Chore;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.NoLockFactory;

/**
 * Hadoop 分布式索引连
 * 内存索引 + HDFS 分布式索引, 定时或定量合并索引写入 HDFS
 * @author Hongs
 */
@Core.Singleton
public class HadoopConn implements Conn {

    @Core.Soliloquy
    public static class Getter implements ConnGetter {

        @Override
        public Conn get (String dbpath, String dbname) {
            return  Core.getInstance().got(Conn.class.getName() +":"+ dbname, () -> new CourseConn(
                    Core.getInterior().got(Conn.class.getName() +"|"+ dbname, () -> new HadoopConn(
                        dbpath, dbname
                    ))
                ));
        }

    }

    /**
     * 基于 HDFS 的 Lucene Directory 实现
     * 将索引文件存储在 HDFS 上, 支持分布式读取
     */
    public static class HdfsDirectory extends Directory {

        private final FileSystem fs;
        private final Path dirPath;
        private volatile boolean isOpen = true;

        public HdfsDirectory(FileSystem fs, Path dirPath) throws IOException {
            this.fs = fs;
            this.dirPath = dirPath;
            if (!fs.exists(dirPath)) {
                fs.mkdirs(dirPath);
            }
        }

        @Override
        public String[] listAll() throws IOException {
            ensureOpen();
            FileStatus[] stats = fs.listStatus(dirPath);
            String[] names = new String[stats.length];
            for (int i = 0; i < stats.length; i++) {
                names[i] = stats[i].getPath().getName();
            }
            return names;
        }

        @Override
        public void deleteFile(String name) throws IOException {
            ensureOpen();
            Path p = new Path(dirPath, name);
            if (fs.exists(p)) {
                fs.delete(p, false);
            }
        }

        @Override
        public long fileLength(String name) throws IOException {
            ensureOpen();
            return fs.getFileStatus(new Path(dirPath, name)).getLen();
        }

        @Override
        public IndexOutput createOutput(String name, IOContext context) throws IOException {
            ensureOpen();
            Path p = new Path(dirPath, name);
            OutputStream out = fs.create(p, true);
            return new HdfsIndexOutput(name, out);
        }

        @Override
        public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
            ensureOpen();
            String name = prefix + "_" + java.util.UUID.randomUUID().toString() + suffix;
            Path p = new Path(dirPath, name);
            OutputStream out = fs.create(p, true);
            return new HdfsIndexOutput(name, out);
        }

        @Override
        public IndexInput openInput(String name, IOContext context) throws IOException {
            ensureOpen();
            Path p = new Path(dirPath, name);
            return new HdfsIndexInput(name, fs.open(p), fs.getFileStatus(p).getLen());
        }

        @Override
        public void sync(Collection<String> names) throws IOException {
            ensureOpen();
            // HDFS 本身保证数据一致性, 无需额外 sync
        }

        @Override
        public void syncMetaData() throws IOException {
            ensureOpen();
            // HDFS 本身保证数据一致性, 无需额外 sync
        }

        @Override
        public void rename(String source, String dest) throws IOException {
            ensureOpen();
            Path src = new Path(dirPath, source);
            Path dst = new Path(dirPath, dest);
            fs.rename(src, dst);
        }

        @Override
        public void close() throws IOException {
            if (isOpen) {
                isOpen = false;
            }
        }

        @Override
        public java.util.Set<String> getPendingDeletions() throws IOException {
            return java.util.Collections.emptySet();
        }

        protected void ensureOpen() throws AlreadyClosedException {
            if (!isOpen) {
                throw new AlreadyClosedException("HdfsDirectory is closed: " + dirPath);
            }
        }

        @Override
        public Lock obtainLock(String name) throws IOException {
            HdfsLock lock = new HdfsLock(fs, dirPath, name);
            lock.obtain();
            return lock;
        }

        public FileSystem getFileSystem() {
            return fs;
        }

        public Path getDirPath() {
            return dirPath;
        }

    }

    /**
     * HDFS 上的 IndexOutput 实现
     */
    private static class HdfsIndexOutput extends IndexOutput {

        private final String name;
        private final OutputStream out;
        private long pos = 0;
        private final java.util.zip.CRC32 crc = new java.util.zip.CRC32();

        HdfsIndexOutput(String name, OutputStream out) {
            super(name, name);
            this.name = name;
            this.out  = out;
        }

        @Override
        public void writeByte(byte b) throws IOException {
            out.write(b);
            crc.update(b);
            pos += 1;
        }

        @Override
        public void writeBytes(byte[] b, int offset, int length) throws IOException {
            out.write(b, offset, length);
            crc.update(b, offset, length);
            pos += length;
        }

        @Override
        public long getFilePointer() {
            return pos;
        }

        @Override
        public long getChecksum() throws IOException {
            return crc.getValue();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }

    }

    /**
     * HDFS 上的 IndexInput 实现
     */
    private static class HdfsIndexInput extends IndexInput {

        private final String name;
        private final org.apache.hadoop.fs.FSDataInputStream in;
        private final long length;
        private boolean isClosed = false;

        HdfsIndexInput(String name, org.apache.hadoop.fs.FSDataInputStream in, long length) {
            super(name);
            this.name   = name;
            this.in     = in;
            this.length = length;
        }

        @Override
        public void close() throws IOException {
            if (!isClosed) {
                in.close();
                isClosed = true;
            }
        }

        @Override
        public long getFilePointer() {
            try {
                return in.getPos();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void seek(long pos) throws IOException {
            in.seek(pos);
        }

        @Override
        public long length() {
            return length;
        }

        @Override
        public byte readByte() throws IOException {
            return in.readByte();
        }

        @Override
        public void readBytes(byte[] b, int offset, int len) throws IOException {
            in.readFully(b, offset, len);
        }

        @Override
        public IndexInput slice(String sliceDescription, long offset, long length) throws IOException {
            return new HdfsSliceInput(sliceDescription, in, offset, length);
        }

    }

    /**
     * HDFS 切片读取
     */
    private static class HdfsSliceInput extends IndexInput {

        private final org.apache.hadoop.fs.FSDataInputStream in;
        private final long offset;
        private final long sliceLength;
        private long pos = 0;

        HdfsSliceInput(String desc, org.apache.hadoop.fs.FSDataInputStream in, long offset, long length) {
            super(desc);
            this.in = in;
            this.offset = offset;
            this.sliceLength = length;
        }

        @Override
        public void close() throws IOException {
            // 共享底层流, 不关闭
        }

        @Override
        public long getFilePointer() {
            return pos;
        }

        @Override
        public void seek(long p) throws IOException {
            pos = p;
        }

        @Override
        public long length() {
            return sliceLength;
        }

        @Override
        public byte readByte() throws IOException {
            in.seek(offset + pos);
            pos += 1;
            return in.readByte();
        }

        @Override
        public void readBytes(byte[] b, int off, int len) throws IOException {
            in.seek(offset + pos);
            in.readFully(b, off, len);
            pos += len;
        }

        @Override
        public IndexInput slice(String sliceDescription, long off, long length) throws IOException {
            return new HdfsSliceInput(sliceDescription, in, offset + off, length);
        }

    }

    /**
     * 基于 HDFS 文件的分布式锁
     * 利用 HDFS 的原子创建和删除实现跨进程互斥
     */
    private static class HdfsLock extends Lock {

        private final String lockName;
        private final FileSystem fs;
        private final Path dirPath;
        private Path lockPath;

        HdfsLock(FileSystem fs, Path dirPath, String name) {
            this.fs = fs;
            this.dirPath = dirPath;
            this.lockName = name;
        }

        @Override
        public void close() {
            if (lockPath != null) {
                try {
                    fs.delete(lockPath, false);
                } catch (IOException e) {
                    CoreLogger.trace("Failed to release HDFS lock {}", lockName);
                }
                lockPath = null;
            }
        }

        @Override
        public void ensureValid() throws IOException {
            if (lockPath == null || !fs.exists(lockPath)) {
                throw new AlreadyClosedException("HDFS lock lost: " + lockName);
            }
        }

        void obtain() throws IOException {
            Path lp = new Path(dirPath, lockName);
            try {
                // 原子创建, create with overwrite=false in HDFS is atomic
                if (fs.exists(lp)) {
                    throw new IOException("Lock already exists: " + lockName);
                }
                OutputStream out = fs.create(lp, false);
                out.write(Core.SERVER_ID.getBytes("UTF-8"));
                out.close();
                lockPath = lp;
            } catch (org.apache.hadoop.fs.FileAlreadyExistsException e) {
                throw new IOException("Lock already exists: " + lockName, e);
            }
        }

    }

    // ---- HadoopConn 构造与属性 ----

    public HadoopConn(String dbpath, String dbname) {
        this(dbpath, dbname, CoreConfig.getInstance());
    }

    public HadoopConn(String dbpath, String dbname, Properties cc) {
        // HDFS 路径: 相对路径拼接 core.hadoop.data.path, 绝对路径直接使用
        String dataPath = cc.getProperty("core.hadoop.data.path", "/hongs/data");
        if (!dbpath.startsWith("/")) {
            dbpath = dataPath + "/" + dbpath;
        }

        this.dbname = dbname;
        this.dbpath = dbpath;

        this.limit  = Integer.parseInt(cc.getProperty("core.lucene.merge.limit", "1000")); // 超量合并
        this.maxBufDocs = Integer.parseInt(cc.getProperty("core.lucene.max.buf.docs", "-1"));
        this.ramBufSize = Double.parseDouble(cc.getProperty("core.lucene.ram.buf.size", "16"));
        int  timed  = Integer.parseInt(cc.getProperty("core.lucene.merge.timed", "600" )); // 超时合并

        // 初始化 HDFS
        try {
            Configuration conf = new Configuration();
            // 使用 datanode hostname (localhost) 连接，方便宿主机访问
            conf.set("dfs.client.use.datanode.hostname", "true");
            String  uri = cc.getProperty("core.hadoop.hdfs.uri", "");
            if (!uri.isEmpty()) {
                this.hdfsFs = FileSystem.get(new java.net.URI(uri), conf);
            } else {
                this.hdfsFs = FileSystem.get(conf);
            }
            this.hdfsDir  = new HdfsDirectory(hdfsFs, new Path(dbpath));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HDFS for " + dbname, e);
        }

        final HadoopConn that = this;
        this.merger = new Runnable() {
            private volatile boolean running; // 不必精确, 内部有锁
            @Override
            public void run() {
                if (!running) {
                    try {
                        running = true ;
                        that . merge ();
                    } finally {
                        running = false;
                    }
                }
            }
        };
        this.merges = Chore.getInstance().ran(this.merger, timed, timed);
    }

    private final ReentrantReadWriteLock WL = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock RL = new ReentrantReadWriteLock();
    private final ScheduledFuture merges;
    private final Runnable   merger;
    private final String     dbpath;
    private final String     dbname;
    private final FileSystem hdfsFs;
    private final HdfsDirectory hdfsDir;
    private final int        maxBufDocs;  // 最大缓冲文档数
    private final double     ramBufSize;  // 内存缓冲大小 MB

    // 内存索引: 快速写入
    private  IndexWriter   writer = null;
    private  Directory     ramDir = null;
    // HDFS 索引: 分布式读取
    private  IndexReader   reader = null;
    private  IndexSearcher finder = null;
    private volatile boolean vary = true; // 变更标识
    private volatile int    count = 0;    // 合并计数
    private final    int    limit    ;    // 合并限定

    @Override
    public String getDbName() {
        return dbname;
    }

    @Override
    public String getDbPath() {
        return dbpath;
    }

    @Override
    public IndexWriter getWriter() throws IOException {
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

            IndexWriterConfig iwc = new IndexWriterConfig();
            iwc.setMaxBufferedDocs(maxBufDocs);
            iwc.setRAMBufferSizeMB(ramBufSize);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            iwc.setCommitOnClose(true);

            ramDir = new ByteBuffersDirectory(NoLockFactory.INSTANCE); // 内存目录不需要锁
            writer = new IndexWriter(ramDir, iwc);

            CoreLogger.trace("Start the lucene ram-writer for {}", dbname);

            return writer;
        } finally {
            WL.writeLock().unlock();
        }
    }

    @Override
    public IndexReader getReader() throws IOException {
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

            // 从 HDFS 目录打开读取器, 但如果有内存写入变化, 优先读取内存
            IndexReader readar = reader;
            if (writer != null && writer.isOpen() && vary && count > 0) {
                // 有未合并的写入, 从内存读取
                reader = DirectoryReader.open(writer);
            } else {
                try {
                    reader = DirectoryReader.open(hdfsDir);
                } catch (IOException e) {
                    // HDFS 目录尚无索引, 尝试从内存读取
                    if (writer != null && writer.isOpen()) {
                        reader = DirectoryReader.open(writer);
                    } else {
                        getWriter();
                        writer.commit(); // 空提交以建立索引结构
                        reader = DirectoryReader.open(hdfsDir);
                    }
                }
            }
            finder = new IndexSearcher(reader);

            // 释放旧的连接
            if (null != readar) try {
                    readar.decRef();
                if (readar.getRefCount() <= 0) {
                    readar.close ();
                    CoreLogger.trace("Close the lucene reader for {}", dbname);
                }
            } catch (AlreadyClosedException e) {
                // Pass
            }

            CoreLogger.trace("Start the lucene hdfs-reader for {}", dbname);

            vary = false ;
            return reader;
        } finally {
            RL.writeLock().unlock();
        }
    }

    @Override
    public IndexSearcher getFinder() throws IOException {
        getReader();

        return finder;
    }

    @Override
    public void write(Map<String, Document> docs) throws IOException {
        if (docs == null || docs.isEmpty()) {
            return;
        }

        RL.writeLock().lock();
        try {
            IndexWriter iw = getWriter();

            for(Map.Entry<String, Document> et : docs.entrySet()) {
                String   id = et.getKey  ();
                Document dc = et.getValue();
                if (dc != null) {
                    iw.updateDocument (new Term("@"+Cnst.ID_KEY, id), dc);
                } else {
                    iw.deleteDocuments(new Term("@"+Cnst.ID_KEY, id)    );
                }

                count += 1;
            }

            vary = true;

            // 超量合并, 后台执行
            if (count >= limit) {
                count  = 0;
                Chore.getInstance()
                     .exe( merger );
            }
        } finally {
            RL.writeLock().unlock();
        }
    }

    /**
     * 将内存索引合并到 HDFS
     * 加分布式锁, 防止多节点同时写入 HDFS
     */
    public void merge() {
        HdfsLock distLock = null;
        try {
        try {
            if (writer == null
            || !writer.isOpen()) {
                return;
            }

            // 获取 HDFS 分布式锁
            distLock = new HdfsLock(hdfsFs, hdfsDir.getDirPath(), "merge-" + dbname + ".lock");
            try {
                distLock.obtain();
            } catch (IOException e) {
                CoreLogger.trace("Cannot obtain HDFS merge lock for {}, skip", dbname);
                return;
            }

            // 先提交内存索引
            writer.commit();

            // 将内存索引合并到 HDFS
            IndexWriterConfig iwc = new IndexWriterConfig();
            iwc.setMaxBufferedDocs(maxBufDocs);
            iwc.setRAMBufferSizeMB(ramBufSize);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            iwc.setCommitOnClose(true);

            IndexWriter hdfsWriter = new IndexWriter(hdfsDir, iwc);
            try {
                hdfsWriter.addIndexes(ramDir);
                hdfsWriter.commit();

                CoreLogger.trace("Merge the lucene ram-index to hdfs for {}", dbname);
            } finally {
                hdfsWriter.close();
            }

            // 合并成功后清空内存索引
            writer.deleteAll();
            writer.commit();
            count = 0;
            vary = true;

        } catch (IOException x) {
            CoreLogger.error(x);
        }
        } catch ( Throwable x ) {
            x.printStackTrace();
        } finally {
            if (distLock != null) {
                distLock.close();
            }
        }
    }

    @Override
    public void close() {
        try {
        try {
            merges.cancel(false);

            // 关闭前合并到 HDFS
            if (writer != null
            &&  writer.isOpen()) {
                merge();
                writer.close ();
                writer  = null ;
            }

            if (reader != null ) {
                reader.close ();
                reader  = null ;
                finder  = null ;
            }

            if (hdfsDir != null) {
                hdfsDir.close();
            }

            CoreLogger.trace("Close the lucene hadoop-conn for {}", dbname);
        } catch (IOException x) {
            CoreLogger.error(x);
        }
        } catch ( Throwable x ) {
            x.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Lucene hadoop-conn " + dbname;
    }

}
