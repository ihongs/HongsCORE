package foo.hongs.db;

import foo.hongs.db.util.FetchCase;
import foo.hongs.Cnst;
import foo.hongs.Core;
import foo.hongs.CoreConfig;
import foo.hongs.CoreLogger;
import foo.hongs.HongsException;
import foo.hongs.db.link.Link;
import foo.hongs.db.link.Loop;
import foo.hongs.db.link.Origin;
import foo.hongs.db.link.Source;
import foo.hongs.util.Synt;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * 数据库基础类
 *
 * <p>
 * 当需要库对象时, 一般情况可调用其工厂方法getInstance获取;
 * 当需要扩展类时, 请从DB继承并实现一个无参getInstance方法.
 * </p>
 *
 * <h3>配置选项:</h3>
 * <pre>
 * core.load.db.[dbName].once 为true则仅加载一次
 * </pre>
 *
 * <h3>异常代码:</h3>
 * <pre>
 * 区间: 0x1020~0x105f
 *
 * 0x1021  找不到外部数据源配置
 * 0x1022  连接外部数据源失败
 * 0x1023  找不到内部数据源配置
 * 0x1024  连接内部数据源失败
 * 0x1025  找不到数据源配置
 * 0x1026  设置自动提交失败
 *
 * 0x1031  开启Connection失败
 * 0x1032  关闭Connection失败
 * 0x1033  取消Statement失败
 * 0x1034  关闭Statement失败
 * 0x1035  关闭ResultSet失败
 *
 * 0x1039  找不到表配置
 * 0x103a  找不到表对应的类
 * 0x103b  无法获取表构造器
 * 0x103c  无法获取表实例
 * 0x103d  找不到模型对应的类
 * 0x103e  无法获取模型构造器
 * 0x103f  无法获取模型实例
 *
 * 0x1041  构建语句失败
 * 0x1042  绑定参数失败
 * 0x1043  查询语句失败
 *
 * 0x104a  执行语句失败
 * 0x104b  插入的值不能为空
 * 0x104c  执行插入语句失败
 * 0x104d  更新的值不能为空
 * 0x104e  执行更新语句失败
 * 0x104f  执行删除语句失败
 *
 * 0x1051  参数的个数与语句中的插入标识数不符
 * </pre>
 *
 * @author Hongs
 */
public class DB
  extends  Link
{

  /**
   * 关联库
   */
  protected DB link = null;

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

  private   final  Map source;
  private   final  Map origin;

  public DB(  String name)
    throws HongsException
  {
    this (new DBConfig(name));
  }

  public DB(DBConfig conf)
    throws HongsException
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
    && (source == null || source.isEmpty( ))
    && (origin == null || origin.isEmpty()))
    {
        this.link  = /**/ DB.getInstance(conf.link);
    }
  }

  @Override
  public Connection open()
    throws HongsException
  {
    TOP: do
    {

    try
    {
      if (this.connection != null
      && !this.connection.isClosed())
      {
        break;
      }
    }
    catch (SQLException ex)
    {
      throw new HongsException(0x1031 , ex);
    }

    /**
     * 如上 link 描述, 直接使用关联对象连接
     */
    if (  this.link  != null)
    {
      this.connection = this.link.open();
      if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG))
      {
        CoreLogger.trace("DB: Connect to '"+name+"' link '"+this.link.name+"'");
      }
      break;
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
        throw new HongsException(0x1021, "Can not find jndi in origin");
      }
      if (origin.containsKey("name") == false)
      {
        throw new HongsException(0x1021, "Can not find name in origin");
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
        throw new HongsException(0x1022 , ex );
      }
      catch (javax.naming.NamingException ex )
      {
        ez = ex ; break ; // 没有对应的数据源, 尝试其他连接方式
      }

      if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG))
      {
        CoreLogger.trace("DB: Connect to '"+name+"' by origin mode: "+mode+" "+namc);
      }

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
        throw new   foo.hongs.HongsException(0x1023, "Can not find jdbc in source");
      }
      if (!source.containsKey("name"))
      {
        throw new   foo.hongs.HongsException(0x1023, "Can not find name in source");
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
        throw new HongsException(0x1024 , ex );
      }

      if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG))
      {
        CoreLogger.trace("DB: Connect to '"+name+"' by source mode: "+mode+" "+namc);
      }

      break TOP;
    }
    while (false);

    if (ez !=null)
    {
      throw new HongsException(0x1025, ez);
    }
    else
    {
      throw new HongsException(0x1025, "Can not find source or origin");
    }

    }
    while (false);

    return this.connection;
  }

  /**
   * 采用查询体获取全部数据
   * <p>注: 调query实现</p>
   * @param caze
   * @return 全部数据
   * @throws foo.hongs.HongsException
   */
  public Loop queryMore(FetchCase caze)
    throws HongsException
  {
    return caze.use(this).select();
  }

  /**
   * 采用查询体获取全部数据
   * <p>注: 调fetch实现</p>
   * @param caze
   * @return 全部数据
   * @throws foo.hongs.HongsException
   */
  public List fetchMore(FetchCase caze)
    throws HongsException
  {
    return caze.use(this).getAll();
  }

  /**
   * 采用查询体获取单条数据
   * <p>注: 调fetch实现</p>
   * @param caze
   * @return 单条数据
   * @throws foo.hongs.HongsException
   */
  public Map  fetchLess(FetchCase caze)
    throws HongsException
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
   * @throws HongsException
   */
  public FetchCase with (String tableName)
    throws HongsException
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
   * 通过表名获取表对象
   * 表名可以为"库名.表名"
   * @param tableName table 对象的 name
   * @return 指定表对象
   * @throws foo.hongs.HongsException
   */
  public Table getTable(String tableName)
    throws HongsException
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
      throw new foo.hongs.HongsException(0x1039, "Can not find config for table '"+this.name+"."+tableName+"'.");
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

    if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG))
    {
      CoreLogger.trace("DB: tableClass("+tcls+") for table("+this.name+"."+tableName+") has been defined, try to get it");
    }

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
      throw new foo.hongs.HongsException(0x103a, ex);
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
      throw new foo.hongs.HongsException(0x103b, ex);
    }
    catch (SecurityException ex)
    {
      throw new foo.hongs.HongsException(0x103b, ex);
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
      throw new foo.hongs.HongsException(0x103c, ex);
    }
    catch (IllegalAccessException ex)
    {
      throw new foo.hongs.HongsException(0x103c, ex);
    }
    catch (IllegalArgumentException ex)
    {
      throw new foo.hongs.HongsException(0x103c, ex);
    }
    catch (InvocationTargetException ex)
    {
      throw new foo.hongs.HongsException(0x103c, ex);
    }

    this.tableObjects.put(tableName, tobj);
    return tobj;
  }

  /**
   * 通过表名获取表模型
   * 表名可以为"库名.表名"
   * @param tableName table 对象的 name
   * @return 指定表模型
   * @throws foo.hongs.HongsException
   */
  public Model getModel(String tableName)
    throws HongsException
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
      throw new foo.hongs.HongsException(0x1039, "Can not find config for table '"+this.name+"."+tableName+"'.");
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

    if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG))
    {
      CoreLogger.trace("DB: modelClass("+mcls+") for table("+this.name+"."+tableName+") has been defined, try to get it");
    }

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
      throw new foo.hongs.HongsException(0x103d, ex);
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
      throw new foo.hongs.HongsException(0x103e, ex);
    }
    catch (SecurityException ex)
    {
      throw new foo.hongs.HongsException(0x103e, ex);
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
      throw new foo.hongs.HongsException(0x103f, ex);
    }
    catch (IllegalAccessException ex)
    {
      throw new foo.hongs.HongsException(0x103f, ex);
    }
    catch (IllegalArgumentException ex)
    {
      throw new foo.hongs.HongsException(0x103f, ex);
    }
    catch (InvocationTargetException ex)
    {
      throw new foo.hongs.HongsException(0x103f, ex);
    }

    this.modelObjects.put(tableName, mobj);
    return mobj;
  }

  //** 构造工厂 **/

  /**
   * 获取指定数据库对象
   * <b>注意:</b>
   * <p>
   * 会根据当前运行环境自动设置 OBJECT_MODE,TRNSCT_MODE;
   * 数据库配置中有指定 dbClass, 请务必添加 getInstance:
   * </p>
   * <pre>
    public static XxDB getInstance()
       throws HongsException
    {
       return new XxDB();
    }
   * </pre>
   * @param name
   * @return 指定DB对象
   * @throws foo.hongs.HongsException
   */
  public static DB getInstance(String name)
    throws HongsException
  {
    DB db;
    do
    {

    String key = DB.class.getName() + ":" + name;

    Core core = Core.THREAD_CORE.get();
    if ( core.containsKey(key))
    {
      db =  (DB) core.get(key);
      break;
    }

    Core gore = Core.GLOBAL_CORE;
    if ( gore.containsKey(key))
    {
      db =  (DB) gore.get(key);
      break;
    }

    /**
     * 如果存在dbClass描述则调用对应类来获取实例
     */

    DBConfig cf = new DBConfig (name);

    if (cf.dbClass != null && cf.dbClass.length() != 0)
    {
      db = (DB)Core.getInstance(cf.dbClass);
    }
    else
    {
      db = new DB(cf);
    }

    /**
     * 如有设置dbName的单次加载则将其放入静态映射
     */

    CoreConfig conf = Core.getInstance(CoreConfig.class);
    if (conf.getProperty("core.load.db."+name+".once", false))
    {
      gore.put(key, db);
    }
    else
    {
      core.put(key, db);
    }

    db.OBJECT_MODE = conf.getProperty("core.in.object.mode", false);
    db.TRNSCT_MODE = conf.getProperty("core.in.trnsct.mode", false);

    }
    while (false);

    db.OBJECT_MODE = Synt.declare(Core.getInstance().got(Cnst.OBJECT_MODE), db.OBJECT_MODE);
    db.TRNSCT_MODE = Synt.declare(Core.getInstance().got(Cnst.TRNSCT_MODE), db.TRNSCT_MODE);

    return db;
  }

  /**
   * 获取默认数据库对象
   * <b>注意:</b>
   * <p>
   * 如果指定数据库配置中有指定dbClass, 务必添加方法:
   * </p>
   * <pre>
  public static XxxDB getInstance()
    throws HongsException
  {
    return new  XxxDB();
  }
   * </pre>
   * @return 默认DB对象
   * @throws foo.hongs.HongsException
   */
  public static DB getInstance()
    throws HongsException
  {
    return DB.getInstance("default");
  }

}
