package io.github.ihongs.db.util;

import io.github.ihongs.HongsException;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.util.Synt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关联查询
 *
 * <h3>异常代码:</h3>
 * <pre>
 * 1170  获取行号失败, 可能缺少关联字段 (废弃, 缺失将跳过)
 * </pre>
 *
 * @author Hongs
 */
public class FetchMore
{

  protected List<Map> rows;

  public FetchMore(List<Map> rows)
  {
    this.rows = rows;
  }

  /**
   * 获取关联ID和行
   *
   * @param map
   * @param rows
   * @param deep
   * @param keys
   */
  private void maping(Map<String, List> map, List rows, boolean deep, String... keys)
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
          if (deep) row  = obj ;
          obj = ((Map)obj).get(keys[i]);
        }
        else
        if (obj instanceof List)
        {
          // 切割子键数组
          int j = keys.length - i ;
          String[] keyz = new String[j];
          System.arraycopy(keys,i, keyz,0,j);

          // 往下递归一层
          maping(map, (List)obj, deep, keyz);

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

      // 登记行
      String str = obj.toString( );
      if (map.containsKey(str))
      {
        map.get(str ).add(row);
      }
      else
      {
        List lst = new ArrayList();
        map.put(str , lst);
        lst.add(row);
      }
    }
  }

  public Map<String, List> maping(boolean deep, String... keys)
  {
    Map<String, List> map = new HashMap();
    maping(map, rows, deep, keys);
    return map;
  }

  public Map<String, List> maping(String... keys) {
    Map<String, List> map = new HashMap();
    maping(map, rows, true, keys);
    return map;
  }

  public Map<String, List> mapped(String key) {
    Map<String, List> map = new HashMap();
    maping(map, rows, true, key.split("\\."));
    return map;
  }

  /**
   * 获取关联数据
   * @param table 关联表
   * @param caze  附加查询
   * @param map   映射关系
   * @param col   关联字段
   * @throws io.github.ihongs.HongsException
   */
  public void join(Table table, FetchCase caze, Map<String, List> map, String col)
    throws HongsException
  {
    if (map.isEmpty())
    {
      return;
    }

    DB db = table.db;
    String       name   = table.name;
    String  tableName   = table.tableName;
    boolean multi       = caze.getOption("ASSOC_MULTI", false);
    boolean merge       = caze.getOption("ASSOC_MERGE", false);
    boolean patch       = caze.getOption("ASSOC_PATCH", false);

    if (null != caze.name && 0 != caze.name.length())
    {
        name  = caze.name;
    }

    // 获取id及行号
    Set ids = map.keySet();
    if (ids.isEmpty())
    {
      //throw new CruxException(1170, "Ids map is empty");
      return;
    }

    // 识别字段别名
    String rel = col;
    if (table.getFields().containsKey(col))
    {
      col = "`" + name + "`.`" + col + "`";
    }
    else
    {
      Pattern pattern;
      Matcher matcher;
      do
      {
        pattern = Pattern.compile(
            "^(.+?)(?:\\s+AS)?\\s+`?(.+?)`?$",
            Pattern.CASE_INSENSITIVE );
        matcher = pattern.matcher(col);
        if (matcher.find())
        {
          col = matcher.group(1);
          rel = matcher.group(2);
          break;
        }

        pattern = Pattern.compile(
            "^(.+?)\\.\\s*`?(.+?)`?$");
        matcher = pattern.matcher(col);
        if (matcher.find())
        {
          col = matcher.group(0);
          rel = matcher.group(2);
          break;
        }
      }
      while (false);
    }

    // 构建查询结构
    caze.filter(col + " IN (?)" , ids)
        .from  (tableName, name);

    // 获取关联数据
    Loop rs = db.queryMore(caze);

    /**
     * 根据之前的 ID=>行 关系以表名为键放入列表中
     */

    String    sid;
    List      lst;
    Map row , sub;

    Set idz = new   HashSet (); // 登记已关联上的ID
    Map tdz = rs.getTypeDict(); // 暂存字段类型字典

    if (! multi)
    {
      while ((sub = rs.next( )) != null )
      {
        sid = Synt.asString(sub.get(rel));
        lst = map.get(sid);
        idz.add(sid);

        if (lst == null)
        {
          //throw new CruxException(1170, "Line nums is null");
          continue;
        }

        Iterator it = lst.iterator();
        while (it.hasNext())
        {
          row = (Map) it.next();

          if (! merge)
          {
            row.put(name, sub);
          }
          else
          {
            sub.putAll(row);
            row.putAll(sub);
          }
        }
      }
    }
    else
    {
      while ((sub = rs.next( )) != null )
      {
        sid = Synt.asString(sub.get(rel));
        lst = map.get(sid);
        idz.add(sid);

        if (lst == null)
        {
          //throw new CruxException(1170, "Line nums is null");
          continue;
        }

        Iterator it = lst.iterator();
        while (it.hasNext())
        {
          row = (Map) it.next();

          if (row.containsKey(name))
          {
            (( List ) row.get(name)).add(sub);
          }
          else
          {
            List lzt = new ArrayList();
            row.put(name, lzt);
            lzt.add(sub);
          }
        }
      }
    }

    /**
     * 自动补全空数据
     * 避免客户端麻烦
     */

    if (! patch) {
        return;
    }

    if (! multi && merge) {
        Set< String > padSet = tdz.keySet( );
        for(Map.Entry<String, List> et : map.entrySet()) {
            String colKey = et.getKey();
            if (idz.contains( colKey )) {
                continue;
            }
            List<Map> mapLst = et.getValue();
            for( Map  mapRow : mapLst ) {
                for(String k : padSet ) {
                    if(!mapRow.containsKey(k)) {
                        mapRow.put( k , null );
                    }
                }
            }
        }
    } else {
        Object padDat = ! multi ? new HashMap() : new ArrayList();
        for(Map.Entry<String, List> et : map.entrySet()) {
            String colKey = et.getKey();
            if (idz.contains( colKey )) {
                continue;
            }
            List<Map> mapLst = et.getValue();
            for( Map  mapRow : mapLst ) {
                if(!mapRow.containsKey(name)) {
                    mapRow.put(name, padDat);
                }
            }
        }
    }
  }

  /**
   * 获取关联数据
   * 类似 SQL: JOIN table ON table.col = super.key
   * @param table 关联表
   * @param caze  附加查询
   * @param key   映射关系
   * @param col   关联字段
   * @throws io.github.ihongs.HongsException
   */
  public void join(Table table, FetchCase caze, String key, String col)
    throws HongsException
  {
    Map<String, List> map = this.mapped(key);
    join(table, caze, map , col);
  }

  /**
   * 获取关联数据
   * 类似 SQL: JOIN table ON table.col = super.key
   * @param table 关联表
   * @param key   映射键名
   * @param col   关联字段
   * @throws io.github.ihongs.HongsException
   */
  public void join(Table table, String key, String col)
    throws HongsException
  {
    join(table, new FetchCase( ), key, col);
  }

  /**
   * 获取关联数据
   * 类似 SQL: JOIN table ON table.col = super.col
   * @param table 关联表
   * @param col   关联字段
   * @throws io.github.ihongs.HongsException
   */
  public void join(Table table, String col)
    throws HongsException
  {
    join(table, new FetchCase( ), col, col);
  }

}
