package io.github.ihongs.util.verify;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认取值
 * <pre>
 * 规则参数:
 *  deforce 强制写, 控制不同阶段, create 创建时, update 更新时, always 任何时, blanks 存 null 读补全
 *  default 默认值, 可使用 @id 新唯一ID, @addr 客户端IP, @lang 访客语言, @zone 访客时区, 更多的如下:
 *      @session.会话属性
 *      @conetxt.应用属性
 *      @now+或-偏移毫秒
 *      @merge:合并字段, 如: ${字段}xxx${字段2}
 *      @alias:别名字段
 *      @count:字段计数
 *      @min:字段最小值
 *      @max:字段最大值
 *      @avg:字段平均值
 *      @sum:字段求和
 *  default 还可为 @abc.Def:param 自定义方法, 需继承下方 Def
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
                return QUIT;
            }
        } else
        if ("update".equals(force)) {
            if (watch.isUpdate() == false) {
                return QUIT;
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
                return QUIT;
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
                return QUIT;
            } else {
                return null;
            }
        }}

        Object val = getParam( "default" );
        if (null == val || ! ( val instanceof String )) {
            return  val ;
        }
        String def = ((String) val).trim();

        if (def.startsWith("@" )) {
        if (def.startsWith("@@")) {
            return def.substring(1);
        } else {
            return get(watch, def.substring(1));
        }}

        // 兼容旧版规则
        if (def.length() > 2) {
        switch (def.substring(0, 2)) {
        case "==":
            return def.substring(1);
        case "=@":
            return get(watch, def.substring(2));
        case "=%":
            return get(watch, def.substring(2));
        case "=$":
            return var("session", def.substring(2));
        case "=#":
            return var("context", def.substring(2));
        }}

        return val;
    }

    private static final Pattern VAR = Pattern.compile("^(request|context|session|cookies)\\.(.+)$");
    private static final Pattern NOW = Pattern.compile("^(time|now)([+\\-]\\d+)?$");
    private static final Pattern INJ = Pattern.compile( "\\$(\\$|\\w+|\\{.+?\\})" );

    private Object now(String flag, String plus) {
        Date now = new Date ( );
        if (flag.length() == 4) {
            now.setTime(Core.ACTION_TIME.get( )); // time 表示使用动作开始时间
        }
        if (plus != null) {
            Long msc = Long.valueOf(plus.substring(1));
            if (plus.charAt(0) == '+') {
                now.setTime(now.getTime() + msc);
            } else {
                now.setTime(now.getTime() - msc);
            }
        }
        return  now;
    }

    private Object var(String feed, String part) {
        Object  data = null;
        String  path = null;
        int p = part.indexOf(".");
        if (p > 0) {
            path = part.substring(1+p);
            part = part.substring(0,p);
        }
        switch (feed) {
            case "request":
                data = ActionHelper.getInstance().getRequestData().get(part);
                break;
            case "context":
                data = ActionHelper.getInstance().getAttribute(part);
                break;
            case "session":
                data = ActionHelper.getInstance().getSessibute(part);
                break;
            case "cookies":
                data = ActionHelper.getInstance().getCookibute(part);
                break;
        }
        if (data != null && path != null) {
            data  = Dict.getParam(Synt.asMap(data), path);
        }
        return data;
    }

    private Object get(Value watch, String def) throws Wrong, Wrongs {
        Matcher mat;
        mat = VAR.matcher(def);
        if (mat.matches()) {
            return var(mat.group(1), mat.group(2));
        }
        mat = NOW.matcher(def);
        if (mat.matches()) {
            return now(mat.group(1), mat.group(2));
        }

        switch (def) {
            case "null" :
                return null;
            case "void" :
                return QUIT;
            case "zone" :
                return Core.ACTION_ZONE.get();
            case "lang" :
                return Core.ACTION_LANG.get();
            case "addr" :
                return Core.CLIENT_ADDR.get();
            case  "id"  :
                return Core.newIdentity(/**/);
            case "uid"  :
                return ActionHelper.getInstance().getSessibute(Cnst.UID_SES);
        }

        String  prm;
        int pos  = def.indexOf(':');
        if (pos != -1) {
            prm  = def.substring(1+ pos);
            def  = def.substring(0, pos);
        } else {
            prm  = null;
        }

        switch (def) {
            case "alias":
                return alias(watch, prm);
            case "merge":
                return merge(watch, prm);
            case "count":
                return count(watch, prm);
            case  "max" :
                return  max (watch, prm);
            case  "min" :
                return  min (watch, prm);
            case  "sum" :
                return  sum (watch, prm);
            case  "avg" :
                return  avg (watch, prm);
        }

        // 自定方法
        return ((Def) Core.getInstance(def)).def(watch, prm);
    }

    public static Object alias(Value watch, String param) {
        Object v = Dict.getParam(watch.getCleans(), QUIT, param);
        if (v == QUIT) {
            return QUIT;
        }
        if (v == null) {
            return "";
        }
        return v;
    }

    public static Object count(Value watch, String param) {
        Object v = Dict.getParam(watch.getCleans(), QUIT, param);
        if (v == QUIT) {
            return QUIT;
        }
        if (v == null) {
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

    public static Object merge(Value watch, String param) throws Wrong {
        Map vars = watch.getCleans();
        Set a = Synt.setOf(); // 缺失的键
        int i = 0; // 缺值数量
        int j = 0; // 取值数量

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
                    i ++;
                }   j ++;

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
        if (watch.isUpdate(  )
            &&  i != 0) {
            if (i != j) {
                throw new Wrong("Default merge need vars: "+a.toString());
            } else {
                return QUIT;
            }
        }

        matcher.appendTail(sb);
        return sb.toString(  );
    }

    public static Object max(Value watch, String param) throws Wrong {
        Object v = Dict.getParam(watch.getCleans(), QUIT, param);
        if (v == QUIT) {
            return QUIT;
        }
        List l = Synt.asList(v);
        if (l == null || l.isEmpty( ) ) {
            return null ;
        }
            Double c = null;
        for(Object o : l ) {
            Double n = Synt.asDouble(o);
            if (n != null
            && (c == null
            ||  c  < n ) ) {
                c  = n ;
            }
        }
        return  c  ;
    }

    public static Object min(Value watch, String param) throws Wrong {
        Object v = Dict.getParam(watch.getCleans(), QUIT, param);
        if (v == QUIT) {
            return QUIT;
        }
        List l = Synt.asList(v);
        if (l == null || l.isEmpty( ) ) {
            return null ;
        }
            Double c = null;
        for(Object o : l ) {
            Double n = Synt.asDouble(o);
            if (n != null
            && (c == null
            ||  c  > n ) ) {
                c  = n ;
            }
        }
        return  c  ;
    }

    public static Object sum(Value watch, String param) throws Wrong {
        Object v = Dict.getParam(watch.getCleans(), QUIT, param);
        if (v == QUIT) {
            return QUIT;
        }
        List l = Synt.asList(v);
        if (l == null || l.isEmpty( ) ) {
            return null ;
        }
            Double c = null;
        for(Object o : l ) {
            Double n = Synt.asDouble(o);
            if (n != null) {
            if (c != null) {
                c += n;
            } else {
                c  = n;
            }}
        }
        return  c  ;
    }

    public static Object avg(Value watch, String param) throws Wrong {
        Object v = Dict.getParam(watch.getCleans(), QUIT, param);
        if (v == QUIT) {
            return QUIT;
        }
        List l = Synt.asList(v);
        if (l == null || l.isEmpty( ) ) {
            return null ;
        }
            Double c = null;
        for(Object o : l ) {
            Double n = Synt.asDouble(o);
            if (n != null) {
            if (c != null) {
                c += n;
            } else {
                c  = n;
            }}
        }
        return  c  / l.size();
    }

    /**
     * 默认值调节器
     * 用默认值配置 @abc.Def:param
     */
    public static interface Def {
        public Object def(Value value, String param) throws Wrong, Wrongs;
    }
}
