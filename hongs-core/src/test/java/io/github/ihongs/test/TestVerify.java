package io.github.ihongs.test;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.cmdlet.CmdletRunner;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.verify.IsNumber;
import io.github.ihongs.util.verify.IsString;
import io.github.ihongs.util.verify.Repeated;
import io.github.ihongs.util.verify.Required;
import io.github.ihongs.util.verify.Verify;
import io.github.ihongs.util.verify.Wrongs;
import java.io.IOException;
import java.util.Arrays;
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
public class TestVerify extends TestCase {
    
    
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
    public void testVerify() throws IOException {
        // 避免路径缺失致写入项目主目录
        CmdletRunner.init(new String[] {"--COREPATH", "target"});
        
        try {
            Verify verify = new Verify()
                .addRule("name",
                    new Required(),
                    new IsString().config(Synt.mapOf("minlength", 3, "maxlength", 8))
                )
                .addRule("size",
                    new Repeated().config(Synt.mapOf("slice", ",")),
                    new IsNumber().config(Synt.mapOf("min", 10, "max", 99))
                );
            Map values = Synt.mapOf(
                "name", "abcdef",
                "size", "11,22"
            );
            Map cleans = verify.verify(values, false, false);
            Map cleanz = Synt.mapOf(
                "name", "abcdef",
                "size", Arrays.asList(11, 22)
            );
            //System.out.println(Dawn.toString(cleans));
            //System.out.println(Dawn.toString(cleanz));
            assertEquals(Dawn.toString(cleans, true), Dawn.toString(cleanz, true));
        }
        catch (Wrongs wr) {
            Core.ACTION_LANG.set(Cnst.LANG_DEF);
            fail(Dawn.toString(wr.getErrors()));
        }
    }
    
}
