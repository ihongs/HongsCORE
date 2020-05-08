package io.github.ihongs;

import io.github.ihongs.util.reflex.Block;
import io.github.ihongs.util.reflex.Block.Larder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 本地缓存工具
 *
 * <p>
 * 不是 transient,static,final 的属性可被持久化存储;
 * 特殊情况可重载 load(Object) 及 save() 自定义处理.
 * </p>
 *
 * <h3>特别注意:</h3>
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
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
   */
  public CoreSerial(String name, Date date)
    throws HongsException
  {
    this.init(name, date);
  }

  /**
   * 构造无限期的实例
   * @param name
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
   */
  abstract protected void imports()
    throws HongsException;

  /**
   * 是否已经过期
   * @param time
   * @return 返回true将重建缓存
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
   */
  protected final void init(String path, String name, long time)
    throws HongsException
  {
    if (path == null)
    {
        path = Core.DATA_PATH + File.separator + "serial";
    }
    File file = new File(path + File.separator + name + ".ser");
    this.init(name, file, time + file.lastModified( ));
  }

  /**
   * 初始化方法(有效到期时间方式)
   * @param path
   * @param name
   * @param date
   * @throws io.github.ihongs.HongsException
   */
  protected final void init(String path, String name, Date date)
    throws HongsException
  {
    if (path == null)
    {
        path = Core.DATA_PATH + File.separator + "serial";
    }
    File file = new File(path + File.separator + name + ".ser");
    this.init(name, file, date!=null?date.getTime():0);
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
   * 加载或引入数据
   * @param name
   * @param file
   * @param time
   * @throws io.github.ihongs.HongsException
   */
  protected final void init(String name, File file, long time)
    throws HongsException
  {
    Larder lock = Block.getLarder(CoreSerial.class.getName() + ":" + name);

    lock.lockr();
    try {
      if (file.exists()
      && !expired(time)) {
          load(file);
          return;
      }
    } finally {
      lock.unlockr();
    }

    lock.lockw();
    try {
      imports( );
      save(file);
    } finally {
      lock.unlockw();
    }
  }

  /**
   * 从文件加载对象
   * @param file
   * @throws io.github.ihongs.HongsException
   */
  protected final void load(File file)
    throws HongsException
  {
    try
    {
        FileInputStream fis = null;
      ObjectInputStream ois = null;
      try
      {
        fis = new   FileInputStream(file);
        ois = new ObjectInputStream(fis );
        load(ois.readObject());
      }
      finally
      {
        if (ois != null) ois.close();
        if (fis != null) fis.close();
      }
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
   * @throws io.github.ihongs.HongsException
   */
  protected final void save(File file)
    throws HongsException
  {
    // 文件不存在则创建
    if (!file.exists()) {
      File dn = file.getParentFile(  );
      if (!dn.exists()) {
           dn.mkdirs();
      }
      try {
        file.createNewFile( );
      } catch (IOException e) {
        throw new HongsException(0x10d0,e);
      }
    }

    try
    {
        FileOutputStream fos = null;
      ObjectOutputStream oos = null;
      try
      {
        fos = new   FileOutputStream(file);
        oos = new ObjectOutputStream(fos );
        oos.writeObject(save());
        oos.flush();
      }
      finally
      {
        if (oos != null) oos.close();
        if (fos != null) fos.close();
      }
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

  /**
   * 从缓存获取属性写入当前对象
   * @param obj
   * @throws io.github.ihongs.HongsException
   */
  protected void load(Object obj)
    throws HongsException
  {
    Map     map = ( Map ) obj ;
    Class   clazz = getClass();
    Field[] fields;

    // 设置所有公共字段
    fields = clazz.getFields();
    for (Field field : fields)
    {
      int ms = field.getModifiers();
      if (Modifier.isTransient(ms )
      ||  Modifier.isStatic(ms )
      ||  Modifier.isFinal (ms))
      {
        continue;
      }

      String name = field.getName();

      try
      {
        field.set(this, map.get(name));
      }
      catch (IllegalAccessException e)
      {
        throw new HongsException(0x10da, e);
      }
      catch (IllegalArgumentException e)
      {
        throw new HongsException(0x10da, e);
      }
    }

    // 设置所有非公字段
    fields = clazz.getDeclaredFields();
    for (Field field : fields)
    {
      int ms = field.getModifiers();
      if (Modifier.isTransient(ms )
      ||  Modifier.isPublic(ms )
      ||  Modifier.isStatic(ms )
      ||  Modifier.isFinal (ms))
      {
        continue;
      }

      String name = field.getName();
      field.setAccessible  ( true );

      try
      {
        field.set(this, map.get(name));
      }
      catch (IllegalAccessException e)
      {
        throw new HongsException(0x10da, e);
      }
      catch (IllegalArgumentException e)
      {
        throw new HongsException(0x10da, e);
      }
    }
  }

  /**
   * 从当前对象获取属性写入缓存
   * @return
   * @throws io.github.ihongs.HongsException
   */
  protected Object save()
    throws HongsException
  {
    Map     map =new HashMap();
    Class   clazz = getClass();
    Field[] fields;

    // 提取所有公共字段
    fields = clazz.getFields();
    for (Field field : fields)
    {
      int ms = field.getModifiers();
      if (Modifier.isTransient(ms )
      ||  Modifier.isStatic(ms )
      ||  Modifier.isFinal (ms))
      {
        continue;
      }

      String name = field.getName();

      try
      {
        map.put(name, field.get(this));
      }
      catch (IllegalAccessException e)
      {
        throw new HongsException(0x10da, e);
      }
      catch (IllegalArgumentException e)
      {
        throw new HongsException(0x10da, e);
      }
    }

    // 提取所有非公字段
    fields = clazz.getDeclaredFields();
    for (Field field : fields)
    {
      int ms = field.getModifiers();
      if (Modifier.isTransient(ms )
      ||  Modifier.isPublic(ms )
      ||  Modifier.isStatic(ms )
      ||  Modifier.isFinal (ms))
      {
        continue;
      }

      String name = field.getName();
      field.setAccessible  ( true );

      try
      {
        map.put(name, field.get(this));
      }
      catch (IllegalAccessException e)
      {
        throw new HongsException(0x10dc, e);
      }
      catch (IllegalArgumentException e)
      {
        throw new HongsException(0x10dc, e);
      }
    }

    return map;
  }

}
