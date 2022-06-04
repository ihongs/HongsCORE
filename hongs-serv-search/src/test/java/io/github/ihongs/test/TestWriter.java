package io.github.ihongs.test;

import io.github.ihongs.HongsException;
import io.github.ihongs.dh.search.SearchEntity;
import io.github.ihongs.util.Synt;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static junit.framework.Assert.assertEquals;
import org.junit.Test;

public class TestWriter {

    //@Test
    public void test() throws HongsException, InterruptedException {
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
        
        final ExecutorService es = Executors.newFixedThreadPool(10);
        final Runnable rn = new Runnable() {
            @Override
            public void run() {
                try {
                //  LuceneRecord se = new LuceneRecord(fields, "test-writer", "test-writer");
                    SearchEntity se = new SearchEntity(fields, "test-writer", "test-writer");
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
        for (int i = 0; i < 10; i ++) {
            es.execute(() -> rn.run());
        }
        
        es.shutdown();
        es.awaitTermination(10, TimeUnit.SECONDS);
        
        // 检查写入的内容
        SearchEntity se = new SearchEntity(fields, "test-writer", "test-writer");
        assertEquals(10 , se.search(Synt.mapOf(), 0, 0).hits() );
        try {
            se.getWriter().close();
        } catch ( IOException ex ) {
            ex.printStackTrace ( );
        }
        
        // 删除测试的目录
        File dir = new File("test-writer");
        for(File file : dir.listFiles()){
            file.delete();
        }
        dir.delete();
    }
    
}
