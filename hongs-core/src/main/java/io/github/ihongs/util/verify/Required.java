package io.github.ihongs.util.verify;

import java.util.Collection;
import java.util.Map;

/**
 * 必填约束
 * @author Hongs
 */
public class Required extends Rule {
    @Override
    public Object verify(Value watch) throws Wrong {
        Object value = watch.get();
        if (value == null) {
            // 更新而未给值则跳过
            if (watch.isUpdate ( )
            && !watch.isDefined()) {
                return  BLANK;
            }
            throw new Wrong("fore.form.required");
        }
        if (value.equals("")) {
            throw new Wrong("fore.form.required");
        }
        if ((value instanceof Map) && ((Map) value).isEmpty()) {
            throw new Wrong("fore.form.required");
        }
        if ((value instanceof Collection) && ((Collection) value).isEmpty()) {
            throw new Wrong("fore.form.required");
        }
        if ((value instanceof Object [ ]) && ((Object [ ]) value).length==0) {
            throw new Wrong("fore.form.required");
        }

        return value;
    }
}
