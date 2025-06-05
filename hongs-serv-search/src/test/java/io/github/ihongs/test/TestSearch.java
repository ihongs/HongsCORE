package io.github.ihongs.test;

import io.github.ihongs.Core;
import io.github.ihongs.CruxException;
import io.github.ihongs.dh.lucene.conn.Conn;
import io.github.ihongs.dh.lucene.conn.FlashyConn;
import io.github.ihongs.dh.search.SearchEntity;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Synt;
import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 * 测试查询
 * @author Hongs
 */
public class TestSearch {

    //@Test
    public void testSearch() throws CruxException {
        // 必要全局变量
        if (Core.DATA_PATH == null) {
            Core.DATA_PATH = "target/test/var";
        }
        // 删除测试目录
        delDir(Core.DATA_PATH + "/lucene/test/search");

        String dbName = "test/search";
        String dbPath = "test/search";
        Map fields = Synt.mapOf(
            "id"   , Synt.mapOf(
                "__name__", "id",
                "__type__", "hidden"
            ),
            "name" , Synt.mapOf(
                "__name__", "name",
                "__type__", "string"
            )
        );

        SearchEntity se = new TestEntity(fields, dbPath, dbName);

        // 写入测试数据
        se.begin ();
        se.create(Synt.mapOf(
            "name" , "test1"
        ));
        se.create(Synt.mapOf(
            "name" , "test2"
        ));
        se.create(Synt.mapOf(
            "name" , "test3"
        ));
        se.create(Synt.mapOf(
            "name" , "test4"
        ));
        se.create(Synt.mapOf(
            "name" , "test5"
        ));
        se.create(Synt.mapOf(
            "name" , "test6"
        ));
        se.commit();

        int i = 0;
        int j = 6;
        do {
            // 测试多项查询
            List<TestEntity.Loop> loops = Synt.listOf(
                se.search(Synt.mapOf(), 0, 0),
                se.search(Synt.mapOf(), 0, 0),
                se.search(Synt.mapOf(), 0, 0)
            );
            Set idx = Synt.setOf();
            
            do {
                Iterator<TestEntity.Loop> it = loops.iterator();
                while (it.hasNext()) {
                    TestEntity.Loop lo  = it.next();
                    if (lo.hasNext()) {
                        Map row = lo.next();
                        String id = (String) row.get("id");
                        if (idx.contains(id)) {
                            continue;
                        }
                        idx.add(id);

                        System.out.println(Dist.toString(row, true));

                        // 读同时写
                        se.create(Synt.mapOf(
                            "name" , "test" + (++ j)
                        ));
                        se.commit();
                    } else {
                        it.remove();
                    }
                }
            }
            while (! loops.isEmpty());
        }
        while (i ++ < 1);
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
