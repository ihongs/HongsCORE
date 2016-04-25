package app.hongs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 本地缓存工具
 *
 * <h3>特别注意:</h3>
 * <p>
 只有"当前类"的"非" static 类型的 public 的属性会被存储, 特殊情况可重载
 save(data) 和 load(data) 来实现.
 <p>
 * <pre>
 * entrySet(),keySet(),values() 返回的对象无法被序列化,
 * 请 new HashSet(x.entrySet()) 预处理后再存入当前对象.
 * 详见: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4756277
 * </pre>
 *
 * <h3>异常代码:</h3>
 * <pre>
 * 区间: 0x10d0~0x10df
 * 0x10d0 创建临时文件失败
 * 0x10d2 写入临时文件失败
 * 0x10d4 读取临时文件失败
 * 0x10d6 找不到临时文件
 * 0x10d8 找不到对应的类
 * 0x10da 无法读取属性
 * 0x10dc 无法写入属性
 * </pre>
 *
 * @author Hongs
 */
public abstract class CoreSerial
  implements Serializable
{

  /**
   * 以有效时间间隔方式构造实例
   * @param path
   * @param name
   * @param time
   * @throws app.hongs.HongsException
   */
  public CoreSerial(String path, String name, long time)
    throws HongsException
  {
    this.init(path, name, time);
  }

  /**
   * 以有效到期时间方式构造实例
   * @param path
   * @param name
   * @param date
   * @throws app.hongs.HongsException
   */
  public CoreSerial(String path, String name, Date date)
    throws HongsException
  {
    this.init(path, name, date);
  }

  /**
   * 以有效时间间隔方式构造实例
   * @param name
   * @param time
   * @throws app.hongs.HongsException
   */
  public CoreSerial(String name, long time)
    throws HongsException
  {
    this.init(name, time);
  }

  /**
   * 以有效到期时间方式构造实例
   * @param name
   * @param date
   * @throws app.hongs.HongsException
   */
  public CoreSerial(String name, Date date)
    throws HongsException
  {
    this.init(name, date);
  }

  /**
   * 构造无限期的实例
   * @param name
   * @throws app.hongs.HongsException
   */
  public CoreSerial(String name)
    throws HongsException
  {
    this.init(name);
  }

  /**
   * 空构造器(请自行执行init方法)
   */
  public CoreSerial()
  {
    // TODO: 请自行执行init方法
  }

  /**
   * 从外部获取属性写入当前对象
   * @throws app.hongs.HongsException
   */
  abstract protected void imports()
    throws HongsException;

  /**
   * 是否已经过期
   * @param time
   * @return 返回true将重建缓存
   * @throws app.hongs.HongsException
   */
  protected boolean expired(long time)
    throws HongsException
  {
    return time != 0 && time < System.currentTimeMillis();
  }

  /**
   * 初始化方法(有效时间间隔方式)
   * @param path
   * @param name
   * @param time
   * @throws app.hongs.HongsException
   */
  protected final void init(String path, String name, long time)
    throws HongsException
  {
    if (path == null)
    {
        path = Core.DATA_PATH + File.separator + "serial";
    }
    File file = new File(path + File.separator + name + ".ser");
    this.load(file, time + file.lastModified( ));
  }

  /**
   * 初始化方法(有效到期时间方式)
   * @param path
   * @param name
   * @param date
   * @throws app.hongs.HongsException
   */
  protected final void init(String path, String name, Date date)
    throws HongsException
  {
    if (path == null)
    {
        path = Core.DATA_PATH + File.separator + "serial";
    }
    File file = new File(path + File.separator + name + ".ser");
    this.load(file, date!=null?date.getTime():0);
  }

  protected final void init(String name, long time)
    throws HongsException
  {
    this.init(null, name, time);
  }

  protected final void init(String name, Date date)
    throws HongsException
  {
    this.init(null, name, date);
  }

  protected final void init(String name)
    throws HongsException
  {
    this.init(null, name, null);
  }

  /**
   * 加载方法
   * @param path
   * @param name
   * @param time
   * @throws app.hongs.HongsException
   */
  protected void load(File file, long time)
    throws HongsException
  {
      ReadWriteLock rwlock = lock(file.getAbsolutePath());
      Lock lock;

      lock = rwlock. readLock();
      lock.lock();
      try {
          if (file.exists() && !expired(time)) {
              load(file);
              return;
          }
      } finally {
          lock.unlock( );
      }

      lock = rwlock.writeLock();
      lock.lock();
      try {
          imports( );
          save(file);
      } finally {
          lock.unlock( );
      }
  }

  /**
   * 从文件加载对象
   * @param file
   * @throws app.hongs.HongsException
   */
  protected final void load(File file)
    throws HongsException
  {
    try
    {
        FileInputStream fis = new   FileInputStream(file);
      ObjectInputStream ois = new ObjectInputStream(fis );
      //fis.getChannel().lock();

      Map map = (Map)ois.readObject();
      load( map );

      ois.close();
    }
    catch (ClassNotFoundException ex)
    {
      throw new HongsException(0x10d8, ex);
    }
    catch (FileNotFoundException ex)
    {
      throw new HongsException(0x10d6, ex);
    }
    catch (IOException ex)
    {
      throw new HongsException(0x10d4, ex);
    }
  }

  /**
   * 将对象存入文件
   * @param file
   * @throws app.hongs.HongsException
   */
  protected final void save(File file)
    throws HongsException
  {
    // 文件不存在则创建
    if (!file.exists()) {
      File dn = file.getParentFile();
      if (!dn.exists()) {
           dn.mkdirs();
      }
      try {
        file.createNewFile(  );
      } catch (IOException ex) {
        throw new HongsException(0x10d0, ex);
      }
    }

    try
    {
        FileOutputStream fos = new   FileOutputStream(file);
      ObjectOutputStream oos = new ObjectOutputStream(fos );
      //fos.getChannel().lock();

      Map map = new HashMap();
      save( map );
      oos.writeObject ( map );

      oos.flush();
      oos.close();
    }
    catch (FileNotFoundException ex)
    {
      throw new HongsException(0x10d6, ex);
    }
    catch (IOException ex)
    {
      throw new HongsException(0x10d2, ex);
    }
  }

  /** 私有方法 **/

  /**
   * 从缓存获取属性写入当前对象
   * @param map
   * @throws app.hongs.HongsException
   */
  private void load(Map<String, Object> map)
    throws HongsException
  {
    Field[] fields = this.getClass().getFields();
    for (Field field : fields)
    {
      int ms = field.getModifiers();
      if (Modifier.isTransient(ms )
      ||  Modifier.isStatic(ms)
      || !Modifier.isPublic(ms))
      {
        continue;
      }

      String name = field.getName();

      try
      {
        field.set(this, map.get(name));
      }
      catch (IllegalArgumentException ex)
      {
        throw new HongsException(0x10da, ex);
      }
      catch (IllegalAccessException ex)
      {
        throw new HongsException(0x10da, ex);
      }
    }
  }

  /**
   * 从当前对象获取属性写入缓存
   * @param map
   * @throws HongsException
   */
  private void save(Map<String, Object> map)
    throws HongsException
  {
    Field[] fields = this.getClass().getFields();
    for (Field field : fields)
    {
      int ms = field.getModifiers();
      if (Modifier.isTransient(ms )
      ||  Modifier.isStatic(ms)
      || !Modifier.isPublic(ms))
      {
        continue;
      }

      String name = field.getName();

      try
      {
        map.put(name, field.get(this));
      }
      catch (IllegalArgumentException ex)
      {
        throw new HongsException(0x10da, ex);
      }
      catch (IllegalAccessException ex)
      {
        throw new HongsException(0x10da, ex);
      }
    }
  }

  private ReadWriteLock lock(String flag)
  {
      ReadWriteLock rwlock;
      Lock lock;

      lock = lockr. readLock();
      lock.lock();
      try {
          rwlock = locks.get(flag);
          if (rwlock != null) {
              return rwlock;
          }
      } finally {
          lock.unlock();
      }

      lock = lockr.writeLock();
      lock.lock();
      try {
          rwlock = new ReentrantReadWriteLock();
          locks.put(flag, rwlock);
          return rwlock;
      } finally {
          lock.unlock();
      }
  }

  private static Map<String, ReadWriteLock> locks = new HashMap(  );
  private static ReadWriteLock lockr = new ReentrantReadWriteLock();

}
