package app.hongs.test;

import app.hongs.util.Tool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 测试 app.hongs.util.Tool
 * @author Hongs
 */
public class TestTool extends TestCase {

    public TestTool() {

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
    public void testHex() {
        long a, b;
        String x, y;
        a = 1234567890;
        x = "KF12OI";

        y = Tool.to36Hex(a);
        assertEquals(y, x);

        b = Tool.as36Hex(y);
        assertEquals(b, a);
    }

    @Test
    public void testEscape() {
        String a, b, s;
        a = "I'm Hongs: \"How are you!\"";
        b = "I\\'m Hongs: \\\"How are you!\\\"";

        s = Tool.escape(a);
        assertEquals(s, b);

        s = Tool.resume(s);
        assertEquals(s, a);
    }

    @Test
    public void TestIndent() {
        String a, b, s;
        a = "I'm Hongs:\r\n\t\"How are you!\"";
        b = "\tI'm Hongs:\r\n\t\t\"How are you!\"";

        s = Tool.indent(a);
        assertEquals(s, b);

        s = Tool.undent(s);
        assertEquals(s, a);
    }

    @Test
    public void TestInject() {
        String a, b, s;
        a = "I'm $0, are you ${1}in?";
        b = "I'm Hongs, are you Kevin?";

        s = Tool.inject(a, "Hongs", "Kev");
        assertEquals(s, b);

        List l = new ArrayList();
        l.add("Hongs");
        l.add("Kev");
        s = Tool.inject(a, l);
        assertEquals(s, b);

        Map m = new HashMap();
        m.put("0", "Hongs");
        m.put("1", "Kev");
        s = Tool.inject(a, m);
        assertEquals(s, b);
    }

}
