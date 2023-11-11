package io.github.ihongs;

import io.github.ihongs.util.daemon.Gate;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
 * <h3>错误代码:</h3>
 * <pre>
 * 区间: 900~909
 * 900 创建临时文件失败
 * 901 写入临时文件失败
 * 902 读取临时文件失败
 * 903 找不到临时文件
 * 904 找不到对应的类
 * 905 无法读取属性
 * 906 无法写入属性
 * </pre>
 *
 * @author Hongs
 */
public abstract class CoreSerial
  implements Serializable
{

  /**
   * 从外部获取属性写入当前对象
   * @throws io.github.ihongs.HongsException
   */
  abstract protected void imports()
    throws HongsException;

  /**
   * 检查缓存有效性
   * @param file
   * @return 0 重载, 1 有效, -1 失效(删除文件)
   * @throws io.github.ihongs.HongsException
   */
  protected byte expires(File file)
    throws HongsException
  {
    return file.exists()
        ? (byte) 1
        : (byte) 0;
  }

  /**
   * 加载或引入数据
   * @param name
   * @throws io.github.ihongs.HongsException
   */
  protected final File init(String name)
    throws HongsException
  {
    File   file = new File(Core.DATA_PATH + "/serial/" + name + ".ser");
    init  (file);
    return file ;
  }

  /**
   * 加载或引入数据
   * @param file
   * @throws io.github.ihongs.HongsException
   */
  protected final void init(File file)
    throws HongsException
  {
         String path = file.getAbsolutePath();
    Gate.Leader lock = Gate.getLeader(CoreSerial.class.getName() + ":" + path);
        boolean drop ;

    lock.lockr();
    try {
      switch (expires(file)) {
        case  1 : // 缓存有效则无需再次引入
          load(file);
          return;
        case  0 : // 缓存过期则需要重新引入
          drop = false;
          break ;
        case -1 : // 缓存失效则文件是多余的
          drop = true ;
          break ;
        default :
          throw new UnsupportedOperationException("Return code for expires must be 1(valid),0(expired),-1(invalid)");
      }
    } finally {
      lock.unlockr();
    }

    lock.lockw();
    try {
      if (!drop) {
        imports  (  );
        save ( file );
      } else {
        file.delete();
      }
    } finally {
      lock.unlockw();
    }

    CoreLogger.debug("Serialized {}", path);
  }

  /**
   * 从文件加载对象
   * @param file
   * @throws io.github.ihongs.CruxException
   */
  protected final void load(File file)
    throws CruxException
  {
    try
    {
          FileInputStream fis = null;
      BufferedInputStream bis = null;
        ObjectInputStream ois = null;
      try
      {
        fis = new     FileInputStream(file);
        bis = new BufferedInputStream(fis );
        ois = new   ObjectInputStream(bis );
        load(ois.readObject());
      }
      finally
      {
        if (ois != null) ois.close();
        if (bis != null) bis.close();
        if (fis != null) fis.close();
      }
    }
    catch (ClassNotFoundException ex)
    {
      throw new CruxException(ex, 904);
    }
    catch (FileNotFoundException ex)
    {
      throw new CruxException(ex, 903);
    }
    catch (IOException ex)
    {
      throw new CruxException(ex, 902);
    }
  }

  /**
   * 将对象存入文件
   * @param file
   * @throws io.github.ihongs.CruxException
   */
  protected final void save(File file)
    throws CruxException
  {
    // 文件不存在则创建
    if (!file.exists()) {
      File dn = file.getParentFile();
      if (!dn.exists()) {
           dn.mkdirs();
      }
      try {
        file.createNewFile( );
      } catch (IOException e) {
        throw new CruxException(e, 900);
      }
    }

    try
    {
          FileOutputStream fos = null;
      BufferedOutputStream bos = null;
        ObjectOutputStream oos = null;
      try
      {
        fos = new     FileOutputStream(file);
        bos = new BufferedOutputStream(fos );
        oos = new   ObjectOutputStream(bos );
        oos.writeObject(save());
        oos.flush();
      }
      finally
      {
        if (oos != null) oos.close();
        if (bos != null) bos.close();
        if (fos != null) fos.close();
      }
    }
    catch (FileNotFoundException ex)
    {
      throw new CruxException(ex, 903);
    }
    catch (IOException ex)
    {
      throw new CruxException(ex, 901);
    }
  }

  /**
   * 从缓存获取属性写入当前对象
   * @param obj
   * @throws io.github.ihongs.CruxException
   */
  protected void load(Object obj)
    throws CruxException
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
        throw new CruxException(e , 905);
      }
      catch (IllegalArgumentException e)
      {
        throw new CruxException(e , 905);
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
        throw new CruxException(e , 905);
      }
      catch (IllegalArgumentException e)
      {
        throw new CruxException(e , 905);
      }
    }
  }

  /**
   * 从当前对象获取属性写入缓存
   * @return
   * @throws io.github.ihongs.CruxException
   */
  protected Object save()
    throws CruxException
  {
    Map     map   = new HashMap();
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
        throw new CruxException(e , 905);
      }
      catch (IllegalArgumentException e)
      {
        throw new CruxException(e , 905);
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
        throw new CruxException(e , 906);
      }
      catch (IllegalArgumentException e)
      {
        throw new CruxException(e , 906);
      }
    }

    return map;
  }

  /**
   * 缓存最后更新时间
   */
  public static interface Mtimes
  {

    /**
     * 缓存数据更新时间
     * @return
     */
    public long dataModified();

    /**
     * 缓存文件更新时间
     * @return
     */
    public long fileModified();

    /**
     * 文件是否已经更新
     * @return
     */
    public default boolean isModified() {
        return  dataModified()
            >=  fileModified();
    }

  }

}
