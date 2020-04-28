package io.github.ihongs.util.verify;

import io.github.ihongs.Core;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认取值
 * <pre>
 * 规则参数:
 *  default 默认值, 可使用 =@别名字段 =$会话属性, =#应用属性, =%now+-偏移毫秒
 *  deforce 强制写, 控制不同阶段, create 创建时, update 更新时, always 任何时
 *  deforce 还可设为 blanks, 此时为空则返回空值, 可用 SelectHelper 在读时补全
 * </pre>
 * @author Hongs
 */
public class Default extends Rule {
    @Override
    public Object verify(Value watch) throws Wrong, Wrongs {
        Object value = watch.get();
        Object force = getParam("deforce");
        if ("create".equals(force)) {
            if (watch.isUpdate() == true ) {
                return BLANK;
            }
        } else
        if ("update".equals(force)) {
            if (watch.isUpdate() == false) {
                return BLANK;
            }
        } else
        if ("always".equals(force) == false) { // 非 always
        if ("blanks".equals(force) == false) { // 非 blanks
            /**
             * 一般情况
             * 空串空值将设为默认值
             * 更新时没赋值将会跳过
             */
            if (value != null && ! "".equals( value ) ) {
                return value;
            }
            if (watch.isUpdate() && !watch.isDefined()) {
                return BLANK;
            }
        } else {
            /**
             * 留空模式
             * 空串空值将设为空对象
             * 更新时没赋值也会跳过
             */
            if (value != null && ! "".equals( value ) ) {
                return value;
            }
            if (watch.isUpdate() && !watch.isDefined()) {
                return BLANK;
            } else {
                return null ;
            }
        }}

        Object val = getParam( "default" );
        if (null == val || ! ( val instanceof String )) {
            return  val ;
        }
        String def = ((String) val).trim();

        // 起始转义
        if (def.startsWith("\\=") ) {
            return def.substring(1);
        }

        if (def.startsWith("=@")) {
            // 别名字段, 通常用于截取字串, 清理 HTML
            if (def.startsWith("=@alias:")) {
                return alias(watch, def.substring(8));
            }

            // 计数字段
            if (def.startsWith("=@count:")) {
                return count(watch, def.substring(8));
            }

            // 组合字段
            if (def.startsWith("=@merge:")) {
                return merge(watch, def.substring(8));
            }

            // 自定方法
            try {
                String c, p ;
                int i  = def.indexOf  (':');
                if (i != -1) {
                    c  = def.substring(1,i);
                    p  = def.substring(1+i);
                } else {
                    c  = def.substring( 1 );
                    p  = "" ;
                }
                return ((Def) Core.getInstance(c)).def(watch, p);
            }
            catch (HongsExemption | ClassCastException ex) {
                throw new HongsExemption(500 , "Wrong default param", ex);
            }
        }

        // 会话属性
        if (def.startsWith("=$")) {
            return Core.getInstance(ActionHelper.class).getSessibute(def.substring(2));
        }

        // 应用属性
        if (def.startsWith("=#")) {
            return Core.getInstance(ActionHelper.class).getAttribute(def.substring(2));
        }

        // 新唯一ID
        if (def.equals("=%id")) {
            return Core.newIdentity();
        }

        // 动作选项
        if (def.equals("=%zone")) {
            return Core.ACTION_ZONE.get();
        }
        if (def.equals("=%lang")) {
            return Core.ACTION_LANG.get();
        }
        if (def.equals("=%addr")) {
            return Core.CLIENT_ADDR.get();
        }

        // 默认时间
        Matcher mat = NOW.matcher(def);
        if (mat.matches()) {
           Date now = new Date();
            String flag = mat.group(1);
            String plus = mat.group(2);
            if (flag.length() == 4) {// time 字串长度
                now.setTime(Core.ACTION_TIME.get( ));
            }
            if (plus != null) {
               Long msc = Long.valueOf(plus.substring(1));
                if ("+".equals(plus.substring(0,1))) {
                    now.setTime(now.getTime() + msc);
                } else {
                    now.setTime(now.getTime() - msc);
                }
            }
            return  now;
        }

            return  val;
    }

    private static final Pattern NOW = Pattern.compile ("^=%(time|now)([+\\-]\\d+)?$");
    private static final Pattern INJ = Pattern.compile ("\\$(\\$|\\w+|\\{.+?\\})");

    public Object alias(Value watch, String param) {
        Object v = Dict.getParam(watch.getCleans(), BLANK, param);
        if (v == BLANK) {
            return BLANK;
        }
        if (v == null ) {
            return "";
        }
        return v;
    }

    public Object count(Value watch, String param) {
        Object v = Dict.getParam(watch.getCleans(), BLANK, param);
        if (v == BLANK) {
            return BLANK;
        }
        if (v == null ) {
            return 0 ;
        }
        if (v instanceof Map) {
            return ( (Map) v).size();
        }
        if (v instanceof Collection) {
            return ( (Collection) v).size();
        }
        if (v instanceof Object [] ) {
            return ( (Object [] ) v).length;
        }
        return v.toString().length();
    }

    public Object merge(Value watch, String param) throws Wrong {
        Map vars = watch.getCleans();
        Set a = Synt.setOf(); // 缺失的键
        int i = 0; // 位置数量
        int j = 0; // 缺值数量

        /**
         * 以下代码源自 Syno.inject
         */

        Matcher matcher = INJ.matcher(param);
        StringBuffer sb = new StringBuffer();
        Object       ob;
        String       st;
        String       sd;

        while  ( matcher.find() ) {
            st = matcher.group(1);

            if (! st.equals("$")) {
                if (st.startsWith("{")) {
                    st = st.substring(1, st.length() - 1);
                    // 默认值
                    int p  = st.indexOf  ("|");
                    if (p != -1) {
                        sd = st.substring(1+p);
                        st = st.substring(0,p);
                    } else {
                        sd = "";
                    }
                } else {
                        sd = "";
                }

                // 记下缺失的字段
                if (! vars.containsKey(st)) {
                    a.add( st );
                    j ++;
                }   i ++;
                // 记下缺失的字段
                if (! vars.containsKey(st)) {
                    a.add( st );
                    j ++;
                }   i ++;

                    ob  = vars.get (st);
                if (ob != null) {
                    st  = ob.toString();
                } else {
                    st  = sd;
                }
            }

            st = Matcher.quoteReplacement(st);
            matcher.appendReplacement(sb, st);
        }

        /**
         * 创建时不理会缺失的值, 作空处理即可
         * 更新时除非全部未给值, 否则校验错误
         */
        if (watch.isUpdate( )) {
            if (i != j) {
                throw new Wrong("core.error.default.need.vars", a.toString());
            } else {
                return  BLANK ;
            }
        }

        matcher.appendTail(sb);
        return sb.toString(  );
    }

    /**
     * 默认值调节器
     * 用默认值配置 =@abc.Def:param
     */
    public static interface Def {
        public Object def(Value value, String param) throws Wrong, Wrongs;
    }
}
