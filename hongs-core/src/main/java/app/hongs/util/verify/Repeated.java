package app.hongs.util.verify;

import app.hongs.util.Synt;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    /**
     * 前置校验
     * @param value
     * @return
     * @throws Wrong
     */
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
                    b = e + s.length (    ) ;
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
        if (value instanceof Object[ ] ) {
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

    /**
     * 最终校准
     * @param value
     * @return
     * @throws Wrong
     */
    public Object verify(Collection value) throws Wrong {
        // 多个值的数量限制
        int n, c = value.size();
        n = Synt.declare(params.get("minrepeat"), 0);
        if (n != 0 && c < n) {
            throw new Wrong("fore.form.lt.minrepeat",
                    String.valueOf(n), String.valueOf(c));
        }
        n = Synt.declare(params.get("maxrepeat"), 0);
        if (n != 0 && c > n) {
            throw new Wrong("fore.form.lt.maxrepeat",
                    String.valueOf(n), String.valueOf(c));
        }
        return value;
    }

    /**
     * 结果容器
     * 注意: 不得返回 null
     * @return
     */
    public Collection getContext() {
        // 是否必须不同的值
        Collection context;
        if (Synt.declare(params.get("diverse"), false )) {
            context =  new LinkedHashSet();
        } else {
            context =  new  ArrayList   ();
        }
        return context;
    }

    /**
     * 忽略的值
     * 注意: 不得返回 null
     * @return
     */
    public Collection getDefiant() {
        // 可设置 defiant   为某个要忽略的值
        // 页面加 hidden    值设为要忽略的值
        // 此时如 check     全部未选代表清空
        // 默认对 null/空串 忽略
        Set ignores =  Synt.toSet(params.get("defiant"));
        if (ignores == null || ignores.isEmpty()) {
            ignores =  new  HashSet( );
            ignores.add("");
        }
        return ignores;
    }
}
