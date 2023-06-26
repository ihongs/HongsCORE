package io.github.ihongs.dh.lucene.conn;

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;

/**
 * 仓库连接
 * @author Hongs
 */
public interface Conn extends AutoCloseable {
    
    public String getDbName();
    
    public String getDbPath();
    
    public IndexWriter getWriter() throws IOException;
    
    public IndexReader getReader() throws IOException;
    
    public IndexSearcher getFinder() throws IOException;
    
    public void write(Map<String, Document> WRITES) throws IOException;

}
