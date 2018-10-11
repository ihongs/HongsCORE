package io.github.ihongs.test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Hongs
 */
public class TestCore {
    
    public TestCore() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testLock1() {
        Object o = new Object();
        synchronized (o) {
            System.out.println("1");
            testLock2(o);
        }
    }
    
    public void testLock2(Object o) {
        synchronized (o) {
            System.out.println("2");
        }
    }

}
