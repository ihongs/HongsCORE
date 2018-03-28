package app.hongs.util.verify;

import app.hongs.util.Synt;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多值约束
 * <pre>
 * 规则参数:
 *  maxrepeat   最大数量
 *  minrepeat   最小数量
 *  defiant     需要忽略的取值列表
 *  diverse     为 true 则执行去重
 *  divorce     按此给出的分隔来拆分字串
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
            return new ArrayList ( ) ;
        }
        if (value instanceof String) {
            String v = (String) value;
            String s ;

            // 普通拆分
            s = Synt.declare(params.get("divorce"), String.class);
            if (s != null) {
                List<String> a = new ArrayList(  );
                int e , b = 0;
                while ((e = v.indexOf(s, b)) > -1) {
                    a.add(v.substring(b, e));
                    b = b + s.length (    ) ;
                }   a.add(v.substring(b   ));
                return  a;
            }

            // 正则拆分
            s = Synt.declare(params.get( "split" ), String.class);
            if (s != null) {
                List<String> a = new ArrayList(  );
                Matcher m = Pattern.compile(  s  )
                                   .matcher(  v  );
                int e , b = 0;
                while ( m.find ()) {
                    e = m.start();
                    a.add(v.substring(b, e));
                    b = m.end  ();
                }   a.add(v.substring(b   ));
                return  a;
            }

            throw  new Wrong("fore.form.repeated");
        }
        if (value instanceof Object[]) {
            return Arrays.asList((Object[]) value);
        }
        if (value instanceof Collection) {
            return value;
        }
        if (value instanceof Map) {
            return value;
        }
        throw  new Wrong("fore.form.repeated");
    }
}
