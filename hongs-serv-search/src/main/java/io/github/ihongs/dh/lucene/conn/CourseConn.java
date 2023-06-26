package io.github.ihongs.dh.lucene.conn;

import io.github.ihongs.CoreLogger;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.AlreadyClosedException;

/**
 * 过程容器
 * 结束时可关闭不再用的连接
 * 注意: 此类为非线程安全的
 * @author Hongs
 */
public class CourseConn implements Conn {

    private final Conn conn;
    private final Set<IndexReader> reades;

    public CourseConn(Conn conn) {
        this.conn = conn;
        this.reades = new HashSet();
    }

    @Override
    public String getDbName() {
        return conn.getDbName();
    }

    @Override
    public String getDbPath() {
        return conn.getDbPath();
    }

    @Override
    public IndexWriter getWriter() throws IOException {
        return conn.getWriter();
    }

    @Override
    public IndexReader getReader() throws IOException {
        IndexReader reader = conn.getReader();
        if (reades.add(reader)) {
            reader.incRef();
        }
        return reader;
    }

    @Override
    public IndexSearcher getFinder() throws IOException {
      IndexSearcher finder = conn.getFinder();
        IndexReader reader = finder.getIndexReader();
        if (reades.add(reader)) {
            reader.incRef();
        }
        return finder;
    }

    @Override
    public void write(Map<String, Document> writes) throws IOException {
        conn.write(writes);
    }

    @Override
    public void close() throws Exception {
        for(IndexReader reader : reades) {
            try {
                    reader.decRef();
                if (reader.getRefCount() <= 0) {
                    reader.close ();
                    CoreLogger.trace("Close the lucene reader for {}", conn.getDbName());
                }
            } catch (AlreadyClosedException e) {
                // Pass
            }
        }
        reades.clear();
    }

}
