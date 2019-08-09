package io.github.ihongs.test;

import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.util.Dawn;
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
public class TestCmdletHelper extends TestCase {

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
        System.out.println(Dawn.toString(opts));
    }

}
