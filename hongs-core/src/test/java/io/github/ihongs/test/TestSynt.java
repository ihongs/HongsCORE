package io.github.ihongs.test;

import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.util.Data;
import io.github.ihongs.util.Synt;
import java.util.Map;
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
    public void testOpts() {
        Map opts = CmdletHelper.getOpts(
            new String[] {
                "--enable",
                "--number=123",
                "--regexp", "abc",
                "--repeat", "def", "--", "--repeat=--xyz",
                "--others", "456", "789"
            },
            "enable:b",
            "number=i",
            "repeat+s",
            "regexp=/^abc$/i",
            "!U",
            "!A"
        );
        System.out.println(Data.toString(opts));
        
        String a = new String("");
        String b = new String("");
        String c = a;
        System.out.println("a == b: " + (a == b) + " a == c: " + (a == c) + " a equals b: " + (a.equals(b)));
    }

    @Test
    public void testDeclare() {
        String str;

        str = "1.23e9";
        long num = Synt.declare(str, 0L);
        System.out.println(num);
    }

}
