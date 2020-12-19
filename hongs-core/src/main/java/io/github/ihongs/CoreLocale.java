package io.github.ihongs;

import io.github.ihongs.util.Syno;
import java.util.List;
import java.util.Map;
import java.io.File;

/**
 * 语言资源读取工具
 *
 * <p>
 * 为与配置保持一致, 故从CoreConfig继承.<br/>
 * 放弃使用"ResourceBundle"类加载语言资源.<br/>
 * 资源文件名为"xxx_语言[_国家].properties".<br/>
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
      throw new HongsExemption(828, "Language is not specified for '" + name + "'.");
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
   * 翻译指定键对应的语句并替换参数
   * 参数名为$n($0,$1...)
   * @param key
   * @param rep
   * @return 翻译后的语句, 会替换特定标识
   */
  public String translate(String key, String... rep)
  {
    String str =  this.getProperty(key);
    if  (  str == null  )  str  =  key ;
    if  (  rep == null  )  return  str ;

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
  public String translate(String key, List<String> rep)
  {
    String str =  this.getProperty(key);
    if  (  str == null  )  str  =  key ;
    if  (  rep == null  )  return  str ;

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
  public String translate(String key, Map<String, String> rep)
  {
    String str =  this.getProperty(key);
    if  (  str == null  )  str  =  key ;
    if  (  rep == null  )  return  str ;

    /**
     * 将语句中的$xxx或${xxx}替换成指定文字
     * 如果指定的替换文字不存在, 则替换为空
     */
    return Syno.inject(str, rep);
  }

  public void setLikeinst(CoreLocale inst)
  {
    that = inst;
  }

  public CoreLocale getLikeinst()
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

    Core core = Core.getInstance();
    if (core.containsKey(ck))
    {
      return (CoreLocale)core.get(ck);
    }

    Core gore = Core.GLOBAL_CORE;
    if (gore.containsKey(ck))
    {
      return (CoreLocale)gore.get(ck);
    }

    CoreLocale inst = new CoreLocale(name, lang);
    CoreConfig conf = CoreConfig.getInstance(  );
    if (conf.getProperty("core.load.locale.once", false))
    {
      gore.put(ck, inst);
    }
    else
    {
      core.put(ck, inst);
    }

    // 默认语言实例
    inst.setLikeinst( getLikeinst(name , lang) );

    return inst;
  }

  /**
   * 获取类似语言实例
   * 在配置中用 core.language.like.xx=xx_XX 这样定义
   * @param name
   * @param lang
   * @return
   */
  public static CoreLocale getLikeinst(String name, String lang)
  {
    CoreConfig conf = CoreConfig.getInstance();
    CoreLocale that;

    that = getBackinst(name, lang, conf);
    if (null!= that)
    {
        return that;
    }

    that = getBaseinst(name, lang, conf);
    if (null!= that)
    {
        return that;
    }

    return null;
  }

  private static CoreLocale getBaseinst(String name, String lang, CoreConfig conf)
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

  private static CoreLocale getBackinst(String name, String lang, CoreConfig conf)
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

    return getBackinst(name,dlng, conf);
    }

    return null;
  }

  private static boolean hasLangFile(String name, String lang)
  {
    String path;

    path = Core.CONF_PATH+ "/" + lang +"_"+ lang;
    if ((new File(path +".prop.xml"  )).exists())
    {
      return true;
    }
    if ((new File(path +".properties")).exists())
    {
      return true;
    }

    path = Cnst.CONF_PACK+ "/" + name +"_"+ lang + ".properties";
    return CoreConfig.class.getClassLoader().getResourceAsStream(path) != null;
  }

  private static String getBaseLang(String lang)
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
   * 从HEAD串中获取支持的语言
   * @param lang
   * @return 语言标识, 如zh,zh_CN, 不存在为null
   */
  public static String getAcceptLanguage(String lang)
  {
    CoreConfig conf = CoreConfig.getInstance();
    String     sups = "," + conf.getProperty("core.language.support", "zh_CN") + ",";
    String[]   arr1 = lang.replace('-','_')
                          .split  (  ","  );
    String[]   arr2 ;

    for (int i = 0; i < arr1.length; i ++ )
    {
      arr2 = arr1[i].split(";" , 2);
      lang = getBaseLang  (arr2[0]);
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
      lang = getBaseLang  (arr2[0]);
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

}
