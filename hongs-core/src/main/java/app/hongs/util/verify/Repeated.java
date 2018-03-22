package app.hongs.util.verify;

import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * 多值约束
 * <pre>
 * 规则参数:
 *  maxrepeat   最大数量
 *  minrepeat   最小数量
 *  defiant     需要忽略的取值列表
 *  deverse     为 true 则执行去重
 *  split       按此给出的正则来拆分字串
 *  strim       为 true 则清理首尾空字符(仅当有 split 时有效)
 *  strip       为 true 则清理首尾含全角(仅当有 split 时有效)
 * </pre>
 * @author Hongs
 */
public class Repeated extends Rule {
    @Override
    public Object verify(Object value) throws Wrong {
        if (value == null) {
            return new ArrayList ( 0 );
        }
        if (value instanceof String  ) {
            return Arrays.asList(s((String) value));
        }
        if (value instanceof Object[]) {
            return Arrays.asList((Object[]) value );
        }
        if (value instanceof Collection) {
            return value;
        }
        if (value instanceof Map) {
            return value;
        }
        throw  new Wrong("fore.form.repeated");
    }

    private String[] s(String v ) throws Wrong {
        String s;
        s = Synt.declare(params.get("split"), String.class);
        if (s != null ) {
            if (Synt.declare(params.get("strip"), false)) {
                v = Tool.clearSC(v);
            }
            if (Synt.declare(params.get("strim"), false)) {
                v = v. trim (/***/);
            }
            return  v.split (s, -1);
        }
        throw  new Wrong("fore.form.repeated");
    }
}
