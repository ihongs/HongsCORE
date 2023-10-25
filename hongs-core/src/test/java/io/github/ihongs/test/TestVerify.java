package io.github.ihongs.test;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.combat.CombatRunner;
import io.github.ihongs.util.Dist;
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
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import org.junit.Test;

/**
 * 测试 io.github.ihongs.util.verify.Verify
 * @author Hongs
 */
public class TestVerify {

    @Test
    public void testVerify() throws IOException {
        // 避免路径缺失致写入项目主目录
        CombatRunner.init(new String[] {"--COREPATH", "target"});

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
            //System.out.println(Dist.toString(cleans));
            //System.out.println(Dist.toString(cleanz));
            assertEquals(Dist.toString(cleans, true), Dist.toString(cleanz, true));
        }
        catch (Wrongs wr) {
            Core.ACTION_LANG.set(Cnst.LANG_DEF);
            fail(Dist.toString(wr.getErrors()));
        }
    }

}
