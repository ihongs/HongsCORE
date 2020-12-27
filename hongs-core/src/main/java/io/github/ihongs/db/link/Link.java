package io.github.ihongs.db.link;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
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

/**
 * 抽象数据连接
 * @author Hongs
 */
abstract public class Link
  implements IReflux, AutoCloseable
{

  /**
   * 是否为对象模式(即获取的是对象)
   */
  protected boolean OBJECT_MODE;

  /**
   * 是否为事务模式(即不会自动提交)
   */
  protected boolean REFLUX_MODE;

  /**
   * 初始的事务模式
   */
  private final boolean REFLUX_BASE;

  /**
   * 库名
   */
  public String name;

  /**
   * 连接对象
   */
  protected Connection connection;

  public Link(String name)
    throws HongsException
  {
    this.name = name;

    // 是否为对象模式
    Object ox  = Core.getInstance().got(Cnst.OBJECT_MODE);
    if ( ( ox != null  &&  Synt.declare( ox , false  )  )
    ||     CoreConfig.getInstance().getProperty("core.in.object.mode", false)) {
        OBJECT_MODE = true;
    }

    // 是否要开启事务
    Object tr  = Core.getInstance().got(Cnst.REFLUX_MODE);
    if ( ( tr != null  &&  Synt.declare( tr , false  )  )
    ||     CoreConfig.getInstance().getProperty("core.in.reflux.mode", false)) {
        REFLUX_MODE = true;
    }

    REFLUX_BASE = REFLUX_MODE;
  }

  /**
   * 开启连接
   * @return
   * @throws HongsException
   */
  public abstract Connection open()
    throws HongsException;

  /**
   * 关闭连接
   */
  @Override
  public void close( )
  {
    try
    {
      if (this.connection != null
      && !this.connection.isClosed())
      {
        try
        {
          /**
           * 关闭之前先将未提交的语句提交
           */
          if (!connection.getAutoCommit())
          {
            try
            {
              connection.commit(  );
            }
            catch (SQLException ex)
            {
              connection.rollback();
              throw ex;
            }
          }
        }
        finally
        {
          this.connection.close(  );

          CoreLogger.trace("DB: Connection '{}' has been closed", name);
        }
      }
    }
    catch (SQLException er )
    {
      CoreLogger.error( er );
    }
    finally
    {
      this.connection = null;
    }
  }

  /**
   * 执行准备
   * @throws HongsException
   */
  public void ready( )
    throws HongsException
  {
    this.open(); // 先连接数据库

    try {
        if (this.connection.getAutoCommit() == this.REFLUX_MODE) {
            this.connection.setAutoCommit(  !  this.REFLUX_MODE);
        }
    } catch (SQLException ex) {
        throw new HongsException(1053, ex);
    }
  }

  /**
   * 事务开始
   */
  @Override
  public void begin( )
  {
    REFLUX_MODE = true;
    try {
        if (connection != null
        && !connection.isClosed()) {
            connection.setAutoCommit(false);
        }
    } catch (SQLException ex) {
        throw new HongsExemption(1054, ex);
    }
  }

  /**
   * 事务提交
   */
  @Override
  public void commit()
  {
    REFLUX_MODE = REFLUX_BASE;
    try {
        if (connection != null
        && !connection.isClosed()
        && !connection.getAutoCommit()) {
            connection.commit(  );
        }
    } catch (SQLException ex) {
        throw new HongsExemption(1055, ex);
    }
  }

  /**
   * 事务回滚
   */
  @Override
  public void revert()
  {
    REFLUX_MODE = REFLUX_BASE;
    try {
        if (connection != null
        && !connection.isClosed()
        && !connection.getAutoCommit()) {
            connection.rollback();
        }
    } catch (SQLException ex) {
        throw new HongsExemption(1056, ex);
    }
  }

  /** 查询辅助 **/

  /**
   * 预编译Statement并设置查询选项
   * <p>可使用cacheStatement开启缓存</p>
   * @param sql
   * @param params
   * @return PreparedStatement对象
   * @throws HongsException
   */
  public PreparedStatement prepareStatement(String sql, Object... params)
    throws HongsException
  {
    /**
     * 检查SQL语句及Params
     * 以发现里面的Set对象
     */
    List      paramz = new ArrayList(Arrays.asList(params));
    StringBuilder sb = new StringBuilder(sql);
    checkSQLParams(sb, paramz);
    sql = sb.toString();

    PreparedStatement ps = this.prepareStatement(sql);

    /**
     * 遍历params以执行PreparedStatement.setObject
     * 如果开启字符串模式, 则参数均以字符串形式绑定
     */
    try
    {
      int i = 0;
      for (Object x : paramz)
      {
        ps.setObject(++ i, x);
      }
    }
    catch ( SQLException ex )
    {
      throw new HongsException(1042, ex);
    }

    return ps;
  }

  /**
   * 当需要在prepareStatement时设定参数, 可重载该方法
   * 异常代码为: 1041
   * @param sql
   * @return PreparedStatement对象
   * @throws HongsException
   */
  public PreparedStatement prepareStatement(String sql)
    throws HongsException
  {
    PreparedStatement ps;

    try
    {
      ps = this.connection.prepareStatement(sql);
    }
    catch (SQLException ex)
    {
      throw new HongsException(1041, ex);
    }

    return ps;
  }

  /**
   * 当需要在createStatement时设定参数, 可重载该方法
   * @return Statement对象
   * @throws HongsException
   */
  public Statement createStatement()
    throws HongsException
  {
    Statement ps;

    try
    {
      ps = this.connection.createStatement();
    }
    catch (SQLException ex)
    {
      throw new HongsException(1041, ex);
    }

    return ps;
  }

  /**
   * 关闭Statement
   * @param ps
   * @throws HongsException
   */
  public void closeStatement(Statement ps)
    throws HongsException
  {
    try
    {
      if (ps == null || ps.isClosed()) return;
      ps.close();
    }
    catch (SQLException ex)
    {
      throw new HongsException(1034, ex);
    }
  }

  /**
   * 关闭ResultSet
   * @param rs
   * @throws HongsException
   */
  public void closeResultSet(ResultSet rs)
    throws HongsException
  {
    try
    {
      if (rs == null || rs.isClosed()) return;
      rs.close();
    }
    catch (SQLException ex)
    {
      throw new HongsException(1035, ex);
    }
  }

  //** 查询语句 **/

  /**
   * 查询方法
   * @param sql
   * @param start
   * @param limit
   * @param params
   * @return 查询结果
   * @throws HongsException
   */
  public Loop query(String sql, int start, int limit, Object... params)
    throws HongsException
  {
    /**
     * 由于 SQLite 等不支持 absolute 方法
     * 故对这样的库采用组织语句的分页查询
     */
    if (limit == 0) {
        this.open();
    }   else   try  {
        String dpn =
        this.open()
        .getMetaData()
        .getDatabaseProductName()
        .toUpperCase();

        if ("SQLITE".equals(dpn)) {
            sql += " LIMIT ?,?";
            Object[] paramz = new Object[params.length + 2];
            System.arraycopy(params, 0, paramz, 0, params.length);
            paramz[params.length + 0] = start;
            paramz[params.length + 1] = limit;
            params = paramz;
            start  = 0;
            limit  = 0;
        }
    } catch (SQLException ex) {
        throw new HongsException(ex);
    }

    if (4 == (4 & Core.DEBUG))
    {
      StringBuilder sb = new StringBuilder(sql);
      List      paramz = new ArrayList(Arrays.asList(params));
      checkSQLParams(sb, paramz);
      mergeSQLParams(sb, paramz);
      CoreLogger.debug("DB.query: "+ sb.toString());
    }

    PreparedStatement ps = this.prepareStatement(sql, params);
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
      throw new HongsException(1043, ex);
    }

    Loop loop = new Loop( rs, ps );
    loop.inObjectMode(OBJECT_MODE);
    return loop;
  }

  /**
   * 获取查询的全部数据
   * <p>会自动执行closeStatement和closeResultSet</p>
   * @param sql
   * @param start
   * @param limit
   * @param params
   * @return 全部数据
   * @throws HongsException
   */
  public List fetch(String sql, int start, int limit, Object... params)
    throws HongsException
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
   * @throws HongsException
   */
  public List fetchAll(String sql, Object... params)
    throws HongsException
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
   * @throws HongsException
   */
  public Map  fetchOne(String sql, Object... params)
    throws HongsException
  {
    try (Loop rs = query(sql, 0, 1, params))
    {
        Map<String , Object> row = rs.next();
        if (row == null) row = new LinkedHashMap( );
        return row;
    }
  }

  /** 执行语句 **/

  /**
   * 执行方法
   * @param sql
   * @param params
   * @return 成功或失败
   * @throws HongsException
   */
  public boolean execute(String sql, Object... params)
    throws HongsException
  {
    this.ready();

    if (4 == (4 & Core.DEBUG))
    {
      StringBuilder sb = new StringBuilder(sql);
      List      paramz = new ArrayList(Arrays.asList(params));
      checkSQLParams(sb, paramz);
      mergeSQLParams(sb, paramz);
      CoreLogger.debug("DB.execute: " + sb.toString());
    }

    PreparedStatement ps = this.prepareStatement(sql, params);

    try
    {
      return ps.execute(/****/);
    }
    catch (  SQLException  ex )
    {
      throw new HongsException(1044, ex);
    }
    finally
    {
      this.closeStatement( ps );
    }
  }

  /**
   * 更新方法
   * @param sql
   * @param params
   * @return 更新的条数
   * @throws HongsException
   */
  public int updates(String sql, Object... params)
    throws HongsException
  {
    this.ready();

    if (4 == (4 & Core.DEBUG))
    {
      StringBuilder sb = new StringBuilder(sql);
      List      paramz = new ArrayList(Arrays.asList(params));
      checkSQLParams(sb, paramz);
      mergeSQLParams(sb, paramz);
      CoreLogger.debug("DB.updates: " + sb.toString());
    }

    PreparedStatement ps = this.prepareStatement(sql, params);

    try
    {
      return ps.executeUpdate();
    }
    catch (  SQLException  ex )
    {
      throw new HongsException(1045, ex);
    }
    finally
    {
      this.closeStatement( ps );
    }
  }

  /**
   * 添加记录
   * <p>注: 调用update(sql, params...)实现</p>
   * @param table
   * @param values
   * @return 插入条数
   * @throws HongsException
   */
  public int insert(String table, Map<String, Object> values)
    throws HongsException
  {
    if (values == null || values.isEmpty())
    {
      throw new HongsException(1046, "Insert value can not be empty.");
    }

    table = quoteField(table);

    /** 组织语句 **/

    List paramz = new ArrayList();
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
   * 更新记录
   * <p>注: 调用update(sql, params...)实现</p>
   * @param table
   * @param values
   * @param where
   * @param params
   * @return 更新条数
   * @throws HongsException
   */
  public int update(String table, Map<String, Object> values, String where, Object... params)
    throws HongsException
  {
    if (values == null || values.isEmpty())
    {
      throw new HongsException(1047, "Update value can not be empty.");
    }
    if ( where == null ||  where.isEmpty())
    {
      throw new HongsException(1048, "Update where can not be empty.");
    }

    table = quoteField(table);

    /** 组织语言 **/

    List paramz = new ArrayList();
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
   * 删除记录
   * <p>注: 调用update(sql, params...)实现</p>
   * @param table
   * @param where
   * @param params
   * @return 删除条数
   * @throws HongsException
   */
  public int delete(String table, String where, Object... params)
    throws HongsException
  {
    if ( where == null ||  where.isEmpty())
    {
      throw new HongsException(1048, "Delete where can not be empty.");
    }

    table = quoteField(table);

    /** 组织语句 **/

    String sql = "DELETE FROM " + table + " WHERE " + where;

    /** 执行更新 **/

    return this.updates(sql, params);
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
   * @throws HongsException
   */
  public static void checkSQLParams(StringBuilder sql, List params)
    throws HongsException
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
      throw new HongsException(1049,
        "The number of '?' and the number of parameters are inconsistent."
        + " ?s: " + num + " params: " + params.size() + " SQL: " + sql);
    }
  }

  /**
   * 绑定SQL数据项
   * 调用本方法前务必先调用内checkSQLParams
   * @param sql
   * @param params
   * @throws HongsException
   */
  public static void mergeSQLParams(StringBuilder sql, List params)
    throws HongsException
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
      throw new HongsException(1049,
        "The number of '?' and the number of parameters are inconsistent."
        + " ?s: " + num + " params: " + params.size() + " SQL: " + sql);
    }
  }

  @Override
  protected void finalize()
    throws Throwable
  {
    try {
      this .close();
    } finally {
      super.finalize();
    }
  }

}
