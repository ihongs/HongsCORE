package io.github.ihongs.test;

import io.github.ihongs.dh.search.StatisGrader.Total;
import io.github.ihongs.dh.search.StatisHandle.Range;
import static junit.framework.TestCase.assertTrue;
import org.junit.Test;

/**
 * 测试统计
 * @author Kevin
 */
public class TestStatis {

    @Test
    public void testRange() {
        Range r = new Range("[123,456)");

        System.out.println(
                r.toString()
                + " 100:" + r.covers(100)
                + " 200:" + r.covers(200)
                + " 123:" + r.covers(123)
                + " 456:" + r.covers(456)
                + " 789:" + r.covers(789)
        );

        assertTrue(r.covers(100) == false);
        assertTrue(r.covers(200) == true );
        assertTrue(r.covers(123) == true );
        assertTrue(r.covers(456) == false);
        assertTrue(r.covers(789) == false);
    }

    @Test
    public void testTotal() {
        Total r = new Total(new Range(""));
        r.tap(123);
        r.tap(456);

        System.out.println(
                r.toString()
        );

        assertTrue((int) r.get(2) == 2);
        assertTrue((double) r.get(3) == 123 + 456);
        assertTrue((double) r.get(4) == 123);
        assertTrue((double) r.get(5) == 456);
    }

}
