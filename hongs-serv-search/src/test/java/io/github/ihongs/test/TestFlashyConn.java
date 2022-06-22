package io.github.ihongs.test;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.dh.lucene.conn.Conn;
import io.github.ihongs.dh.lucene.conn.FlashyConn;
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

public class TestFlashyConn {

    int dataSize = 600; // 测试数据数量
    int poolSize = 300; // 测试线程数量
    int waitTime = 30;  // 测试等待时间(秒)

    //@Test
    public void testWriter() throws HongsException, InterruptedException, IOException {
        // 必要全局变量
        if (Core.DATA_PATH == null) {
            Core.DATA_PATH = "target/test/var";
        }
        // 删除测试目录
        delDir(Core.DATA_PATH + "/lucene/test/flashy");

        String dbName = "test/flashy";
        String dbPath = "test/flashy";
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

        final ExecutorService es = Executors.newFixedThreadPool(poolSize);
        final Runnable rn = new Runnable() {
            @Override
            public void run() {
                try {
                    SearchEntity se = new TestEntity(fields, dbPath, dbName);

                    se.begin ();
                    String id = se.create(Synt.mapOf(
                        "name" , "test",
                        "time" , System.currentTimeMillis()
                    ));
                    se.commit();

                    Map  info = se.getOne(Synt.mapOf(
                        Cnst.ID_KEY, id,
                        Cnst.RB_KEY, Synt.setOf(Cnst.ID_KEY)
                    ));
                    assertEquals(id, info. get (Cnst.ID_KEY));
                }
                catch (HongsException ex) {
                    ex.printStackTrace( );
                }
            }
        };

        // 预设连接
        new TestEntity(fields, dbPath, dbName).getDbConn();

        // 写入数据
        for (int i = 0; i < dataSize; i ++) {
            es.execute(() -> rn.run());
        }

        // 等待结束
        es.shutdown();
        es.awaitTermination(waitTime, TimeUnit.SECONDS);

        // 复查数量
        SearchEntity se = new TestEntity( fields , dbPath , dbName );
        assertEquals(dataSize, se.search(Synt.mapOf(), 0, 0).hits());
    }

    private void delDir(String path) {
        File dir = new File(path);
        if ( dir.exists() ) {
            for(File file : dir.listFiles()){
                file.delete();
            }   dir .delete();
        }
    }

    private static class TestEntity extends SearchEntity {


        public TestEntity(Map form, String path, String name) {
            super(form, path, name);
        }

        private Conn dbconn = null ;
        @Override
        public Conn getDbConn() {
            if (dbconn == null) {
                dbconn  = new  FlashyConn.Getter().get(getDbPath(), getDbName());
            }
            return dbconn;
        }

    }

}
