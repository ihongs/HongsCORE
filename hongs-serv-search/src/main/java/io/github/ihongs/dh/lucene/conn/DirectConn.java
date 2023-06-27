package io.github.ihongs.dh.lucene.conn;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.util.Syno;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * 简单直连
 * 提交立即写入磁盘
 * @author Hongs
 */
public class DirectConn implements Conn {

    public static class Getter implements ConnGetter, Core.Soliloquy {

        @Override
        public Conn get (String dbpath, String dbname) {
            return  Core.getInstance().got(
                    Conn.class.getName() +":"+ dbname,
                () -> new CourseConn(Core.GLOBAL_CORE.got(
                    Conn.class.getName() +"|"+ dbname,
                () -> new DirectConn( dbpath , dbname) ) )
            );
        }

    }

    private DirectConn (String dbpath, String dbname) {
        // 处理路径中的变量
        Map mm = new HashMap(3);
        mm.put("SERVER_ID", Core.SERVER_ID);
        mm.put("CORE_PATH", Core.CORE_PATH);
        mm.put("DATA_PATH", Core.DATA_PATH);
        dbpath = Syno.inject( dbpath , mm );
        if (!new File(dbpath).isAbsolute())
        dbpath = Core.DATA_PATH + "/lucene/" + dbpath;

        this.dbname = dbname;
        this.dbpath = dbpath;
    }

    private final ReentrantReadWriteLock WL = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock RL = new ReentrantReadWriteLock();
    private final String   dbpath;
    private final String   dbname;
    private  IndexWriter   writer = null;
    private  IndexReader   reader = null;
    private  IndexSearcher finder = null;
    private volatile boolean vary = true;

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

            CoreConfig cc = CoreConfig.getInstance();
            IndexWriterConfig iwc = new IndexWriterConfig();
        //  IndexWriterConfig iwc = new IndexWriterConfig(getAnalyzer());
            iwc.setMaxBufferedDocs(cc.getProperty("core.lucene.max.buf.docs", -1 ));
            iwc.setRAMBufferSizeMB(cc.getProperty("core.lucene.ram.buf.size", 16D));
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            iwc.setCommitOnClose(true);

            boolean   dix = new File(dbpath).exists();
            Directory dir = FSDirectory.open(Paths.get(dbpath));

            writer = new IndexWriter(dir, iwc);

            if (! dix) writer.commit(); // 初始化目录, 规避首次读报错

            CoreLogger.trace("Start the lucene writer for {}", dbname);

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

            // 目录不存在需开写并提交从而建立索引
            // 否则会抛出: IndexNotFoundException
            if (! new File(dbpath).exists()) {
                getWriter ( );
            }

            Directory dir = FSDirectory.open(Paths.get(dbpath));

            IndexReader readar = reader;
            reader = DirectoryReader.open(dir);
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

            /*
            // 已明确知道有写入, 无需再做判断
            if (reader != null) {
              IndexReader readar, readax;
                readar  = DirectoryReader.openIfChanged
                  ( (DirectoryReader) reader );
            if (readar != null) {
                readax  = reader;
                reader  = readar;
                finder  = new  IndexSearcher  (reader);
                readax.close( ) ;
                vary    = false ;
            }} else {
                reader  = DirectoryReader.open(writer);
                finder  = new  IndexSearcher  (reader);
                vary    = false ;
            }
            */

            CoreLogger.trace("Start the lucene reader for {}", dbname);

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
        IndexWriter iw = getWriter();

        RL.writeLock().lock();
        try {
            if (docs != null)
            for(Map.Entry<String, Document> et : docs.entrySet()) {
                String   id = et.getKey  ();
                Document dc = et.getValue();
                if (dc != null) {
                    iw.updateDocument (new Term("@"+Cnst.ID_KEY, id), dc);
                } else {
                    iw.deleteDocuments(new Term("@"+Cnst.ID_KEY, id)    );
                }
            }
            vary = true;
            iw.commit();
            iw.maybeMerge( );
            iw.deleteUnusedFiles( );
        } finally {
            RL.writeLock().unlock();
        }
    }

    @Override
    public void close() {
        try {
            if (writer != null
            &&  writer.isOpen()) {
                writer.close ();
                writer  = null ;
            }

            if (reader != null ) {
                reader.close ();
                reader  = null ;
                finder  = null ;
            }

            CoreLogger.trace("Close the lucene conn for {}", dbname);
        } catch (IOException x) {
            CoreLogger.error(x);
        }
    }

    @Override
    public String toString() {
        return "Lucene conn " + dbname;
    }

}
