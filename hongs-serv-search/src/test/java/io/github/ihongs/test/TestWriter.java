package io.github.ihongs.test;

import io.github.ihongs.HongsException;
import io.github.ihongs.dh.lucene.LuceneRecord;
import io.github.ihongs.dh.search.SearchEntity;
import io.github.ihongs.util.Synt;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static junit.framework.Assert.assertEquals;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestWriter extends TestCase {

    @Before
    @Override
    public void setUp() {
        // 删除测试的目录
        delDir("test");
        delDir("null");
    }

    @After
    @Override
    public void tearDown() {
        // 删除测试的目录
        delDir("test");
        delDir("null");
    }

    @Test
    public void testWriter() throws HongsException, InterruptedException, IOException {
        if (1 == 1) return;
        
        Map fields = Synt.mapOf(
            "id"   , Synt.mapOf(
                "__name__", "id",
                "__type__", "hidden"
            ),
            "name" , Synt.mapOf(
                "__name__", "name",
                "__type__", "string"
            ),
            "time" , Synt.mapOf(
                "__name__", "time",
                "__type__", "datetime"
            )
        );

        final ExecutorService es = Executors.newFixedThreadPool(40);
        final Runnable rn = new Runnable() {
            @Override
            public void run() {
                try {
                //  LuceneRecord se = new LuceneRecord(fields, "test", "test");
                    SearchEntity se = new SearchEntity(fields, "test", "test");
                    se.begin ();
                    se.create(Synt.mapOf(
                        "name" , "12ab",
                        "time" , System.currentTimeMillis()
                    ));
                    se.commit();
                }
                catch (HongsException ex) {
                    ex.printStackTrace( );
                }
            }
        };
        for (int i = 0; i < 80; i ++) {
            es.execute(() -> rn.run());
        }

        es.shutdown();
        es.awaitTermination(20, TimeUnit.SECONDS);

        // 检查写入的内容
    //  LuceneRecord se = new LuceneRecord(fields, "test", "test");
        SearchEntity se = new SearchEntity(fields, "test", "test");
        assertEquals(80 , se.search( Synt.mapOf(), 0, 0 ).hits() );
        se.getWriter().maybeMerge();
        se.getWriter().deleteUnusedFiles();
        se.getWriter().close();
    }

    private void delDir(String path) {
        File dir = new File(path);
        if ( dir.exists() ) {
            for(File file : dir.listFiles()){
                file.delete();
            }   dir .delete();
        }
    }

}
