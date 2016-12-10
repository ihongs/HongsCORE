package app.hongs.util.verify;

import java.util.Collection;
import java.util.Map;

/**
 * 单值约束
 * @author Hongs
 */
public class NoRepeat2 extends Rule {
    @Override
    public Object verify(Object value) throws Wrong {
        if (value instanceof Object[ ] ) {
            throw new Wrong("fore.form.norepeat");
        }
        if (value instanceof Collection) {
            throw new Wrong("fore.form.norepeat");
        }
        if (value instanceof Map) {
            throw new Wrong("fore.form.norepeat");
        }
        return value;
    }
}
