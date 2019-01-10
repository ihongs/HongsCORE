package io.github.ihongs.util.verify;

import java.util.Collection;
import java.util.Map;

/**
 * 必填约束
 * @author Hongs
 */
public class Required extends Rule {
    @Override
    public Object verify(Object value, Verity watch) throws Wrong {
        if (null  ==  value ) {
            if (watch.isUpdate()) {
                return BLANK;
            }
            throw new Wrong("fore.form.required");
        }
        if ("".equals(value)) {
            throw new Wrong("fore.form.required");
        }
        if ((value instanceof Map) && ((Map) value).isEmpty()) {
            throw new Wrong("fore.form.required");
        }
        if ((value instanceof Collection) && ((Collection) value).isEmpty()) {
            throw new Wrong("fore.form.required");
        }
        if ((value instanceof Object[ ] ) && ((Object[ ] ) value).length==0) {
            throw new Wrong("fore.form.required");
        }
        return value;
    }
}
