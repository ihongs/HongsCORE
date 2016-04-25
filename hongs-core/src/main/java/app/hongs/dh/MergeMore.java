package app.hongs.dh;

import app.hongs.util.Dict;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 合并列表
 * @author Hongs
 */
public class MergeMore
{

  protected List<Map> rows;

  public MergeMore(List<Map> rows)
  {
    this.rows = rows;
  }

  /**
   * 获取关联ID和行
   *
   * @param map
   * @param keys
   */
  private void maping(Map<Object, List> map, List rows, String... keys)
  {
    Iterator it = rows.iterator();
    W:while (it.hasNext())
    {
      Object row = it.next();
      Object obj = row;

      // 获取id值
      for (int i = 0; i < keys.length; i ++)
      {
        if (obj instanceof Map )
        {
          obj = ((Map)obj).get(keys[i]);
        }
        else
        if (obj instanceof List)
        {
          // 切割子键数组
          int j  = keys.length - i;
          String[] keyz = new String[j];
          System.arraycopy(keys,i, keyz,0,j);

          // 往下递归一层
          this.maping(map, (List) obj, keyz);

          continue W;
        }
        else
        {
          continue W;
        }
      }
      if (obj == null)
      {
          continue;
      }

      /**
       * 登记行
       * 如果对应多个值
       * 则将多个值加入映射关系
       * 这种情况必须使用append
       */
      if  (obj instanceof Collection)
      {
      for (Object xbj : ( Collection) obj)
      {
        if (map.containsKey(xbj))
        {
          map.get(xbj ).add(row);
        }
        else
        {
          List lst = new ArrayList();
          map.put(xbj , lst);
          lst.add(row);
        }
      }
      }
      else
      if  (obj instanceof Object [ ])
      {
      for (Object xbj : ( Object [ ]) obj)
      {
        if (map.containsKey(xbj))
        {
          map.get(xbj ).add(row);
        }
        else
        {
          List lst = new ArrayList();
          map.put(xbj , lst);
          lst.add(row);
        }
      }
      }
      else
      {
        if (map.containsKey(obj))
        {
          map.get(obj ).add(row);
        }
        else
        {
          List lst = new ArrayList();
          map.put(obj , lst);
          lst.add(row);
        }
      }
    }
  }

  /**
   * 获取关联ID和行
   *
   * @param keys
   * @return 
   */
  public Map<Object, List> maping(String... keys)
  {
    Map<Object, List> map = new HashMap();
    maping(map, rows, keys);
    return map;
  }

  /**
   * 获取关联ID和行
   *
   * @param key 使用"."分割的键
   * @return
   */
  public Map<Object, List> mapped(String key)
  {
    Map<Object, List> map = new HashMap();
    maping(map, rows, key.split( "\\." ));
    return map;
  }

  /**
   * 一对多关联
   * @param iter 数据迭代
   * @param map 映射表
   * @param col 关联键
   * @param sub 子键, 不可以为空
   */
  public void append(Iterator iter, Map<Object, List> map, String col, String sub)
  {
    Map     row, raw;
    List    lst;
    Object  rid;

    while (iter.hasNext())
    {
      raw = ( Map  ) iter.next( );
      rid =          raw.get(col);
      lst = ( List ) map.get(rid);

      if (lst == null)
      {
        //throw new HongsException(0x10c0, "Line nums is null");
        continue;
      }

      Iterator it = lst.iterator();
      while (it.hasNext())
      {
        row = (Map) it.next();

        if (row.containsKey(sub))
        {
          (( List ) row.get(sub)).add(raw);
        }
        else
        {
          List lzt = new ArrayList();
          row.put(sub, lzt);
          lzt.add(raw);
        }
      }
    }
  }

  public void append(List<Map> rows, Map<Object, List > map, String col, String sub)
  {
    append(rows.iterator(), map, col, sub);
  }

  public void append(List<Map> rows, String key, String col, String sub)
  {
    append(rows, mapped ( key ), col, sub);
  }

  /**
   * 一对一关联
   * @param iter 数据迭代
   * @param map 映射表
   * @param col 关联键
   * @param sub 子键, 为空并到行
   */
  public void extend(Iterator iter, Map<Object, List> map, String col, String sub)
  {
    Map     row, raw;
    List    lst;
    Object  rid;

    while (iter.hasNext())
    {
      raw = ( Map  ) iter.next( );
      rid =          raw.get(col);
      lst = ( List ) map.get(rid);

      if (lst == null)
      {
        //throw new HongsException(0x10c0, "Line nums is null");
        continue;
      }

      Iterator it = lst.iterator();
      while (it.hasNext())
      {
        row = (Map) it.next();

        if (sub != null)
        {
          Dict.setParam(row, raw, sub);
        }
        else
        {
          raw.putAll(row);
          row.putAll(raw);
        }
      }
    }
  }

  public void extend(List<Map> rows, Map<Object, List > map, String col, String sub)
  {
    extend(rows.iterator(), map, col, sub);
  }

  public void extend(List<Map> rows, String key, String col, String sub)
  {
    extend(rows, mapped ( key ), col, sub);
  }

  public void extend(List<Map> rows, String key, String col)
  {
    extend(rows, mapped ( key ), col,null);
  }

  public void extend(List<Map> rows, String key)
  {
    extend(rows, mapped ( key ), key,null);
  }

}
