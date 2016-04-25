package app.hongs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 配置信息读取工具
 *
 * <p>
 * 采用Properties加载配置选项, 资源文件名为"xxx.properties"
 * </p>
 *
 * <h3>配置选项:</h3>
 * <pre>
 * core.load.config.once    为true则仅加载一次, 为false由Core控制
 * </pre>
 *
 * <h3>错误代码:</h3>
 * <pre>
 * 0x2a 无法找到配置文件
 * 0x2b 无法读取配置文件
 * </pre>
 *
 * @author Hongs
 */
public class CoreConfig
  extends Properties
{

  /**
   * 加载指定名称的配置
   * @param path
   * @param name
   */
  public CoreConfig(String name)
  {
    super();

    if (null != name)
    {
      this.load(name);
    }
  }

  /**
   * 加载默认配置
   */
  public CoreConfig()
  {
    this("default");
  }

  @Override
  public CoreConfig clone()
  {
    return (CoreConfig) super.clone();
  }

  /**
   * 根据配置名称加载配置
   * @param name
   */
  public void load(String name)
  {
    InputStream is;
    String      fn;

    try
    {
        fn = Core.CONF_PATH + File.separator + name +".properties";
        is = new FileInputStream(fn);
    }
    catch (FileNotFoundException ex)
    {
        fn = name.contains("/") ? name + ".properties"
           :"app/hongs/config/" + name + ".properties";
        is = this.getClass().getClassLoader().getResourceAsStream(fn);
        if ( null ==  is)
        {
            throw new app.hongs.HongsError(0x2a, "Can not find the properties file '" + name + ".properties'.");
        }
    }

    try
    {
        /**/this.load(is);
    }
    catch (IOException e)
    {
        /**/throw new app.hongs.HongsError(0x2b, "Can not read the properties file '" + name + ".properties'.");
    }
  }

  /**
   * 根据配置名称加载配置(忽略文件不存在)
   * @param name
   */
  public void loadIgnrFNF(String name)
  {
    try {
        this.load(name);
    } catch (app.hongs.HongsError e) {
        if  (  e.getCode( ) != 0x2a) {
            throw e;
        }
    }
  }

  /**
   * 获取配置属性的数字形式
   * @param key
   * @param def
   * @return 数字类型属性
   */
  public int getProperty(String key, int def)
  {
    String value = this.getProperty(key, "");
    if (value.length() != 0)
    {
      return Integer.parseInt(value);
    }
    else
    {
      return def;
    }
  }

  /**
   * 获取配置属性的数字形式
   * @param key
   * @param def
   * @return 数字类型属性
   */
  public long getProperty(String key, long def)
  {
    String value = this.getProperty(key, "");
    if (value.length() != 0)
    {
      return Long.parseLong(value);
    }
    else
    {
      return def;
    }
  }

  /**
   * 获取配置属性的数字形式
   * @param key
   * @param def
   * @return 数字类型属性
   */
  public float getProperty(String key, float def)
  {
    String value = this.getProperty(key, "");
    if (value.length() != 0)
    {
      return Float.parseFloat(value);
    }
    else
    {
      return def;
    }
  }

  /**
   * 获取配置属性的数字形式
   * @param key
   * @param def
   * @return 数字类型属性
   */
  public double getProperty(String key, double def)
  {
    String value = this.getProperty(key, "");
    if (value.length() != 0)
    {
      return Double.parseDouble(value);
    }
    else
    {
      return def;
    }
  }

  /**
   * 获取配置属性的布尔形式
   * @param key
   * @param def
   * @return 布尔类型属性
   */
  public boolean getProperty(String key, boolean def)
  {
    String value = this.getProperty(key, "");
    if (value.length() != 0)
    {
      return Boolean.parseBoolean(value);
    }
    else
    {
      return def;
    }
  }

  //** 静态属性及方法 **/

  /**
   * 获取唯一实例
   * 如果配置为core.load.config.once为true则仅加载一次
   * @return 唯一配置实例
   */
  public static CoreConfig getInstance()
  {
    return getInstance("default");
  }

  /**
   * 按配置名获取唯一实例
   * 如果配置为core.load.config.once为true则仅加载一次
   * @param name 配置名
   * @return 唯一配置实例
   */
  public static CoreConfig getInstance(String name)
  {
    String ck = CoreConfig.class.getName() + ":" + name;

    Core core = Core.getInstance();
    if (core.containsKey(ck))
    {
      return (CoreConfig)core.get(ck);
    }

    Core gore = Core.GLOBAL_CORE;
    if (gore.containsKey(ck))
    {
      return (CoreConfig)gore.get(ck);
    }

    CoreConfig conf =  new  CoreConfig(name);
    CoreConfig gonf = "default".equals(name) ? conf : getInstance();
    if (gonf.getProperty("core.load.config.once", false))
    {
      gore.put(ck, conf);
    }
    else
    {
      core.put(ck, conf);
    }

    return conf;
  }
}
