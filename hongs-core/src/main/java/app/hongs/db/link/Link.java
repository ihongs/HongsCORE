package app.hongs.db.link;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreLogger;
import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.dh.ITrnsct;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 抽象数据连接
 * @author Hongs
 */
abstract public class Link
implements  ITrnsct , Core.Destroy
{

  /**
   * 是否为事务模式(即不会自动提交)
   */
  public boolean IN_TRNSCT_MODE;

  /**
   * 是否为对象模式(即获取的是对象)
   */
  public boolean IN_OBJECT_MODE;

  /**
   * 库名
   */
  public     String    name;

  /**
   * 执行标识
   */
  protected  boolean   initialled;

  /**
   * 连接对象
   */
  protected Connection connection;

  public Link(String name)
    throws HongsException
  {
    this.name = name;
  }

  public abstract Connection connect()
    throws HongsException;

  public void initial()
    throws HongsException
  {
    // 自动提交设置
    try
    {
      this.connection.setAutoCommit(! this.IN_TRNSCT_MODE);
    }
    catch (SQLException ex)
    {
      throw new HongsException(0x1026, ex);
    }

    // 准备执行标识
    this.initialled = true;
  }

  @Override
  public void destroy()
  {
    try
    {
      if (this.connection == null
      ||  this.connection.isClosed(/***/))
      {
        return;
      }

      // 退出自动提交
      if (this.initialled
      && !this.connection.getAutoCommit())
      {
        try
        {
          this.commit();
        }
        catch (Exception | Error e)
        {
          CoreLogger.error(e);

        try
        {
          this.rolbak();
        }
        catch (Exception | Error x)
        {
          CoreLogger.error(x);
        }

        }
      }

      this.connection.close();
    }
    catch (SQLException ex)
    {
      CoreLogger.error( ex);
    }
    finally
    {
      this.connection = null ;
      this.initialled = false;
    }

    if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG))
    {
      CoreLogger.trace("DB: Connection '"+name+"' has been closed");
    }
  }

  /**
   * 事务:开始
   */
  @Override
  public void trnsct()
  {
    this.IN_TRNSCT_MODE = true;
    try {
        if (connection != null
        && !connection.isClosed()) {
            connection.setAutoCommit(false);
        }
    } catch (SQLException ex) {
        throw new HongsError(0x3a, ex);
    }
  }

  /**
   * 事务:提交
   */
  @Override
  public void commit()
  {
    if (!IN_TRNSCT_MODE) {
        return;
    }
    IN_TRNSCT_MODE = Synt.declare(Core.getInstance().got(Cnst.TRNSCT_MODE), false);
    try {
        if (connection != null
        && !connection.isClosed()
        && !connection.getAutoCommit()) {
            connection.commit(  );
        }
    } catch (SQLException ex) {
        throw new HongsError(0x3b, ex);
    } finally {
        this.initialled = false  ;
    }
  }

  /**
   * 事务:回滚
   */
  @Override
  public void rolbak()
  {
    if (!IN_TRNSCT_MODE) {
        return;
    }
    IN_TRNSCT_MODE = Synt.declare(Core.getInstance().got(Cnst.TRNSCT_MODE), false);
    try {
        if (connection != null
        && !connection.isClosed()
        && !connection.getAutoCommit()) {
            connection.rollback();
        }
    } catch (SQLException ex) {
        throw new HongsError(0x3c, ex);
    } finally {
        this.initialled = false  ;
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
      throw new HongsException(0x1042, ex);
    }

    return ps;
  }

  /**
   * 当需要在prepareStatement时设定参数, 可重载该方法
   * 异常代码为: 0x1041
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
      throw new HongsException(0x1041, ex);
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
      throw new HongsException(0x1041, ex);
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
      throw new app.hongs.HongsException(0x1034, ex);
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
      throw new app.hongs.HongsException(0x1035, ex);
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
        this.connect();
    }   else   try  {
        String dpn =
        this.connect()
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
        throw new HongsError(0x10, ex);
    }

    if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG))
    {
      StringBuilder sb = new StringBuilder(sql);
      List      paramz = new ArrayList(Arrays.asList(params));
      checkSQLParams(sb, paramz);
      mergeSQLParams(sb, paramz);
      app.hongs.CoreLogger.debug("DB.query: "+ sb.toString());
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
      throw new app.hongs.HongsException(0x1043, ex);
    }

    return new Loop(this, ps, rs);
  }

  /**
   * 获取查询的全部数据
   * <p>会自动执行closeStatement和closeResultSet</p>
   * @param sql
   * @param start
   * @param limit
   * @param params
   * @return 全部数据
   * @throws app.hongs.HongsException
   */
  public List fetch(String sql, int start, int limit, Object... params)
    throws HongsException
  {
    List<Map<String, Object>> rows = new ArrayList();
         Map<String, Object>  row;

    Loop rs  = this.query(sql, start, limit, params);
    while (( row = rs.next() ) != null)
    {
      rows.add(row);
    }

    return rows;
  }

  /**
   * 获取查询的全部数据
   * <p>注: 调fetch实现</p>
   * @param sql
   * @param params
   * @return 全部数据
   * @throws app.hongs.HongsException
   */
  public List fetchAll(String sql, Object... params)
    throws HongsException
  {
    return this.fetch(sql, 0, 0, params);
  }

  /**
   * 获取查询的单条数据
   * <p>注: 调fetch实现</p>
   * @param sql
   * @param params
   * @return 单条数据
   * @throws app.hongs.HongsException
   */
  public Map  fetchOne(String sql, Object... params)
    throws HongsException
  {
    List<Map<String, Object>> rows = this.fetch(sql, 0, 1, params);
    if (! rows.isEmpty( ))
    {
      return rows.get( 0 );
    }
    else
    {
      return new HashMap();
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
    this.connect();
    this.initial();

    if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG))
    {
      StringBuilder sb = new StringBuilder(sql);
      List      paramz = new ArrayList(Arrays.asList(params));
      checkSQLParams(sb, paramz);
      mergeSQLParams(sb, paramz);
      app.hongs.CoreLogger.debug("DB.execute: " + sb.toString());
    }

    PreparedStatement ps = this.prepareStatement(sql, params);

    try
    {
      return ps.execute(/****/);
    }
    catch (  SQLException  ex )
    {
      throw new app.hongs.HongsException(0x104a, ex);
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
    this.connect();
    this.initial();

    if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG))
    {
      StringBuilder sb = new StringBuilder(sql);
      List      paramz = new ArrayList(Arrays.asList(params));
      checkSQLParams(sb, paramz);
      mergeSQLParams(sb, paramz);
      app.hongs.CoreLogger.debug("DB.updates: " + sb.toString());
    }

    PreparedStatement ps = this.prepareStatement(sql, params);

    try
    {
      return ps.executeUpdate();
    }
    catch (  SQLException  ex )
    {
      throw new app.hongs.HongsException(0x104e, ex);
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
   * @throws app.hongs.HongsException
   */
  public int insert(String table, Map<String, Object> values)
    throws HongsException
  {
    if (values == null || values.isEmpty())
    {
      throw new app.hongs.HongsException(0x104b, "Insert value can not be empty.");
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
   * @throws app.hongs.HongsException
   */
  public int update(String table, Map<String, Object> values, String where, Object... params)
    throws HongsException
  {
    if (values == null || values.isEmpty())
    {
      throw new app.hongs.HongsException(0x104d, "Update value can not be empty.");
    }
    if ( where == null ||  where.isEmpty())
    {
      throw new app.hongs.HongsException(0x1052, "Update where can not be empty.");
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
   * @throws app.hongs.HongsException
   */
  public int delete(String table, String where, Object... params)
    throws HongsException
  {
    if ( where == null ||  where.isEmpty())
    {
      throw new app.hongs.HongsException(0x1052, "Delete where can not be empty.");
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
    return "`" + Tool.escape(field, "`") + "`";
  }

  /**
   * 引用值
   * @param value
   * @return 引用后的串
   */
  public static String quoteValue(String value)
  {
    return "'" + Tool.escape(value, "'") + "'";
  }

  /**
   * 检查SQL数据项
   * @param sql
   * @param params
   * @throws app.hongs.HongsException
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
          set = new HashSet();
          set.add( null );
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
      throw new HongsException(0x1051,
        "The number of '?' and the number of parameters are inconsistent."
        + " ?s: " + num + " params: " + params.size() + " SQL: " + sql);
    }
  }

  /**
   * 绑定SQL数据项
   * 调用本方法前务必先调用内checkSQLParams
   * @param sql
   * @param params
   * @throws app.hongs.HongsException
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
        str = obj.toString();
      }
      else
      {
        str = obj.toString();
        str = quoteValue(str);
      }

      sql.replace(pos, pos + 1, str);

      pos += str.length() - 1;
      num += 1;
    }

    if (num != params.size())
    {
      throw new HongsException(0x1051,
        "The number of '?' and the number of parameters are inconsistent."
        + " ?s: " + num + " params: " + params.size() + " SQL: " + sql);
    }
  }

}
