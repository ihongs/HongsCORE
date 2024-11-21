package io.github.ihongs.test;

import io.github.ihongs.util.Synt;
import static junit.framework.TestCase.assertEquals;
import org.junit.Test;

/**
 * 测试 io.github.ihongs.util.Synt
 * @author Hongs
 */
public class TestUtilSynt {

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
    
    @Test
    public void testAsString() {
        assertEquals(
            "1234567890123456",
            Synt.asString(1234567890123456l)
        );
        assertEquals(
            "12345678.90123456",
            Synt.asString(12345678.90123456d)
        );
        assertEquals(
            "0.1234567890123456",
            Synt.asString(0.1234567890123456d)
        );
    }

}
