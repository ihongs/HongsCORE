package io.github.ihongs;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Locale;
import java.util.HashMap;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.concurrent.ThreadLocalRandom;
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
public class Core
{

  /**
   * 对象容器
   */
  private final Map<String, Object> SUPER;

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
          return new Locals();
      }
      @Override
      public void remove() {
        try {
          ( (Core) get()).reset();
        } catch (Throwable e) {
          throw  new Error(e);
        }
          super.remove( );
      }
  };

  /**
   * 服务路径标识
   * 同 SERV_PATH
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
  public static final Variable<String> SERVER_HREF
                = new Variable("!SERVER_HREF") {
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
  public static final Variable<String> CLIENT_ADDR
                = new Variable("!CLIENT_ADDR") {
      @Override
      protected String initialValue() {
        try {
          return io.github.ihongs.action.ActionDriver.getClientAddr(
                 io.github.ihongs.action.ActionHelper.getInstance( )
                                                     .getRequest ( ) );
        } catch (NullPointerException|UnsupportedOperationException e) {
          return "";
        }
      }
  };

  /**
   * 动作路径标识
   */
  public static final Variable<String> ACTION_NAME
                = new Variable("!ACTION_NAME") {
      @Override
      protected String initialValue() {
          return "";
      }
  };

  /**
   * 动作开始时间
   */
  public static final Variable< Long > ACTION_TIME
                = new Variable("!ACTION_TIME") {
      @Override
      protected  Long  initialValue() {
          return System.currentTimeMillis();
      }
  };

  /**
   * 动作时区标识
   */
  public static final Variable<String> ACTION_ZONE
                = new Variable("!ACTION_ZONE") {
      @Override
      protected String initialValue() {
          return CoreConfig.getInstance()
                           .getProperty("core.timezone.default", Cnst.ZONE_DEF);
      }
  };

  /**
   * 动作语言标识
   */
  public static final Variable<String> ACTION_LANG
                = new Variable("!ACTION_LANG") {
      @Override
      protected String initialValue() {
          return CoreConfig.getInstance()
                           .getProperty("core.language.default", Cnst.LANG_DEF);
      }
  };

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
    return getInstance().got(clas);
  }

  /**
   * 类名获取单例
   *
   * @param name
   * @return 类的对象
   */
  public static final Object getInstance(String name)
  {
    return getInstance().got(name);
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
          throw ( StackOverflowError ) ta;
        }

        // 框架常规异常, 可以直接抛出
        if (ta instanceof HongsCause )
        {
          throw ((HongsCause) ta).toExemption();
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
          throw ( StackOverflowError ) ta;
        }

        // 框架常规异常, 可以直接抛出
        if (ta instanceof HongsCause )
        {
          throw ((HongsCause) ta).toExemption();
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
    Locale   inst = (Locale)core.get(name);
    if (null != inst) {
        return  inst;
    }

    String[] lang = Core.ACTION_LANG.get().split("_",2);
    if (2 <= lang.length) {
        inst = new Locale(lang[0],lang[1]);
    } else {
        inst = new Locale(lang[0]);
    }

    core.set(name, inst);
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
    TimeZone inst = (TimeZone)core.get(name);
    if (null != inst) {
        return  inst;
    }

    inst = TimeZone.getTimeZone(Core.ACTION_ZONE.get());

    core.set(name, inst);
    return inst;
  }

  //** 核心方法 **/

  protected Core ()
  {
    this(new HashMap());
  }

  protected Core (Core core)
  {
    this(core . SUPER );
  }

  protected Core (Map  sup )
  {
    SUPER = sup ;
  }

  protected Map<String, Object> sup()
  {
    return SUPER;
  }

  public Object  get(String key)
  {
    return sup().get(key);
  }

  public Object  put(String key, Object obj)
  {
    return sup().put(key, obj);
  }

  public Object  remove(String key)
  {
    return sup().remove( key );
  }

  public boolean exists(String key)
  {
    return sup().containsKey ( key);
  }

  /**
   * 获取名对应的唯一对象
   *
   * @param name 包路径.类名称
   * @return 唯一对象
   */
  public Object got(String name)
  {
    Class clas;
    try {
      clas = Class.forName(name);
    } catch (ClassNotFoundException x) {
      throw new HongsExemption(821, x);
    }

    return got(name, clas);
  }

  /**
   * 获取类对应的唯一对象
   *
   * @param <T>
   * @param clas [包.]类.class
   * @return 唯一对象
   */
  public <T>T got(Class<T> clas)
  {
    String name = clas.getName();

    return got(name, clas);
  }

  /**
   * 获取指定对象
   * 缺失则在构建后存入
   * @param <T>
   * @param cln 存储键名
   * @param cls 对应的类
   * @return
   */
  protected <T>T got(String cln, Class<T> cls)
  {
    Object val = get(cln);
    if (null  != val)
    {
      return (T) val;
    }
    if (Singleton.class.isAssignableFrom(cls))
    {
      return Core.GLOBAL_CORE.got (cln , cls);
    }
    if (Soliloquy.class.isAssignableFrom(cls))
    {
      return newInstance(cls);
    }

    T obj  = newInstance(cls);
    set(cln, obj);
    return   obj ;
  }

  /**
   * 获取指定对象
   * 用函数式构造 core.got(xxx, () -> new Yyy(zzz))
   * 可在构建时抛出异常
   * @param <T>
   * @param key 存储键名
   * @param sup 供应方法
   * @return
   * @throws HongsException
   */
  public <T>T got(String key, Provider<T> sup)
  throws HongsException, HongsExemption
  {
    Object val = get(key);
    if (null  != val)
    {
      return (T) val;
    }
    if (Singleton.class.isAssignableFrom(sup.getClass()))
    {
      return Core.GLOBAL_CORE.got (key , sup);
    }
    if (Soliloquy.class.isAssignableFrom(sup.getClass()))
    {
      return sup.get();
    }

    T obj  = sup.get();
    set(key, obj);
    return   obj ;
  }

  /**
   * 获取指定对象
   * 用函数式构造 core.get(xxx, () -> new Yyy(zzz))
   * 缺失则在构建后存入
   * @param <T>
   * @param key 存储键名
   * @param sup 供应方法
   * @return
   */
  public <T>T get(String key, Supplier<T> sup)
  {
    Object val = get(key);
    if (null  != val)
    {
      return (T) val;
    }
    if (Singleton.class.isAssignableFrom(sup.getClass()))
    {
      return Core.GLOBAL_CORE.get (key , sup);
    }
    if (Soliloquy.class.isAssignableFrom(sup.getClass()))
    {
      return sup.get();
    }

    T obj  = sup.get();
    set(key, obj);
    return   obj ;
  }

  /**
   * 设置指定对象
   * 会关闭旧对象(如果是 AutoCloseable)
   * @param key
   * @param val
   */
  public void set(String key, Object val)
  {
    Object  old = put(key, val);
    if (old != val
    &&  old != null
    &&  old instanceof AutoCloseable) {
      try
      {
        ((AutoCloseable) old).close();
      }
      catch (Throwable x )
      {
        x.printStackTrace(System.err);
      }
    }
  }

  /**
   * 清除指定对象
   * 会关闭旧对象(如果是 AutoCloseable)
   * @param key
   */
  public void unset(String key)
  {
    Object  old = remove ( key);
    if (old != null
    &&  old instanceof AutoCloseable)
    {
      try
      {
        ((AutoCloseable) old).close();
      }
      catch (Throwable x )
      {
        x.printStackTrace(System.err);
      }
    }
  }

  /**
   * 存在并且非空
   * @param key
   * @return
   */
  public boolean isset(String key)
  {
    return null != get( key );
  }

  /**
   * 重置整个环境
   * 先 close 后 clear
   */
  public void reset()
  {
    try
    {
      /* */ close();
      sup().clear();
    }
    catch (Throwable x )
    {
      x.printStackTrace(System.err);
    }
  }

  /**
   * 关闭资源
   * 规避托管自身后递归调用, Core 未标示 AutoCloseable
   */
  public void close()
  {
    if (sup().isEmpty())
    {
      return;
    }

    /**
     * 为规避 ConcurrentModificationException,
     * 只能采用遍历数组而非迭代循环的方式进行.
     * 不用迭代中的 Entry.remove 是因为实例的 close 中也可能变更 core.
     */

    Object[] a = sup().values().toArray();
    for (Object o : a)
    {
      try
      {
        if (o instanceof AutoCloseable)
        {
           ((AutoCloseable) o ).close();
        }
      }
      catch ( Throwable x )
      {
        x.printStackTrace(System.err);
      }
    }
  }

  /**
   * 释放资源
   * 规避托管自身后递归调用, Core 不标示 Unuseable
   */
  public void unuse()
  {
    if (sup().isEmpty())
    {
      return;
    }

    /**
     * 为规避 ConcurrentModificationException,
     * 只能采用遍历数组而非迭代循环的方式进行.
     * 不用迭代中的 Entry.remove 是因为实例的 cloze 中也可能变更 core.
     */

    Object[] a = sup().values().toArray();
    for (Object o : a)
    {
      try
      {
        if (o instanceof Unuseable)
        {
           ((Unuseable) o ).unuse();
        }
      }
      catch ( Throwable x )
      {
        x.printStackTrace(System.err);
      }
    }
  }

  /**
   * 维护资源
   * 规避托管自身后递归调用, Core 不标示 Unuseable
   */
  public void reuse()
  {
    if (sup().isEmpty())
    {
      return;
    }

    /**
     * 为规避 ConcurrentModificationException,
     * 只能采用遍历数组而非迭代循环的方式进行.
     * 不用迭代中的 Entry.remove 是因为实例的 cloze 中也可能变更 core.
     */

    Object[] a = sup().values().toArray();
    for (Object o : a)
    {
      try
      {
        if (o instanceof Reuseable)
        {
           ((Reuseable) o ).reuse();
        }
      }
      catch ( Throwable x )
      {
        x.printStackTrace(System.err);
      }
    }
  }

  @Override
  public String toString()
  {
    Set< String > st = new TreeSet(sup().keySet());
    StringBuilder sb = new StringBuilder();
    for( String ss : st )
    {
      sb.append(ss )
        .append(',')
        .append(' ');
    }
    if ( sb.length() > 0)
    {
      sb.setLength(sb.length() - 2);
    }
    return sb.toString( );
  }

  /**
   * 全局容器
   * 带锁的容器, 内部采用了读写锁, 并对写过程可包裹
   */
  private static final class Global extends Core
  {

    private final ReentrantReadWriteLock RWL = new ReentrantReadWriteLock();

    @Override
    protected <T>T got(String cln, Class<T> cls)
    {
      RWL.readLock( ).lock();
      try {
        Object obj = super.get(cln);
        if ( null != obj ) {
            return (T) obj;
        }
      } finally {
        RWL.readLock( ).unlock();
      }

      RWL.writeLock().lock();
      try {
        T obj = newInstance(cls);
        super.set(cln, obj);
        return obj;
      } finally {
        RWL.writeLock().unlock();
      }
    }

    @Override
    public <T>T got(String key, Provider<T> sup)
    throws HongsException, HongsExemption
    {
      RWL.readLock( ).lock();
      try {
        Object obj = super.get(key);
        if ( null != obj ) {
            return (T) obj;
        }
      } finally {
        RWL.readLock( ).unlock();
      }

      RWL.writeLock().lock();
      try {
        T obj  =  sup. get();
        super.set(key, obj );
        return obj;
      } finally {
        RWL.writeLock().unlock();
      }
    }

    @Override
    public <T>T get(String key, Supplier<T> sup)
    {
      RWL.readLock( ).lock();
      try {
        Object obj = super.get(key);
        if ( null != obj ) {
            return (T) obj;
        }
      } finally {
        RWL.readLock( ).unlock();
      }

      RWL.writeLock().lock();
      try {
        T obj  =  sup. get();
        super.set(key, obj );
        return obj;
      } finally {
        RWL.writeLock().unlock();
      }
    }

    @Override
    public Object get(String key)
    {
      RWL.readLock( ).lock();
      try {
        return super.get(key);
      } finally {
        RWL.readLock( ).unlock();
      }
    }

    @Override
    public void set(String key, Object obj)
    {
      RWL.writeLock().lock();
      try {
        super.set(key,obj);
      } finally {
        RWL.writeLock().unlock();
      }
    }

    @Override
    public void unset(String key)
    {
      RWL.writeLock().lock();
      try {
        super.unset( key );
      } finally {
        RWL.writeLock().unlock();
      }
    }

    @Override
    public void close()
    {
      RWL.writeLock().lock();
      try {
        super.close();
      } finally {
        RWL.writeLock().unlock();
      }
    }

    @Override
    public void unuse()
    {
      RWL.writeLock().lock();
      try {
        super.unuse();
      } finally {
        RWL.writeLock().unlock();
      }
    }

    @Override
    public void reuse()
    {
      RWL.writeLock().lock();
      try {
        super.reuse();
      } finally {
        RWL.writeLock().unlock();
      }
    }

  }

  /**
   * 局部容器
   * 实现 finalize 以供 Finalizer 回收
   */
  private static final class Locals extends Core
  {

    private final  long  ID  ;
    private final String NAME;

    public Locals ()
    {
      ID   = Thread.currentThread().getId  ();
      NAME = Thread.currentThread().getName();
    }

    @Override
    protected void finalize() throws Throwable
    {
      try {
        CoreLogger.trace("Core in thread {}:{} is finalized. {}", ID, NAME, this);
        this . reset  ();
      } finally {
        super.finalize();
      }
    }

  }

  //** 核心接口 **/

  /**
   * 核心变量
   * @param <T>
   */
  static public class Variable<T> implements Supplier
  {
    private  final  String k;

    public Variable(String k)
    {
      this.k = k;
    }

    @Override
    public T get()
    {
      Core c  = Core.getInstance();
      T    v  = ( T ) c.get(k);
      if ( v == null) {
           v  = initialValue();
      if ( v != null) {
           c.put(k,v);
      }}
      return v;
    }

    public void set(T v)
    {
      Core.getInstance().put(k, v);
    }

    public void remove()
    {
      Core.getInstance().remove(k);
    }

    protected T initialValue()
    {
      return null;
    }
  }

  /**
   * 构造工厂
   * @param <T>
   */
  @FunctionalInterface
  static public interface Provider<T>
  {
    public T get() throws HongsException, HongsExemption;
  }

  /**
   * 清理资源
   * 实现此接口, 并且放入全局托管, 将会间隔定时执行
   */
  static public interface Unuseable
  {
    public void unuse();
  }

  /**
   * 维护资源
   * 实现此接口, 并且放入全局托管, 将会每天定时执行
   */
  static public interface Reuseable
  {
    public void reuse();
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
