package io.github.ihongs.util.verify;

import io.github.ihongs.CruxException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.util.Synt;
import java.util.List;
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

        // 内部 datalist 优先
        List list = Synt.asList(getParam("datalist"));
        if (null != list) {
            String v = Synt.asString(value);
            for(Object entry : list) {
                List a = Synt.asList(entry);
                String s = Synt.asString (a.get( 0 ));
                if (v.equals(s)) {
                    return value;
                }
            }
            throw new Wrong("@fore.form.not.in.enum", value);
        }

        String conf = Synt.asString(getParam("conf"));
        String name = Synt.asString(getParam("enum"));
        if (conf == null || "".equals(conf)) {
            conf = Synt.asString(getParam("__conf__"));
        }
        if (name == null || "".equals(name)) {
            name = Synt.asString(getParam("__name__"));
        }

        Map data;
        try {
            data = FormSet.getInstance(conf).getEnum( name );
        } catch ( CruxException e) {
            throw e.toExemption( );
        }
        if (! data.containsKey(Synt.asString(value))) {
            throw new Wrong("@fore.form.not.in.enum", value);
        }

        return value;
    }
}
