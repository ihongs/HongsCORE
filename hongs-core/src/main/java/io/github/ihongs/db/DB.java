package io.github.ihongs.db;

import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.db.link.Link;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.db.link.Origin;
import io.github.ihongs.db.link.Source;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * 数据库基础类
 *
 * <p>
 * 当需要库对象时, 一般情况可调用其工厂方法getInstance获取;
 * 当需要扩展类时, 请从DB继承并实现一个无参getInstance方法.
 * 当配置有dock时, 读写分离, 读连接为open, 写连接为open(true|false).
 * </p>
 *
 * <h3>异常代码:</h3>
 * <pre>
 * 区间: 1020~1059
 *
 * 1021  找不到外部数据源配置
 * 1022  连接外部数据源失败
 * 1023  找不到内部数据源配置
 * 1024  连接内部数据源失败
 * 1025  找不到数据源配置
 *
 * 1031  开启Connection失败
 * 1032  关闭Connection失败
 * 1033  取消Statement失败
 * 1034  关闭Statement失败
 * 1035  关闭ResultSet失败
 *
 * 1026  找不到表配置
 * 1027  找不到表对应的类
 * 1028  无法获取表构造器
 * 1029  无法获取表实例
 * 1036  找不到模型对应的类
 * 1037  无法获取模型构造器
 * 1038  无法获取模型实例
 *
 * 1041  构建语句失败
 * 1042  绑定参数失败
 * 1043  查询语句失败
 * 1044  执行语句失败
 * 1045  执行更新语句失败
 * 1046  插入的值不能为空
 * 1047  更新的值不能为空
 * 1048  语句条件不能为空
 * 1049  语句参数个数不符
 * </pre>
 *
 * @author Hongs
 */
public class DB
  extends  Link
{

  /**
   * 模型类
   */
  protected String modelClass;

  /**
   * 库表类
   */
  protected String tableClass;

  /**
   * 表前缀
   */
  protected String tablePrefix;

  /**
   * 表后缀
   */
  protected String tableSuffix;

  /**
   * 表配置
   */
  protected Map<String, Map  > tableConfigs;

  /**
   * 库表对象
   */
  protected Map<String, Table> tableObjects;

  /**
   * 模型对象
   */
  protected Map<String, Model> modelObjects;

  private final Map source;
  private final Map origin;
  private final DB  link  ;
  private final DB  dock  ;

  protected DB(DBConfig conf)
    throws CruxException
  {
    super(conf.name);

    this.source       = conf.source;
    this.origin       = conf.origin;
    this.modelClass   = conf.modelClass;
    this.tableClass   = conf.tableClass;
    this.tablePrefix  = conf.tablePrefix;
    this.tableSuffix  = conf.tableSuffix;
    this.tableConfigs = conf.tableConfigs;
    this.tableObjects = new HashMap();
    this.modelObjects = new HashMap();

    /**
     * 当有指定 link 而又没指定 source 和 origin 时
     * 则直接用 link 库进行连接
     */
    if (conf.link != null && ! "".equals(conf.link)
    && (source == null || source.isEmpty() )
    && (origin == null || origin.isEmpty()))
    {
        this.link  = /**/ DB.getInstance(conf.link);
    }
    else
    {
        this.link  = null;
    }

    /**
     * 当有指定 dock 则用此执行 execute 和 updates
     */
    if (conf.dock != null && ! "".equals(conf.dock))
    {
        this.dock  = /**/ DB.getInstance(conf.dock);
    }
    else
    {
        this.dock  = null;
    }
  }

  /**
   * 开启读连接
   * @return
   * @throws CruxException
   */
  @Override
  public Connection open()
    throws CruxException
  {
    if (link != null)
    {
      return link.open();
    }

    /** 连接数据源 **/

    TOP: do
    {

    try
    {
      if (connection != null
      && !connection.isClosed())
      {
        break;
      }
    }
    catch (SQLException ex)
    {
      throw new CruxException(ex, 1031);
    }

    Exception ez  = null;

    /** 使用外部数据源 **/

    do
    {
      if (origin == null || origin.isEmpty( ))
      {
        break;
      }

      if (origin.containsKey("jndi") == false)
      {
        throw new CruxException(1021, "Can not find jndi in origin");
      }
      if (origin.containsKey("name") == false)
      {
        throw new CruxException(1021, "Can not find name in origin");
      }

      String mode = (String)origin.get("jndi");
      String namc = (String)origin.get("name");
      Properties info = (Properties)origin.get("info");

      try
      {
        connection = Origin.open(mode, namc, info );
      }
      catch (SQLException ex)
      {
        throw new CruxException(ex, 1022);
      }
      catch (javax.naming.NamingException ex )
      {
        ez = ex ; break ; // 没有对应的数据源, 尝试其他连接方式
      }

      CoreLogger.trace("DB: Connect to '{}' by origin mode: {} {}", name, mode, namc);

      break TOP;
    }
    while (false);

    /** 使用内部数据源 **/

    do
    {
      if (source == null || source.isEmpty())
      {
        break;
      }

      if (!source.containsKey("jdbc"))
      {
        throw new CruxException(1023, "Can not find jdbc in source");
      }
      if (!source.containsKey("name"))
      {
        throw new CruxException(1023, "Can not find name in source");
      }

      String mode = (String)source.get("jdbc");
      String namc = (String)source.get("name");
      Properties info = (Properties)source.get("info");

      try
      {
        connection = Source.open(mode, namc, info );
      }
      catch (SQLException ex)
      {
        throw new CruxException(ex, 1024);
      }

      CoreLogger.trace("DB: Connect to '{}' by source mode: {} {} ", name, mode, namc);

      break TOP;
    }
    while (false);

    if (ez !=null)
    {
      throw new CruxException(ez, 1025);
    }
    else
    {
      throw new CruxException(1025, "Can not find source or origin");
    }

    }
    while (false);

    return connection;
  }

  /**
   * 开启写连接
   * @param  open false 可以不连 true 立即连接
   * @return open false 时可能为 null
   * @throws CruxException
   */
  @Override
  public Connection open(boolean open)
    throws CruxException
  {
    if (dock != null) {
      return dock .open(open);
    } else
    if (link != null) {
      return link .open(open);
    } else
    {
      return super.open(open);
    }
  }

  /**
   * 采用查询体获取全部数据
   * <p>注: 调query实现</p>
   * @param caze
   * @return 全部数据
   * @throws io.github.ihongs.CruxException
   */
  public Loop queryMore(FetchCase caze)
    throws CruxException
  {
    return caze.use(this).select();
  }

  /**
   * 采用查询体获取全部数据
   * <p>注: 调fetch实现</p>
   * @param caze
   * @return 全部数据
   * @throws io.github.ihongs.CruxException
   */
  public List fetchMore(FetchCase caze)
    throws CruxException
  {
    return caze.use(this).getAll();
  }

  /**
   * 采用查询体获取单条数据
   * <p>注: 调fetch实现</p>
   * @param caze
   * @return 单条数据
   * @throws io.github.ihongs.CruxException
   */
  public Map  fetchLess(FetchCase caze)
    throws CruxException
  {
    return caze.use(this).getOne();
  }

  /**
   * 调用 FetchCase 构建查询
   * 可用 getAll , getOne 得到结果, 以及 delete, update 等操作数据
   * @return 绑定了 db 的查询对象
   */
  public FetchCase fetchCase()
  {
    return new FetchCase().use( this );
  }

  /**
   * 快速取 FetchCase
   * @param tableName 真实表名
   * @return
   */
  public FetchCase from (String tableName)
  {
    return fetchCase().from(tableName);
  }

  /**
   * 快速取 FetchCase
   * @param tableName 内部表名
   * @return
   * @throws io.github.ihongs.CruxException
   */
  public FetchCase with (String tableName)
    throws CruxException
  {
    Table  tableInst = getTable(tableName);
    return fetchCase().from(tableInst.tableName, tableInst.name);
  }

  //** 模型方法 **/

  /**
   * 获得全部的关联表名
   * @return 关联名集合
   */
  public Set<String> getTableNames()
  {
    return this.tableConfigs.keySet();
  }

  /**
   * 获得对应的完整表名
   * @param name
   * @return
   */
  public String getTableName(String name)
  {
    Map cnf = this.tableConfigs.get(name);
    if (cnf == null) {
        return null;
    }

    if (cnf.containsKey("tableName")) {
        name = (String) cnf.get("tableName");
    }
    if (this.tablePrefix != null) {
        name = this.tablePrefix + name;
    }
    if (this.tableSuffix != null) {
        name = name + this.tableSuffix;
    }
    return name;
  }

  /**
   * 通过表名获取表对象
   * 表名可以为"库名.表名"
   * @param tableName table 对象的 name
   * @return 指定表对象
   * @throws io.github.ihongs.CruxException
   */
  public Table getTable(String tableName)
    throws CruxException
  {
    /**
     * 表名可以是"数据库.表名"
     * 用于引用另一个库中的表
     */
    int pos = tableName.indexOf('.');
    if (pos > 0)
    {
      String db = tableName.substring(0,  pos);
      tableName = tableName.substring(pos + 1);
      return DB.getInstance(db).getTable(tableName);
    }

    if ( this.tableObjects.containsKey(tableName))
    {
      return this.tableObjects.get(tableName);
    }

    if (!this.tableConfigs.containsKey(tableName))
    {
      throw new CruxException(1026, "Can not find config for table '"+this.name+"."+tableName+"'.");
    }

    /**
     * 读取库指定的tableClass
     * 读取表对应的tableConfig
     */
    Map<String, String> tcfg = this.tableConfigs.get(tableName);
    //this.tableConfigs.remove(tableName);
    String tcls = this.tableClass;
    String tpfx = this.tablePrefix;
    String tsfx = this.tableSuffix;

    /**
     * 就近原则:
     * 如果表配置中有设置class则采用表配置中的
     * 如果表配置中没有设置prefix则采用库配置中的
     * 如果表配置中没有设置suffix则采用库配置中的
     */
    if (tcfg.containsKey("class"))
    {
      tcls = tcfg.get("class");
    }
    if (!tcfg.containsKey("prefix"))
    {
      tcfg.put("prefix", tpfx);
    }
    if (!tcfg.containsKey("suffix"))
    {
      tcfg.put("suffix", tsfx);
    }

    /**
     * 如果class为空则直接使用默认的Table
     */
    if (tcls == null || tcls.length() == 0)
    {
      Table tobj = new Table(this, tcfg);
      this.tableObjects.put(tableName, tobj);
      return tobj;
    }

    CoreLogger.trace("DB: tableClass({}) for table({}.{}) has been defined, try to get it", tcls, this.name, tableName);

    /**
     * 获取指定的Table类
     */
    Class cls;
    try
    {
      cls = Class.forName(tcls);
    }
    catch (ClassNotFoundException ex)
    {
      throw new CruxException(ex, 1027);
    }

    /**
     * 获取构造器
     */
    Constructor cst;
    try
    {
      cst = cls.getConstructor(new Class[]{DB.class, Map.class});
    }
    catch (NoSuchMethodException ex)
    {
      throw new CruxException(ex, 1028);
    }
    catch (SecurityException ex)
    {
      throw new CruxException(ex, 1028);
    }

    /**
     * 获取表实例
     */
    Table tobj;
    try
    {
      tobj = (Table)cst.newInstance(new Object[]{this, tcfg});
    }
    catch (InstantiationException ex)
    {
      throw new CruxException(ex, 1029);
    }
    catch (IllegalAccessException ex)
    {
      throw new CruxException(ex, 1029);
    }
    catch (IllegalArgumentException ex)
    {
      throw new CruxException(ex, 1029);
    }
    catch (InvocationTargetException ex)
    {
      throw new CruxException(ex, 1029);
    }

    this.tableObjects.put(tableName, tobj);
    return tobj;
  }

  /**
   * 通过表名获取表模型
   * 表名可以为"库名.表名"
   * @param tableName table 对象的 name
   * @return 指定表模型
   * @throws io.github.ihongs.CruxException
   */
  public Model getModel(String tableName)
    throws CruxException
  {
    /**
     * 表名可以是"数据库.表名"
     * 用于引用另一个库中的表
     */
    int pos = tableName.lastIndexOf('.');
    if (pos > 0)
    {
      String db = tableName.substring(0,  pos);
      tableName = tableName.substring(pos + 1);
      return DB.getInstance(db).getModel(tableName);
    }

    if ( this.modelObjects.containsKey(tableName))
    {
      return this.modelObjects.get(tableName);
    }

    if (!this.tableConfigs.containsKey(tableName))
    {
      throw new CruxException(1026, "Can not find config for table '"+this.name+"."+tableName+"'.");
    }

    /**
     * 读取库指定的modelClass
     */
    Map<String, String> tcfg = this.tableConfigs.get(tableName);
    //this.tableConfigs.remove(tableName);
    String mcls = this.modelClass;

    /**
     * 就近原则:
     * 如果表配置中有设置model则采用表配置中的
     */
    if (tcfg.containsKey("model"))
    {
      mcls = tcfg.get("model");
    }

    /**
     * 如果class为空则直接使用默认的Table
     */
    if (mcls == null || mcls.length() == 0)
    {
      Model mobj = new Model(this.getTable(tableName));
      this.modelObjects.put(tableName, mobj);
      return mobj;
    }

    CoreLogger.trace("DB: modelClass({}) for table({}.{}) has been defined, try to get it", mcls, this.name, tableName);

    /**
     * 获取指定的Table类
     */
    Class cls;
    try
    {
      cls = Class.forName(mcls);
    }
    catch (ClassNotFoundException ex)
    {
      throw new CruxException(ex, 1037);
    }

    /**
     * 获取构造器
     */
    Constructor cst;
    try
    {
      cst = cls.getConstructor(new Class[]{Table.class});
    }
    catch (NoSuchMethodException ex)
    {
      throw new CruxException(ex, 1038);
    }
    catch (SecurityException ex)
    {
      throw new CruxException(ex, 1038);
    }

    /**
     * 获取表实例
     */
    Model mobj;
    try
    {
      mobj = (Model)cst.newInstance(new Object[]{this.getTable(tableName)});
    }
    catch (InstantiationException ex)
    {
      throw new CruxException(ex, 1039);
    }
    catch (IllegalAccessException ex)
    {
      throw new CruxException(ex, 1039);
    }
    catch (IllegalArgumentException ex)
    {
      throw new CruxException(ex, 1039);
    }
    catch (InvocationTargetException ex)
    {
      throw new CruxException(ex, 1039);
    }

    this.modelObjects.put(tableName, mobj);
    return mobj;
  }

  //** 构造工厂 **/

  /**
   * 获取指定数据库对象
   * <b>注意:</b>
   * <p>
   * 数据库配置中有指定 dbClass, 请务必添加 getInstance:
   * </p>
   * <pre>
    public static XxDB getInstance()
       throws CruxException
    {
       return new XxDB();
    }
   * </pre>
   * @param name
   * @return 指定DB对象
   * @throws io.github.ihongs.CruxException
   */
  public static DB getInstance(String name)
    throws CruxException
  {
    String cn = DB.class.getName() +":"+ name;
    Core core = Core.getInstance();
    DB  db  =  (DB) core.get( cn );
    if (db != null)
    {
      return db;
    }

    DBConfig cc = new DBConfig(name);
    if (cc.dbClass != null
    &&  cc.dbClass.length(  ) != 0 )
    {
      return (DB)Core.getInstance(cc.dbClass);
    }
    else
    {
      db = new DB(cc);
      core.set(cn,db);
      return db;
    }
  }

  /**
   * 获取默认数据库对象
   * <b>注意:</b>
   * <p>
   * 数据库配置中有指定 dbClass, 请务必添加 getInstance:
   * </p>
   * <pre>
    public static XxxDB getInstance()
      throws CruxException
    {
      return new  XxxDB();
    }
   * </pre>
   * @return 默认DB对象
   * @throws io.github.ihongs.CruxException
   */
  public static DB getInstance()
    throws CruxException
  {
    return DB.getInstance("default");
  }

}
