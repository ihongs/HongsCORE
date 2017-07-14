package app.hongs.util;

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
 * @author Hongs
 */
public final class Synt {

    private static final Number  ZERO = 0 ;
    private static final String  EMPT = "";
    private static final Boolean FALS = false;

    /**
     * Each.run 或 EachLeaf.run 里
 返回 LOOP.NEXT 跳过此项
 返回 LOOP.LAST 跳出循环
     */
    public  static  enum LOOP { NEXT, LAST };

    /**
     * 拆分字符: 空字符 或 +,;
     */
    private static final Pattern TEXP = Pattern.compile("\\s*[\\s\\+,;]\\s*");

    /**
     * 拆分字符: 空字符 或 +,;.:!?'" 及 同类全角标点
     */
    private static final Pattern WEXP = Pattern.compile("\\s*[\\p{Space}\\p{Punct}\\u3000-\\u303F]\\s*");

    /**
     * 区间参数: 表达式 [min,max] (min,max) [min,max) 空则表示无限, 逗号可替换为 ~
     */
    private static final Pattern RNGP = Pattern.compile("^([\\(\\[])?([^,~]+)?[,~]([^,~]+)?([\\]\\)])?");

    /**
     * 视为假的字符串有: 0,n,f,no,false 和 空串
     */
    public  static final Pattern FAKE = Pattern.compile("^(|0|n|f|no|false)$", Pattern.CASE_INSENSITIVE);

    /**
     * 视为真的字符串有: 1,y,t,yes,true
     */
    public  static final Pattern TRUE = Pattern.compile( "^(1|y|t|yes|true)$", Pattern.CASE_INSENSITIVE);

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
     * 但其他类型均无法转为 Map.
     * 通常针对外部数据
     * @param <T>
     * @param val
     * @param cls
     * @return
     */
    public static <T> T declare(Object val, Class<T> cls) {
        if (val == null) {
            return null;
        }
        if (cls == null) {
            throw  new NullPointerException("declare cls can not be null");
        }

        if (Object[].class.isAssignableFrom(cls)) {
            if (val instanceof Object[] ) {
            } else if (val instanceof List) {
                val = ((List)val).toArray();
            } else if (val instanceof Set ) {
                val = ((Set) val).toArray();
            } else if (val instanceof Map ) {
                val = ((Map) val).values( ).toArray();
            } else {
                val = new Object[ ] { val };
            }
        } else
        if (List.class.isAssignableFrom(cls)) {
            if (val instanceof List) {
            } else if (val instanceof Set ) {
                val = new ArrayList(( Set ) val);
            } else if (val instanceof Map ) {
                val = new ArrayList(((Map ) val).values());
            } else if (val instanceof Object[] ) {
                val = new ArrayList(Arrays.asList((Object[]) val));
            } else {
                List lst = new ArrayList ();
                lst.add(val);
                val = lst;
            }
        } else
        if ( Set.class.isAssignableFrom(cls)) {
            if (val instanceof Set ) {
            } else if (val instanceof List) {
                val = new LinkedHashSet((List) val);
            } else if (val instanceof Map ) {
                val = new LinkedHashSet(((Map) val).values());
            } else if (val instanceof Object[] ) {
                val = new LinkedHashSet(Arrays.asList((Object[]) val));
            } else {
                Set set = new LinkedHashSet();
                set.add(val);
                val = set;
            }
        } else
        if ( Map.class.isAssignableFrom(cls)) {
            if (val instanceof Map ) {
            } else {
                // 其他类型均无法转换为 Map
                throw new ClassCastException("'" + val + "' can not be cast to Map");
            }
        } else {
            /**
             * 针对 servlet 的 requestMap 制定的规则, 多个值取第一个值
             */
            if (val instanceof Object[]) {
                val = ((Object[]) val)[0];
            } else
            if (val instanceof List) {
                val = ((List) val).get(0);
            } else
            if (val instanceof Set ) {
                val = ((Set ) val).toArray()[0];
            } else
            if (val instanceof Map ) {
                val = ((Map ) val).values().toArray()[0];
            }

            if (String.class.isAssignableFrom(cls)) {
                val = val.toString();
            } else
            if (Number.class.isAssignableFrom(cls)) {
                if (EMPT.equals(val)) {
                    return null; // 空串视为未取值
                }

                // 将日期先转换为时间戳
                if (val instanceof Date) {
                    val = ( (Date) val ).getTime();
                }

                if (Integer.class.isAssignableFrom(cls)) {
                    if (val instanceof Number) {
                        val = /**/ ((Number) val).intValue();
                    } else
                    if (val instanceof String) {
                      String str = ((String) val).trim();
                      try {
                        val = new BigDecimal(str).intValue();
                      } catch (NumberFormatException ex) {
                        throw new ClassCastException("'" + val + "' can not be cast to int");
                      }
                    }
                } else if (Byte.class.isAssignableFrom(cls)) {
                    if (val instanceof Number) {
                        val = ((Number) val).byteValue();
                    } else
                    if (val instanceof String) {
                      String str = ((String) val).trim();
                      try {
                        val = new BigDecimal(str).byteValue();
                      } catch (NumberFormatException ex) {
                        throw new ClassCastException("'" + val + "' can not be cast to byte");
                      }
                    }
                } else if (Short.class.isAssignableFrom(cls)) {
                    if (val instanceof Number) {
                        val = /**/ ((Number) val).shortValue();
                    } else
                    if (val instanceof String) {
                      String str = ((String) val).trim();
                      try {
                        val = new BigDecimal(str).shortValue();
                      } catch (NumberFormatException ex) {
                        throw new ClassCastException("'" + val + "' can not be cast to short");
                      }
                    }
                } else if (Long.class.isAssignableFrom(cls)) {
                    if (val instanceof Number) {
                        val = /**/ ((Number) val).longValue();
                    } else
                    if (val instanceof String) {
                      String str = ((String) val).trim();
                      try {
                        val = new BigDecimal(str).longValue();
                      } catch (NumberFormatException ex) {
                        throw new ClassCastException("'" + val + "' can not be cast to long");
                      }
                    }
                } else if (Float.class.isAssignableFrom(cls)) {
                    if (val instanceof Number) {
                        val = /**/ ((Number) val).floatValue();
                    } else
                    if (val instanceof String) {
                      String str = ((String) val).trim();
                      try {
                        val = new BigDecimal(str).floatValue();
                      } catch (NumberFormatException ex) {
                        throw new ClassCastException("'" + val + "' can not be cast to float");
                      }
                    }
                } else {
                    if (val instanceof Number) {
                        val = /**/ ((Number) val).doubleValue();
                    } else
                    if (val instanceof String) {
                      String str = ((String) val).trim();
                      try {
                        val = new BigDecimal(str).doubleValue();
                      } catch (NumberFormatException ex) {
                        throw new ClassCastException("'" + val + "' can not be cast to double");
                      }
                    }
                }
            } else
            if (Boolean.class.isAssignableFrom(cls)) {
                if (val instanceof Number) {
                    val = ((Number) val).intValue() != 0;
                } else if (val instanceof String) {
                    String str = ( (String) val ).trim();
                    if (TRUE.matcher(str).matches()) {
                        val = true ;
                    } else
                    if (FAKE.matcher(str).matches()) {
                        val = false;
                    } else {
                        throw new ClassCastException("'" + str + "' can not be cast to boolean");
                    }
                }
            }
        }

        return (T) val;
    }

    /**
     * 确保此变量类型为 def 的类型
     * val 为空时则返回 def
     * 其他的说明请参见 declare(val, cls)
     * 通常针对外部数据
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
     * 同 decalare(val, def)
     * 但转换不了则返回 def 而不抛出错误
     * 通常针对内部数据
     * @param <T>
     * @param val
     * @param def
     * @return
     */
    public static <T> T asserts(Object val, T def) {
        try {
            return declare(val, def);
        }
        catch (ClassCastException e) {
            app.hongs.CoreLogger.error(e); // 记录错误, 以备调错
            return def;
        }
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
    public static Set<String> asTerms(Object val) {
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
        return declare(val, Set.class );
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
    public static Set<String> asWords(Object val) {
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
        return declare(val, Set.class );
    }

    /**
     * 解析区间值
     * 可将数学表达式 [min,max] (min,max) 等解析为 {min,max,gte,lte}
     * 空串空值将被认为无限小或无限大
     * 方括号可以省略, 格式不符返回空
     * @param val
     * @return
     */
    public static Object[] asRange(Object val) {
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
                return new Object[] {
                    m.group(2).trim(),
                    m.group(3).trim(),
                  ! "(".equals(m.group(1)),
                  ! ")".equals(m.group(4))
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
                boolean lteq, gteq;
                try {
                    lteq = declare(arr[2], false);
                    gteq = declare(arr[3], false);
                }
                catch (ClassCastException e) {
                    throw new ClassCastException("Range index 2,3 must be boolean: "+arr);
                }

                return new Object[] {
                    arr[0], arr[1], lteq, gteq
                };
            case 2:
                return new Object[] {
                    arr[0], arr[1], true, true
                };

            default:throw new ClassCastException("Range index size must be 2 or 4: "+arr);
        }
    }

    /**
     * 快捷构建 List
     * 不等同于 Arrays.listOf,
     * 可以在此 List 上增删改.
     * @param objs
     * @return
     */
    public static List listOf(Object... objs) {
        return new ArrayList(Arrays.asList(objs));
    }

    /**
     * 快捷构建 Set
     * @param objs
     * @return
     */
    public static Set setOf(Object... objs) {
        Set set = new LinkedHashSet();
        for (int i = 0; i < objs.length; i += 1) {
            set.add ( objs[i] );
        }
        return set;
    }

    /**
     * 快捷构建 Map
     * @param objs
     * @return
     */
    public static Map mapOf(Object... objs) {
        Map map = new LinkedHashMap();
        for (int i = 0; i < objs.length; i += 2) {
            map.put ( objs[i] , objs[i+1] );
        }
        return map;
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
