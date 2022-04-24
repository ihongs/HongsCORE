package io.github.ihongs;

import io.github.ihongs.util.Syno;
import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * 语言资源读取工具
 *
 * <p>
 * 为与配置保持一致, 故从CoreConfig继承.<br/>
 * 放弃使用"ResourceBundle"类加载语言资源.<br/>
 * 资源文件名为"xxx_语言[_地区].properties".<br/>
 * </p>
 *
 * <h3>配置选项:</h3>
 * <pre>
 * core.load.locale.once  为true则仅加载一次, 为false由Core控制
 * core.langauge.link.xx  语言链接, xx为语言, 如:link.zh=zh_CN
 * </pre>
 *
 * <h3>错误代码:</h3>
 * <pre>
 * 828 没有指定语言区域
 * </pre>
 *
 * @author Hongs
 */
public class CoreLocale
  extends CoreConfig
{

  private     String lang ;

  private CoreLocale that = null;

  /**
   * 加载指定路径\语言和名称的配置
   * @param name
   * @param lang
   */
  public CoreLocale(String name, String lang)
  {
    super(null);

    this.lang = lang;

    if (null == lang)
    {
      throw new HongsExemption(828, "Language is not specified for '" + name + "'");
    }

    if (null != name)
    {
      this.load(name);
    }
  }

  /**
   * 加载指定名称的配置
   * @param name
   */
  public CoreLocale(String name)
  {
    this(/**/name , Core.ACTION_LANG.get());
  }

  /**
   * 加载默认配置
   */
  public CoreLocale()
  {
    this("default", Core.ACTION_LANG.get());
  }

  @Override
  public CoreLocale clone()
  {
    return (CoreLocale) super.clone();
  }

  /**
   * 加载指定语言文件
   * 注意:
   * 如果通过 getInstance 取对象且 core.load.locale.once=true (默认),
   * 务必要先 clone 然后再去 load,
   * 从而避免对全局配置对象的破坏.
   * @param name
   */
  @Override
  public void load(String name)
  {
    if (that != null)
    {
      that.fill(name);
    }
     super.load(name + "_" + this.lang);
  }

  /**
   * 加载指定语言文件(会忽略文件不存在)
   * 注意:
   * 如果通过 getInstance 取对象且 core.load.locale.once=true (默认),
   * 务必要先 clone 然后再去 fill,
   * 从而避免对全局配置对象的破坏.
   * @param name
   */
  @Override
  public void fill(String name)
  {
     super.fill(name);
  }

  /**
   * 翻译指定键对应的语句
   * @param key
   * @return 翻译后的语句
   */
  @Override
  public String getProperty(String key)
  {
    String str = super.getProperty(key);
    if  (  str == null && that != null) {
           str =  that.getProperty(key);
    }
    return str ;
  }

  /**
   * translate(String, Object...) 的别名
   * @param key
   * @return
   */
  public String translate(String key)
  {
    return translate(key,(Object[]) null);
  }

  /**
   * translate(String, Object...) 的别名
   * @param key
   * @param rep
   * @return
   */
  public String translate(String key, String ... rep)
  {
    return translate(key,(Object[]) rep );
  }

  /**
   * 翻译指定键对应的语句并替换参数
   * 参数名为$n($0,$1...)
   * @param key
   * @param rep
   * @return 翻译后的语句, 会替换特定标识
   */
  public String translate(String key, Object ... rep)
  {
    String str =  this.getProperty(key);
    if  (  str == null  )  str  =  key ;
    if  (  rep == null  )  return  str ;
    if  (  rep.length==0)  return  str ;

    /**
     * 将语句中替换$n或${n}为指定的文字, n从0开始
     */
    return Syno.inject(str, rep);
  }

  /**
   * 翻译指定键对应的语句并替换参数
   * 参数名为$n($0,$1...)
   * @param key
   * @param rep
   * @return 翻译后的语言, 会替换特定标识
   */
  public String translate(String key, Collection rep)
  {
    String str =  this.getProperty(key);
    if  (  str == null  )  str  =  key ;
    if  (  rep == null  )  return  str ;
    if  (  rep.isEmpty())  return  str ;

    /**
     * 将语句中替换$n或${n}为指定的文字, n从0开始
     */
    return Syno.inject(str, rep);
  }

  /**
   * 翻译指定键对应的语句并替换参数
   * 参数名为$xxx或${xxx}($one,${two}...)
   * @param key
   * @param rep
   * @return 翻译后的语句, 会替换特定标识
   */
  public String translate(String key, Map rep)
  {
    String str =  this.getProperty(key);
    if  (  str == null  )  str  =  key ;
    if  (  rep == null  )  return  str ;
    if  (  rep.isEmpty())  return  str ;

    /**
     * 将语句中的$xxx或${xxx}替换成指定文字
     * 如果指定的替换文字不存在, 则替换为空
     */
    return Syno.inject(str, rep);
  }

  public void setLocalism(CoreLocale inst)
  {
    that = inst;
  }

  public CoreLocale getLocalism()
  {
    return that;
  }

  //** 静态属性及方法 **/

  /**
   * 获取唯一语言对象
   * 如果配置core.load.locale.once为true则仅加载一次
   * @return 唯一语言实例
   */
  public static CoreLocale getInstance()
  {
    return getInstance("default");
  }

  /**
   * 按配置名获取唯一语言对象
   * 如果配置core.load.locale.once为true则仅加载一次
   * @param name 配置名
   * @return 唯一语言实例
   */
  public static CoreLocale getInstance(String name)
  {
    return getInstance(name, Core.ACTION_LANG.get());
  }

  /**
   * 按配置名和语言名获取唯一语言对象
   * 如果配置core.load.locale.once为true则仅加载一次
   * @param name 配置名
   * @param lang
   * @return 唯一语言实例
   */
  public static CoreLocale getInstance(String name, String lang)
  {
    String ck = CoreLocale.class.getName() + ":" + name + ":" + lang;

    CoreLocale  inst;
    Core core = Core.getInstance();
    inst = (CoreLocale) core.get(ck);
    if (inst != null)
    {
      return inst;
    }
    Core gore = Core.GLOBAL_CORE;
    inst = (CoreLocale) gore.get(ck);
    if (inst != null)
    {
      return inst;
    }

    CoreConfig conf;
    inst = new CoreLocale(name, lang);
    conf = CoreConfig.getInstance(  );
    if (conf.getProperty("core.load.locale.once", false))
    {
      gore.set(ck, inst);
    }
    else
    {
      core.set(ck, inst);
    }

    // 默认语言实例
    inst.setLocalism( getLocalism(name , lang) );

    return inst;
  }

  /**
   * 获取类似语言实例
   * 在配置中用 core.language.like.xx=xx_XX 这样定义
   * @param name
   * @param lang
   * @return
   */
  public static CoreLocale getLocalism(String name, String lang)
  {
    CoreConfig conf = CoreConfig.getInstance();
    CoreLocale that;

    that = getBackInst(name, lang, conf);
    if (null!= that)
    {
        return that;
    }

    that = getBaseInst(name, lang, conf);
    if (null!= that)
    {
        return that;
    }

    return null;
  }

  private static CoreLocale getBaseInst(String name, String lang, CoreConfig conf)
  {
    String dlng;
    int    p;

    dlng = conf.getProperty("core.langauge.default" , "zh");
    if (dlng == null || dlng.equals(""))
    {
      return null;
    }
    if (!dlng.equals(lang) && hasLangFile(name, dlng))
    {
      return new CoreLocale(name, dlng);
    }

    /**/p  = dlng.indexOf('_');
    if (p != -1)
    {
    dlng = dlng.substring(0,p);

    dlng = conf.getProperty("core.langauge.like."+dlng, "");
    if (dlng == null || dlng.equals(""))
    {
      return null;
    }
    if (!dlng.equals(lang) && hasLangFile(name, dlng))
    {
      return new CoreLocale(name, dlng);
    }
    }

    return null;
  }

  private static CoreLocale getBackInst(String name, String lang, CoreConfig conf)
  {
    String dlng;
    int    p;

    dlng = conf.getProperty("core.langauge.like."+lang, "");
    if (dlng == null || dlng.equals(""))
    {
      return null;
    }
    if (!dlng.equals(lang) && hasLangFile(name, dlng))
    {
      return new CoreLocale(name, dlng);
    }

    /**/p  = dlng.indexOf('_');
    if (p != -1)
    {
    dlng = dlng.substring(0,p);

    dlng = conf.getProperty("core.langauge.like."+dlng, "");
    if (dlng == null || dlng.equals(""))
    {
      return null;
    }
    if (!dlng.equals(lang) && hasLangFile(name, dlng))
    {
      return new CoreLocale(name, dlng);
    }
    }

    /**/p  = lang.indexOf('_');
    if (p != -1)
    {
    dlng = lang.substring(0,p);

    return getBackInst(name,dlng, conf);
    }

    return null;
  }

  private static boolean hasLangFile(String name, String lang)
  {
    String path;

    path = Core.CONF_PATH+ "/" + name +"_"+ lang;
    if ((new File(path +".prop.xml"  )).exists())
    {
      return true;
    }
    if ((new File(path +".properties")).exists())
    {
      return true;
    }

    path = Cnst.CONF_PACK+ "/" + name +"_"+ lang + ".properties";
    return CoreConfig.class.getClassLoader().getResource(path) != null;
  }

  /**
   * 从HEAD串中获取支持的语言
   * @param lang
   * @return 语言标识, 如zh,zh_CN, 不存在为null
   */
  public static String getAcceptLanguage(String lang)
  {
    CoreConfig conf = CoreConfig.getInstance();
    String     sups = "," + conf.getProperty("core.language.support", Cnst.LANG_DEF) + ",";
    String[]   arr1 = lang.replace('-','_')
                          .split  (  ","  );
    String[]   arr2 ;

    for (int i = 0; i < arr1.length; i ++ )
    {
      arr2 = arr1[i].split(";" , 2);
      lang = getHigherLanguage( arr2[0] ) ;
      if (sups.contains("," + lang + ",") )
      {
        return lang;
      }

      lang = conf.getProperty("core.language.like." + lang);
      if (sups.contains("," + lang + ",") )
      {
        return lang;
      }

      /**
       * 如果语言字串中带有"_"符号, 则按"_"拆分去后面部分,
       * 检查其是否是允许的语种.
       */
      if (arr2[0].contains("_")) continue ;

      arr2 = arr2[0].split("_" , 2);
      lang = getHigherLanguage( arr2[0] ) ;
      if (sups.contains("," + lang + ",") )
      {
        return lang;
      }

      lang = conf.getProperty("core.language.like." + lang);
      if (sups.contains("," + lang + ",") )
      {
        return lang;
      }
    }

    return null;
  }

  private static String getHigherLanguage(String lang)
  {
    int p  = lang.indexOf( "_" );
    if (p != -1) {
      return lang.substring(0 , p).toLowerCase()
        +"_"+lang.substring(1 + p).toUpperCase();
    } else {
      return lang.toLowerCase( );
    }
  }

  /**
   * 持久化短语
   * 以便在输出时才进行转换
   */
  public static class Property implements CharSequence, Serializable {

      private final String   conf;
      private final String   text;
      private final String[] reps;

      public Property(String conf, String text) {
          this.conf = conf;
          this.text = text;
          this.reps = null;
      }

      public Property(String conf, String text, String... reps) {
          this.conf = conf;
          this.text = text;
          this.reps = reps;
      }

      @Override
      public String toString() {
          Core core = Core.getInstance();
          String nc = CoreLocale.class.getName()+"!"+conf;
          if (!core.exists(nc)) try {
              return  CoreLocale.getInstance(conf).translate(text, reps);
          }
          catch (HongsExemption ex) {
          if (826 == ex.getErrno()) {
              core.put( nc , null );
          } else throw  ex ;
          }
          return text;
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
                &&   p.conf.equals(conf)
                &&   Arrays.equals(reps, p.reps);
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
