package io.github.ihongs.util.verify;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.util.Synt;
import java.util.Map;

/**
 * 枚举校验
 * <pre>
 * 规则参数:
 *  conf    配置名, 默认为当前配置
 *  enum    枚举名, 默认同 field.name
 * </pre>
 * @author Hongs
 */
public class IsEnum extends Rule {
    @Override
    public Object verify(Object value) throws Wrong, HongsException {
        if (value == null || "".equals(value)) {
            return   null; // 允许为空
        }

        String conf = Synt.asString(params.get("conf"));
        String name = Synt.asString(params.get("enum"));
        if (conf == null || "".equals(conf)) {
            conf = Synt.declare(params.get("__conf__"), "");
        }
        if (name == null || "".equals(name)) {
            name = Synt.declare(params.get("__name__"), "");
        }

        Map data = FormSet.getInstance(conf).getEnum (name);
        if (! data.containsKey( value.toString() ) ) {
            throw new Wrong("fore.form.not.in.enum");
        }

        return  value;
    }
}
