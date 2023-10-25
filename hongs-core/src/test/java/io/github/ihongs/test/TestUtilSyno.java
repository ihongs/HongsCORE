package io.github.ihongs.test;

import io.github.ihongs.util.Syno;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static junit.framework.TestCase.assertEquals;
import org.junit.Test;

/**
 * 测试 io.github.ihongs.util.Syno
 * @author Hongs
 */
public class TestUtilSyno {

    @Test
    public void testHex() {
        long a, b;
        String x, y;
        a = 1234567890;
        x = "KF12OI";

        y = Syno.to36Hex(a);
        assertEquals(y, x);

        b = Syno.as36Hex(y);
        assertEquals(b, a);

        x = "012/345/678/9AB/CDE/0123456789ABCDE";
        y = "0123456789ABCDE";
        y = Syno.splitPath(y);
        assertEquals(y, x);

        x = "012/345/678/9AB/CDE/0123456789ABCDEF";
        y = "0123456789ABCDEF";
        y = Syno.splitPath(y);
        assertEquals(y, x);

        x = "012/345/678/9AB/CDE/0123456789ABCDEFG";
        y = "0123456789ABCDEFG";
        y = Syno.splitPath(y);
        assertEquals(y, x);
    }

    @Test
    public void testEscape() {
        String a, b, s;
        a = "I'm Hongs: \"How are you!\"";
        b = "I\\'m Hongs: \\\"How are you!\\\"";

        s = Syno.escape(a);
        assertEquals(s, b);

        s = Syno.resume(s);
        assertEquals(s, a);
    }

    @Test
    public void testIndent() {
        String a, b, s;
        a = "I'm Hongs:\r\n\t\"How are you!\"";
        b = "\tI'm Hongs:\r\n\t\t\"How are you!\"";

        s = Syno.indent(a);
        assertEquals(s, b);

        s = Syno.undent(s);
        assertEquals(s, a);
    }

    @Test
    public void testInject() {
        String a, b, s;
        a = "I'm $0, are you ${1}in? Yes, I ${2|am}. $$10 please.";
        b = "I'm Hongs, are you Kevin? Yes, I am. $10 please.";

        s = Syno.inject(a, "Hongs", "Kev");
        assertEquals(s, b);

        List l = new ArrayList();
        l.add("Hongs");
        l.add("Kev");
        s = Syno.inject(a, l);
        assertEquals(s, b);

        Map m = new HashMap();
        m.put("0", "Hongs");
        m.put("1", "Kev");
        s = Syno.inject(a, m);
        assertEquals(s, b);

        // 模板参数个数与实际替换参数个数可以不同
        assertEquals("abc 123 ", Syno.inject("abc $0 $2", 123, 456));
    }

}
