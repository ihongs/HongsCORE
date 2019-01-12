package io.github.ihongs.util.verify;

import io.github.ihongs.util.Synt;
import java.util.Collection;
import java.util.LinkedList;
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
 *  striped     为 true 则合成一串
 *  split       按此给出的正则来拆分字串
 *  slice       按此给出的分隔来拆分字串, 或按此分隔符在最终做合并
 * </pre>
 * @author Hongs
 */
public class Repeated extends Rule implements Rulx {
    /**
     * 前置校验
     * @param value
     * @param watch
     * @return
     * @throws Wrong
     */
    @Override
    public Object verify(Object value, Veri watch) throws Wrong {
        if (value == UNDEF) {
            return   value;
        }
        if (value == null ) {
            return new LinkedList( ) ;
        }

        if (value instanceof String) {
            String v = (String) value;
            String s ;

            // 正则拆分
            s = Synt.asString( getParam("split") );
            if (s != null) {
                List<String> a = new LinkedList( );
                Matcher m = Pattern.compile( s )
                                   .matcher( v );
                int e , b = 0;
                while ( m.find ()) {
                    e = m.start();
                    a.add(v.substring(b, e));
                    b = m.end  ();
                }   a.add(v.substring(b   ));
                return  a;
            }

            // 普通拆分
            s = Synt.asString( getParam("slice") );
            if (s != null) {
                List<String> a = new LinkedList( );
                int e , b = 0;
                while ((e = v.indexOf(s, b))>-1) {
                    a.add(v.substring(b, e));
                    b = e + s.length (    ) ;
                }   a.add(v.substring(b   ));
                return  a;
            }

            throw new Wrong ("fore.form.repeated");
        }

        if (value instanceof Object [ ]) {
            return value;
        }
        if (value instanceof Collection) {
            return value;
        }
        if (value instanceof Map) {
            return value;
        }

            throw new Wrong ("fore.form.repeated");
    }

    /**
     * 最终校准
     * @param value
     * @param watch
     * @return
     * @throws Wrong
     */
    @Override
    public Object remedy(Collection value, Veri watch) throws Wrong {
        // 多个值的数量限制
        int n, c = value.size();
        n = Synt.declare(getParam("minrepeat"), 0);
        if (n != 0 && c < n) {
            throw new Wrong("fore.form.lt.minrepeat",
                    String.valueOf(n), String.valueOf(c) );
        }
        n = Synt.declare(getParam("maxrepeat"), 0);
        if (n != 0 && c > n) {
            throw new Wrong("fore.form.lt.maxrepeat",
                    String.valueOf(n), String.valueOf(c) );
        }

        // 将结果集串连起来
        if (Synt.declare(getParam("striped"), false)) {
            String s = Synt.declare(getParam("slice"),",");
            StringBuilder b = new StringBuilder();
            for(  Object v : value  ) {
                b.append(s).append(v);
            }
            if (b.length( )  !=  0  ) {
            if (s.length( )  !=  0  ) {
                return b.substring( s.length( ) );
            }
                return b. toString( );
            } else {
                return "";
            }
        }

        return value;
    }

    /**
     * 结果容器
     * 注意: 不得返回 null
     * @return
     */
    @Override
    public Collection getContext() {
        // 是否必须不同的值
        Collection context;
        if (Synt.declare(getParam("diverse") , false)) {
            context =  new LinkedHashSet();
        } else {
            context =  new LinkedList(   );
        }
        return context;
    }

    /**
     * 忽略的值
     * 注意: 不得返回 null
     * @return
     */
    @Override
    public Collection getDefiant() {
        // 可设置 defiant   为某个要忽略的值
        // 页面加 hidden    值设为要忽略的值
        // 此时如 check     全部未选代表清空
        // 默认对 null/空串 忽略
        Set ignores =  Synt.toSet(getParam("defiant"));
        if (ignores == null || ignores.isEmpty()) {
            ignores =  new HashSet(  );
            ignores.add("");
        }
        return ignores;
    }
}
