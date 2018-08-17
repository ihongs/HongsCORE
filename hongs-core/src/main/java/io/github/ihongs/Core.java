package io.github.ihongs;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Locale;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * 核心类
 *
 * <p>
 * Servlet 在众多实现中总是以单实例多线程的方式工作,
 * 即对于单个请求有且仅有一个线程在为其服务;
 * 此 Core 类为框架的对象代理, 对象在线程内唯一存在,
 * 可在此管理的类需提供一个公共无参构造方法,
 * getInstance 公共无参静态方法可自定构造和存储规则.
 * 获取当前 Core 的唯一实例总是用 Core.getInstance()
 * </p>
 *
 * <p>
 * 注意: 这不是线程安全的, 请自行加锁.
 * THREAD_CORE 被包装成了 ThreadLocal,
 * 实例在单一线程内使用并没有什么问题,
 * 如果跨线程使用则可能有线程安全问题;
 * GLOBAL_CORE 可全局使用, 需小心对待,
 * Cleanable,Singleton 类别放入非全局.
 * </p>
 *
 * <h3>静态属性:</h3>
 * <pre>
 * ENVIR     标识不同运行环境(0 cmd, 1 web)
 * DEBUG     标识不同调试模式(0 无 , 1 输出, 2 日志, 4 禁止跟踪 8 禁止调试; 可使用位运算例如 3 表示既输出又记录)
 * BASE_HREF 应用访问路径(WEB应用中为ContextPath)
 * BASE_PATH 应用目录路径(WEB应用中为RealPath(/))
 * CORE_PATH 应用目录路径(WEB应用中为WEB-INF目录)
 * CONF_PATH 配置文件存放目录
 * DATA_PATH 数据文件存放目录
 * SERVER_ID 服务器ID (依附于 Core.getUniqueId())
 * 注: 以上属性需要在 Servlet/Filter/Cmdlet 等初始化时进行设置.
 * </pre>
 *
 * <h3>错误代码:</h3>
 * <pre>
 * 0x24 实例名称不能为空
 * 0x25 无法获取对应的类
 * 0x26 禁止访问工厂方法
 * 0x27 无法执行工厂方法
 * 0x28 执行构造方法失败
 * </pre>
 *
 * @author Hongs
 */
public final class Core
     extends HashMap<String, Object>
  implements AutoCloseable
{

  /**
   * 获取类对应的唯一对象
   *
   * @param <T> 返回类型同请求类型
   * @param klass [包.]类名.class
   * @return 唯一对象
   */
  public <T> T get(Class<T> klass)
  {
    String name = klass.getName(  );
    Core   core = Core.GLOBAL_CORE ;
    Object inst = check(core, name);
    return (T) (inst != null ? inst : build(core, name, klass));
  }

  /**
   * 获取名对应的唯一对象
   *
   * @param name [包路径.]类名
   * @return 唯一对象
   */
  public Object get(String name)
  {
    Core   core = Core.GLOBAL_CORE ;
    Object inst = check(core, name);
    return inst != null ? inst : build(core, name);
  }

  /**
   * 调用原始 get 方法
   *
   * @param name
   * @return 唯一对象, 不存在则返回空
   */
  public Object got(String name)
  {
    return super.get(name);
  }

  /**
   * 弃用 get(Object), 请用 got(String),get(String|Class)
   *
   * @param name
   * @return 抛出异常, 为避歧义禁用之
   * @throws UnsupportedOperationException 总是抛出此异常
   * @deprecated
   */
  @Override
  public Object get(Object name)
  {
    throw new UnsupportedOperationException(
      "May cause an error on 'get(Object)', use 'got(String)' or 'get(String|Class)'");
  }

  /**
   * 清理可关闭的
   */
  public void clean()
  {
    if (this.isEmpty())
    {
      return;
    }

    Iterator i = this.entrySet().iterator();
    while  ( i.hasNext( ) )
    {
      Entry  e = (Entry)i.next();
      Object o =  e . getValue();
      try
      {
        if (o instanceof Cleanable)
        {
          Cleanable c = (Cleanable) o;
        if (c.cleanable ())
        {
        if (o instanceof AutoCloseable )
        {
           ((AutoCloseable) c ).close( );
        }
            i.remove();
        }
        }
      }
      catch ( Throwable x )
      {
        x.printStackTrace ( System.err );
      }
    }
  }

  /**
   * 关闭后清空
   */
  @Override
  public void clear()
  {
    if (this.isEmpty())
    {
      return;
    }

    /**
     * 为规避 ConcurrentModificationException,
     * 只能采用遍历数组而非迭代循环的方式进行.
     */

    Object[] a = this.values().toArray();
    for (int i = 0; i < a.length; i ++ )
    {
      Object o = a [i];
      try
      {
        if (o instanceof AutoCloseable )
        {
           ((AutoCloseable) o ).close( );
        }
      }
      catch ( Throwable x )
      {
        x.printStackTrace ( System.err );
      }
    }

    super.clear();
  }

  /**
   * 关闭并清空
   */
  @Override
  public void close()
  {
    if (this.isEmpty())
    {
      return;
    }

    /**
     * 为规避 ConcurrentModificationException,
     * 只能采用遍历数组而非迭代循环的方式进行.
     */

    Object[] a = this.values().toArray();
    for (int i = 0; i < a.length; i ++ )
    {
      Object o = a [i];
      try
      {
        if (o instanceof AutoCloseable )
        {
           ((AutoCloseable) o ).close( );
        }
      }
      catch ( Throwable x )
      {
        x.printStackTrace ( System.err );
      }
    }

    super.clear();
  }

  @Override
  protected void finalize() throws Throwable
  {
    try
    {
       this.close(   );
    }
    finally
    {
      super.finalize();
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    for(Map.Entry<String, Object> et : entrySet())
    {
      Object ob = et.getValue();
      if (ob instanceof Singleton)
      {
      if (ob instanceof Cleanable)
      {
        sb.append("[Z]");
      } else
      {
        sb.append("[S]");
      }
      } else
      if (ob instanceof Cleanable)
      {
        sb.append("[D]");
      } else
      if (ob instanceof AutoCloseable)
      {
        sb.append("[C]");
      }
      sb.append(et.getKey()).append( ", ");
    }

    // 去掉尾巴上多的逗号
    int sl = sb.length();
    if (sl > 0 )
    {
      sb.delete(sb.length() - 2 , sb.length());
    }

    return sb.toString();
  }

  private Object check(Core core, String name)
  {
    if (super.containsKey(name))
    {
      return super.get(name);
    }

    if ( core.containsKey(name))
    {
      return  core.get(name);
    }

    if ( name == null || name.length() == 0)
    {
      throw new HongsError(0x24, "Instance name can not be empty.");
    }

    return null;
  }

  private Object build(Core core, String name)
  {
    Class klass;

    // 获取类
    try
    {
      klass  =  Class.forName( name );
    }
    catch (ClassNotFoundException ex)
    {
      throw new HongsError(0x25, "Can not find class by name '" + name + "'.");
    }

    return build( core, name, klass );
  }

  private Object build(Core core, String name, Class klass)
  {
    try
    {
      // 获取工厂方法
      Method method = klass.getMethod("getInstance", new Class[] {});

      // 获取工厂对象
      try
      {
        Object object = method.invoke(null, new Object[] {});

        /**
         * 如果该对象被声明成全局单例,
         * 则将其放入顶层核心区
         */
        if (object instanceof Singleton)
        {
          core.put(name, object);
        }
        else
        {
          this.put(name, object);
        }

        return object;
      }
      catch (IllegalAccessException ex)
      {
        throw new HongsError(0x27, "Can not build "+name, ex);
      }
      catch (IllegalArgumentException ex)
      {
        throw new HongsError(0x27, "Can not build "+name, ex);
      }
      catch (InvocationTargetException ex)
      {
        Throwable ta = ex.getCause();

        // 调用层级过多, 最好直接抛出
        if (ta instanceof StackOverflowError)
        {
            throw ( StackOverflowError ) ta ;
        }

        throw new HongsError(0x27, "Can not build "+name, ta);
      }
    }
    catch (NoSuchMethodException ez)
    {
      // 获取标准对象
      try
      {
        Object object = klass.newInstance();

        /**
         * 如果该对象被声明成全局单例,
         * 则将其放入顶层核心区
         */
        if (object instanceof Singleton)
        {
          core.put(name, object);
        }
        else
        {
          this.put(name, object);
        }

        return object;
      }
      catch (IllegalAccessException ex)
      {
        throw new HongsError(0x28, "Can not build "+name, ex);
      }
      catch (InstantiationException ex)
      {
        Throwable ta = ex.getCause();

        // 调用层级过多, 最好直接抛出
        if (ta instanceof StackOverflowError)
        {
            throw ( StackOverflowError ) ta ;
        }

        throw new HongsError(0x28, "Can not build "+name, ex);
      }
    }
    catch (SecurityException se)
    {
        throw new HongsError(0x26, "Can not build "+name, se);
    }
  }

  //** 静态属性及方法 **/

  /**
   * 运行环境(0 Cmd , 1 Web )
   */
  public static byte ENVIR;

  /**
   * 调试级别(0 静默, 1 输出, 2 日志, 4 禁止Trace, 8 禁止Debug)
   * 注意: 错误总是会记录日志, 故生产环境中设为 0
   */
  public static byte DEBUG;

  /**
   * 服务编号, 可用于分布式场景下防止产生重复的 ID
   */
  public static String SERVER_ID = "0" ;

  /**
   * WEB基础链接, 注意: 不以斜杠结尾, 根链接为空串
   */
  public static String BASE_HREF = null;

  /**
   * WEB顶级目录, 注意: 不以斜杠结尾, 其他同此规则
   */
  public static String BASE_PATH = null;

  /**
   * 应用文件顶级目录
   */
  public static String CORE_PATH = null;

  /**
   * 配置文件存放目录
   */
  public static String CONF_PATH;

  /**
   * 数据文件存放目录
   */
  public static String DATA_PATH;

  /**
   * 默认配置资源路径
   */
  public static final String CONF_PACK = "io/github/ihongs/config" ;

  /**
   * 系统启动时间
   */
  public static final long STARTS_TIME = System.currentTimeMillis();

  /**
   * 全局核心对象
   */
  public static final Core GLOBAL_CORE = new Core();

  /**
   * 线程核心对象
   */
  public static ThreadLocal<Core> THREAD_CORE
         =  new ThreadLocal() {
      @Override
      protected Core initialValue() {
            return new Core();
      }
      @Override
      public void remove() {
          try {
            ( (Core) get()).close();
          } catch (Throwable ex) {
            throw  new Error(ex);
          }
            super.remove();
      }
  };

  /**
   * 动作开始时间
   */
  public static InheritableThreadLocal< Long > ACTION_TIME
         =  new InheritableThreadLocal();

  /**
   * 动作时区标识
   */
  public static InheritableThreadLocal<String> ACTION_ZONE
         =  new InheritableThreadLocal();

  /**
   * 动作语言标识
   */
  public static InheritableThreadLocal<String> ACTION_LANG
         =  new InheritableThreadLocal();

  /**
   * 动作路径标识
   */
  public static InheritableThreadLocal<String> ACTION_NAME
         =  new InheritableThreadLocal();

  /**
   * 客户地址标识
   */
  public static InheritableThreadLocal<String> CLIENT_ADDR
         =  new InheritableThreadLocal();

  /**
   * 获取核心对象
   * @return 核心对象
   */
  public static Core getInstance()
  {
    return THREAD_CORE.get();
  }

  /**
   * 按类获取单例
   *
   * @param <T>
   * @param clas
   * @return 类的对象
   */
  public static <T>T getInstance(Class<T> clas)
  {
    return getInstance().get(clas);
  }

  /**
   * 类名获取单例
   *
   * @param name
   * @return 类的对象
   */
  public static Object getInstance(String name)
  {
    return getInstance().get(name);
  }

  /**
   * 新建唯一标识
   *
   * 36进制的12位字串(不包括服务器ID),
   * 至少支持到"2059/01/01 00:00:00".
   * 取值范围: 0~9A~Z
   *
   * @param svid 服务器ID
   * @return 唯一标识
   */
  public static String newIdentity(String svid)
  {
    long n;

    n = System.currentTimeMillis( );
    String time = String.format("%8s", Long.toString(n, 36));

    n = Thread.currentThread().getId();
    String trid = String.format("%4s", Long.toString(n, 36));

    n = (long) ( Math.random() * 1679615); //36^4-1
    String rand = String.format("%4s", Long.toString(n, 36));

    // 确保位数不超限量
    if (time.length() > 8) time = time.substring(time.length() - 8);
    if (trid.length() > 4) trid = trid.substring(trid.length() - 4);
    if (rand.length() > 4) rand = rand.substring(rand.length() - 4);

    return new StringBuilder()
        .append(time).append(trid)
        .append(rand).append(svid)
        .toString().toUpperCase( )
        .replace ( ' ' , '0' );
  }

  /**
   * 新建唯一标识
   *
   * 采用当前服务器ID(Core.SERVER_ID)
   *
   * @return 唯一标识
   */
  public static String newIdentity()
  {
    return Core.newIdentity(Core.SERVER_ID);
  }

  /**
   * 获取语言地区
   * @return
   */
  public static Locale getLocality()
  {
    Core     core = Core.getInstance();
    String   name = Locale.class.getName();
    Locale   inst = (Locale)core.got(name);
    if (null != inst) {
        return  inst;
    }

    String[] lang = Core.ACTION_LANG.get().split("_",2);
    if (2 <= lang.length) {
        inst = new Locale(lang[0],lang[1]);
    } else {
        inst = new Locale(lang[0]);
    }

    core.put(name, inst);
    return inst;
  }

  /**
   * 获取当前时区
   * @return
   */
  public static TimeZone getTimezone()
  {
    Core     core = Core.getInstance();
    String   name = TimeZone.class.getName();
    TimeZone inst = (TimeZone)core.got(name);
    if (null != inst) {
        return  inst;
    }

    inst = TimeZone.getTimeZone(Core.ACTION_ZONE.get());

    core.put(name, inst);
    return inst;
  }

  //** 核心接口 **/

  /**
   * 单例模式
   * 实现此接口, 则在全局环境唯一, 常驻且仅构造一次
   */
  static public interface Singleton {}

  /**
   * 可关闭的
   * 实现此接口, 会询问是否可清理, 许可则会被删除掉
   */
  static public interface Cleanable
  {
         public  boolean  cleanable ();
  }

}
