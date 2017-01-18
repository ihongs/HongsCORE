package app.hongs;

import java.io.Closeable;
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
 * <h3>静态属性:</h3>
 * <pre>
 * ENVIR            标识不同运行环境(0 cmd, 1 web)
 * DEBUG            标识不同调试模式(0 无 , 1 输出, 2 日志, 4 禁止跟踪 8 禁止调试; 可使用位运算例如 3 表示既输出又记录)
 * BASE_HREF        应用访问路径(WEB应用中为ContextPath)
 * BASE_PATH        应用目录路径(WEB应用中为RealPath(/))
 * CORE_PATH        应用目录路径(WEB应用中为WEB-INF目录)
 * CONF_PATH        配置文件存放目录
 * DATA_PATH        数据文件存放目录
 * SERVER_ID        服务器ID(会附在 Core.getUniqueId() 的左侧)
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
   * 不支持 get(Object), 仅支持 got(String),get(String|Class)
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
   * 不支持 clear, 可使用 close, 将会在结束时自动销毁对象
   *
   * @throws UnsupportedOperationException 总是抛出此异常
   * @deprecated
   */
  @Override
  public void clear()
  {
    throw new UnsupportedOperationException(
      "May cause an error on 'clear', use the 'close'");
  }

  /**
   * 销毁核心
   *
   * AutoCloseabe 和 Closeable 对象会执行其 close 方法
   * GlobalSingleton 和 ThreadSingleton 对象不会被移除
   */
  @Override
  public void close()
  {
    if (this.isEmpty())
    {
      return;
    }

    Iterator it;

    /**
     * 先执行 close 再执行 remove 就
     * 不会因 close 里用到了其他对象而导致失败了
     */

    it = this.entrySet().iterator( );
    while (it.hasNext())
    {
      Map.Entry  et = (Map.Entry) it.next( );
      Object object =  et.getValue();
      try
      {
        if (object instanceof AutoCloseable)
        {
          ((AutoCloseable) object).close(  );
        } else
        if (object instanceof Closeable)
        {
          ((Closeable) object).close(  );
        }
      }
      catch (Throwable ta)
      {
        ta.printStackTrace( System.err );
      }
    }

    it = this.entrySet().iterator( );
    while (it.hasNext())
    {
      Map.Entry  et = (Map.Entry) it.next( );
      Object object =  et.getValue();
      if (object instanceof GlobalSingleton
      ||  object instanceof ThreadSingleton)
      {
         continue;
      }
      it.remove();
    }
  }

  @Override
  protected void finalize()
  throws Throwable
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
        if (object instanceof GlobalSingleton)
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
        if (ta instanceof StackOverflowError)
            throw (Error) ta;

        throw new HongsError(0x27, "Can not build "+name, ta);
      }
    }
    catch (NoSuchMethodException ex2)
    {
      // 获取标准对象
      try
      {
        Object object = klass.newInstance();

        /**
         * 如果该对象被声明成全局单例,
         * 则将其放入顶层核心区
         */
        if (object instanceof GlobalSingleton)
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
        Throwable e = ex.getCause();
        if (e instanceof StackOverflowError)
            throw (Error) e;

        throw new HongsError(0x28, "Can not build "+name, ex);
      }
    }
    catch (SecurityException ex2)
    {
        throw new HongsError(0x26, "Can not build "+name, ex2);
    }
  }

  //** 静态属性及方法 **/

  /**
   * 运行环境(0 Cmd , 1 Web )
   */
  public static byte ENVIR;

  /**
   * 调试级别(0 静默, 1 输出, 2 日志, 4 禁止Trace, 8 禁止Debug)
   * 注意: 错误总是会记录日志
   */
  public static byte DEBUG;

  /**
   * WEB基础链接
   */
  public static String BASE_HREF = null;

  /**
   * WEB顶级目录
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
   * 服务器编号
   */
  public static String SERVER_ID;

  /**
   * 系统启动时间
   */
  public static long STARTS_TIME = System.currentTimeMillis();

  /**
   * 全局核心对象
   */
  public static Core GLOBAL_CORE = new Core();

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

  /**
   * 获取语言地区
   * @return
   */
  public static  Locale  getLocality()
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
   * 获取唯一ID
   *
   * 采用当前服务器ID(Core.SERVER_ID)
   *
   * @return 唯一ID
   */
  public static String getUniqueId()
  {
    return Core.getUniqueId(Core.SERVER_ID);
  }

  /**
   * 获取唯一ID
   *
   * 36进制的12位字串(不包括服务器ID),
   * 至少支持到"2059/01/01 00:00:00".
   * 取值范围: 0~9A~Z
   *
   * @param svid 服务器ID
   * @return 唯一ID
   */
  public static String getUniqueId(String svid)
  {
    long n;

    n = System.currentTimeMillis();
    String time = String.format("%8s", Long.toString(n, 36));

    n = Thread.currentThread().getId();
    String trid = String.format("%4s", Long.toString(n, 36));

    n = (long) ( Math.random() * 1679615 ); //36^4-1
    String rand = String.format("%4s", Long.toString(n, 36));

    if (time.length() > 8) time = time.substring(time.length() - 8);
    if (trid.length() > 4) trid = trid.substring(trid.length() - 4);
    if (rand.length() > 4) rand = rand.substring(rand.length() - 4);

    return new StringBuilder()
        .append(time).append(trid)
        .append(rand).append(svid)
        .toString( ).replace(' ', '0');
  }

  //** 核心接口 **/

  /**
   * 全局唯一
   * 实现此接口, 则在全局范围内仅构造一次(常驻进程, 通过Core.getInstance获取)
   */
  public static interface GlobalSingleton {}

  /**
   * 线程唯一
   * 实现此接口, 则在线程范围内仅构造一次(常驻线程, 通过Core.getInstance获取)
   */
  public static interface ThreadSingleton {}

}
