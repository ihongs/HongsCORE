package io.github.ihongs.util.verify;

import io.github.ihongs.CruxException;
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
    public Object verify(Value watch) throws Wrong {
        // 跳过空值和空串
        Object value = watch.get();
        if (value  ==  null ) {
            return PASS;
        }
        if (value.equals("")) {
            return PASS;
        }

        String v = Synt.asString(value);

        // 内部 menu 优先
        Map data  = Synt.asMap(getParam("menu"));
        if (data != null) {
            if ( ! data.containsKey(v)) {
                throw new Wrong("@fore.form.not.in.enum", value);
            }
            return value;
        }

        // 查找 enum 配置
        String conf = Synt.asString(getParam("conf"));
        String name = Synt.asString(getParam("enum"));
        if (conf == null || "".equals(conf)) {
            conf = Synt.asString(getParam("__conf__"));
        }
        if (name == null || "".equals(name)) {
            name = Synt.asString(getParam("__name__"));
        }

        try {
            data = FormSet.getInstance(conf).getEnum(name);
            if ( ! data.containsKey(v)) {
                throw new Wrong("@fore.form.not.in.enum", value);
            }
            return value;
        } catch (CruxException ex) {
            throw ex.toExemption();
        }
    }
}
