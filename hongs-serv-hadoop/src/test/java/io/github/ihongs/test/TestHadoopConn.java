package io.github.ihongs.test;

import io.github.ihongs.dh.lucene.conn.Conn;
import io.github.ihongs.dh.lucene.conn.HadoopConn;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * HadoopConn 测试类
 * 测试写、读、查操作
 */
public class TestHadoopConn {
    
    private Conn conn;
    private String serverIP = "172.24.253.70";
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    private Properties createConfig(String dbname) {
        Properties pp = new Properties();
        pp.setProperty("core.hadoop.fs.defaultFS", "hdfs://" + serverIP + ":9000");
        pp.setProperty("core.hadoop.data.path", "/hongs/indexes");
        pp.setProperty("core.hadoop.dfs.replication", "2");
        pp.setProperty("core.zookeeper.connect", serverIP + ":2181");
        pp.setProperty("core.zookeeper.timeout", "30000");
        pp.setProperty("core.zookeeper.lock.path", "/hongs/locks");
        return pp;
    }
    
    @Test
    public void testWrite() throws IOException {
        System.out.println("=== 测试写入 ===");
        
        Properties pp = createConfig("test_write");
        conn = new HadoopConn("test_write", "test_write", pp);
        
        Map<String, Document> docs = new HashMap<>();
        
        Document doc1 = new Document();
        doc1.add(new StringField("@id", "test1", Field.Store.YES));
        doc1.add(new TextField("title", "Hello HDFS", Field.Store.YES));
        doc1.add(new TextField("content", "This is a test document for Hadoop Conn", Field.Store.YES));
        docs.put("test1", doc1);
        
        Document doc2 = new Document();
        doc2.add(new StringField("@id", "test2", Field.Store.YES));
        doc2.add(new TextField("title", "Hadoop Lucene Integration", Field.Store.YES));
        doc2.add(new TextField("content", "Testing HDFS storage with Lucene", Field.Store.YES));
        docs.put("test2", doc2);
        
        conn.write(docs);
        System.out.println("成功写入 " + docs.size() + " 个文档");
        
        assertNotNull(conn);
    }
    
    @Test
    public void testRead() throws IOException {
        System.out.println("=== 测试读取 ===");
        
        Properties pp = createConfig("test_read");
        conn = new HadoopConn("test_read", "test_read", pp);
        
        Map<String, Document> docs = new HashMap<>();
        Document doc1 = new Document();
        doc1.add(new StringField("@id", "test1", Field.Store.YES));
        doc1.add(new TextField("title", "Hello HDFS", Field.Store.YES));
        doc1.add(new TextField("content", "This is a test document for Hadoop Conn", Field.Store.YES));
        docs.put("test1", doc1);
        conn.write(docs);
        
        IndexReader reader = conn.getReader();
        IndexSearcher searcher = new IndexSearcher(reader);
        
        Query query = new TermQuery(new Term("@id", "test1"));
        TopDocs results = searcher.search(query, 1);
        
        assertEquals("应找到一个文档", 1, results.totalHits.value);
        
        Document doc = searcher.doc(results.scoreDocs[0].doc);
        
        assertNotNull("文档不应为 null", doc);
        assertEquals("test1", doc.get("@id"));
        assertEquals("Hello HDFS", doc.get("title"));
        assertEquals("This is a test document for Hadoop Conn", doc.get("content"));
        
        reader.close();
        
        System.out.println("成功读取文档:");
        System.out.println("  @id: " + doc.get("@id"));
        System.out.println("  title: " + doc.get("title"));
        System.out.println("  content: " + doc.get("content"));
    }
    
    @Test
    public void testFind() throws IOException, org.apache.lucene.queryparser.classic.ParseException {
        System.out.println("=== 测试查询 ===");
        
        Properties pp = createConfig("test_find");
        conn = new HadoopConn("test_find", "test_find", pp);
        
        Map<String, Document> docs = new HashMap<>();
        Document doc1 = new Document();
        doc1.add(new StringField("@id", "test1", Field.Store.YES));
        doc1.add(new TextField("title", "Hello HDFS", Field.Store.YES));
        docs.put("test1", doc1);
        conn.write(docs);
        
        IndexSearcher searcher = conn.getFinder();
        
        QueryParser parser = new QueryParser("title", new org.apache.lucene.analysis.standard.StandardAnalyzer());
        Query query = parser.parse("HDFS");
        
        TopDocs results = searcher.search(query, 10);
        
        assertNotNull("查询结果不应为 null", results);
        assertTrue("应有查询结果", results.totalHits.value > 0);
        
        System.out.println("查询结果:");
        System.out.println("  总数: " + results.totalHits.value);
        
        for (ScoreDoc sd : results.scoreDocs) {
            Document doc = searcher.doc(sd.doc);
            System.out.println("  - " + doc.get("@id") + ": " + doc.get("title") + " (score: " + sd.score + ")");
        }
    }
    
    @Test
    public void testDelete() throws IOException {
        System.out.println("=== 测试删除 ===");
        
        Properties pp = createConfig("test_delete");
        conn = new HadoopConn("test_delete", "test_delete", pp);
        
        Map<String, Document> docs = new HashMap<>();
        Document doc1 = new Document();
        doc1.add(new StringField("@id", "test1", Field.Store.YES));
        doc1.add(new TextField("title", "Hello HDFS", Field.Store.YES));
        docs.put("test1", doc1);
        conn.write(docs);
        
        docs = new HashMap<>();
        docs.put("test1", null);
        conn.write(docs);
        
        IndexReader reader = conn.getReader();
        IndexSearcher searcher = new IndexSearcher(reader);
        
        Query query = new TermQuery(new Term("@id", "test1"));
        TopDocs results = searcher.search(query, 1);
        
        assertEquals("文档应已删除", 0, results.totalHits.value);
        
        reader.close();
        
        System.out.println("成功删除文档");
    }
}