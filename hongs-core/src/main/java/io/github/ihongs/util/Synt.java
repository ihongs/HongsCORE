package io.github.ihongs.util;

import static io.github.ihongs.Core.getZoneId;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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
 * 创建的 List,Set,Map 为 ArrayList,LinkedHashSet,LinkedHashMap.
 * listOf 与 Arrays.asList 并不相同, 后者生成后不允许增删.
 * </p>
 *
 * @author Hongs
 */
public final class Synt {

    private Synt() {}

    /**
     * LOOP.NEXT 跳过此项
     * LOOP.LAST 跳出循环
     */
    public  static enum LOOP {
        NEXT,
        LAST;
        @Override
        public String toString () {
            return "";
        }
    };

    /**
     * 遍历每个节点
     */
    public  static interface Each {
        public Object run (Object v, Object k, int i);
    }

    /**
     * 空值对象, 默认跳过
     */
    private static final Number  ZERO = 00;
    private static final String  EMPT = "";

    /**
     * 视为真的字符串有: True , Yes, On, T, Y, I, 1
     */
    public  static final Pattern TRUE = Pattern.compile( "(1|I|Y|T|ON|YES|TRUE)" , Pattern.CASE_INSENSITIVE);

    /**
     * 视为假的字符串有: False, No, Off, F, N, O, 0
     */
    public  static final Pattern FAKE = Pattern.compile("(|0|O|N|F|NO|OFF|FALSE)", Pattern.CASE_INSENSITIVE);

    /**
     * 拆分字符: 用于解 Set,Map
     */
    private static final Pattern SEXP = Pattern.compile("\\s*,\\s*");
    private static final Pattern MEXP = Pattern.compile("\\s*:\\s*");

    /**
     * 拆分字符: 空字符 或 ,;|
     */
    private static final Pattern TEXP = Pattern.compile("\\s*[\\s,;|]\\s*");

    /**
     * 拆分字符: 空字符 或 ,;|.:!?&# 等及全角的标点
     */
    private static final Pattern WEXP = Pattern.compile("[\\p{Space}\\p{Punct}\\u3000-\\u303F]+"); // &&[^\\-+_] 原排除

    /**
     * 区间参数: [min,max] (min,max) 类似数学表达式
     */
    private static final Pattern RNGQ = Pattern.compile("[\\[\\(,\\)\\]]");
    private static final Pattern RNGP = Pattern.compile("^([\\(\\[])?(.*?),(.*?)([\\]\\)])?$");

    /**
     * 数字转换工具
     */
    private static final ThreadLocal<NumberFormat> DIGI = new ThreadLocal() {
        @Override
        protected NumberFormat initialValue() {
            NumberFormat nf;
            nf  = NumberFormat.getInstance ();
            nf  . setGroupingUsed (  false  );
            return nf;
        }
    };

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
        Map map = new LinkedHashMap ( objs.length/2 );
        while ( idx < objs.length ) {
            map.put ( objs [idx ++] , objs [idx ++] );
        }
        return  map;
    }

    /**
     * 快捷构建 Map
     * 注意: 可以增删, 须偶数个
     * @param objs
     * @return
     */
    public static Map  mapOf (String... objs) {
        return mapOf ((Object[]) objs);
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
     * 快捷构建 Set
     * 注意: 可以增删
     * @param objs
     * @return
     */
    public static Set  setOf (String... objs) {
        return setOf ((Object[]) objs);
    }

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
     * 快速构建 List
     * 注意: 可以增删
     * @param objs
     * @return
     */
    public static List listOf(String... objs) {
        return listOf((Object[]) objs);
    }

    /**
     * 尝试转为 Map
     * 可将 数组,List,Set 转为 Map, 其他情况构建一个值为空的 Map
     * @param val
     * @return
     */
    public static Map  asMap (Object val) {
        if (val == null) {
            return null;
        }

        if (val instanceof Map ) {
            return ( Map ) val ;
        } else if (val instanceof Collection) {
            int i = 0;
            Map m = new LinkedHashMap( );
            for (Object v : (Collection) val) {
                m.put(i ++, v );
            }
            return m;
        } else if (val instanceof Object[ ] ) {
            int i = 0;
            Map m = new LinkedHashMap( );
            for (Object o : (Object[ ] ) val) {
                m.put(i ++, o );
            }
            return m;
        } else {
            Map m = new LinkedHashMap(1);
            m.put ( null, val );
            return m;
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
            Set set = new LinkedHashSet(1);
            set.add(val);
            return  set ;
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
            return new  ArrayList(( Set ) val);
        } else if (val instanceof Map ) {
            return new  ArrayList(((Map ) val).values());
        } else if (val instanceof Object[]) {
            return new  ArrayList(Arrays.asList((Object[]) val));
        } else {
            List lst = new ArrayList(1);
            lst.add(val);
            return  lst ;
        }
    }

    /**
     * 尝试转为 Collection
     * 可将 数组,Map 转为 Collection, 其他情况构建一个单一值 List
     * @param val
     * @return
     */
    public static Collection asColl (Object val) {
        if (val == null) {
            return null;
        }

        if (val instanceof Collection ) {
            return ( Collection ) val ;
        } else if (val instanceof Map ) {
            return new  ArrayList(((Map ) val).values());
        } else if (val instanceof Object[]) {
            return new  ArrayList(Arrays.asList((Object[]) val));
        } else {
            List lst = new ArrayList(1);
            lst.add(val);
            return  lst ;
        }
    }

    /**
     * 尝试转为布尔型
     * 数组和集合仅取第一个, 数字零为 false 非零则为 true, 字符串按 TRUE,FAKE 正则判断
     * @param val
     * @return
     */
    public static Boolean asBool(Object val) {
        val = asSingle(val);
        if (val == null) {
            return null;
        }

        if (val instanceof Number) {
            return ((Number) val ).intValue( ) != 0;
        } else if (val instanceof String) {
            String str = ((String) val).trim();
            if (FAKE.matcher(str).matches( ) ) {
                return false;
            } else
            if (TRUE.matcher(str).matches( ) ) {
                return true ;
            } else {
                throw new ClassCastException("'" + str + "' can not be cast to boolean");
            }
        }

        return (Boolean) val;
    }

    /**
     * 尝试转为字节型
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳
     * @param val
     * @return
     */
    public static Byte asByte(Object val) {
        Number num = asNumber(val);
        if (num == null) {
            return null;
        }

        return num.byteValue();
    }

    /**
     * 尝试转为短整型
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳
     * @param val
     * @return
     */
    public static Short asShort(Object val) {
        Number num = asNumber(val);
        if (num == null) {
            return null;
        }

        return num.shortValue();
    }

    /**
     * 尝试转为整数
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳
     * @param val
     * @return
     */
    public static Integer asInt(Object val) {
        Number num = asNumber(val);
        if (num == null) {
            return null;
        }

        return num.intValue();
    }

    /**
     * 尝试转为长整型
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳
     * @param val
     * @return
     */
    public static Long asLong(Object val) {
        Number num = asNumber(val);
        if (num == null) {
            return null;
        }

        return num.longValue();
    }

    /**
     * 尝试转为浮点型
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳
     * @param val
     * @return
     */
    public static Float asFloat(Object val) {
        Number num = asNumber(val);
        if (num == null) {
            return null;
        }

        return num.floatValue();
    }

    /**
     * 尝试转为双精度浮点型
     * 数组和集合仅取第一个, 日期类型取毫秒时间戳
     * @param val
     * @return
     */
    public static Double asDouble(Object val) {
        Number num = asNumber(val);
        if (num == null) {
            return null;
        }

        return num.doubleValue();
    }

    /**
     * 转数字预处理, 空串视为无值, 日期取时间戳
     * @param val
     * @return
     */
    public static Number asNumber(Object val) {
        val = asSingle(val);
        if (val == null) {
            return null;
        }

        if (val instanceof Number) {
            return ( Number ) val;
        }

        // 日期转为时间戳
        if (val instanceof Date) {
            return ((Date) val ).getTime();
        } else
        if (val instanceof Calendar) {
            return ((Calendar) val ).getTimeInMillis();
        } else
        if (val instanceof Instant ) {
            return ((Instant ) val ). toEpochMilli ( );
        } else
        if (val instanceof ZonedDateTime) {
            return ((ZonedDateTime ) val).toInstant( ).toEpochMilli();
        } else
        if (val instanceof LocalDateTime) {
            return ((LocalDateTime ) val).atZone(getZoneId()).toInstant().toEpochMilli();
        } else
        if (val instanceof LocalDate) {
            return ((LocalDate) val ).atTime(LocalTime. MIN ).atZone(getZoneId()).toInstant().toEpochMilli();
        } else
        if (val instanceof LocalTime) {
            return ((LocalTime) val ).atDate(LocalDate.EPOCH).atZone(getZoneId()).toInstant().toEpochMilli();
        }

        /**
         * 尝试通过字符串转数字
         */
        String str = val.toString().trim();
        if (str.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(  str  );
        } catch (NumberFormatException ex) {
            throw  new ClassCastException("'" + val + "' can not be cast to number");
        }
    }

    /**
     * 数字转为字符串
     * @param val
     * @return
     */
    public static String asString(Number val) {
        if (val == null) {
            return null;
        }

        return DIGI.get( ).format(val);
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

        // 规避用科学计数法表示小数
        if (val  instanceof  Number) {
            return asString((Number) val);
        }

        return val.toString();
    }

    /**
     * 取单一值
     * 有多个值将会抛出 ClassCastException
     * @param val
     * @return
     */
    public static Object asSingle(Object val) {
        if (val == null) {
            return null;
        }

        if (val instanceof Object[]) {
            Object[] v2 = (Object[]) val;
            switch ( v2.length ) {
                case 0 : return null ;
                case 1 : return v2[0];
                default: throw new ClassCastException("'" + val + "' is not single");
            }
        } else
        if (val instanceof Collection) {
          Collection v2 = (Collection) val;
            switch ( v2.size() ) {
                case 0 : return null;
                case 1 : for (Object v3 : v2) return v3;
                default: throw new ClassCastException("'" + val + "' is not single");
            }
        } else
        if (val instanceof Map ) {
            Map v2 = (Map) val ;
            switch ( v2.size() ) {
                case 0 : return null;
                case 1 : for (Object v3 : v2.values()) return v3;
                default: throw new ClassCastException("'" + val + "' is not single");
            }
        } else
        if (val instanceof Iterable || val instanceof Iterator) {
            throw new ClassCastException ("`" + val + "` can not assert for single");
        }

        return val;
    }

    /**
     * 尝试转为 Map
     * 与 asMap 的不同在于当 val 是字符串时, 尝试解析 JSON 或拆分逗号冒号
     * @param val
     * @return
     */
    public static Map  toMap (Object val) {
        if (val instanceof String) {
            return toMap ((String) val);
        }

        return asMap (val);
    }

    /**
     * 尝试转为 Map
     * 尝试解析 JSON 或拆分逗号冒号
     * @param txt
     * @return
     */
    public static Map  toMap (String txt) {
        if (txt == null) {
            return null;
        }
        txt = txt.trim();
        if (txt.length() == 0) {
            return new LinkedHashMap();
        }
        if (txt.startsWith("{") && txt.endsWith("}")) {
            return  (Map ) Dist.toObject(txt);
        } else
        if (txt.startsWith("[") && txt.endsWith("]")) {
           List a = (List) Dist.toObject(txt);
            Map m = new LinkedHashMap();
            int i = 0;
            for (Object v : a ) {
                m.put(i ++, v );
            }
            return  m;
        } else {
            Map m = new LinkedHashMap();
            for(String   s : SEXP.split(txt)) {
                String[] a = MEXP.split(s,2);
                if ( 2 > a.length ) {
                    m.put( a[0], a[0] );
                } else {
                    m.put( a[0], a[1] );
                }
            }
            return  m;
        }
    }

    /**
     * 尝试转为 Set
     * 尝试通过 JSON 解析或逗号分隔
     * @param val
     * @return
     */
    public static Set  toSet (Object val) {
        if (val instanceof String) {
            return toSet ((String) val);
        }

        return asSet (val);
    }

    /**
     * 尝试转为 Set
     * 尝试通过 JSON 解析或逗号分隔
     * @param txt
     * @return
     */
    public static Set  toSet (String txt) {
        if (txt == null) {
            return null;
        }
        txt = txt.trim();
        if (txt.length() == 0) {
            return  new LinkedHashSet();
        }
        if (txt.startsWith("[") && txt.endsWith("]")) {
            return  new LinkedHashSet(
                   (List) Dist.toObject (txt)
            );
        } else
        if (txt.startsWith("{") && txt.endsWith("}")) {
            return  new LinkedHashSet(
                   ((Map) Dist.toObject (txt)).values()
            );
        } else {
            return  new LinkedHashSet(
                Arrays.asList(SEXP.split(txt))
            );
        }
    }

    /**
     * 尝试转为 List
     * 与 asList 的不同在于当 val 是字符串时, 尝试通过 JSON 解析或逗号分隔
     * @param val
     * @return
     */
    public static List toList(Object val) {
        if (val instanceof String) {
            return toList((String) val);
        }

        return asList(val);
    }

    /**
     * 尝试转为 List
     * 尝试通过 JSON 解析或逗号分隔
     * @param txt
     * @return
     */
    public static List toList(String txt) {
        if (txt == null) {
            return null;
        }
        txt = txt.trim();
        if (txt.length() == 0) {
            return  new  ArrayList();
        }
        if (txt.startsWith("[") && txt.endsWith("]")) {
            return (List) Dist.toObject (txt);
        } else
        if (txt.startsWith("{") && txt.endsWith("}")) {
            return  new  ArrayList(
                   ((Map) Dist.toObject (txt)).values()
            );
        } else {
            return  new  ArrayList(
                Arrays.asList(SEXP.split(txt))
            );
        }
    }

    /**
     * 尝试转为 Collection
     * 与 asColl 的不同在于当 val 是字符串时, 尝试通过 JSON 解析或逗号分隔
     * @param val
     * @return
     */
    public static Collection toColl(Object val) {
        if (val instanceof String) {
            return toColl((String) val);
        }

        return asColl(val);
    }

    /**
     * 尝试转为 Collection
     * 尝试通过 JSON 解析或逗号分隔
     * @param txt
     * @return
     */
    public static Collection toColl(String txt) {
        return toList(txt);
    }

    /**
     * 确保此变量为 Set  类型
     * 本方法用于处理排序字段参数
     * 这个不同于 asSet
     * 当数据为字符串时
     * 空串会返回空 Set
     * 否则按此类字符拆分: 空字符或,;|
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
        // 有非字符串则逐个转换
        Set set = asSet(val);
        DO: {
            if (set != null) for (Object vxl : set /**/ ) {
            if (vxl != null && ! (vxl instanceof String)) {
                break DO;
            }}
            return set;
        }
            set = filter(set, (v, k, i) -> asString( v ));
            return set;
    }

    /**
     * 确保此变量为 Set  类型
     * 本方法用于处理模糊查找参数
     * 这个不同于 asSet
     * 当数据为字符串时
     * 空串会返回空 Set
     * 否则按此类字符拆分: 空字符或,;|.:!等及同类全角标点
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
        // 有非字符串则逐个转换
        Set set = asSet(val);
        DO: {
            if (set != null) for (Object vxl : set /**/ ) {
            if (vxl != null && ! (vxl instanceof String)) {
                break DO;
            }}
            return set;
        }
            set = filter(set, (v, k, i) -> asString( v ));
            return set;
    }

    /**
     * 解析区间值
     * 可将数学表达式 [min,max] (min,max) 等解析为 {min,max,gte,lte}
     * 空串被视为无限, 省略括号视为开区间, 数组长二视为开区间
     * @param val
     * @return
     */
    public static Object[] toRange(Object val) {
        if (val == null || "".equals(val)) {
            return null;
        }

        /**
         * 单一数值, 直接构建
         */
        if (val instanceof Number ) {
            return new Object[] {
                val, val, true, true
            };
        }

        /**
         * 按区间格式进行解析
         */
        if (val instanceof String ) {
            String vs = declare(val, "");
            Matcher m = RNGP.matcher(vs);
            if (m.matches()) {
                val = new Object [] {
                    m.group( 2 ).trim( ),
                    m.group( 3 ).trim( ),
                    !"(".equals( m.group( 1 ) ),
                    !")".equals( m.group( 4 ) )
                };
            } else
            if ( ! RNGQ.matcher( vs ).find( ) ) {
                val = new Object [] {
                    vs , vs , true, true
                };
            } else
            throw new ClassCastException("'"+val+"' can not be cast to range");
        }

        /**
         * 也可以直接给到数组
         * 两位时默认为开区间
         */
        Object[] arr;
        if (val instanceof Object [ ] ) {
            arr = (Object[ ]) val;
        } else if (val instanceof List) {
            arr = ((List)val).toArray();
        } else {
            throw new ClassCastException("'"+val+"' can not be cast to range");
        }

        /**
         * 空串表示无穷大或小
         * 为便判断统一为空值
         */
        if (0 < arr.length) {
            if ( "".equals(arr[0])
            ||  "-".equals(arr[0])
            ||  "∞".equals(arr[0])
            || "-∞".equals(arr[0]) ) {
                arr[0] = null;
            }
        }
        if (1 < arr.length) {
            if ( "".equals(arr[1])
            ||  "+".equals(arr[1])
            ||  "∞".equals(arr[1])
            || "+∞".equals(arr[1]) ) {
                arr[1] = null;
            }
        }

        switch (arr.length) {
            case 4:
                // 格式符合可直接返回
                if (arr[2] instanceof Boolean
                &&  arr[3] instanceof Boolean) {
                    return arr;
                }

                Boolean gt_e, lt_e;
                try {
                    gt_e = defoult(asBool(arr[2]), false);
                    lt_e = defoult(asBool(arr[3]), false);
                }
                catch (ClassCastException e) {
                    throw new ClassCastException("Range index 2,3 must be boolean: "+arr);
                }

                return new Object[] {
                    arr[0], arr[1], gt_e, lt_e
                };
            case 3:
                Boolean gl_e;
                try {
                    gl_e = defoult(asBool(arr[2]), false);
                }
                catch (ClassCastException e) {
                    throw new ClassCastException("Range index 2,3 must be boolean: "+arr);
                }

                return new Object[] {
                    arr[0], arr[1], gl_e, gl_e
                };
            case 2:
                return new Object[] {
                    arr[0], arr[1], true, true
                };
            case 1:
                return new Object[] {
                    arr[0], arr[0], true, true
                };

            default:throw new ClassCastException("Range index size must be 2 to 4: "+arr);
        }
    }

    /**
     * 比较两个值的大小（顺序）
     * @param a
     * @param b
     * @param l true 表示 null 是最大, 反之最小
     * @return
     */
    public static int compare(Comparable a, Comparable b, boolean l) {
        if (a != b) {
            if (a != null
            &&  b != null) {
                return a.compareTo(b);
            }
            if (a == null) {
                return l ? +1 : -1 ;
            }
            if (b == null) {
                return l ? -1 : +1 ;
            }
        }
        return 0;
    }

    /**
     * 比较两个值的大小（顺序）
     * @param a
     * @param b
     * @return
     */
    public static int compare(Comparable a, Comparable b) {
        return compare(a, b, false);
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

        if (String.class.isAssignableFrom(cls)) {
            val = asString(val);
        } else
        if (Boolean.class.isAssignableFrom(cls)) {
            val = asBool(val);
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
        if (List.class.isAssignableFrom(cls)) {
            val = asList(val);
        } else
        if (Set.class.isAssignableFrom(cls)) {
            val = asSet(val);
        } else
        if (Map.class.isAssignableFrom(cls)) {
            val = asMap(val);
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
     * 取默认值(null, 0 均视为无值)
     * @param <T>
     * @param vals
     * @return
     */
    public static <T extends Number> T defxult(T... vals) {
        if (vals.length == 0) {
            return null;
        }
        for(int i = 0; i < vals.length - 1; i ++ ) {
            T val = vals[i];
            if (val != null && ! ZERO.equals(val)) {
                return val ;
            }
        }
        return  vals[vals.length - 1];
    }

    /**
     * 取默认值(null,"" 均视为无值)
     * @param <T>
     * @param vals
     * @return
     */
    public static <T extends String> T defxult(T... vals) {
        if (vals.length == 0) {
            return null;
        }
        for(int i = 0; i < vals.length - 1; i ++ ) {
            T val = vals[i];
            if (val != null && ! EMPT.equals(val)) {
                return val ;
            }
        }
        return  vals[vals.length - 1];
    }

    /**
     * 过滤 Map
     * @param data
     * @param leaf
     * @return
     */
    public static Map filter(Map data, Each leaf) {
        Map dat = new LinkedHashMap();
        for (Object o : data.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            Object k = e.getKey(  );
            Object v = e.getValue();
            v = leaf.run(v, k, -1 );
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
     * @param leaf
     * @return
     */
    public static Set filter(Set data, Each leaf) {
        Set dat = new LinkedHashSet();
        for (Object v : data) {
            v = leaf.run(v, null, -1);
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
     * @param leaf
     * @return
     */
    public static List filter(List data, Each leaf) {
        List dat = new LinkedList();
        int i = 0;
        for (Object v : data) {
            v = leaf.run(v, null, i ++);
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
     * @param leaf
     * @return
     */
    public static Object[] filter(Object[] data, Each leaf) {
        List dat = new LinkedList();
        int i = 0;
        for (Object v : data) {
            v = leaf.run(v, null, i ++);
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
     * 过滤 Map
     * @param data
     * @param leaf
     */
    public static void falter(Map data, Each leaf) {
        for (Object o : data.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            Object k = e.getKey(  );
            Object v = e.getValue();
            v = leaf.run(v, k, -1 );
            if (v == LOOP.LAST) {
                break;
            }
        }
    }

    /**
     * 过滤 Set
     * @param data
     * @param leaf
     */
    public static void falter(Set data, Each leaf) {
        for (Object v : data) {
            v = leaf.run(v, null, -1);
            if (v == LOOP.LAST) {
                break;
            }
        }
    }

    /**
     * 过滤 List
     * @param data
     * @param leaf
     */
    public static void falter(List data, Each leaf) {
        int i = 0;
        for (Object v : data) {
            v = leaf.run(v, null, i ++);
            if (v == LOOP.LAST) {
                break;
            }
        }
    }

    /**
     * 遍历数组
     * @param data
     * @param leaf
     */
    public static void falter(Object[] data, Each leaf) {
        int i = 0;
        for (Object v : data) {
            v = leaf.run(v, null, i ++);
            if (v == LOOP.LAST) {
                break;
            }
        }
    }

}
