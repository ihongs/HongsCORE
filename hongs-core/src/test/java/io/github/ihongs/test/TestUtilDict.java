package io.github.ihongs.test;

import io.github.ihongs.util.Dict;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 测试 io.github.ihongs.util.Dict
 * @author Hongs
 */
public class TestUtilDict extends TestCase {

    private Map dict;

    public TestUtilDict() {
        super();
    }

    @Before
    @Override
    public void setUp() {
        dict = new HashMap();
        Map  sub1 = new HashMap();
        List sub2 = new ArrayList();
        List sub3 = new ArrayList();
        Set  sub4 = new HashSet();
        dict.put("sub1", sub1);
        sub1.put("key1", "xx");
        sub1.put("sub2", sub2);
        sub1.put("sub3", sub3);
        sub2.add("sub2");
        sub2.add( sub4 );
        sub4.add("sub4");
        Map m;
        m = new HashMap();
        m.put("a", 1);
        sub3.add(m);
        m = new HashMap();
        m.put("a", 2);
        sub3.add(m);
        /**
         *  {
         *      "sub1": {
         *          "key1": "xx",
         *          "sub2": [
         *              "sub2",
         *              [ // Set
         *                  "sub4"
         *              ]
         *          ],
         *          "sub3": [
         *              {
         *                  "a": 1
         *              },
         *              {
         *                  "a": 2
         *              }
         *          ]
         *      }
         *  }
         */
    }

    @After
    @Override
    public void tearDown() {
        dict.clear();
    }

    @Test
    public void testGet() {
        Object x;
        List   y;
        Set    z;

        x = Dict.get(dict, null, "sub1", "key1");
        assertEquals("xx", x);

        x = Dict.get(dict, null, "sub1", "sub2", 1);
        z = new HashSet( ); z.add("sub4");
        assertEquals(z, x);

        x = Dict.get(dict, null, "sub1", "sub2", 1, null);
        z = new HashSet( ); z.add("sub4");
        assertEquals(z, x);

        x = Dict.get(dict, null, "sub1", "sub3", null, "a");
        y = new ArrayList(); y.add(1); y.add(2);
        assertEquals(y, x);

        //io.github.ihongs.combat.CombatHelper.preview(dict);
    }

    @Test
    public void testPut() {
        Object x;
        List   y;
        Set    z;

        Dict.put(dict, "yy", "sub1", "key1");
        x = Dict.get(dict, null, "sub1", "key1");
        assertEquals("yy", x);

        z = new HashSet(  ); z.add(1);
        Dict.put(dict, z, "sub1", "sub2", 1);
        x = Dict.get(dict, null, "sub1", "sub2", 1);
        assertEquals(z, x);

        z = new HashSet(  ); z.add(1); z.add(2);
        Dict.put(dict, 2, "sub1", "sub2", 1, null);
        x = Dict.get(dict, null, "sub1", "sub2", 1, null);
        assertEquals(z, x);

        y = new ArrayList(); y.add(1); y.add(2); y.add(3);
        Dict.put(dict, 3, "sub1", "sub3", null, "a");
        x = Dict.get(dict, null, "sub1", "sub3", null, "a");
        assertEquals(y, x);

        // 设置多层列表, 获取归并结果
        y = new ArrayList(); y.add(4); y.add(5);
        Dict.put(dict, 4, "sub1", "sub3", null, "c", null);
        Dict.put(dict, 5, "sub1", "sub3", null, "c", null);
        x = Dict.get(dict, null, "sub1", "sub3", null, "c", null);
        assertEquals(y, x);

        //io.github.ihongs.combat.CombatHelper.preview(dict);
    }

    @Test
    public void testDel() {
        Map  a = new HashMap();
        Map  b = new HashMap();
        List c = new ArrayList(Arrays.asList("abc", 123, "xyz"));
        a.put("abc", 123);
        a.put(123, "abc");
        b.put("abc", 456);
        b.put(456, "abc");
        Dict.put(a, b, "b");
        Dict.put(a, c, "b", "c");

        Dict.del(a, "b", 456);
        assertEquals(Dict.get(a, "xyz", "b", 456), "xyz");

        Dict.del(a, "b", "c", 1);
        assertEquals(Dict.get(a, 11, "b", "c", 1), "xyz");

        Dict.del(a, "b", "c", null);
        assertEquals(Dict.get(a, 22, "b", "c", 0),  22  );

        Dict.del(a, "b", null);
        assertEquals(Dict.get(a, 789, "b", "abc"), 789  );

        //io.github.ihongs.combat.CombatHelper.preview(a);
    }

    @Test
    public void testSplitKeys() {
        Object[] a = new Object [] {"", "a", "b", "c", "d", null, "e", "f", "g", "h", null, "x.y:z", "def", null};
        Object[] b = Dict.splitKeys(".a.b[c][d].:e:f[g]:h[][x.y:z].def[]");
        Object[] c = Dict.splitKeys(".a.b[c][d]..e.f[g].h[][x.y:z].def." );
        //System.out.println(Dist.toString(a));
        //System.out.println(Dist.toString(b));
        //System.out.println(Dist.toString(c));
        assertTrue(Arrays.equals(a, b));
        assertTrue(Arrays.equals(a, c));
    }

}
