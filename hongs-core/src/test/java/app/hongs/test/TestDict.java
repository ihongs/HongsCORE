package app.hongs.test;

import app.hongs.util.Dict;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 测试 app.hongs.util.Dict
 * @author Hongs
 */
public class TestDict extends TestCase {

    private Map dict;

    public TestDict() {
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
         *              "sub2,
         *              [
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

        //Data.dumps(dict);

        x = Dict.get(dict, null, "sub1", "key1");
        assertEquals("xx", x);

        x = Dict.get(dict, null, "sub1", "sub2", 1);
        z = new HashSet(  ); z.add("sub4");
        assertEquals(z, x);

        x = Dict.get(dict, null, "sub1", "sub2", 1, null);
        y = new ArrayList(); y.add("sub4");
        assertEquals(y, x);

        x = Dict.get(dict, null, "sub1", "sub3", null, "a");
        y = new ArrayList(); y.add(1); y.add(2);
        assertEquals(y, x);
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

        y = new ArrayList(); y.add(1); y.add(2);
        Dict.put(dict, 2, "sub1", "sub2", 1, null);
        x = Dict.get(dict, null, "sub1", "sub2", 1, null);
        assertEquals(y, x);

        y = new ArrayList(); y.add(1); y.add(2); y.add(3);
        Dict.put(dict, 3, "sub1", "sub3", null, "a");
        x = Dict.get(dict, null, "sub1", "sub3", null, "a");
        assertEquals(y, x);

        //Data.dumps(dict);
    }
    
    public void testSplitKeys() {
        Object[] a = new Object[ ] {"", "a", "b", "c", "d", "!e", "!f", "g", "!h", null, "x.y!z", "def"};
        Object[] b = Dict.splitKeys(".a.b[c][d].!e!f[g]!h[][x.y!z].def");
        app.hongs.util.Data.print(a);
        app.hongs.util.Data.print(b);
        assertTrue( Arrays.equals(a, b) );
    }

}
