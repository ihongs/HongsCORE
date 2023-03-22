package io.github.ihongs.test;

import io.github.ihongs.action.ActionDriver.PathPattern;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Synt;
import java.util.Arrays;
import java.util.HashMap;
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
        Map opts = CombatHelper.getOpts(
            new String[] {
                "--enable",
                "--number=123",
                "--regexp", "abc",
                "--repeat", "def", "--repeat=--xyz", "--",
                "--others", "456", "789"
            },
            "enable:b",
            "number=i",
            "repeat+s",
            "regexp=/^abc$/i",
            "!U",
            "!A"
        );
        Map optz = new HashMap(Synt.mapOf(
            "", new String[] {"--others", "456", "789"},
            "number",  123 ,
            "regexp", "abc",
            "enable", true ,
            "repeat", Arrays.asList("def", "--xyz")
        ));
        //System.out.println(Dist.toString(opts));
        //System.out.println(Dist.toString(optz));
        assertEquals(Dist.toString(opts), Dist.toString(optz));
    }

    @Test
    public void testUrlPatterns() {
        String ss = new PathPattern(
                    "*.js,*.css,*.html, *.gif,*.jpg,*.png,*.bmp",
                    "/centre/sign/* , \r\n /centre/login.html ,"
                ).toString();
        String zz = "Include: ^(.*\\.js|.*\\.css|.*\\.html|.*\\.gif|.*\\.jpg|.*\\.png|.*\\.bmp)$\r\n"
                  + "Exclude: ^(/centre/sign/.*|/centre/login\\.html)$";
        assertEquals(ss, zz);

        assertEquals(new PathPattern("", "").matches("/a/b.js"), true);

        assertEquals(new PathPattern("/a/*", "").matches("/a/b.js"), true );
        assertEquals(new PathPattern("/a/*", "").matches("/b/a.js"), false);
        assertEquals(new PathPattern("*.js", "").matches("/a/b.js"), true );
        assertEquals(new PathPattern("*.js", "").matches("/a/b.ps"), false);

        assertEquals(new PathPattern("", "/a/*").matches("/a/b.js"), false);
        assertEquals(new PathPattern("", "/a/*").matches("/b/a.js"), true );
        assertEquals(new PathPattern("", "*.js").matches("/a/b.js"), false);
        assertEquals(new PathPattern("", "*.js").matches("/a/b.ps"), true );

        assertEquals(new PathPattern("/a/*", "*.js").matches("/a/b.js"), false);
        assertEquals(new PathPattern("*.js", "/a/*").matches("/b/a.js"), true );
    }

    @Test
    public void testForwarded() {
        String fw, ip;

        fw = "for=127.0.0.1" ;
        ip = getForwarded(fw);
        assertEquals(ip, "127.0.0.1");

        fw = "by=127.0.0.1; for=127.0.0.1:8080, for=unknown";
        ip = getForwarded(fw);
        assertEquals(ip, "127.0.0.1");

        fw = "by=127.0.0.1; for=unknown, for=\"[::1]:8080\"";
        ip = getForwarded(fw);
        assertEquals(ip, "::1");
    }

    /**
     * @see io.github.ihongs.action.ActionDriver getClientAddr
     * @param h_0
     * @return
     */
    private static String getForwarded(String h_0) {
        if ( h_0 != null && h_0.length() != 0 ) {
            // 按逗号拆分代理节点
            int e_0,b_0 = 0;
            String  h_1;
            while (true) {
                e_0 = h_0.indexOf(',' , b_0);
                if (e_0 != -1) {
                    h_1 = h_0.substring(b_0, e_0);
                    b_0 = e_0 + 1;
                } else
                if (b_0 !=  0) {
                    h_1 = h_0.substring(b_0);
                } else
                {
                    h_1 = h_0;
                }

                // 按分号拆分条目
                int e_1,b_1 = 0;
                String  h_2;
                while (true) {
                    e_1 = h_1.indexOf(';' , b_1);
                    if (e_1 != -1) {
                        h_2 = h_1.substring(b_1, e_1);
                        b_1 = e_1 + 1;
                    } else
                    if (b_1 !=  0) {
                        h_2 = h_1.substring(b_1);
                    } else
                    {
                        h_2 = h_1;
                    }

                    // 拆分键值对
                    int e_2  = h_2.indexOf ('=');
                    if (e_2 != -1) {
                        String key = h_2.substring(0 , e_2).trim();
                        String val = h_2.substring(1 + e_2).trim();
                        key =  key.toLowerCase(    );

                        // 源地址
                        if (    "for"  .equals(key )
                        &&  ! "unknown".equals(val )) {
                            /**
                             * 按照官方文档的格式描述
                             * IPv4 的形式为 X.X.X.X:PORT
                             * IPv6 的形式为 "[X:X:X:X:X]:PORT"
                             * 需去掉端口引号和方括号
                             */
                            if (val.startsWith("\"")
                            &&  val.  endsWith("\"")) {
                                val = val.substring(1 , val.length() - 1);
                            }
                            if (val.startsWith("[" )) {
                                e_2 = val.indexOf("]" );
                                if (e_2 != -1) {
                                    val = val.substring(1 , e_2);
                                }
                            } else {
                                e_2 = val.indexOf(":" );
                                if (e_2 != -1) {
                                    val = val.substring(0 , e_2);
                                }
                            }
                            return  val;
                        }
                    }

                    if (e_1 == -1) {
                        break;
                    }
                }

                if (e_0 == -1) {
                    break;
                }
            }
        }

        return null;
    }

}
