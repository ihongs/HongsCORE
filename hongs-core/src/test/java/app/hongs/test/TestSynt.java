package app.hongs.test;

import app.hongs.util.Synt;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Hongs
 */
public class TestSynt extends TestCase {
    
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
    public void testDeclare() {
        String str;
        
        str = "1.23e9";
        long num = Synt.declare(str, 0L);
        System.out.println(num);
    }
    
}
