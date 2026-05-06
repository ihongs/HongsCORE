package io.github.ihongs.test;

import io.github.ihongs.Cnst;
import io.github.ihongs.dh.lucene.conn.HadoopConn;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.index.Term;
import static junit.framework.Assert.*;

public class TestHadoopConn {

    String hdfsUri = "hdfs://localhost:9000";

    //@Test
    public void testHdfsDirectory() throws Exception {
        Properties cc = new Properties();
        cc.setProperty("core.hadoop.hdfs.uri", hdfsUri);
        cc.setProperty("core.hadoop.data.path", "/hongs/data");

        String dbPath = "lucene/test/hadoop";
        String dbName = "test/hadoop";

        HadoopConn conn = new HadoopConn(dbPath, dbName, cc);

        try {
            // 验证路径拼接: 相对路径应拼接 core.hadoop.data.path
            assertEquals("/hongs/data/" + dbPath, conn.getDbPath());
            assertEquals(dbName, conn.getDbName());

            // 写入测试文档
            Map<String, Document> docs = new HashMap();
            for (int i = 0; i < 10; i++) {
                Document doc = new Document();
                doc.add(new StringField("@" + Cnst.ID_KEY, "id-" + i, Field.Store.YES));
                doc.add(new StringField("name", "test-" + i, Field.Store.YES));
                docs.put("id-" + i, doc);
            }
            conn.write(docs);

            // 从内存索引读取
            IndexSearcher finder = conn.getFinder();
            TopDocs hits = finder.search(new TermQuery(new Term("name", "test-5")), 1);
            assertEquals(1, hits.totalHits.value);

            // 合并到 HDFS
            conn.merge();

            // 合并后从 HDFS 读取
            finder = conn.getFinder();
            hits = finder.search(new TermQuery(new Term("name", "test-5")), 1);
            assertEquals(1, hits.totalHits.value);

            // 写入更多数据后再次合并
            docs.clear();
            for (int i = 10; i < 20; i++) {
                Document doc = new Document();
                doc.add(new StringField("@" + Cnst.ID_KEY, "id-" + i, Field.Store.YES));
                doc.add(new StringField("name", "test-" + i, Field.Store.YES));
                docs.put("id-" + i, doc);
            }
            conn.write(docs);
            conn.merge();

            // 验证合并后总数
            finder = conn.getFinder();
            hits = finder.search(new TermQuery(new Term("name", "test-15")), 1);
            assertEquals(1, hits.totalHits.value);

            System.out.println("TestHadoopConn passed: write, merge, read all OK");
        } finally {
            conn.close();
        }
    }

    //@Test
    public void testAbsolutePath() throws Exception {
        Properties cc = new Properties();
        cc.setProperty("core.hadoop.hdfs.uri", hdfsUri);
        cc.setProperty("core.hadoop.data.path", "/hongs/data");

        // 绝对路径不拼接
        HadoopConn conn = new HadoopConn("/absolute/path", "abs-test", cc);
        try {
            assertEquals("/absolute/path", conn.getDbPath());
        } finally {
            conn.close();
        }
    }

    public static void main(String[] args) throws Exception {
        TestHadoopConn t = new TestHadoopConn();
        t.testHdfsDirectory();
        t.testAbsolutePath();
        System.out.println("All tests passed!");
    }

}
