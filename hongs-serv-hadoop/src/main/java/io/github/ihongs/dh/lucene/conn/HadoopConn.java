package io.github.ihongs.dh.lucene.conn;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.util.Syno;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
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
import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

/**
 * Hadoop 连接实现
 * 将 Lucene 索引存储到 Hadoop HDFS
 *
 * <pre>
 * 配置示例:
 * core.lucene.conn.getter.class=io.github.ihongs.dh.lucene.conn.HadoopConn$Getter
 *
 * Hadoop 配置:
 * core.hadoop.fs.defaultFS=hdfs://localhost:9000
 * core.hadoop.data.path=/hongs/indexes
 * core.hadoop.dfs.replication=2
 * core.hadoop.dfs.blocksize=134217728
 * core.hadoop.ram.buf.size=16
 * core.hadoop.max.buf.docs=-1
 * </pre>
 *
 * @author Hongs
 */
@Core.Singleton
public class HadoopConn implements Conn {

    @Core.Soliloquy
    public static class Getter implements ConnGetter {

        @Override
        public Conn get(String dbpath, String dbname) {
            return  Core.getInstance().got(Conn.class.getName() + ":" + dbname, () -> new CourseConn(
                    Core.getInterior().got(Conn.class.getName() + "|" + dbname, () -> new HadoopConn(
                            dbpath, dbname
                    ))
            ));
        }

    }

    private HadoopConn(String dbpath, String dbname) {
        String dataPath = CoreConfig.getInstance().getProperty("core.hadoop.data.path", "");

        Map mm = new HashMap(2);
        mm.put("SERVER_ID" , Core.SERVER_ID);
        mm.put("DATA_PATH" , dataPath  );
        dbpath = Syno.inject(dbpath, mm);

        if (! dbpath.startsWith("/")
        &&  ! dbpath.startsWith("hdfs://")
        &&  ! dbpath.startsWith("file://") ) {
            dbpath = dataPath + "/" + dbpath;
        }

        this.dbname = dbname;
        this.dbpath = dbpath;
    }

    private final ReentrantReadWriteLock WL = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock RL = new ReentrantReadWriteLock();
    private final String dbpath;
    private final String dbname;
    private IndexWriter writer = null;
    private IndexReader reader = null;
    private IndexSearcher finder = null;
    private HdfsDirectory dfsDir = null;
    private volatile boolean vary = true;

    @Override
    public String getDbName() {
        return dbname;
    }

    @Override
    public String getDbPath() {
        return dbpath;
    }

    private HdfsDirectory getDirectory() throws IOException {
        if (dfsDir == null) {
            // 配置 hadoop 连接参数
            Configuration conf = new Configuration();
            CoreConfig cc = CoreConfig.getInstance();

            String fsDefault = cc.getProperty("core.hadoop.fs.defaultFS");
            if (fsDefault != null && !fsDefault.isEmpty()) {
                conf.set("fs.defaultFS", fsDefault);
            }

            String nnAddr = cc.getProperty("core.hadoop.dfs.namenode.http-address");
            if (nnAddr != null && !nnAddr.isEmpty()) {
                conf.set("dfs.namenode.http-address", nnAddr);
            }

            String replication = cc.getProperty("core.hadoop.dfs.replication");
            if (replication != null && !replication.isEmpty()) {
                conf.set("dfs.replication", replication);
            }

            String blockSize = cc.getProperty("core.hadoop.dfs.blocksize");
            if (blockSize != null && !blockSize.isEmpty()) {
                conf.set("dfs.blocksize", blockSize);
            }

            // ZooKeeper 配置传递
            String zkConnect = cc.getProperty("core.zookeeper.connect");
            String zkTimeout = cc.getProperty("core.zookeeper.timeout");
            if (zkConnect != null && !zkConnect.isEmpty()) {
                conf.set( "zookeeper.connect", zkConnect );
            }
            if (zkTimeout != null && !zkTimeout.isEmpty()) {
                conf.set( "zookeeper.timeout", zkTimeout );
            }
            conf.set( "zookeeper.lock.root", "/hongs/locks/" + dbname );

            dfsDir = new HdfsDirectory(new Path(dbpath), conf);
        }
        return dfsDir;
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

            CoreConfig cc = CoreConfig.getInstance();
            IndexWriterConfig iwc = new IndexWriterConfig();
            iwc.setRAMBufferSizeMB(cc.getProperty("core.hadoop.ram.buf.size", 16D));
            iwc.setMaxBufferedDocs(cc.getProperty("core.hadoop.max.buf.docs", -1 ));
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            iwc.setCommitOnClose(true);

            HdfsDirectory dir = getDirectory();
            writer = new IndexWriter( dir, iwc );

            CoreLogger.trace("Start the lucene writer for {} on HDFS", dbname);

            return writer;
        } finally {
            WL.writeLock().unlock();
        }
    }

    @Override
    public IndexReader getReader() throws IOException {
        RL.readLock().lock();
        try {
            if (!vary && reader != null && 0 < reader.getRefCount()) {
                return reader;
            }
        } finally {
            RL.readLock().unlock();
        }

        RL.writeLock().lock();
        try {
            if (!vary && reader != null && 0 < reader.getRefCount()) {
                return reader;
            }

            getWriter();

            IndexReader readar = reader;
            reader = DirectoryReader.open(writer);
            finder = new IndexSearcher(reader);

            if (null != readar) {
                try {
                    readar.decRef();
                    if (readar.getRefCount() <= 0) {
                        readar.close();
                        CoreLogger.trace("Close the lucene reader for {}", dbname);
                    }
                } catch (AlreadyClosedException e) {
                    // Pass
                }
            }

            CoreLogger.trace("Start the lucene reader for {} on HDFS", dbname);

            vary = false;
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

            for (Map.Entry<String, Document> et : docs.entrySet()) {
                String id = et.getKey();
                Document dc = et.getValue();
                if (dc != null) {
                    iw.updateDocument (new Term("@" + Cnst.ID_KEY, id), dc);
                } else {
                    iw.deleteDocuments(new Term("@" + Cnst.ID_KEY, id));
                }
            }

            vary = true;
            iw.commit();
        } finally {
            RL.writeLock().unlock();
        }
    }

    @Override
    public void close() {
        try {
            if (writer != null && writer.isOpen()) {
                writer.close();
                writer = null;
            }

            if (reader != null) {
                reader.close();
                reader = null;
                finder = null;
            }

            if (dfsDir != null) {
                dfsDir.close();
                dfsDir = null;
            }

            CoreLogger.trace("Close the lucene conn for {} on HDFS", dbname);
        } catch (IOException x) {
            CoreLogger.error(x);
        }
    }

    @Override
    public String toString() {
        return "Hadoop conn " + dbname;
    }

    /**
     * 基于 HDFS 的 Lucene Directory 实现
     */
    private static class HdfsDirectory extends BaseDirectory {

        private final FileSystem fs;
        private final Path path;

        public HdfsDirectory(Path path, Configuration conf) throws IOException {
            super(new ZkLockFactory(conf));

            this.path  =  path ;
            this.fs = path.getFileSystem( conf );
            if (fs.exists(path) == false) {
                fs.mkdirs(path);
            }
        }

        @Override
        public String[] listAll() throws IOException {
            ensureOpen();
            FileStatus[] statuses = fs.listStatus(path);
            String[] names = new String[statuses.length];
            for (int i = 0; i < statuses.length; i++) {
                names[i] = statuses[i].getPath().getName();
            }
            return names;
        }

        @Override
        public long fileLength(String name) throws IOException {
            ensureOpen();
            Path filePath = new Path(path, name);
            if (!fs.exists(filePath)) {
                throw new IOException("File not found: " + name);
            }
            return fs.getFileStatus(filePath).getLen();
        }

        @Override
        public void deleteFile(String name) throws IOException {
            ensureOpen();
            fs.delete(new Path(path, name), false);
        }

        @Override
        public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
            ensureOpen();
            Path tempPath = new Path(path, prefix + "-" + System.currentTimeMillis() + suffix);
            FSDataOutputStream out = fs.create(tempPath, true);
            return new HdfsIndexOutput(out, tempPath.getName());
        }

        @Override
        public IndexOutput createOutput(String name, IOContext context) throws IOException {
            ensureOpen();
            Path filePath = new Path(path, name);
            FSDataOutputStream out = fs.create(filePath, true);
            return new HdfsIndexOutput(out, name);
        }

        @Override
        public IndexInput openInput(String name, IOContext context) throws IOException {
            ensureOpen();
            Path filePath = new Path(path, name);
            FSDataInputStream in = fs.open(filePath);
            return new HdfsIndexInput(in, name, fileLength(name));
        }

        @Override
        public Set<String> getPendingDeletions() throws IOException {
            ensureOpen();
            return Set.of();
        }

        @Override
        public void rename(String source, String dest) throws IOException {
            ensureOpen();
            fs.rename(new Path(path, source), new Path(path, dest));
        }

        @Override
        public void sync(Collection<String> names) throws IOException {
            ensureOpen();
            for (String name : names) {
                Path filePath = new Path(path, name);
                try (FSDataOutputStream out = fs.create(filePath, true)) {
                    out.hflush();
                }
            }
        }

        @Override
        public void syncMetaData() throws IOException {
            ensureOpen();
        }

        @Override
        public void close() throws IOException {
        }
    }

    /**
     * HDFS IndexOutput 实现
     */
    private static class HdfsIndexOutput extends IndexOutput {

        private final FSDataOutputStream out;

        public HdfsIndexOutput(FSDataOutputStream out, String name) {
            super("HadoopIndexOutput(path=\"" + name + "\")", name);
            this.out = out;
        }

        @Override
        public void writeByte(byte b) throws IOException {
            out.writeByte(b);
        }

        @Override
        public void writeBytes(byte[] b, int offset, int length) throws IOException {
            out.write(b, offset, length);
        }

        public void flush() throws IOException {
            out.hflush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }

        @Override
        public long getFilePointer() {
            return out.getPos();
        }

        @Override
        public long getChecksum() throws IOException {
            return 0;
        }
    }

    /**
     * HDFS IndexInput 实现
     */
    private static class HdfsIndexInput extends IndexInput {

        private final FSDataInputStream in;
        private final long length;

        public HdfsIndexInput(FSDataInputStream in, String name, long length) {
            super("HadoopIndexInput(path=\"" + name + "\")");
            this.in = in;
            this.length = length;
        }

        @Override
        public void close() throws IOException {
            in.close();
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
        public long length() {
            return length;
        }

        @Override
        public void seek(long pos) throws IOException {
            in.seek(pos);
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
            return new HdfsIndexInput(in, sliceDescription, length);
        }
    }

    /**
     * 基于 ZooKeeper 的分布式锁工厂
     * 使用 ZooKeeper 临时节点实现，进程异常终止时锁自动释放
     */
    private static class ZkLockFactory extends LockFactory {

        private final ZooKeeper zk;
        private final String lockRoot;

        public ZkLockFactory(Configuration conf) throws IOException {
            int timeout = conf.getInt("zookeeper.timeout", 30000);
            String connect = conf.get("zookeeper.connect", "localhost:2181");
            this.lockRoot = conf.get("zookeeper.lock.root", "/hongs/locks");

            try {
                // 使用 CountDownLatch 等待连接建立
                final CountDownLatch connectedLatch = new CountDownLatch(1);
                this.zk = new org.apache.zookeeper.ZooKeeper(connect, timeout, new org.apache.zookeeper.Watcher() {
                    @Override
                    public void process(org.apache.zookeeper.WatchedEvent event) {
                        if (event.getState() == org.apache.zookeeper.Watcher.Event.KeeperState.SyncConnected) {
                            connectedLatch.countDown();
                        }
                    }
                });

                // 等待连接建立
                if (!connectedLatch.await(timeout, TimeUnit.MILLISECONDS)) {
                    throw new IOException("ZooKeeper connection timeout");
                }

                // 创建锁根目录（如果不存在）
                createPathIfNotExists(this.lockRoot);
            } catch (Exception e) {
                throw new IOException("Failed to connect to ZooKeeper", e);
            }
        }

        private void createPathIfNotExists(String path) throws KeeperException, InterruptedException {
            if (zk.exists(path, false) == null) {
                // 递归创建父目录
                String[] parts = path.split("/");
                StringBuilder current = new StringBuilder();
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        current.append("/").append(part);
                        if (zk.exists(current.toString(), false) == null) {
                            zk.create(current.toString(), new byte[0],
                                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                CreateMode.PERSISTENT
                            );
                        }
                    }
                }
            }
        }

        @Override
        public Lock obtainLock(Directory dir, String lockName) throws IOException {
            String lockPath = lockRoot + "/" + lockName;
            ZkLock lock = new ZkLock(zk, lockPath);
            lock.obtain();  // 实际获取锁
            return lock;
        }
    }

    /**
     * 基于 ZooKeeper 的分布式锁实现
     * 使用临时节点，进程异常终止时自动释放锁
     */
    private static class ZkLock extends Lock {

        private final ZooKeeper zk;
        private final String lockPath;
        private volatile boolean isLocked = false;

        public ZkLock(ZooKeeper zk, String lockPath) {
            this.zk = zk;
            this.lockPath = lockPath;
        }

        public void obtain() throws IOException {
            if (isLocked) {
                return;
            }

            try {
                // 创建临时节点获取锁
                // EPHEMERAL: 会话断开时自动删除
                zk.create(lockPath, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL
                );
                isLocked = true;
            } catch (KeeperException.NodeExistsException e) {
                throw new IOException("Lock already held: " + lockPath, e);
            } catch (Exception e) {
                throw new IOException("Failed to obtain lock", e);
            }
        }

        @Override
        public void close() throws IOException {
            if (isLocked) {
                try {
                    if (zk.exists(lockPath, false) != null) {
                        zk.delete(lockPath, -1);
                    }
                } catch (Exception e) {
                    // 忽略删除失败（可能已自动过期）
                } finally {
                    isLocked = false;
                }
            }
        }

        @Override
        public void ensureValid() throws IOException {
            if (isLocked) {
                try {
                    if (zk.exists(lockPath, false) == null) {
                        isLocked = false;
                        throw new IOException("Lock not valid: " + lockPath);
                    }
                } catch (Exception e) {
                    throw new IOException("Failed to check lock", e);
                }
            } else {
                throw new IOException("Lock not held");
            }
        }
    }

}
