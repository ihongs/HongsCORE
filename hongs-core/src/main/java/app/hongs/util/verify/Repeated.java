package app.hongs.util.verify;

import app.hongs.util.Synt;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * 多值约束
 * @author Hongs
 */
public class Repeated extends Rule {
    @Override
    public Object verify(Object value) throws Wrong {
        if (value == null) {
            return new Object[0];
        }
        // 字符串则检查是否要拆分
        if (value instanceof String/**/) {
            String s = Synt.declare(params.get("split"), "");
            if (s.length() != 0) {
                return ((String) value ).split( s , -1);
            }
        }
        if (value instanceof Object[ ] ) {
            return Arrays.asList((Object[]) value);
        }
        if (value instanceof Collection) {
            return value;
        }
        if (value instanceof Map) {
            return value;
        }
        throw new Wrong("fore.form.repeated");
    }
}
