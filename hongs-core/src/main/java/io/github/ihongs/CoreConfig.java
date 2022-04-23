package io.github.ihongs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
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
 * core.load.config.once  为true则仅加载一次, 为false由Core控制
 * </pre>
 *
 * <h3>错误代码:</h3>
 * <pre>
 * 826 无法找到配置文件
 * 827 无法读取配置文件
 * </pre>
 *
 * @author Hongs
 */
public class CoreConfig
  extends Properties
{

  /**
   * 加载指定名称的配置
   * @param name
   */
  public CoreConfig(String name)
  {
    super(new Properties());

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
   * 加载指定配置文件
   * 注意:
   * 如果通过 getInstance 取对象且 core.load.config.once=true (默认),
   * 务必要先 clone 然后再去 load,
   * 从而避免对全局配置对象的破坏.
   * @param name
   */
  public void load(String name)
  {
    String      fn;
    boolean     ld;
    Exception   er;
    InputStream is;

    // 资源中的作为默认配置
    fn = name.contains("/") ? name + ".properties"
       : Cnst.CONF_PACK +"/"+ name + ".properties";
    is = this.getClass().getClassLoader().getResourceAsStream(fn);
    ld = is != null;
    if ( ld )  try {
        defaults.load (is);
    }
    catch (IOException ex) {
        throw new HongsExemption(ex, 827, "Can not read '"+name+".properties'");
    }

    // 优先尝试从配置目录的 .properties 加载数据
    try {
        fn = Core.CONF_PATH + File.separator + name + ".properties";
        is = new FileInputStream(fn);
        this.load(is);
        return;
    }
    catch (FileNotFoundException ex) {
        er = ex;
    }
    catch (IOException ex) {
        throw new HongsExemption(ex, 827, "Can not read '"+name+".properties'");
    }

    // 然后尝试从配置目录的 .prop.xml 中加载数据
    try {
        fn = Core.CONF_PATH + File.separator + name + Cnst.PROP_EXT + ".xml";
        is = new FileInputStream(fn);
        this.loadFromXML(is);
        return;
    }
    catch (FileNotFoundException ex) {
        er = ex;
    }
    catch (IOException ex) {
        throw new HongsExemption(ex, 827, "Can not read '"+name+Cnst.PROP_EXT+".xml'");
    }

    // 没有额外的配置则将默认配置放到前台
    if (ld) {
        this.putAll(defaults);
    } else {
        throw new HongsExemption(er, 826, "Can not find '"+name+"' properties config");
    }
  }

  /**
   * 加载指定配置文件(会忽略文件不存在)
   * 注意:
   * 如果通过 getInstance 取对象且 core.load.config.once=true (默认),
   * 务必要先 clone 然后再去 fill,
   * 从而避免对全局配置对象的破坏.
   * @param name
   */
  public void fill(String name)
  {
    try {
        this.load(name);
    } catch (HongsExemption  e  ) {
        if  (e.getErrno() != 826) {
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

  /**
   * 获取默认配置
   * @return
   */
  public Properties getDefaults()
  {
    return defaults;
  }

  /**
   * 用默认值填充
   * @return
   */
  public CoreConfig padDefaults()
  {
    if (defaults != null) {
        for (Map.Entry et : defaults.entrySet()) {
            Object k = et.getKey( );
            if (this.containsKey(k) == false) {
                this.put( k , et.getValue() );
            }
        }
        defaults  = null;
    }
    return  this;
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

    CoreConfig  inst;
    Core core = Core.getInstance();
    inst = (CoreConfig) core.get(ck);
    if (inst != null)
    {
      return inst;
    }
    Core gore = Core.GLOBAL_CORE;
    inst = (CoreConfig) gore.get(ck);
    if (inst != null)
    {
      return inst;
    }

    CoreConfig conf;
    inst =  new  CoreConfig(name);
    conf = "default".equals(name) ? inst : getInstance();
    if (conf.getProperty("core.load.config.once", false))
    {
      gore.set(ck, inst);
    }
    else
    {
      core.set(ck, inst);
    }

    return inst;
  }

  /**
   * 持久化配置
   * 以便在输出时才进行转换
   */
  public static class Property implements CharSequence, Serializable {

      private final String   conf;
      private final String   text;
      private final String   defs;

      public Property(String conf, String text) {
          this.conf = conf;
          this.text = text;
          this.defs = null;
      }

      public Property(String conf, String text, String defs) {
          this.conf = conf;
          this.text = text;
          this.defs = defs;
      }

      @Override
      public String toString() {
          Core core = Core.getInstance();
          String nc = CoreConfig.class.getName()+"!"+conf;
          if (!core.exists(nc)) try {
              return  CoreConfig.getInstance(conf).getProperty(text, defs);
          }
          catch (HongsExemption ex) {
          if (826 == ex.getErrno()) {
              core.put( nc , null );
          } else throw  ex ;
          }
          return defs;
      }

      @Override
      public boolean equals(Object o) {
          if (o == this) {
            return true ;
          }
          if (o == null) {
            return false;
          }
          if (o.equals("")) {
            return o.equals(text);
          }
          if (o instanceof Property ) {
            Property p = ( Property ) o ;
            return   p.text.equals(text)
                &&   p.conf.equals(conf);
          }
          if (o instanceof CharSequence) {
            return o.equals(toString( ));
          }
          return false;
      }

      @Override
      public int hashCode() {
          return toString().hashCode();
      }

      @Override
      public int  length () {
          return toString().length ( );
      }

      @Override
      public char charAt (int i) {
          return toString().charAt (i);
      }

      @Override
      public CharSequence subSequence(int b, int e) {
          return toString().subSequence ( b, e );
      }

  }

}
