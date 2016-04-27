package app.hongs.util;

import app.hongs.HongsError;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * @author Hongs
 */
public class Dict
{

  private static Object gat(List lst, Object def, Object[] keys, int pos)
  {
    // 获取下面所有的节点的值
    List col = new ArrayList();
    for(Object sub : lst) {
        Object obj = get(sub, def, keys, pos);
        if (obj !=  null) {
            col.add( obj);
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
        List lst = Synt.declare(obj, List.class);

        if (keys.length == pos + 1) {
            return lst;
        } else {
            return gat(lst, def, keys, pos + 1 );
        }
    } else
    if (key instanceof Integer) {
        List lst = Synt.declare(obj, List.class);

        // 如果列表长度不够, 则直接返回默认值
        int idx = (Integer)key;
        if (lst.size( ) <= idx) {
            return def;
        }

        if (keys.length == pos + 1) {
            return Synt.defoult(lst.get(idx), def);
        } else {
            return get(lst.get(idx), def, keys, pos + 1);
        }
    } else {
        Map  map = Synt.declare(obj,  Map.class);

        if (keys.length == pos + 1) {
            return Synt.defoult(map.get(key), def);
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
        List lst;
        if (obj == null) {
            lst = new ArrayList();
        } else {
            lst = Synt.declare(obj , List.class);
        }

        if (keys.length == pos + 1) {
            lst.add(val);
        } else {
            lst.add(put(null, val, keys, pos+1));
        }

        return lst;
    } else
    if (key instanceof Integer) {
        List lst;
        if (obj == null) {
            lst = new ArrayList();
        } else {
            lst = Synt.declare(obj , List.class);
        }

        // 如果列表长度不够, 填充到索引的长度
        int idx = (Integer)key;
        if (lst.size( ) <= idx) {
            for(int i = 0; i <= idx; i++) {
                lst.add( null );
            }
        }

        if (keys.length == pos + 1) {
            lst.set(idx, val);
        } else {
            lst.set(idx, put(lst.get(idx), val, keys, pos + 1));
        }

        return lst;
    } else {
        Map map;
        if (obj == null) {
            map = new LinkedHashMap();
        } else {
            map = Synt.declare(obj, Map.class);
        }

        if (keys.length == pos + 1) {
            map.put(key, val);
        } else {
            map.put(key, put(map.get(key), val, keys, pos + 1));
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
    if (map == null)
    {
      throw new HongsError(0x45, "`map` can not be null" );
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
    if (map == null)
    {
      throw new HongsError(0x46, "`map` can not be null" );
    }
    if (keys.length ==  0)
    {
      throw new HongsError(0x47,"`keys` can not be empty");
    }
    if (keys[0] == null || keys[0] instanceof Integer)
    {
      throw new HongsError(0x48,"first key can not be null or integer");
    }

    put(map, val, keys, 0);
  }

  /**
   * 将 oth 追加到 map 中
   * 与 Map.putAll 的不同在于: 本函数会将其子级的 Map 也进行合并
   * @param map
   * @param oth
   */
  public static void putAll(Map map, Map oth) {
    Iterator i = oth.entrySet().iterator();
    while (i.hasNext()) {
        Map.Entry e = (Map.Entry) i.next();
        Object k2 = e.getKey(  );
        Object v2 = e.getValue();
        Object v1 =  map.get(k2);

        if (v1 instanceof Map && v2 instanceof Map) {
            putAll((Map)v1, (Map)v2);
        }
        else {
            map.put(k2, v2);
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
   * @param map
   * @param path
   * @return 键对应的值
   */
  public static Object getParam(Map map, String path)
  {
    return getDepth(map, parsePath(path));
  }

  /**
   * 获取树纵深值
   * @param map
   * @param def
   * @param path
   * @return 键对应的值
   */
  public static <T> T getParam(Map map, T def, String path)
  {
    return Dict.getValue(map, def, parsePath(path));
  }

  /**
   * 获取树纵深值(以属性或键方式"a.b[c]"获取)
   * @param map
   * @param cls
   * @param path
   * @return 键对应的值
   */
  public static <T> T getParam(Map map, Class<T> cls, String path)
  {
    return Dict.getValue(map, cls, parsePath(path));
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
   * @param map
   * @param val
   * @param path
   */
  public static void setParam(Map map, Object val, String path)
  {
    put(map, val, parsePath(path));
  }

  /**
   * 将 oth 追加到 map.keys 中
   * 与 Map.putAll 的不同在于: 本函数会将其子级的 Map 也进行合并
   * @param map
   * @param oth
   * @param keys
   */
  public static void setValues(Map map, Map oth, Object... keys) {
      Object sub = get(map, keys);
      if (sub == null || !(sub instanceof Map)) {
        put   ( map, oth, keys);
      } else {
        putAll((Map) sub, oth );
      }
  }

  /**
   * 将 oth 追加到 map.path 中(path会按 .|[] 拆分)
   * 与 Map.putAll 的不同在于: 本函数会将其子级的 Map 也进行合并
   * @param map
   * @param oth
   * @param path
   */
  public static void setParams(Map map, Map oth, String path) {
    setValues(map, oth, parsePath(path));
  }

  private static Object[] parsePath(String path) {
    String[] keyz = path.replaceAll("([^\\.])!", "$1.!") // id!eq 转换为 id.!eq
                        .replaceAll("\\]\\[", ".")
                        .replace("[" , ".")
                        .replace("]" , "" )
                        .split("\\." , -1 );
    Object[] keys = new Object[keyz.length];
    int i = 0;
    for ( String keyn : keyz) {
        /*
        if (keyn.startsWith("#")) {
            keys[i++] = Synt.declare(keyn.substring(1) , 0);
        } else
        */
        if (keyn.length() == 0 && i != 0) {
            keys[i++] = null;
        } else
        {
            keys[i++] = keyn;
        }
    }
    return  keys;
  }

}
