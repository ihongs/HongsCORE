package io.github.ihongs.test;

import io.github.ihongs.action.ActionDriver.URLPatterns;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.util.Dawn;
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
public class Tests extends TestCase {

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
    public void testGetOpts() {
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

    @Test
    public void testUrlPatterns() {
        System.out.println(new URLPatterns(
            "*.js,*.css,*.html, *.gif,*.jpg,*.png,*.bmp",
            "/centre/sign/* , \r\n /centre/login.html ,"
        ).toString());

        assertEquals(new URLPatterns("", "").matches("/a/b.js"), true);

        assertEquals(new URLPatterns("/a/*", "").matches("/a/b.js"), true );
        assertEquals(new URLPatterns("/a/*", "").matches("/b/a.js"), false);
        assertEquals(new URLPatterns("*.js", "").matches("/a/b.js"), true );
        assertEquals(new URLPatterns("*.js", "").matches("/a/b.ps"), false);

        assertEquals(new URLPatterns("", "/a/*").matches("/a/b.js"), false);
        assertEquals(new URLPatterns("", "/a/*").matches("/b/a.js"), true );
        assertEquals(new URLPatterns("", "*.js").matches("/a/b.js"), false);
        assertEquals(new URLPatterns("", "*.js").matches("/a/b.ps"), true );

        assertEquals(new URLPatterns("/a/*", "*.js").matches("/a/b.js"), false);
        assertEquals(new URLPatterns("*.js", "/a/*").matches("/b/a.js"), true );
    }

}
