package io.github.ihongs.test;

import io.github.ihongs.Core;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static junit.framework.TestCase.assertEquals;
import org.junit.Test;

/**
 * 测试核心多线程写入
 * @author Hongs
 */
public class TestCore {

    @Test
    public void test() throws InterruptedException {
        final ExecutorService es = Executors.newFixedThreadPool(10);
        final AtomicInteger ai = new AtomicInteger();
        final Runnable rn = new Runnable () {
            @Override
            public void run() {
                Core.GLOBAL_CORE.got(
                    "test",
                    () -> {
                        ai.addAndGet(  1  );
                        return new Object();
                    }
                );
            }
        };

        for (int i = 0; i < 10; i ++ ) {
            es.execute(() -> rn.run());
        }

        es.shutdown ();
        es.awaitTermination(10, TimeUnit.SECONDS);

        assertEquals(1, ai.get());
    }

}
