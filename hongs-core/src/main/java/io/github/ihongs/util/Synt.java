package io.github.ihongs.util;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 常用语法补充
 *
 * <p>
 * asXxx,toXxx 都可以对类型进行转换,
 * asXxx 不会对字符串按特别格式解析,
 * toXxx 遇到字符串时尝试按格式解析, 如按分隔符拆解或解析 JSON.
 * 但需要注意这两类方法都是转为对象, 尤其是基础类型要注意 null;
 * 可加上 defoult, 或使用 declare, 类型明确时不推荐用后者.
 * </p>
 *
 * <p>
 * 创建的 List,Set,Map 为 ArayList,LinkedHashSet,LinkedHashMap.
 * listOf 与 Arrays.asList 并不相同, 后者生成后不允许增删.
 * </p>
 *
 * @author Hongs
 */
public final class Synt {

    /**
     * LOOP.NEXT 跳过此项
     * LOOP.LAST 跳出循环
     */
    public static enum LOOP {

        NEXT, LAST;

        @Override
        public String toString() {
            return "";
        }

    };

    private static final Number  ZERO = 0 ;
    private static final String  EMPT = "";
    private static final Boolean FALS = false;

    private static final Pattern SEXP = Pattern.compile("\\s*,\\s*");
    private static final Pattern MEXP = Pattern.compile("\\s*:\\s*");

    /**
     * 拆分字符: 空字符 或 +,;
     */
    private static final Pattern TEXP = Pattern.compile("\\s*[\\s\\+,;]\\s*");

    /**
     * 拆分字符: 空字符 或 +,;.:!?'" 及全角标点符号
     */
    private static final Pattern WEXP = Pattern.compile("\\s*[\\p{Space}\\p{Punct}\\u3000-\\u303F]\\s*");

    /**
     * 区间参数: [min,max] (min,max) 类似数学表达式
     */
    private static final Pattern RNGP = Pattern.compile("^([\\(\\[])?(.*?),(.*?)([\\]\\)])?$");

    /**
     * 视为真的字符串有: True , Yes, On, T, Y, I, 1
     */
    public  static final Pattern TRUE = Pattern.compile( "(1|I|Y|T|ON|YES|TRUE)" , Pattern.CASE_INSENSITIVE);

    /**
     * 视为假的字符串有: False, No, Off, F, N, O, 0
     */
    public  static final Pattern FAKE = Pattern.compile("(|0|O|N|F|NO|OFF|FALSE)", Pattern.CASE_INSENSITIVE);

    /**
     * 快速构建 List
     * 注意: 可以增删
     * @param objs
     * @return
     */
    public static List listOf(Object... objs) {
        return new  ArrayList   (Arrays.asList(objs));
    }

    /**
     * 快捷构建 Set
     * 注意: 可以增删
     * @param objs
     * @return
     */
    public static Set  setOf (Object... objs) {
        return new LinkedHashSet(Arrays.asList(objs));
    }

    /**
     * 快捷构建 Map
     * 注意: 可以增删, 须偶数个
     * @param objs
     * @return
     */
    public static Map  mapOf (Object... objs) {
        if ( objs.length % 2 != 0 ) {
            throw new IndexOutOfBoundsException("mapOf must provide even numbers of entries");
        }

        int idx = 0;
        Map map = new LinkedHashMap();
        while ( idx < objs.length ) {
            map.put ( objs [idx ++] , objs [idx ++] );
        }
        return  map;
    }

    /**
     * 尝试转为数组
     * 与 asArray 的不同在于当 val 是字符串时, 尝试通过 JSON 解析或逗号分隔
     * @param val
     * @return
     */
    public static Object[] toArray(Object val) {
        if (val == null) {
            return null;
        }

        if (val instanceof String) {
            if ("".equals(val)) {
                return new Object[0];
            }
            String text = ( (String) val).trim(  );
            if (text.startsWith("[") && text.endsWith("]")) {
                return ((List) Data.toObject(text))
                                   .toArray (    );
            } else {
                return SEXP.split(text);
            }
        }

        return asArray(val);
    }

    /**
     * 尝试转为 List
     * 与 asList 的不同在于当 val 是字符串时, 尝试通过 JSON 解析或逗号分隔
     * @param val
     * @return
     */
    public static List toList(Object val) {
        if (val == null) {
            return null;
        }

        if (val instanceof String) {
            if ("".equals(val)) {
                return  new ArrayList();
            }
            String text = ( (String) val).trim(  );
            if (text.startsWith("[") && text.endsWith("]")) {
                return (List) Data.toObject (text);
            } else {
                return  new ArrayList(
                    Arrays.asList(SEXP.split(text))
                );
            }
        }

        return asList(val);
    }

    /**
     * 尝试转为 Set
     * 与 asSet 的不同在于当 val 是字符串时, 尝试通过 JSON 解析或逗号分隔
     * @param val
     * @return
     */
    public static Set  toSet (Object val) {
        if (val == null) {
            return null;
        }

        if (val instanceof String) {
            if ("".equals(val)) {
                return  new LinkedHashSet();
            }
            String text = ( (String) val).trim(  );
            if (text.startsWith("[") && text.endsWith("]")) {
                return  new LinkedHashSet(
                       (List) Data.toObject (text)
                );
            } else {
                return  new LinkedHashSet(
                    Arrays.asList(SEXP.split(text))
                );
            }
        }

        return asSet (val);
    }

    /**
     * 尝试转为 Map
     * 与 asMap 的不同在于当 val 是字符串时, 尝试解析 JSON 或拆分逗号冒号
     * @param val
     * @return
     */
    public static Map  toMap (Object val) {
        if (val == null) {
            return null;
        }

        if (val instanceof String) {
            if ("".equals(val)) {
                return  new LinkedHashMap();
            }
            String text = ( (String) val).trim(  );
            if (text.startsWith("{") && text.endsWith("}")) {
                return (Map ) Data.toObject (text);
            } else {
                Map m = new LinkedHashMap();
                for(String   s : SEXP.split (text)) {
                    String[] a = MEXP.split (s, 2);
                    if ( 2 > a.length ) {
                        m.put( a[0], a[0] );
                    } else {
                        m.put( a[0], a[1] );
                    }
                }
                return  m ;
            }
        }

        return asMap (val);
    }

    /**
     * 尝试转为数组
     * 可将 List,Set,Map 转为数组, 其他情况构建一个单一值的数组
     * @param val
     * @return
     */
    public static Object[] asArray(Object val) {
        if (val == null) {
            return null;
        }

        if (val instanceof Object[] ) {
            return (Object[]) val;
        } else if (val instanceof List) {
            return ((List)val).toArray();
        } else if (val instanceof Set ) {
            return ((Set) val).toArray();
        } else if (val instanceof Map ) {
            return ((Map) val).values( ).toArray();
        } else {
            return new Object[ ] { val };
        }
    }

    /**
     * 尝试转为 List
     * 可将 数组,Set,Map 转为 List, 其他情况构建一个单一值的 List
     * @param val
     * @return
     */
    public static List asList(Object val) {
        if (val == null) {
            return null;
        }

        if (val instanceof List) {
            return ( List) val ;
        } else if (val instanceof Set ) {
            return new ArrayList(( Set ) val);
        } else if (val instanceof Map ) {
            return new ArrayList(((Map ) val).values());
        } else if (val instanceof Object[]) {
            return new ArrayList(Arrays.asList((Object[]) val));
        } else {
            List lst = new ArrayList ();
            lst.add(val);
            return  lst ;
        }
    }

    /**
     * 尝试转为 Set
     * 可将 数组,List,Map 转为 Set, 其他情况构建一个单一值的 Set
     * @param val
     * @return
     */
    public static Set  asSet (Object val) {
        if (val == null) {
            return null;
        }

        if (val instanceof Set ) {
            return ( Set ) val ;
        } else if (val instanceof List) {
            return new LinkedHashSet((List) val);
        } else if (val instanceof Map ) {
            return new LinkedHashSet(((Map) val).values());
        } else if (val instanceof Object[]) {
            return new LinkedHashSet(Arrays.asList((Object[]) val));
        } else {
            Set set = new LinkedHashSet();
            set.add(val);
            return  set ;
        }
    }

    /**
     * 尝试转为 List
     * 非 Map 类型均会转换失败, 因 Map 转集合扔掉键即可,
     * 但无法从其他集合类型中得到明确的可以作为键的东西.
     * @param val
     * @return
     */
    public static Map  asMap (Object val) {
        if (val == null) {
            return null;
        }

        if (val instanceof Map ) {
            return ( Map ) val ;
        }

        // 其他类型均无法转换为 Map
        throw new ClassCastException("'" + val + "' can not be cast to Map");
    }

    /**
     * 确定转为字符串
     * 数组和集合仅取第一个
     * @param val
     * @return
     */
    public static String asString(Object val) {
        val = asSingle(val);
        if (val == null) {
            return null;
        }

        return val.toString();
    }

    /**
     * 尝试转为整数
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳
     * @param val
     * @return
     */
    public static Integer asInt(Object val) {
        val = asNumber(val);
        if (val == null) {
            return null;
        }

        if (val instanceof Number) {
            return ((Number) val ).intValue( );
        } else
        if (val instanceof String) {
            String str = ((String) val).trim();
            try {
                return new BigDecimal(str).intValue();
            } catch (NumberFormatException ex) {
                throw new ClassCastException("'" + val + "' can not be cast to int");
            }
        }

        return (Integer) val;
    }

    /**
     * 尝试转为长整型
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳
     * @param val
     * @return
     */
    public static Long asLong(Object val) {
        val = asNumber(val);
        if (val == null) {
            return null;
        }

        if (val instanceof Number) {
            return ((Number) val ).longValue();
        } else
        if (val instanceof String) {
            String str = ((String) val).trim();
            try {
                return new BigDecimal(str).longValue();
            } catch (NumberFormatException ex) {
                throw new ClassCastException("'" + val + "' can not be cast to long");
            }
        }

        return (Long) val;
    }

    /**
     * 尝试转为浮点型
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳
     * @param val
     * @return
     */
    public static Float asFloat(Object val) {
        val = asNumber(val);
        if (val == null) {
            return null;
        }

        if (val instanceof Number) {
            return ((Number) val).floatValue();
        } else
        if (val instanceof String) {
            String str = ((String) val).trim();
            try {
                return new BigDecimal(str).floatValue();
            } catch (NumberFormatException ex) {
                throw new ClassCastException("'" + val + "' can not be cast to float");
            }
        }

        return (Float) val;
    }

    /**
     * 尝试转为双精度浮点型
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳
     * @param val
     * @return
     */
    public static Double asDouble(Object val) {
        val = asNumber(val);
        if (val == null) {
            return null;
        }

        if (val instanceof Number) {
            return ((Number)val).doubleValue();
        } else
        if (val instanceof String) {
            String str = ((String) val).trim();
            try {
                return new BigDecimal(str).doubleValue();
            } catch (NumberFormatException ex) {
                throw new ClassCastException("'" + val + "' can not be cast to double");
            }
        }

        return (Double) val;
    }

    /**
     * 尝试转为短整型
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳
     * @param val
     * @return
     */
    public static Short asShort(Object val) {
        val = asNumber(val);
        if (val == null) {
            return null;
        }

        if (val instanceof Number) {
            return ((Number) val).shortValue();
        } else
        if (val instanceof String) {
            String str = ((String) val).trim();
            try {
                return new BigDecimal(str).shortValue();
            } catch (NumberFormatException ex) {
                throw new ClassCastException("'" + val + "' can not be cast to short");
            }
        }

        return (Short) val;
    }

    /**
     * 尝试转为字节型
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳
     * @param val
     * @return
     */
    public static Byte asByte(Object val) {
        val = asNumber(val);
        if (val == null) {
            return null;
        }

        if (val instanceof Number) {
            return ((Number) val ).byteValue();
        } else
        if (val instanceof String) {
            String str = ((String) val).trim();
            try {
                return new BigDecimal(str).byteValue();
            } catch (NumberFormatException ex) {
                throw new ClassCastException("'" + val + "' can not be cast to byte");
            }
        }

        return (Byte) val;
    }

    /**
     * 尝试转为布尔型
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳, 数字非零为 true
     * @param val
     * @return
     */
    public static Boolean asBool(Object val) {
        val = asNumber(val);
        if (val == null) {
            return null;
        }

        if (val instanceof Number) {
            return ((Number) val ).intValue( ) != 0;
        } else if (val instanceof String) {
            String str = ((String) val).trim();
            if (TRUE.matcher(str).matches( ) ) {
                return true ;
            } else
            if (FAKE.matcher(str).matches( ) ) {
                return false;
            } else {
                throw new ClassCastException("'" + str + "' can not be cast to boolean");
            }
        }

        return (Boolean) val;
    }

    /**
     * 转数字预处理, 空串视为无值, 日期取时间戳
     * @param val
     * @return
     */
    private static Object asNumber(Object val) {
        val = asSingle(val);

        if (val == null) {
            return null;
        }

        // 空串视为未取值
        if (EMPT.equals(val)) {
            return null;
        }

        // 日期转为时间戳
        if (val instanceof Date) {
            val = ( (Date) val ).getTime();
        }

        return val;
    }

    /**
     * 针对 servlet 的 requestMap 规则, 多个取首个值
     * @param val
     * @return
     */
    private static Object asSingle(Object val) {
        if (val == null) {
            return null;
        }

        try {
            if (val instanceof Object[]) {
                return ((Object[]) val )[0];
            } else
            if (val instanceof List) {
                return ((List) val ).get(0);
            } else
            if (val instanceof Set ) {
                return ((Set ) val ).toArray()[0];
            } else
            if (val instanceof Map ) {
                return ((Map ) val ).values( ).toArray()[0];
            }
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }

        return val;
    }

    /**
     * 确保此变量为 Set  类型
     * 本方法用于处理排序字段参数
     * 与 declare(Object, T) 不同
     * 当数据为字符串时
     * 空串会返回空 Set
     * 否则按此类字符拆分: 空字符或+,;
     * @param val
     * @return
     */
    public static Set<String> toTerms(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof String) {
            String   s = ((String) val).trim();
            if ("".equals(s)) {
                return new LinkedHashSet();
            }
            String[] a = TEXP.split (s);
            List   b = Arrays.asList(a);
            return new LinkedHashSet(b);
        }
        return asSet(val);
    }

    /**
     * 确保此变量为 Set  类型
     * 本方法用于处理模糊查找参数
     * 与 declare(Object, T) 不同
     * 当数据为字符串时
     * 空串会返回空 Set
     * 否则按此类字符拆分: 空字符或+,;.:!?'"及同类全角标点
     * @param val
     * @return
     */
    public static Set<String> toWords(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof String) {
            String   s = ((String) val).trim();
            if ("".equals(s)) {
                return new LinkedHashSet();
            }
            String[] a = WEXP.split (s);
            List   b = Arrays.asList(a);
            return new LinkedHashSet(b);
        }
        return asSet(val);
    }

    /**
     * 解析区间值
     * 可将数学表达式 [min,max] (min,max) 等解析为 {min,max,gte,lte}
     * 方括号可以省略, 空串被视为无限, 数组仅给两个则后两个认为 true
     * @param val
     * @return
     */
    public static Object[] toRange(Object val) {
        if (val == null || "".equals(val)) {
            return null;
        }

        /**
         * 按区间格式进行解析
         */
        if (val instanceof String ) {
            String vs = declare(val, "");
            Matcher m = RNGP.matcher(vs);
            if (m.matches()) {
                String m2 = m.group (2 ).trim();
                String m3 = m.group (3 ).trim();
                return new Object[] {
                    ! "".equals(m2) ? m2 : null,
                    ! "".equals(m3) ? m3 : null,
                    !"(".equals(m.group ( 1 ) ),
                    !")".equals(m.group ( 4 ) )
                };
            }
            throw new ClassCastException("'"+val+"' can not be cast to range");
        }

        /**
         * 也可以直接给到数组
         * 两位时默认为开区间
         */
        Object[] arr;
        if (val instanceof Object[]) {
            arr = (Object[ ]) val;
        } else if (val instanceof List) {
            arr = ((List)val).toArray();
        } else {
            throw new ClassCastException("'"+val+"' can not be cast to range");
        }

        switch (arr.length) {
            case 4:
                Boolean lte, gte;
                try {
                    lte = defoult(asBool(arr[2]), false);
                    gte = defoult(asBool(arr[3]), false);
                }
                catch (ClassCastException e) {
                    throw new ClassCastException("Range index 2,3 must be boolean: "+arr);
                }

                return new Object[] {
                    arr[0], arr[1], lte , gte
                };
            case 2:
                return new Object[] {
                    arr[0], arr[1], true, true
                };

            default:throw new ClassCastException("Range index size must be 2 or 4: "+arr);
        }
    }

    /**
     * 确保此变量类型为 cls 类型
     * string,number(int,long...) 类型间可互转;
     * cls 为 Boolean  时:
     *      非 0 数字为 true,
     *      空字符串为 false,
     *      字符串 1,y,t,yes,true 为真,
     *      字符串 0,n,f,no,false 为假;
     * cls 为 Array,List,Set 时:
     *      val 非 List,Set,Map 时构建 Array,List,Set 后将 val 加入其下,
     *      val 为 Map 则取 values;
     * 但其他类型均无法转为 Map 的类型.
     * 如果类型明确建议使用 asXxx 方法.
     * @param <T>
     * @param val
     * @param cls
     * @return
     */
    public static <T> T declare(Object val, Class<T> cls) {
        if (cls == null) {
            throw  new NullPointerException("declare cls can not be null");
        }
        if (val == null) {
            return null;
        }

        if (Object[].class.isAssignableFrom(cls)) {
            val = asArray(val);
        } else
        if (List.class.isAssignableFrom(cls)) {
            val = asList(val);
        } else
        if (Set.class.isAssignableFrom(cls)) {
            val = asSet(val);
        } else
        if (Map.class.isAssignableFrom(cls)) {
            val = asMap(val);
        } else
        if (String.class.isAssignableFrom(cls)) {
            val = asString(val);
        } else
        if (Integer.class.isAssignableFrom(cls)) {
            val = asInt(val);
        } else
        if (Long.class.isAssignableFrom(cls)) {
            val = asLong(val);
        } else
        if (Float.class.isAssignableFrom(cls)) {
            val = asFloat(val);
        } else
        if (Double.class.isAssignableFrom(cls)) {
            val = asDouble(val);
        } else
        if (Short.class.isAssignableFrom(cls)) {
            val = asShort(val);
        } else
        if (Byte.class.isAssignableFrom(cls)) {
            val = asByte(val);
        } else
        if (Boolean.class.isAssignableFrom(cls)) {
            val = asBool(val);
        }

        return (T) val;
    }

    /**
     * 确保 val 的类型为 def 的类型
     * 如果 val 为空则取 def 默认值
     * 其他的说明请参见 declare(Object, Class)
     * @param <T>
     * @param val
     * @param def
     * @return
     */
    public static <T> T declare(Object val, T def) {
        if (def == null) {
            throw  new NullPointerException("declare def can not be null");
        }
        val = declare(val, def.getClass());
        if (val == null) {
            return def;
        }
        return (T) val;
    }

    /**
     * 取默认值(null 视为无值)
     * @param <T>
     * @param vals
     * @return
     */
    public static <T> T defoult(T... vals) {
        for (T  val :  vals) {
            if (val != null) {
                return val ;
            }
        }
        return  null;
    }

    /**
     * 取默认值(null 视为无值)
     * Java8 环境的函数式支持
     * 可以更好的做到惰性计算
     * @param <T>
     * @param val
     * @param vals 由于在 java7 下编译, 无法使用 Supplier
     * @return
     */
    public static <T> T defoult(T val, Defn<T>... vals) {
        if (val != null) {
            return val ;
        }
        for (Defn<T> def : vals) {
            val = def.get();
            if (val != null) {
                return val ;
            }
        }
        return  null;
    }

    /**
     * 取默认值(null,false,0,"" 均视为无值, 同 javascript)
     * @param <T>
     * @param vals
     * @return
     */
    public static <T> T defxult(T... vals) {
        for (T  val :  vals) {
            if (val != null
            && !FALS.equals(val)
            && !EMPT.equals(val)
            && !ZERO.equals(val)) {
                return val ;
            }
        }
        return  null;
    }

    /**
     * 取默认值(null,false,0,"" 均视为无值, 同 javascript)
     * Java8 环境的函数式支持
     * 可以更好的做到惰性计算
     * @param <T>
     * @param val
     * @param vals 由于在 java7 下编译, 无法使用 Supplier
     * @return
     */
    public static <T> T defxult(T val, Defn<T>... vals) {
        if (val != null
        && !FALS.equals(val)
        && !EMPT.equals(val)
        && !ZERO.equals(val)) {
            return val ;
        }
        for (Defn<T> def : vals) {
            val = def.get();
            if (val != null
            && !FALS.equals(val)
            && !EMPT.equals(val)
            && !ZERO.equals(val)) {
                return val ;
            }
        }
        return  null;
    }

    /**
     * 过滤 Map
     * @param data
     * @param conv
     * @return
     */
    public static Map filter(Map data, Each conv) {
        Map dat = new LinkedHashMap();
        for (Object o : data.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            Object k = e.getKey(  );
            Object v = e.getValue();
            v = conv.run(v, k, -1 );
            if (v == LOOP.NEXT) {
                continue;
            }
            if (v == LOOP.LAST) {
                break;
            }
            dat.put(k, v);
        }
        return dat;
    }

    /**
     * 过滤 Set
     * @param data
     * @param conv
     * @return
     */
    public static Set filter(Set data, Each conv) {
        Set dat = new LinkedHashSet();
        for (Object v : data) {
            v = conv.run(v, null, -1);
            if (v == LOOP.NEXT) {
                continue;
            }
            if (v == LOOP.LAST) {
                break;
            }
            dat.add(v);
        }
        return dat;
    }

    /**
     * 过滤 List
     * @param data
     * @param conv
     * @return
     */
    public static List filter(List data, Each conv) {
        List dat = new ArrayList();
        for (int i = 0; i < data.size(); i++) {
            Object v = data.get(i);
            v = conv.run(v, null, i);
            if (v == LOOP.NEXT) {
                continue;
            }
            if (v == LOOP.LAST) {
                break;
            }
            dat.add(v);
        }
        return dat;
    }

    /**
     * 过滤数组
     * @param data
     * @param conv
     * @return
     */
    public static Object[] filter(Object[] data, Each conv) {
        List dat = new ArrayList();
        for (int i = 0; i < data.length; i++) {
            Object v = data[i];
            v = conv.run(v, null, i);
            if (v == LOOP.NEXT) {
                continue;
            }
            if (v == LOOP.LAST) {
                break;
            }
            dat.add(v);
        }
        return dat.toArray();
    }

    /**
     * 过滤全部叶子节点
     * @param data
     * @param conv
     * @return
     */
    public static Map digest(Map data, Deep conv) {
        return filter(data, new EachLeaf(conv));
    }

    /**
     * 过滤全部叶子节点
     * @param data
     * @param conv
     * @return
     */
    public static Set digest(Set data, Deep conv) {
        return filter(data, new EachLeaf(conv));
    }

    /**
     * 过滤全部叶子节点
     * @param data
     * @param conv
     * @return
     */
    public static List digest(List data, Deep conv) {
        return filter(data, new EachLeaf(conv));
    }

    /**
     * 过滤全部叶子节点
     * @param data
     * @param conv
     * @return
     */
    public static Object[] digest(Object[] data, Deep conv) {
        return filter(data, new EachLeaf(conv));
    }

    //** 内部工具类 **/

    /**
     * 用于提供默认取值
     * @param <T>
     */
    public static interface Defn<T> {
        public T get();
    }

    /**
     * 用于遍历每个节点
     */
    public static interface Each {
        public Object run(Object v, Object k, int i);
    }

    /**
     * 用于遍历叶子节点
     */
    public static interface Deep {
        public Object run(Object v, List p);
    }

    private static class EachLeaf implements Each {
        private final Deep deep;
        private final List path;

        public EachLeaf(Deep leaf) {
            this.deep = leaf;
            this.path = new ArrayList( );
        }

        @Override
        public Object run(Object v, Object k, int i) {
            List p = new ArrayList(path);
                 p.add(i != -1 ? i : k );
            if (v instanceof Map ) {
                return filter((Map ) v, this);
            } else
            if (v instanceof Set ) {
                return filter((Set ) v, this);
            } else
            if (v instanceof List) {
                return filter((List) v, this);
            } else
            if (v instanceof Object[]) {
                return filter((Object[]) v, this);
            } else {
                return deep.run(v, p);
            }
        }
    }

    /*
    public static void main(String[] args) {
        // filter, digest 的函数式特性测试
        Object[] a = new Object[] {"a", "b", "c",
                     new Object[] {"d", "e", "f"},
                     setOf("g", "h", "i", "j"),
                     mapOf("k", "l", "m", "n")};

        System.err.println("filter:");
        Object[] b = filter(a, (v, k, i) -> {
            System.err.println (i+":"+v);
            return v.toString().toUpperCase();
        });
        System.err.println("");

        System.err.println("digest:");
        Object[] c = digest(a, (v, p) -> {
            System.err.println (p+":"+v);
            return v.toString().toUpperCase();
        });
        System.err.println("");

        System.err.print("a = ");
        Data.dumps(a);
        System.err.print("b = ");
        Data.dumps(b);
        System.err.print("c = ");
        Data.dumps(c);

        // defoult, defxult 的函数式特性测试
        String v;
        v = null;
        System.err.println(defoult(v, ()->"a"));
        v = "";
        System.err.println(defxult(v, ()->"1"));
    }
    */

}
