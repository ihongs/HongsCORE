package io.github.ihongs.dh.lucene.conn;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.daemon.Chore;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
 * 近实时连
 * 间隔时间写入磁盘
 * @author Hongs
 */
public class FlashyConn implements Conn, Core.Singleton {

    public static class Getter implements ConnGetter, Core.Soliloquy {

        @Override
        public Conn get(String dbpath, String dbname) {
            return Core.GLOBAL_CORE.get(
                Conn.class.getName () + ":" + dbname,
                new Supplier <Conn>() {
                    @Override
                    public Conn get() {
                        String mypath = dbpath;
                        String myname = dbname;

                        // 处理路径中的变量
                        Map mm = new HashMap();
                        mm.put("SERVER_ID", Core.SERVER_ID);
                        mm.put("CORE_PATH", Core.CORE_PATH);
                        mm.put("DATA_PATH", Core.DATA_PATH);
                        mypath = Syno.inject( mypath , mm );
                        if (!new File(dbpath).isAbsolute())
                        mypath = Core.DATA_PATH + "/lucene/" + mypath;

                        return new FlashyConn(mypath, myname);
                    }
                }
            );
        }

    }

    private FlashyConn (String dbpath, String dbname) {
        this.dbname = dbname;
        this.dbpath = dbpath;

        Chore timer = Chore.getInstance();
        this.flushs = timer.runTimed ( () -> this.flush() ); // 间隔时间提交
        this.merges = timer.runDaily ( () -> this.merge() ); // 每天合并索引
    }

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

            getWriter();

            IndexReader readar = reader;
            reader = DirectoryReader.open(writer);
            finder = new  IndexSearcher  (reader);
            if (null != readar ) readar.close ( );

            /*
            // 已明确知道有写入, 无需再做判断
            if (reader != null) {
              IndexReader readar, readax;
                readar  = DirectoryReader.openIfChanged
                  ( (DirectoryReader) reader , writer);
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
        //  iw.commit(); // 无需立即提交
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

            writer.maybeMerge();
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
                writer.close ();
                writer  = null ;
            }
            if (reader != null ) {
                reader.close ();
                reader  = null ;
                finder  = null ;
            }

            CoreLogger.trace("Close the lucene writer for {}", dbname);
        } catch (IOException x) {
            CoreLogger.error(x);
        }
    }

}
