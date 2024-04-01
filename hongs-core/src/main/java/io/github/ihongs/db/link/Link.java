package io.github.ihongs.db.link;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.dh.IReflux;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 抽象数据连接
 * @author Hongs
 */
abstract public class Link
  implements IReflux, AutoCloseable
{

  /**
   * 库名
   */
  public final String name;

  /**
   * 连接对象
   */
  protected Connection connection;

  /**
   * 事务模式
   */
  protected  boolean  REFLUX_MODE;

  /**
   * 匹配查询语句
   */
  private static final Pattern SELECT_PATT = Pattern.compile("^(SHOW|SELECT|EXPLAIN|DESCRIBE)\\s", Pattern.CASE_INSENSITIVE);

  public Link(String name)
    throws CruxException
  {
    this.name = name;
  }

  /**
   * 开启连接
   *
   * 当读写分离时,
   * 将返回读连接.
   *
   * @return
   * @throws CruxException
   */
  public abstract Connection open()
    throws CruxException;

  /**
   * 准备连接
   *
   * 当读写分离时,
   * 将返回写连接.
   *
   * @param  open false 可以不连 true 立即连接
   * @return open false 时可能为 null
   * @throws CruxException
   */
  public Connection open(boolean open)
    throws CruxException
  {
    if (! open)
    try {
        if (connection == null
        ||  connection.isClosed()) {
            return connection;
        }
    } catch (SQLException ex) {
        throw new CruxExemption(ex, 1053);
    }

    this.open();
    try {
        if (connection.getAutoCommit() == this.REFLUX_MODE) {
            connection.setAutoCommit(  !  this.REFLUX_MODE);
        }
    } catch (SQLException ex) {
        throw new CruxExemption(ex, 1053);
    }

    return  connection;
  }

  /**
   * 事务开始
   */
  @Override
  public void begin( )
  {
    Connection connection;
    try {
        connection = open(false);
    } catch (CruxException ex) {
        throw ex.toExemption();
    }

    try {
        if (connection != null
        && !connection.isClosed()) {
            connection.setAutoCommit(false);
        }
    } catch (SQLException ex) {
        throw new CruxExemption(ex, 1054);
    }

    REFLUX_MODE = true;
  }

  /**
   * 事务提交
   */
  @Override
  public void commit()
  {
    Connection connection;
    try {
        connection = open(false);
    } catch (CruxException ex) {
        throw ex.toExemption();
    }

    try {
        if (connection != null
        && !connection.isClosed()
        && !connection.getAutoCommit()) {
            connection.commit ( );
        }
    } catch (SQLException ex) {
        throw new CruxExemption(ex, 1055);
    }

    REFLUX_MODE = false;
  }

  /**
   * 事务回滚
   */
  @Override
  public void cancel()
  {
    Connection connection;
    try {
        connection = open(false);
    } catch (CruxException ex) {
        throw ex.toExemption();
    }

    try {
        if (connection != null
        && !connection.isClosed()
        && !connection.getAutoCommit()) {
            connection.rollback();
        }
    } catch (SQLException ex) {
        throw new CruxExemption(ex, 1056);
    }

    REFLUX_MODE = false;
  }

  /**
   * 关闭连接
   */
  @Override
  public void close( )
  {
    try
    {
      if (connection != null
      && !connection.isClosed())
      {
        if (! connection.getAutoCommit())
        {
          // 关闭之前先将未提交的语句提交
          try
          {
            connection.commit(  );
          }
          catch (SQLException ex)
          {
            connection.rollback();
            throw ex;
          }
          finally
          {
            connection.close();
            CoreLogger.trace("DB: Connection '{}' has been closed", name);
          }
        }
        else
        {
            connection.close();
            CoreLogger.trace("DB: Connection '{}' has been closed", name);
        }
      }
    }
    catch (SQLException er)
    {
      CoreLogger.error (er);
    }
    finally
    {
      connection = null;
    }
  }

  /** 查询辅助 **/

  /**
   * 关闭ResultSet
   * @param rs
   * @throws CruxException
   */
  public void close(ResultSet rs)
    throws CruxException
  {
    try
    {
      if (rs != null && !rs.isClosed())
      {
        rs.close();
      }
    }
    catch (SQLException ex)
    {
      throw new CruxException(ex, 1035);
    }
  }

  /**
   * 关闭Statement
   * @param ps
   * @throws CruxException
   */
  public void close(Statement ps)
    throws CruxException
  {
    try
    {
      if (ps != null && !ps.isClosed())
      {
        ps.close();
      }
    }
    catch (SQLException ex)
    {
      throw new CruxException(ex, 1034);
    }
  }

  /**
   * 预处理语句
   * 异常代码为: 1041, 1042
   * @param dc
   * @param sql
   * @param params
   * @return PreparedStatement对象
   * @throws CruxException
   */
  public PreparedStatement prepare(Connection dc, String sql, Object... params)
    throws CruxException
  {
    /**
     * 检查语句及参数.
     * 参数里有集合时,
     * 将向上扩充选项.
     * 例如 WHERE xx = ? AND yy IN (?); [1, [2, 3]]
     * 变为 WHERE xx = ? AND yy IN (?,?); [1, 2, 3]
     */
    StringBuilder sb = new StringBuilder (  sql  );
    List pz = new ArrayList(Arrays.asList(params));
    checkSQLParams(sb,pz);
    sql = sb.toString(  );

    PreparedStatement ps ;
    try
    {
      ps = dc . prepareStatement( sql );
    }
    catch (SQLException ex)
    {
      throw new CruxException(ex, 1041);
    }

    if (params.length > 0 )
    try
    {
      int i = 0;
      for ( Object po : pz)
      {   i ++ ;
        ps.setObject(i, po);
      }
    }
    catch (SQLException ex)
    {
      throw new CruxException(ex, 1042);
    }

    return ps;
  }

  /**
   * 预处理语句
   * 异常代码为: 1041, 1042
   * @param sql
   * @param params
   * @return PreparedStatement对象
   * @throws CruxException
   */
  public PreparedStatement prepare(String sql, Object... params)
    throws CruxException
  {
    // 分辨是查询语句还是执行语句
    if ( SELECT_PATT.matcher(sql).find())
    {
      return prepare(open(/**/), sql, params);
    }
    else
    {
      return prepare(open(true), sql, params);
    }
  }

  /** 执行语句 **/

  /**
   * 执行方法
   * @param sql
   * @param params
   * @return 成功或失败
   * @throws CruxException
   */
  public boolean execute(String sql, Object... params)
    throws CruxException
  {
    if (4 == (4 & Core.DEBUG))
    {
      StringBuilder sb = new StringBuilder(sql);
      List paramz = new ArrayList(Arrays.asList(params));
      checkSQLParams(sb, paramz);
      mergeSQLParams(sb, paramz);
      CoreLogger.debug( "DB.execute: " + sb.toString() );
    }

    PreparedStatement ps = prepare(open(true), sql, params);

    try
    {
      return ps.execute( /**/ );
    }
    catch (  SQLException ex  )
    {
      throw new CruxException(ex, 1044);
    }
    finally
    {
            Link.this.close(ps);
    }
  }

  /**
   * 更新方法
   * @param sql
   * @param params
   * @return 更新的条数
   * @throws CruxException
   */
  public int updates(String sql, Object... params)
    throws CruxException
  {
    if (4 == (4 & Core.DEBUG))
    {
      StringBuilder sb = new StringBuilder(sql);
      List paramz = new ArrayList(Arrays.asList(params));
      checkSQLParams(sb, paramz);
      mergeSQLParams(sb, paramz);
      CoreLogger.debug( "DB.updates: " + sb.toString() );
    }

    PreparedStatement ps = prepare(open(true), sql, params);

    try
    {
      return ps.executeUpdate();
    }
    catch (  SQLException ex  )
    {
      throw new CruxException(ex, 1045);
    }
    finally
    {
            Link.this.close(ps);
    }
  }

  /**
   * 更新记录
   * <p>注: 调用update(sql, params...)实现</p>
   * @param table
   * @param values
   * @param where
   * @param params
   * @return 更新条数
   * @throws CruxException
   */
  public int update(String table, Map<String, Object> values, String where, Object... params)
    throws CruxException
  {
    if (values == null || values.isEmpty())
    {
      throw new CruxException(1047, "Update value can not be empty.");
    }
    if ( where == null ||  where.isEmpty())
    {
      throw new CruxException(1048, "Update where can not be empty.");
    }

    table = quoteField(table);

    /** 组织语言 **/

    List paramz = new ArrayList(values.size());
    String   vs = "";
    Iterator it = values.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry entry = (Map.Entry)it.next();
      String field = (String)entry.getKey();
      paramz.add((Object)entry.getValue());

      vs += ", " + quoteField(field) + " = ?";
    }

    String sql = "UPDATE " + table
               + " SET "   + vs.substring(2)
               + " WHERE " + where;

    if (params.length > 0)
    {
      paramz.addAll(Arrays.asList(params));
    }

    /** 执行更新 **/

    return this.updates(sql, paramz.toArray());
  }

  /**
   * 添加记录
   * <p>注: 调用update(sql, params...)实现</p>
   * @param table
   * @param values
   * @return 插入条数
   * @throws CruxException
   */
  public int insert(String table, Map<String, Object> values)
    throws CruxException
  {
    if (values == null || values.isEmpty())
    {
      throw new CruxException(1046, "Insert value can not be empty.");
    }

    table = quoteField(table);

    /** 组织语句 **/

    List paramz = new ArrayList(values.size());
    String   fs = "";
    String   vs = "";
    Iterator it = values.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry entry = (Map.Entry)it.next();
      String field = (String)entry.getKey();
      paramz.add((Object)entry.getValue());

      fs += ", " + quoteField(field);
      vs += ", ?";
    }

    String sql = "INSERT INTO " + table
               + " (" + fs.substring(2) + ")"
               + " VALUES"
               + " (" + vs.substring(2) + ")";

    /** 执行更新 **/

    return this.updates(sql, paramz.toArray());
  }

  /**
   * 删除记录
   * <p>注: 调用update(sql, params...)实现</p>
   * @param table
   * @param where
   * @param params
   * @return 删除条数
   * @throws CruxException
   */
  public int delete(String table, String where, Object... params)
    throws CruxException
  {
    if ( where == null ||  where.isEmpty())
    {
      throw new CruxException(1048, "Delete where can not be empty.");
    }

    table = quoteField(table);

    /** 组织语句 **/

    String sql = "DELETE FROM " + table + " WHERE " + where;

    /** 执行更新 **/

    return this.updates(sql, params);
  }

  //** 查询语句 **/

  /**
   * 查询方法
   * @param sql
   * @param start
   * @param limit
   * @param params
   * @return 查询结果
   * @throws CruxException
   */
  public Loop query(String sql, int start, int limit, Object... params)
    throws CruxException
  {
    Connection co = open();

    // 处理不同数据库的分页
    Lump l = new Lump(co, sql, start, limit, params);
    sql    = l.getSQL(   );
    start  = l.getStart( );
    limit  = l.getLimit( );
    params = l.getParams();

    if (4 == (4 & Core.DEBUG))
    {
      CoreLogger.debug("DB.query: " + l.toString());
    }

    PreparedStatement ps = prepare(co, sql, params);
            ResultSet rs;

    try
    {
      if (limit > 0)
      {
        ps.setFetchSize   (   limit);
        ps.setMaxRows(start + limit);
      }
      rs = ps.executeQuery();
      if (start > 0)
      {
        rs. absolute (start);
      }
    }
    catch (SQLException ex )
    {
      throw new CruxException(ex, 1043);
    }

    return  new Loop(rs, ps);
  }

  /**
   * 获取查询的全部数据
   * <p>会自动执行closeStatement和closeResultSet</p>
   * @param sql
   * @param start
   * @param limit
   * @param params
   * @return 全部数据
   * @throws CruxException
   */
  public List fetch(String sql, int start, int limit, Object... params)
    throws CruxException
  {
    List<Map<String, Object>> rows = new ArrayList();
         Map<String, Object>  row  ;

    try (Loop rs = query(sql, start, limit, params))
    {
        while ( (row = rs.next()) != null )
        {
          rows.add(row);
        }
    }

    return rows;
  }

  /**
   * 获取查询的全部数据
   * @param sql
   * @param params
   * @return 全部数据
   * @throws CruxException
   */
  public List fetchAll(String sql, Object... params)
    throws CruxException
  {
    List<Map<String, Object>> rows = new ArrayList();
         Map<String, Object>  row  ;

    try (Loop rs = query(sql, 0, 0, params))
    {
        while ( (row = rs.next()) != null )
        {
          rows.add(row);
        }
    }

    return rows;
  }

  /**
   * 获取查询的单条数据
   * @param sql
   * @param params
   * @return 单条数据
   * @throws CruxException
   */
  public Map  fetchOne(String sql, Object... params)
    throws CruxException
  {
    try (Loop rs = query(sql, 0, 1, params))
    {
        Map<String , Object> row = rs.next();
        if (row == null) row = new LinkedHashMap( );
        return row;
    }
  }

  //** 静态工具 **/

  /**
   * 引用字段名
   * @param field
   * @return 引用后的串
   */
  public static String quoteField(String field)
  {
    return "`" + Syno.escape(field, "`") + "`";
  }

  /**
   * 引用值
   * @param value
   * @return 引用后的串
   */
  public static String quoteValue(String value)
  {
    return "'" + Syno.escape(value, "'") + "'";
  }

  /**
   * 检查SQL数据项
   * @param sql
   * @param params
   * @throws CruxException
   */
  public static void checkSQLParams(StringBuilder sql, List params)
    throws CruxException
  {
    if (params == null)
    {
      params = new ArrayList();
    }

    int pos = 0;
    int num = 0;

    while ((pos = sql.indexOf("?", pos)) != -1)
    {
      if (num >= params.size())
      {
        break;
      }

      /**
       * 如果参数是数组或List
       * 则将其全部转化为Set
       * 以供继续后面的处理
       */
      Object obj = params.get(num);
      if (obj != null && obj.getClass( ).isArray( ) )
      {
        obj = new LinkedHashSet(Arrays.asList((Object[]) obj));
      }
      else
      if (obj instanceof Map )
      {
        obj = new LinkedHashSet(((Map) obj).values());
      }
      else
      if (obj instanceof List)
      {
        obj = new LinkedHashSet((List) obj);
      }

      /**
       * 如果参数是Set,
       * 则视为"SQL IN"语句,
       * 将在当前问号后补充足量的问号,
       * 并将参数补充到当前参数列表中.
       */
      if (obj instanceof Collection)
      {
        Collection set =(Collection) obj;
        int off  = num;

        // 加一个空参数防止语法错误
        if (set.isEmpty())
        {
          set = Arrays.asList((Object)  null);
        }

        // 从第二个参数开始补充问号
        for (int i = 1; i < set.size(); i ++)
        {
          sql.insert(pos + 1, ",?");
          pos += 2;
          num += 1;
        }

        // 平铺到参数列表中
        params.remove(off/***/);
        params.addAll(off, set);
      }

      pos += 1;
      num += 1;
    }

    if (num != params.size())
    {
      throw new CruxException(1049,
        "The number of '?' and the number of parameters are inconsistent."
        + " ?s: " + num + " params: " + params.size() + " SQL: " + sql);
    }
  }

  /**
   * 绑定SQL数据项
   * 调用本方法前务必先调用内checkSQLParams
   * @param sql
   * @param params
   * @throws CruxException
   */
  public static void mergeSQLParams(StringBuilder sql, List params)
    throws CruxException
  {
    if (params == null)
    {
      params = new ArrayList();
    }

    int pos = 0;
    int num = 0;

    /**
     * 填充参数
     */
    while ((pos = sql.indexOf("?", pos)) != -1)
    {
      if (num >= params.size())
      {
        break;
      }

      /**
       * 如果参数是NULL,
       * 则直接加入NULL;
       * 如果参数是数字,
       * 则直接加入数字;
       * 如果参数是其他类型,
       * 则转换成字符串并加引号.
       */

      Object obj = params.get(num);

      String str;
      if (obj == null)
      {
        str = "NULL";
      }
      else
      if (obj instanceof Number)
      {
        str = Synt.asString((Number) obj);
      }
      else
      {
        str = quoteValue(obj.toString( ));
      }

      sql.replace(pos, pos + 1, str);

      pos += str.length( ) - 1;
      num += 1;
    }

    if (num != params.size())
    {
      throw new CruxException(1049,
        "The number of '?' and the number of parameters are inconsistent."
        + " ?s: " + num + " params: " + params.size() + " SQL: " + sql);
    }
  }

  @Override
  protected void finalize()
    throws Throwable
  {
    try {
      this .close (  );
    } finally {
      super.finalize();
    }
  }

}
