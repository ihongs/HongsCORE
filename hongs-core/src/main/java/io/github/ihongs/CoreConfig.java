package io.github.ihongs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

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
  extends    Properties
  implements CoreSerial.Mtimes
{

  protected transient File file = null;
  protected transient long time = -01L;

  protected CoreConfig()
  {
    super(0);
  }

  /**
   * 加载指定名称的配置
   * @param name
   * @throws io.github.ihongs.CruxException
   */
  public CoreConfig(String name)
    throws CruxException
  {
    super(0);

    if (null != name)
    {
      this.lead(name);
    }
  }

  @Override
  public CoreConfig clone()
  {
    return (CoreConfig) super.clone();
  }

  @Override
  public long dataModified()
  {
    return time > 0 ? time : 0;
  }

  @Override
  public long fileModified()
  {
    return time > 0 ? file.lastModified() : 0;
  }

  protected final void lead(String name)
    throws CruxException
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
        if (defaults == null)
            defaults  = new Properties ();
        defaults.load (is);
    }
    catch (IOException ex) {
        throw new CruxException(ex, 827, "Can not read '"+name+".properties'");
    }

    // 优先尝试从配置目录的 .properties 加载数据
    try {
        fn = Core.CONF_PATH + File.separator + name + ".properties";
        is = new FileInputStream(fn);
        this.load( is );
        if (time == -1) {
            file = new File(fn);
            time = file.lastModified();
        }
        return;
    }
    catch (FileNotFoundException ex) {
        er = ex;
    }
    catch (IOException ex) {
        throw new CruxException(ex, 827, "Can not read '"+name+".properties'");
    }

    // 然后尝试从配置目录的 .prop.xml 中加载数据
    try {
        fn = Core.CONF_PATH + File.separator + name + Cnst.PROP_EXT + ".xml";
        is = new FileInputStream(fn);
        this.loadFromXML(is);
        if (time == -1) {
            file = new File(fn);
            time = file.lastModified();
        }
        return;
    }
    catch (FileNotFoundException ex) {
        er = ex;
    }
    catch (IOException ex) {
        throw new CruxException(ex, 827, "Can not read '"+name+Cnst.PROP_EXT+".xml'");
    }

    if (! ld) {
        throw new CruxException(er, 826, "Can not find '"+name+"' properties config");
    }
    if (time == -1) {
        time  =  0;
    }
  }

  /**
   * 加载指定配置文件
   * 注意:
   * 如果通过 getInstance 取对象且 core.load.config.once=true (默认),
   * 务必要先 clone 然后再去 load,
   * 从而避免对全局配置对象的破坏.
   * @param name
   * @throws io.github.ihongs.CruxException
   * @deprecated 多重配置请使用 getMoreInst
   */
  public void load(String name)
    throws CruxException
  {
    lead(name);
  }

  /**
   * 加载指定配置文件(会忽略文件不存在)
   * 注意:
   * 如果通过 getInstance 取对象且 core.load.config.once=true (默认),
   * 务必要先 clone 然后再去 fill,
   * 从而避免对全局配置对象的破坏.
   * @param name
   * @return false 无加载
   * @deprecated 多重配置请使用 getMoreInst
   */
  public boolean fill(String name)
  {
    try {
      load(name);
    } catch ( CruxException e) {
      if (826 != e.getErrno()) {
        throw e.toExemption( );
      }
      return false;
    }
    return true ;
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
   * @throws CruxExemption
   */
  public static CoreConfig getInstance(String name)
  {
    String ck = CoreConfig.class.getName() + ":" + name;

    CoreConfig  inst;
    Core core = Core.getInstance();
    inst = (CoreConfig) core.get(ck);
    if (inst != null && inst.isModified() != true)
    {
      return inst;
    }
    Core gore = Core.getInterior();
    inst = (CoreConfig) gore.get(ck);
    if (inst != null && inst.isModified() != true)
    {
      return inst;
    }

    try {
      inst = new CoreConfig(name);
    } catch (CruxException ex) {
      throw ex.toExemption ( );
    }

    CoreConfig conf;
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
   * 把多个配置合并到一起
   *
   * 从左往右的优先级递减.
   * 给定参数为一个以上时,
   * 总是创建新的视图对象.
   * 配置文件缺失不报异常.
   * 可用于替代旧 load/fill 加载子集方式
   *
   * @param names
   * @return
   */
  public static CoreConfig getMultiple(String... names)
  {
    try {
        if (names.length == 0) {
            return getInstance();
        }
        if (names.length == 1) {
            return getInstance(names[0]);
        }
    } catch (CruxExemption e ) {
        if (e.getErrno() != 826) { // 826 文件不存在, 下同
            throw e;
        }
        return new Multiple( );
    }

    List<Properties> p = new ArrayList(names.length);
    for (String name : names ) {
        try {
            p.add(getInstance(name));
        } catch (CruxExemption  e  ) {
            if (e.getErrno() != 826) {
                throw e;
            }
        }
    }

    return new Multiple(p.toArray(new Properties[p.size()]));
  }

  /**
   * 复合型配置
   */
  public static class Multiple extends CoreConfig {

      private  final  Properties [] props;

      public Multiple(Properties... props) {
          super();
          this.props = props;
      }

      @Override
      public String getProperty(String key) {
          String val;
          for(Properties prop : props) {
              val = prop.getProperty(key);
              if (val != null) {
                  return val ;
              }
          }
          return  super .getProperty(key);
      }

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
          if (!core.exists(nc))
          try {
              return  CoreConfig.getInstance(conf).getProperty(text, defs);
          } catch ( CruxExemption e) {
            if (826 == e.getErrno()) {
              core.put ( nc , null );
            } else {
              throw e;
            }
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
