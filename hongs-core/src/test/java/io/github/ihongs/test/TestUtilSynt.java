package io.github.ihongs.test;

import io.github.ihongs.util.Synt;
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
public class TestUtilSynt extends TestCase {

    public TestUtilSynt() {

    }

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
    public void testCompare() {
        assertEquals(Synt.compare("a" , "a" , false),  0);
        assertEquals(Synt.compare("a" , "b" , false), -1);
        assertEquals(Synt.compare("a" , null, false),  1);
        assertEquals(Synt.compare("a" , null, true ), -1);
    }

    @Test
    public void testToTerms() {
        assertEquals(
            Synt.toTerms(Synt.setOf(1, "a", "b", "c")),
            Synt.toTerms(Synt.setOf(1, "a", Synt.setOf("b"), Synt.mapOf("b", "c")))
        );
        assertEquals(
            Synt.toTerms(Synt.setOf(1, "a", "b", "c")),
            Synt.toWords(Synt.setOf(1, "a", Synt.setOf("b"), Synt.mapOf("b", "c")))
        );
    }

}
