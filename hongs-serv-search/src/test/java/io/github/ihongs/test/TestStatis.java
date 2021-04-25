package io.github.ihongs.test;

import io.github.ihongs.dh.search.StatisGrader;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Kevin
 */
public class TestStatis extends TestCase {

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    @Override
    public void setUp() {
    }

    @After
    @Override
    public void tearDown() {
    }

    @Test
    public void testRange() {
        StatisGrader.Range r = new StatisGrader.Range("[123,456)");
        
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
    public void testRatio() {
        StatisGrader.Ratio r = new StatisGrader.Ratio();
        r.add(123);
        r.add(456);
        
        System.out.println(
                r.toString()
        );
        
        assertTrue(r.cnt == 2);
        assertTrue(r.sum == 123 + 456);
        assertTrue(r.min == 123);
        assertTrue(r.max == 456);
    }

}
