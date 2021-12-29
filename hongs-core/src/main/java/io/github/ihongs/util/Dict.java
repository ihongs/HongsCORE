package io.github.ihongs.util;

import io.github.ihongs.HongsExemption;
import io.github.ihongs.util.Synt.LOOP;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 字典数据工具
 *
 * <p>
 * 用于获取和设置树型结构
 * (&lt;Object,Map&lt;Object,Map&lt;...&gt;&gt;&gt;)
 * 数据的值;
 * 切记：keys
 * </p>
 *
 * <h3>异常代码</h3>
 * <pre>
 * 855 解析JSON数据失败
 * 856 写入JSON数据失败
 * </pre>
 *
 * @author Hongs
 */
public final class Dict
{

  private static int  asIdx (Object val) {
    Integer idx ;
    if (val instanceof Number) {
        idx = ( (Number) val ).intValue( );
    }  else
    if (val instanceof String) {
        String str = ((String) val).trim();
    try {
        idx = Integer.parseInt(str);
    } catch ( NumberFormatException e ) {
        idx = -1;
    }} else {
        idx = -1;
    }
    if (idx <  0) {
        throw new ClassCastException("'"+val+"' is not a valid array index");
    }
    return idx;
  }

  private static Map  asMap (Object val) {
    if (val != null) {
        if (val instanceof Map ) {
            return ( Map ) val ;
        } else if (val instanceof Collection) {
            Collection  a = (Collection) val;
            int i = 0;
            Map m = new LinkedHashMap(a.size());
            for( Object v : a ) {
                m.put(i ++, v );
            }
            return m;
        } else if (val instanceof Object [ ]) {
            Object [ ]  a = (Object [ ]) val;
            int i = 0;
            Map m = new LinkedHashMap(a.length);
            for( Object o : a ) {
                m.put(i ++, o );
            }
            return m;
        }
    }
    return  new LinkedHashMap(0);
  }

  private static List asList(Object val) {
    if (val != null) {
        if (val instanceof List) {
            return ( List) val ;
        } else if (val instanceof Set ) {
            return new ArrayList(( Set) val );
        } else if (val instanceof Map ) {
            return new ArrayList(((Map) val ).values( ) );
        } else if (val instanceof Object[]) {
            return new ArrayList(Arrays.asList((Object[]) val));
        }
    }
    return  new ArrayList(0);
  }

  private static Collection asColl(Object val) {
    if (val != null) {
        if (val instanceof Collection ) {
            return ( Collection ) val ;
        } else if (val instanceof Map ) {
            return new ArrayList(((Map) val ).values( ) );
        } else if (val instanceof Object[]) {
            return new ArrayList(Arrays.asList((Object[]) val));
        }
    }
    return  new ArrayList(0);
  }

  private static List azList(Object val) {
    if (val != null) {
        if (val instanceof List) {
            return ( List) val ;
        } else if (val instanceof Set ) {
            return new ArrayList(( Set) val );
        } else if (val instanceof Map ) {
            return new ArrayList(((Map) val ).values( ) );
        } else if (val instanceof Object[]) {
            return Arrays.asList((Object[]) val);
        }
    }
    return  new ArrayList(0);
  }

  private static Collection azColl(Object val) {
    if (val != null) {
        if (val instanceof Collection ) {
            return ( Collection ) val ;
        } else if (val instanceof Map ) {
            return ((Map) val ).values( );
        } else if (val instanceof Object[]) {
            return Arrays.asList((Object[]) val);
        }
    }
    return  new ArrayList(0);
  }

  private static Object gat(Collection lst, Object def, Object[] keys, int pos)
  {
    /**
     * 获取下面所有的节点的值
     * 下面也要列表则向上合并
     */
    boolean one = true ;
    for ( int j = pos; j < keys.length; j ++ ) {
        if (keys[j] == null) {
            one = false;
            break;
        }
    }

    /**
     * 当默认值也是集合类型时
     * 直接将获取的值放入其中
     */
    Collection col;
    if (def != null && def instanceof Collection) {
        col  = (Collection) def;
    } else {
        col  = new LinkedList();
    }

    if (one) {
        for(Object sub : lst) {
            Object obj = get(sub, LOOP.NEXT, keys, pos);
            if (obj != LOOP.NEXT) {
                col.add (obj);
            }
        }
    } else {
        for(Object sub : lst) {
            Object obj = get(sub, LOOP.NEXT, keys, pos);
            if (obj != LOOP.NEXT) {
                col.addAll(azColl(obj));
            }
        }
    }

    if (! col.isEmpty( )) {
        return col;
    } else {
        return def;
    }
  }

  private static Object get(Object obj, Object def, Object[] keys, int pos)
  {
    Object key = keys[pos];
    if (obj == null) {
        return def;
    }

    // 按键类型来决定容器类型
    if (key == null) {
        Collection lst = azColl(obj);

        if (keys.length == pos + 1) {
            return lst;
        } else {
            return gat(lst, def, keys, pos + 1);
        }
    } else
    if (obj instanceof Collection
    ||  obj instanceof Object [ ] ) {
        List lst = azList(obj);
        int  idx = asIdx (key);

        // 如果列表长度不够, 则直接返回默认值
        if ( idx > lst.size( ) - 1) {
            return def;
        }

        if (keys.length == pos + 1) {
            return lst.get(idx);
        } else {
            return get(lst.get(idx), def, keys, pos + 1);
        }
    } else {
        Map  map = asMap (obj);

        // 如果没有对应的键, 则直接返回默认值
        if (! map.containsKey(key)) {
            return def;
        }

        if (keys.length == pos + 1) {
            return map.get(key);
        } else {
            return get(map.get(key), def, keys, pos + 1);
        }
    }
  }

  private static Object put(Object obj, Object val, Object[] keys, int pos)
  {
    Object key = keys[pos];

    // 按键类型来决定容器类型
    if (key == null) {
        Collection lst = asColl(obj);

        if (keys.length == pos + 1) {
            lst.add(val);
        } else {
            lst.add(put(null, val, keys, pos + 1));
        }

        return lst;
    } else
    if (key instanceof Integer
    && (obj instanceof Collection
    ||  obj instanceof Object [] )) {
        List lst = asList(obj);
        int  idx = asIdx (key);
        int  idz = lst.size( );

        // 如果列表长度不够, 填充到索引的长度
        for( ; idz <= idx; idz ++ ) {
            lst.add (  null  );
        }

        if (keys.length == pos + 1) {
            lst.set(idx , val);
        } else {
            lst.set(idx , put(lst.get(idx), val, keys, pos + 1));
        }

        return lst;
    } else {
        Map  map = asMap (obj);

        if (keys.length == pos + 1) {
            map.put(key , val);
        } else {
            map.put(key , put(map.get(key), val, keys, pos + 1));
        }

        return map;
    }
  }

  /**
   * 获取树纵深值
   * @param map
   * @param def
   * @param keys
   * @return 键对应的值
   */
  public static Object get(Map map, Object def, Object... keys)
  {
    if (map  == null)
    {
      throw new NullPointerException("`map` can not be null");
    }

    return get(map, def, keys, 0);
  }

  /**
   * 设置树纵深值
   * @param map
   * @param val
   * @param keys
   */
  public static void put(Map map, Object val, Object... keys)
  {
    if (map  == null)
    {
      throw new NullPointerException("`map` can not be null");
    }
    if (keys.length ==  0)
    {
      throw new HongsExemption(856, "Keys required, but empty gives");
    }
    if (keys[0] == null || keys[0] instanceof Integer)
    {
      throw new HongsExemption(856, "First key can not be null or ints, but it is " + keys[0]);
    }

    put(map, val, keys, 0);
  }

  /**
   * 将 oth 追加到 map 中
   * 与 Map.putAll 的不同在于: 本函数会将其子级的 Map 和 Collection 也进行合并
   * @param map
   * @param oth
   */
  public static void putAll(Map map, Map oth) {
    for(Object o : oth.entrySet()) {
        Map.Entry e = (Map.Entry) o;
        Object k2 = e.getKey(  );
        Object v2 = e.getValue();
        Object v1 =  map.get(k2);

        if (v1 instanceof Collection
        &&  v2 instanceof Collection  ) {
           ((Collection) v1).addAll((Collection) v2);
        } else
        if (v1 instanceof Map
        &&  v2 instanceof Map  ) {
            putAll((Map) v1 , (Map) v2);
        } else {
            map.put(k2 , v2);
        }
    }
  }

  /**
   * 获取树纵深值
   * @param map
   * @param keys
   * @return
   */
  public static Object getDepth(Map map, Object... keys)
  {
    return get(map, null, keys);
  }

  /**
   * 获取树纵深值
   * @param <T>
   * @param map
   * @param def
   * @param keys
   * @return 键对应的值
   */
  public static <T> T getValue(Map map, T def, Object... keys)
  {
    try {
      return Synt.declare(get(map, def, keys), def);
    } catch (ClassCastException ex) {
      throw new ClassCastException("Wrong type for key '"+Arrays.toString(keys)+"'");
    }
  }

  /**
   * 获取树纵深值
   * @param <T>
   * @param map
   * @param cls
   * @param keys
   * @return 键对应的值
   */
  public static <T> T getValue(Map map, Class<T> cls, Object... keys)
  {
    try {
      return Synt.declare(get(map,null, keys), cls);
    } catch (ClassCastException ex) {
      throw new ClassCastException("Wrong type for key '"+Arrays.toString(keys)+"'");
    }
  }

  /**
   * 获取树纵深值(以属性或键方式"a.b[c]"获取)
   * @see splitKeys
   * @param map
   * @param path
   * @return 键对应的值
   */
  public static Object getParam(Map map, String path)
  {
    return getDepth(map, splitKeys(path));
  }

  /**
   * 获取树纵深值
   * @see splitKeys
   * @param <T>
   * @param map
   * @param def
   * @param path
   * @return 键对应的值
   */
  public static <T> T getParam(Map map, T def, String path)
  {
    return getValue(map, def, splitKeys(path));
  }

  /**
   * 获取树纵深值(以属性或键方式"a.b[c]"获取)
   * @see splitKeys
   * @param <T>
   * @param map
   * @param cls
   * @param path
   * @return 键对应的值
   */
  public static <T> T getParam(Map map, Class<T> cls, String path)
  {
    return getValue(map, cls, splitKeys(path));
  }

  /**
   * 设置树纵深值(put的别名)
   * @param map
   * @param val
   * @param keys
   */
  public static void setValue(Map map, Object val, Object... keys)
  {
    put(map, val, keys);
  }

  /**
   * 设置树纵深值(以属性或键方式"a.b[c]"设置)
   * @see splitKeys
   * @param map
   * @param val
   * @param path
   */
  public static void setParam(Map map, Object val, String path)
  {
    put(map, val, splitKeys(path));
  }

  /**
   * 拆分键
   *
   * <pre>
   * 可以将 a.b[c][.d][]. 解析为 a b c .d null null
   * 参考自 Javascript 和 PHP 的语法,
   * Javascript 方括号里面用的是变量,
   * 而 PHP 对象键并不支持点符号写法.
   * </pre>
   *
   * @param path
   * @return
   */
  public static Object[] splitKeys(String path) {
    if (path == null) {
        throw new NullPointerException("`path` can not be null");
    }

    /**
     * 原代码为
     * <code>
     *  path.replaceAll("([^\\.\\]])!", "$1.!")
     *      .replace("[", ".")
     *      .replace("]", "" )
     *      .split("\\.")
     * </code>
     * 然后遍历将空串换成 null 空值,
     * 无法处理 [] 里面有 .! 的情况;
     * 采用下面的程序可规避此种问题,
     * 且无需正则而仅做一次遍历即可.
     * 2018/03/09 已将 '!' 换为 ':'.
     * 2019/07/28 已将 ':' 等同 '.'.
     */

    List    lst = new ArrayList();
    int     len = path.length(  );
    int     beg = 0;
    int     end = 0;
    char    pnt;
    boolean fkh = false;

    while (end < len ) {
        pnt = path.charAt(end);
        switch ( pnt ) {
            case ':' :/*
                if (fkh) {
                    break; // [] 内可以用 :
                }
                if (beg != end) {
                    lst.add(path.substring(beg, end));
                    beg  = end;
                }
                break;*/
            case '.' :
                if (fkh) {
                    break; // [] 内可以用 .
                }
                if (beg != end) {
                    lst.add(path.substring(beg, end));
                } else
                if (beg == 0  ) {
                    lst.add( "" );
                } else
                if (']' != path.charAt(beg - 1)) { // 规避 a[b].c. 中的 ].
                    lst.add(null);
                }
                beg  = end + 1;
                break;
            case '[' :
                if (fkh) {
                    throw new HongsExemption(855, "Syntax error at " + end + " in " + path);
                }
                if (beg != end) {
                    lst.add(path.substring(beg, end));
                } else
                if (beg == 0  ) {
                    lst.add( "" );
                } else
                if (']' != path.charAt(beg - 1)) { // 规避 a[b][c] 中的 ][
                    throw new HongsExemption(855, "Syntax error at " + end + " in " + path);
                }
                beg  = end + 1;
                fkh  = true;
                break;
            case ']' :
                if (! fkh) {
                    throw new HongsExemption(855, "Syntax error at " + end + " in " + path);
                }
                if (beg != end) {
                    lst.add(path.substring(beg, end));
                } else
                if (beg != 0  ) {
                    lst.add(null);
                } else
//              if ('[' != path.charAt(beg - 1)) { // 这种情况其实并不存在
                    throw new HongsExemption(855, "Syntax error at " + end + " in " + path);
//              }
                beg  = end + 1;
                fkh  = false;
                break;
        }
        end  ++;
    }

    if (beg  ==  0
    &&  end  ==  0  ) {
        lst.add(path);
    } else
    if (beg  !=  len) {
        lst.add(path.substring(beg));
    } else
    if ('.'  == path.charAt(-1+len)) {
        lst.add(null); // 点结尾列表.
    }

    return lst.toArray();
  }

}
