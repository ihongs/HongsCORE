package io.github.ihongs.test;

import io.github.ihongs.Core;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import org.junit.Test;

/**
 * 测试核心多线程写入及生成ID
 * @author Hongs
 */
public class TestCore {

    @Test
    public void test() throws InterruptedException {
        final ExecutorService es = Executors.newFixedThreadPool(10);
        final AtomicInteger ai = new AtomicInteger();
        final Runnable rn = () -> {
            Core.GLOBAL_CORE.got(
                "test",
                () -> {
                    ai.addAndGet(  1  );
                    return new Object();
                }
            );
        };

        for ( int i = 0; i < 10; i ++ ) {
            es.execute(() -> rn.run() );
        }

        es.shutdown ();
        es.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(1, ai.get());
    }

    @Test
    public void testId() throws InterruptedException {
        final ExecutorService es = Executors.newFixedThreadPool(10);
        final String[][] aa = new String[10][1000];
        final Consumer<Integer> co = (i) -> {
            for(int j = 0; j < aa[i].length; j ++) {
                aa[i][j] = Core.newIdentity();
            }
        };

        for (int i = 0; i < aa.length; i ++ ) {
            final  int  j = i ;
            es.execute( ( ) -> co.accept(j) );
        }

        es.shutdown ();
        es.awaitTermination(10, TimeUnit.SECONDS);

        // 校验
        Set ids = new HashSet();
        for (int i = 0; i < aa.length; i ++ ) {
            String[] bb = aa[i];
        for (int j = 0; j < bb.length; j ++ ) {
            String   id = bb[j];
            if (ids.contains(id)) {
                fail( "Id "+ id +" repeats" );
            }
            ids.add (id);
        }}
        //System.out.println(ids);
    }

}
