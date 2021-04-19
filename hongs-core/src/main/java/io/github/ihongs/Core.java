package io.github.ihongs;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
 * DEBUG     标识不同调试模式(0 off, 1 log, 2 warn/info, 4 debug/trace ; 可以多个标识相加, 错误总是需要记录)
 * SERV_PATH 应用访问路径(Web应用中为ContextPath)
 * BASE_PATH 应用目录路径(Web应用中为RealPath(/))
 * CORE_PATH 应用目录路径(Web应用中为WEB-INF目录)
 * CONF_PATH 配置目录路径(CORE_PATH/etc)
 * DATA_PATH 数据目录路径(CORE_PATH/var)
 * SERVER_ID 服务器ID (依附于 Core.newIdentity())
 * 注意: 以上属性将在 Servlet/Filter/Cmdlet 等初始化时进行设置. 为保持简单, 整个容器是开放的, 留意勿被恶意修改.
 * </pre>
 *
 * <h3>错误代码:</h3>
 * <pre>
 * 821 无法获取对应的类
 * 822 禁止访问工厂方法
 * 823 无法执行工厂方法
 * 824 执行构造方法失败
 * </pre>
 *
 * @author Hongs
 */
abstract public class Core
  implements AutoCloseable, Map<String, Object>
{
  private final Map<String, Object> SUPER = new HashMap();

  /**
   * 运行环境(0 Cmd, 1 Web)
   */
  public static byte ENVIR;

  /**
   * 调试级别(0 Off, 1 Log, 2 Warn/Info, 4 Debug/Trace)
   */
  public static byte DEBUG;

  /**
   * 服务编号, 可用于分布式场景下防止产生重复的 ID
   */
  public static String SERVER_ID = "0" ;

  /**
   * WEB服务域名, 注意: 不以斜杠结尾, 协议域名端口
   */
  public static String SERV_HREF = null;

  /**
   * WEB服务路径, 注意: 不以斜杠结尾, 默认为空字串
   */
  public static String SERV_PATH = null;

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
          return new Simple();
      }
      @Override
      public void remove() {
        try {
          ( (Core) get()).close();
        } catch (Throwable e) {
          throw  new Error(e);
        }
          super.remove();
      }
  };

  /**
   * 服务路径标识
   */
  public static final Supplier<String> SERVER_PATH
                = new Supplier() {
      @Override
      public String get() {
          return SERV_PATH;
      }
  };

  /**
   * 服务域名标识
   */
  public static final ThreadLocal<String> SERVER_HREF
                = new ThreadLocal() {
      @Override
      protected String initialValue() {
        try {
          return io.github.ihongs.action.ActionDriver.getServerHref(
                 io.github.ihongs.action.ActionHelper.getInstance( )
                                                     .getRequest ( ) );
        } catch (NullPointerException|UnsupportedOperationException e) {
          return SERV_HREF;
        }
      }
  };

  /**
   * 客户地址标识
   */
  public static final ThreadLocal<String> CLIENT_ADDR
                = new ThreadLocal() {
      @Override
      protected String initialValue() {
        try {
          return io.github.ihongs.action.ActionDriver.getClientAddr(
                 io.github.ihongs.action.ActionHelper.getInstance( )
                                                     .getRequest ( ) );
        } catch (NullPointerException|UnsupportedOperationException e) {
          return null;
        }
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
   * 获取核心对象
   * @return 核心对象
   */
  public static final Core getInstance()
  {
    return THREAD_CORE.get();
  }

  /**
   * 获取或设置全局单例
   * @param <T>
   * @param key
   * @param sup
   * @return
   */
  public static final <T>T getOrPutInGlobal(String key, Supplier<T> sup)
  {
    return ((Global) GLOBAL_CORE).get(key, sup);
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
        throw new HongsExemption(823, "Can not build "+clas.getName(), ex);
      }
      catch (IllegalArgumentException  ex)
      {
        throw new HongsExemption(823, "Can not build "+clas.getName(), ex);
      }
      catch (InvocationTargetException ex)
      {
        Throwable ta = ex.getCause();

        // 调用层级过多, 最好直接抛出
        if (ta instanceof StackOverflowError)
        {
            throw ( StackOverflowError ) ta ;
        }

        throw new HongsExemption(823, "Can not build "+clas.getName(), ta);
      }
    }
    catch (NoSuchMethodException ez)
    {
      // 获取标准对象
      try
      {
        return clas.getDeclaredConstructor().newInstance();
      }
      catch ( NoSuchMethodException ex)
      {
        throw new HongsExemption(824, "Can not build "+clas.getName(), ex);
      }
      catch (InstantiationException ex)
      {
        throw new HongsExemption(824, "Can not build "+clas.getName(), ex);
      }
      catch (IllegalAccessException ex)
      {
        throw new HongsExemption(824, "Can not build "+clas.getName(), ex);
      }
      catch (InvocationTargetException ex)
      {
        Throwable ta = ex.getCause();

        // 调用层级过多, 最好直接抛出
        if (ta instanceof StackOverflowError)
        {
            throw ( StackOverflowError ) ta ;
        }

        throw new HongsExemption(824, "Can not build "+clas.getName(), ex);
      }
    }
    catch (SecurityException se)
    {
        throw new HongsExemption(822, "Can not build "+clas.getName(), se);
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
      throw new HongsExemption(821, "Can not find class by name '" + name + "'.");
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
   * @param cls [包路径.]类名
   * @return 唯一对象
   */
  abstract public Object get(String cls);

  /**
   * 获取类对应的唯一对象
   *
   * @param <T> 返回类型同请求类型
   * @param cls [包.]类.class
   * @return 唯一对象
   */
  abstract public <T>T get(Class<T> cls);

  //** 读写方法 **/

  protected Map<String, Object> sup()
  {
    return SUPER;
  }

  public Object got(String key)
  {
    return sup().get(key);
  }

  public Object got(Object key)
  {
    return sup().get(key);
  }

  /**
   * 弃用 get(Object), 请用 got(String),get(String|Class)
   *
   * @param key
   * @return 抛出异常, 为避歧义禁用之
   * @throws UnsupportedOperationException
   * @deprecated
   */
  @Override
  public Object get(Object key)
  {
    if (key instanceof String) {
      return get((String) key);
    } else
    if (key instanceof Class ) {
      return get((Class ) key);
    }
    throw new UnsupportedOperationException(
      "Please change 'get(Object)' to 'got(Object)' or 'get(String|Class)'"
    );
  }

  @Override
  public Object put(String key, Object obj)
  {
    return sup().put(key, obj);
  }

  @Override
  public Object remove(Object key)
  {
    return sup().remove(key);
  }

  @Override
  public boolean containsKey(Object key)
  {
    return sup().containsKey(key);
  }

  @Override
  public boolean containsValue(Object obj)
  {
    return sup().containsValue(obj);
  }

  @Override
  public boolean isEmpty()
  {
    return sup().isEmpty();
  }

  @Override
  public int size()
  {
    return sup().size();
  }

  @Override
  public Set<String> keySet()
  {
    return sup().keySet();
  }

  @Override
  public Collection<Object> values()
  {
    return sup().values();
  }

  @Override
  public Set<Map.Entry<String, Object>> entrySet()
  {
    return sup().entrySet();
  }

  @Override
  public void putAll(Map<? extends String, ? extends Object> map)
  {
    sup().putAll(map);
  }

  @Override
  public void clear()
  {
    sup().clear();
  }

  /**
   * 关闭资源
   */
  @Override
  public void close()
  {
    if (sup().isEmpty())
    {
      return;
    }

    /**
     * 为规避 ConcurrentModificationException,
     * 只能采用遍历数组而非迭代循环的方式进行.
     */

    Object[] a = this.values().toArray();
    for (Object o : a)
    {
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
  }

  /**
   * 清理资源
   * 用于定时清理, 不一定会关闭
   */
  public void cloze()
  {
    if (sup().isEmpty())
    {
      return;
    }

    /**
     * 为规避 ConcurrentModificationException,
     * 只能采用遍历数组而非迭代循环的方式进行.
     */

    Object[] a = this.values().toArray();
    for (Object o : a)
    {
      try
      {
        if ( o instanceof Clozeable)
        {
           ((Clozeable) o ).cloze( );
        }
      }
      catch ( Throwable x )
      {
        x.printStackTrace ( System.err );
      }
    }
  }

  /**
   * 销毁内容
   * 先 close 后 clear
   */
  public void destroy()
  {
    this.close();
    this.clear();
  }

  @Override
  protected void finalize()
  throws Throwable
  {
    try {
      this .destroy ();
    } finally {
      super.finalize();
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    for(Map.Entry<String, Object> et : this.entrySet())
    {
        sb.append('[');
      int ln = sb.length();
      Object ob = et.getValue();
      if (ob instanceof AutoCloseable)
      {
        sb.append('A');
      }
      if (ob instanceof Clozeable)
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

  }

  /**
   * 全局容器
   * 带锁的容器, 内部采用了读写锁, 并对写过程可包裹
   */
  private static final class Global extends Core
  {
  private final ReadWriteLock LOCK = new ReentrantReadWriteLock();

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
    public Object got(Object key)
    {
      LOCK.readLock( ).lock();
      try {
        return super.got(key);
      } finally {
        LOCK.readLock( ).unlock();
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
    public Object remove(Object key)
    {
      LOCK.writeLock().lock();
      try {
        return super.remove(key );
      } finally {
        LOCK.writeLock().unlock();
      }
    }

    public void putAll(Map<? extends String, ? extends Object> map)
    {
      LOCK.writeLock().lock();
      try {
        super.putAll(  map  );
      } finally {
        LOCK.writeLock().unlock();
      }
    }

    @Override
    public void clear()
    {
      LOCK.writeLock().lock();
      try {
        super.clear( );
      } finally {
        LOCK.writeLock().unlock();
      }
    }

    @Override
    public void close()
    {
      LOCK.writeLock().lock();
      try {
        super.close( );
      } finally {
        LOCK.writeLock().unlock();
      }
    }

    @Override
    public void cloze()
    {
      LOCK.writeLock().lock();
      try {
        super.cloze( );
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
  static public interface Clozeable
  {
    /**
     * 清理方法
     */
    public void cloze();
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
