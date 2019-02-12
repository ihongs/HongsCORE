package io.github.ihongs;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
 * THREAD_CORE 被包装成了 ThreadLocal,
 * 实例在单一线程内使用并没有什么问题,
 * 如果跨线程使用则可能有线程安全问题;
 * GLOBAL_CORE 用于存放全局对象和参数,
 * 对读写等过程有加锁, 但仍需小心对待.
 * Cleanable,Singleton 类别放入非全局.
 * </p>
 *
 * <h3>静态属性:</h3>
 * <pre>
 * ENVIR     标识不同运行环境(0 cmd, 1 web)
 * DEBUG     标识不同调试模式(0 无 , 1 输出, 2 日志, 4 禁止跟踪 8 禁止调试; 可使用位运算例如 3 表示既输出又记录)
 * BASE_HREF 应用访问路径(Web应用中为ContextPath)
 * BASE_PATH 应用目录路径(Web应用中为RealPath(/))
 * CORE_PATH 应用目录路径(Web应用中为WEB-INF目录)
 * CONF_PATH 配置目录路径(CORE_PATH/etc)
 * DATA_PATH 数据目录路径(CORE_PATH/var)
 * SERVER_ID 服务器ID (依附于 Core.newIdentity())
 * 注: 以上属性需要在 Servlet/Filter/Cmdlet 等初始化时进行设置. 为保持简单, 整个容器是开放的 , 留意勿被恶意修改.
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
abstract public class Core
     extends HashMap<String, Object>
  implements AutoCloseable
{
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
   * WEB基础域名, 注意: 不以斜杠结尾, 协议域名端口
   */
  public static String SITE_HREF = null;

  /**
   * WEB基础链接, 注意: 不以斜杠结尾, 默认为空字串
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
  public static String CONF_PATH = null;

  /**
   * 数据文件存放目录
   */
  public static String DATA_PATH = null;

  /**
   * 系统启动时间
   */
  public static final long STARTS_TIME = System.currentTimeMillis();

  /**
   * 全局核心对象
   */
  public static final Core GLOBAL_CORE = new Global();

  /**
   * 线程核心对象
   */
  public static final ThreadLocal<Core> THREAD_CORE
                = new ThreadLocal() {
      @Override
      protected Core initialValue() {
            return new Simple ();
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
  public static final InheritableThreadLocal< Long > ACTION_TIME
                = new InheritableThreadLocal();

  /**
   * 动作时区标识
   */
  public static final InheritableThreadLocal<String> ACTION_ZONE
                = new InheritableThreadLocal();

  /**
   * 动作语言标识
   */
  public static final InheritableThreadLocal<String> ACTION_LANG
                = new InheritableThreadLocal();

  /**
   * 动作路径标识
   */
  public static final InheritableThreadLocal<String> ACTION_NAME
                = new InheritableThreadLocal();

  /**
   * 客户地址标识
   */
  public static final InheritableThreadLocal<String> CLIENT_ADDR
                = new InheritableThreadLocal();

  /**
   * 获取核心对象
   * @return 核心对象
   */
  public static final Core getInstance()
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
  public static final <T>T getInstance(Class<T> clas)
  {
    return getInstance().get(clas);
  }

  /**
   * 类名获取单例
   *
   * @param name
   * @return 类的对象
   */
  public static final Object getInstance(String name)
  {
    return getInstance().get(name);
  }

  public static final <T>T newInstance(Class<T> clas)
  {
    try
    {
      // 获取工厂方法
      java.lang.reflect.Method method;
      method =  clas.getMethod("getInstance", new Class [] {});

      // 获取工厂对象
      try
      {
        return  ( T )   method.invoke( null , new Object[] {});
      }
      catch (IllegalAccessException ex)
      {
        throw new HongsError(0x27, "Can not build "+clas.getName(), ex);
      }
      catch (IllegalArgumentException ex)
      {
        throw new HongsError(0x27, "Can not build "+clas.getName(), ex);
      }
      catch (java.lang.reflect.InvocationTargetException  ex )
      {
        Throwable ta = ex.getCause();

        // 调用层级过多, 最好直接抛出
        if (ta instanceof StackOverflowError)
        {
            throw ( StackOverflowError ) ta ;
        }

        throw new HongsError(0x27, "Can not build "+clas.getName(), ta);
      }
    }
    catch (NoSuchMethodException ez)
    {
      // 获取标准对象
      try
      {
        return clas.newInstance();
      }
      catch (IllegalAccessException ex)
      {
        throw new HongsError(0x28, "Can not build "+clas.getName(), ex);
      }
      catch (InstantiationException ex)
      {
        Throwable ta = ex.getCause();

        // 调用层级过多, 最好直接抛出
        if (ta instanceof StackOverflowError)
        {
            throw ( StackOverflowError ) ta ;
        }

        throw new HongsError(0x28, "Can not build "+clas.getName(), ex);
      }
    }
    catch (SecurityException se)
    {
        throw new HongsError(0x26, "Can not build "+clas.getName(), se);
    }
  }

  public static final Object newInstance(String name)
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

    return newInstance(klass);
  }

  /**
   * 新建唯一标识
   *
   * 36进制的16位字串(服务器ID占两位),
   * 可以支持到"2101/01/18 02:32:27".
   * 取值范围: 0~9A~Z
   *
   * @param svid 服务器ID
   * @return 唯一标识
   */
  public static final String newIdentity(String svid)
  {
    long time = (System.currentTimeMillis ()- 1314320040000L); // 2011/08/26, 溜溜生日
    long trid = (Thread.currentThread/**/ (). getId ()%1296L); // 36^2
    int  rand =  ThreadLocalRandom.current().nextInt(1679616); // 36^4
         time =  time  %  2821109907456L;                      // 36^8

    return  String.format(
            "%8s%4s%2s%2s",
            Long.toString(time, 36),
            Long.toString(rand, 36),
            Long.toString(trid, 36),
            svid
        ).replace(' ','0')
         .toUpperCase(   );
  }

  /**
   * 新建唯一标识
   *
   * 采用当前服务器ID(Core.SERVER_ID)
   *
   * @return 唯一标识
   */
  public static final String newIdentity()
  {
    return Core.newIdentity(Core.SERVER_ID);
  }

  /**
   * 获取语言地区
   * @return
   */
  public static final Locale getLocality()
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
  public static final TimeZone getTimezone()
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

  //** 核心方法 **/

  /**
   * 获取名对应的唯一对象
   *
   * @param key [包路径.]类名
   * @return 唯一对象
   */
  abstract public Object get(String key);

  /**
   * 获取类对应的唯一对象
   *
   * @param <T> 返回类型同请求类型
   * @param cls [包.]类.class
   * @return 唯一对象
   */
  abstract public <T>T get(Class<T> cls);

  /**
   * 获取实例, 无则构建
   * @param <T>
   * @param key
   * @param fun
   * @return 会执行 fun 进行构建, 当没有对象时
   */
  abstract public <T>T get(String key, Supplier<T> fun);

  /**
   * 写入实例, 过程加锁
   * @param <T>
   * @param key
   * @param fun
   * @return 不同于 put 返回旧的, 这里返回新的
   */
  abstract public <T>T set(String key, Supplier<T> fun);

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
   * @throws UnsupportedOperationException
   * @deprecated
   */
  @Override
  public Object get(Object name)
  {
    throw new UnsupportedOperationException(
      "May cause an error on 'get(Object)', use 'got(String)' or 'get(String|Class)'");
  }

  /**
   * 可关闭清理
   */
  public void clean()
  {
    if (this.isEmpty())
    {
      return;
    }

    /**
     * 为规避 ConcurrentModificationException,
     * 只能采用遍历数组而非迭代循环的方式进行.
     */

    Object[] a = this.keySet().toArray();
    for (int i = 0; i < a.length; i ++ )
    {
      Object k = a [i];
      Object o = super.get( k );
      try
      {
        if ((o instanceof /**/Cleanable)
        && ((Cleanable) o ).clean() !=0)
        {
        if ( o instanceof AutoCloseable)
        {
           ((AutoCloseable) o ).close( );
        }
           this.remove( k );
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
        if ( o instanceof AutoCloseable)
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
  public void close()
  {
      clear();
  }

  @Override
  protected void finalize() throws Throwable
  {
    try {
      close();
    } finally {
      super.finalize();
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    for(Map.Entry<String, Object> et : entrySet())
    {
        sb.append('[');
      int ln = sb.length();
      Object ob = et.getValue();
      if (ob instanceof AutoCloseable)
      {
        sb.append('A');
      }
      if (ob instanceof Cleanable)
      {
        sb.append('C');
      }
      if (ob instanceof Singleton)
      {
        sb.append('S');
      }
      if (ob instanceof Soliloquy)
      {
        sb.append('O');
      }
      if (ln < sb.length() )
      {
        sb.append(']');
      } else {
        sb.setLength(ln - 1);
      }
      sb.append(et.getKey()).append(", ");
    }

    // 去掉尾巴上多的逗号
    int sl = sb.length();
    if (sl > 0 )
    {
      sb.setLength(sl-2);
    }

    return sb.toString();
  }

  /**
   * 简单容器
   * 非线程安全, 未对任何过程加锁, 作线程内共享对象
   */
  private static final class Simple extends Core
  {

    @Override
    public Object get(String name)
    {
      Core   core = Core.GLOBAL_CORE;
      if (this.containsKey(name))
      {
        return    this.got(name);
      }
      if (core.containsKey(name))
      {
        return    core.got(name);
      }

      Object inst = newInstance( name );
      if (inst instanceof Soliloquy)
      {
          // Do not keep it-self.
      } else
      if (inst instanceof Singleton)
      {
          core.put( name, inst );
      } else
      {
          this.put( name, inst );
      }
      return inst;
    }

    @Override
    public <T>T get(Class<T> clas)
    {
      String name = clas.getName(  );
      Core   core = Core.GLOBAL_CORE;
      if (this.containsKey(name))
      {
        return (T)this.got(name);
      }
      if (core.containsKey(name))
      {
        return (T)core.got(name);
      }

      T   inst = newInstance( clas );
      if (inst instanceof Soliloquy)
      {
          // Do not keep it-self.
      } else
      if (inst instanceof Singleton)
      {
          core.put( name, inst );
      } else
      {
          this.put( name, inst );
      }
      return inst;
    }

    @Override
    public <T>T get(String key, Supplier<T> fun)
    {
      Object abj=super.got(key);
      if (null != abj)
      return  (T) abj;

      T obj = fun.get( );
      super.put(key,obj);
      return  obj;
    }

    @Override
    public <T>T set(String key, Supplier<T> fun)
    {
      T obj = fun.get( );
      super.put(key,obj);
      return  obj;
    }

  }

  /**
   * 全局容器
   * 带锁的容器, 内部采用了读写锁, 并对写过程可包裹
   */
  private static final class Global extends Core
  {
  private final ReadWriteLock LOCK = new ReentrantReadWriteLock();

    @Override
    public Object got(String key)
    {
      LOCK.readLock( ).lock();
      try {
        return super.got(key);
      } finally {
        LOCK.readLock( ).unlock();
      }
    }

    @Override
    public Object get(String key)
    {
      LOCK.readLock( ).lock();
      try {
      if  (super.containsKey(key)) {
        return     super.got(key);
      }
      } finally {
        LOCK.readLock( ).unlock();
      }

      LOCK.writeLock().lock();
      try {
        Object obj = newInstance(key);
        super.put( key, obj );
        return obj;
      } finally {
        LOCK.writeLock().unlock();
      }
    }

    @Override
    public <T>T get(Class<T> cls)
    {
      String key = cls.getName( );

      LOCK.readLock( ).lock();
      try {
      if  (super.containsKey(key)) {
        return (T) super.got(key);
      }
      } finally {
        LOCK.readLock( ).unlock();
      }

      LOCK.writeLock().lock();
      try {
        T  obj = newInstance(cls);
        super.put( key, obj );
        return obj;
      } finally {
        LOCK.writeLock().unlock();
      }
    }

    @Override
    public <T>T get(String key, Supplier<T> fun)
    {
      LOCK.readLock( ).lock();
      try {
        Object obj=super.got(key);
        if (null != obj)
        return  (T) obj;
      } finally {
        LOCK.readLock( ).unlock();
      }

      LOCK.writeLock().lock();
      try {
        T obj = fun.get( );
        super.put(key,obj);
        return  obj;
      } finally {
        LOCK.writeLock().unlock();
      }
    }

    @Override
    public <T>T set(String key, Supplier<T> fun)
    {
      LOCK.writeLock().lock();
      try {
        T obj = fun.get( );
        super.put(key,obj);
        return  obj;
      } finally {
        LOCK.writeLock().unlock();
      }
    }

    @Override
    public Object put(String key, Object obj)
    {
      LOCK.writeLock().lock();
      try {
        return super.put(key,obj);
      } finally {
        LOCK.writeLock().unlock();
      }
    }

    @Override
    public void clear()
    {
      LOCK.writeLock().lock();
      try {
        super.clear();
      } finally {
        LOCK.writeLock().unlock();
      }
    }

    @Override
    public void clean()
    {
      LOCK.writeLock().lock();
      try {
        super.clean();
      } finally {
        LOCK.writeLock().unlock();
      }
    }

  }

  //** 核心接口 **/

  /**
   * 可关闭的
   * 实现此接口, 会询问是否可清理, 许可则会被删除掉
   */
  static public interface Cleanable
  {
    /**
     * 清理方法
     * @return 0 继续留用, 1 可以回收
     */
    public byte clean();
  }

  /**
   * 单例模式
   * 实现此接口, 则在全局环境唯一, 常驻且仅构造一次
   */
  static public interface Singleton {}

  /**
   * 自持模式
   * 实现此接口, 则自行维护其实例, 不会主动进行存储
   */
  static public interface Soliloquy {}

}
