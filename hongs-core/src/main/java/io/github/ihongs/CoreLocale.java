package io.github.ihongs;

import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Properties;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.Locale;
import java.util.Set;

/**
 * 语言资源读取工具
 *
 * <p>
 * 为与配置保持一致, 故从CoreConfig继承.<br/>
 * 放弃使用"ResourceBundle"类来加载语言资源.<br/>
 * 资源名为"xxxx_lang[_xx[_XX]].properties".<br/>
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
  extends    CoreConfig
  implements CoreSerial.Mtimes
{

  protected final String lang;

  protected CoreLocale(String lang)
  {
    super();

    if (null == lang)
    {
      this.lang = "lang";
    }
    else
    {
      this.lang = "lang_"+ lang ;
    }
  }

  protected CoreLocale(String lang, Properties defs)
  {
    super(defs);

    if (null == lang)
    {
      this.lang = "lang";
    }
    else
    {
      this.lang = "lang_"+ lang ;
    }
  }

  /**
   * 加载指定名称的资源
   * @param name
   * @param lang
   * @throws io.github.ihongs.CruxException
   */
  public CoreLocale(String name, String lang)
    throws CruxException
  {
    super();

    if (null == lang)
    {
      this.lang = "lang";
    }
    else
    {
      this.lang = "lang_"+ lang ;
    }

    if (null != name)
    {
      this.lead(name +"_"+ lang);
    }
  }

  /**
   * 加载指定名称的资源并绑定默认资源
   * @param name
   * @param lang
   * @param defs
   * @throws io.github.ihongs.CruxException
   */
  public CoreLocale(String name, String lang, Properties defs)
    throws CruxException
  {
    super(defs);

    if (null == lang)
    {
      this.lang = "lang";
    }
    else
    {
      this.lang = "lang_"+ lang ;
    }

    if (null != name)
    {
      this.lead(name +"_"+ lang);
    }
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
   * @throws io.github.ihongs.CruxException
   * @deprecated 多重语言请使用 getMoreInst
   */
  @Override
  public void load(String name)
    throws CruxException
  {
    lead(name + "_" + lang);
  }

  /**
   * 加载指定语言文件(会忽略文件不存在)
   * 注意:
   * 如果通过 getInstance 取对象且 core.load.locale.once=true (默认),
   * 务必要先 clone 然后再去 fill,
   * 从而避免对全局配置对象的破坏.
   * @param name
   * @return false 无加载
   * @deprecated 多重语言请使用 getMoreInst
   */
  @Override
  public boolean fill(String name)
  {
    try {
      load(name);
    } catch ( CruxException e) {
      if (826 != e.getErrno()) {
        throw e.toExemption( );
      }
      CoreLogger.debug("CoreLocale {} is not found", name);
      return false;
    }
    return true;
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

  /**
   * 获取语言标识, 如 zh_CN
   * @return
   */
  public String getLang()
  {
    return lang.length() > 5 ? lang.substring(5) : null;
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
   * @param name 配置名称
   * @return 唯一语言实例
   */
  public static CoreLocale getInstance(String name)
  {
    return getInstance(name, Core.ACTION_LANG.get());
  }

  /**
   * 按配置名和语言名获取唯一语言对象
   * 如果配置core.load.locale.once为true则仅加载一次
   * @param name 配置名称
   * @param lang 语言代码
   * @return 唯一语言实例
   */
  public static CoreLocale getInstance(String name, String lang)
  {
    String ck = CoreLocale.class.getName() + ":" + name + "_" + lang;

    CoreLocale  inst;
    Core core = Core.getInstance();
    inst = (CoreLocale) core.get(ck);
    if (inst != null && inst.isModified() != true)
    {
      return inst;
    }
    Core gore = Core.getInterior();
    inst = (CoreLocale) gore.get(ck);
    if (inst != null && inst.isModified() != true)
    {
      return inst;
    }

    CoreLocale  ins2 = null;
    CruxException ax = null;

    // 加载后备语言资源
    try {
      ins2 = new CoreLocale(name, null, null);
    } catch (CruxException ex ) {
      if (826 != ex.getErrno()) {
        throw ex.toExemption();
      } else {
        ax =  ex;
      }
    }

    // 加载当前语言资源
    try {
      inst = new CoreLocale(name, lang, ins2);
    } catch (CruxException ex ) {
      if (826 != ex.getErrno()) {
        throw ex.toExemption();
      } else
      if ( ax != null ) {
        throw ex.toExemption();
      }
    }

    if (CoreConfig.getInstance().getProperty("core.load.locale.once", false))
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
  public static CoreLocale getMultiple(String... names)
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
        CoreLogger.debug("CoreLocale {} is not found", names.length > 0 ? names[0] : "default");
        return new Multiple(Core.ACTION_LANG.get( ));
    }

    List<Properties> p = new ArrayList(names.length);
    for (String name : names) {
        try {
            p.add(getInstance(name));
        } catch (CruxExemption  e  ) {
            if (e.getErrno() != 826) {
                throw e;
            }
            CoreLogger.debug("CoreLocale {} is not found", name);
        }
    }

    return new Multiple(Core.ACTION_LANG.get(), p.toArray(new Properties[p.size()]));
  }

  /**
   * 从语言标识构建Locale对象
   * @param lang
   * @return
   */
  public static Locale getLocaleFromLang(String lang)
  {
    String   dist;
    int p  = lang. indexOf ('_');
    if (p != -1) {
      dist = lang.substring(1+p);
      lang = lang.substring(0,p);
    } else {
      dist = "";
    }
    return new Locale(lang,dist);
  }

  /**
   * 从HEAD串中获取支持的语言
   * @param lang
   * @return 语言标识, 如 zh,zh_CN, 不存在为 null
   */
  public static String getAcceptLanguage(String lang)
  {
    CoreConfig  conf = CoreConfig.getInstance();
    Set<String> sups = Synt.toTerms(conf.getProperty("core.language.support", Cnst.LANG_DEF));

    /**
     * 由原来的 split 改为字符查找, 更为简单快速
     */

    String  ln, dn;
    int p0, p1, p2;

    do {
        // 按逗号拆出节段
        ln = lang;
        p0 = lang.indexOf(',');
        if (p0 != -1) {
          lang  = ln.substring(1+ p0);
            ln  = ln.substring(0, p0);
        }

        // 按分号拆除权重
        p1   = ln.indexOf(';');
        if (p1 != -1) {
        //  dn  = ln.substring(1+ p1);
            ln  = ln.substring(0, p1);
        }

        // 按横杠拆分方言
        p2   = ln.indexOf('-');
        if (p2 == -1) {
        // 兼容下划线格式
        p2   = ln.indexOf('_');
        }
        if (p2 != -1) {
            // 转标准格式 xx_XX
            dn  = ln.substring(1+ p2);
            ln  = ln.substring(0, p2);
            dn  = dn.toUpperCase();
            ln  = ln.toLowerCase();
            dn  = ln +"_"+ dn ;

            if (sups.contains(dn)) {
                return  dn;
            }
            dn  = conf.getProperty("core.language.like."+dn);
            if (null != dn) {
                return  dn;
            }

            if (sups.contains(ln)) {
                return  ln;
            }
            ln  = conf.getProperty("core.language.like."+ln);
            if (null != ln) {
                return  ln;
            }
        } else {
            ln  = ln.toLowerCase();

            if (sups.contains(ln)) {
                return  ln;
            }
            ln  = conf.getProperty("core.language.like."+ln);
            if (null != ln) {
                return  ln;
            }
        }
    }
    while (p0 != -1);

    return null;
  }

  /**
   * 检查时区是否有效
   * @param zone
   * @return 时区标识, 如 UT+08:00, 错误则为 null
   */
  public static String getNormalTimeZone(String zone)
  {
    try
    {
      return ZoneId.of(zone).getId();
    }
    catch (DateTimeException e)
    {
      return null;
    }
  }

  /**
   * 复合型配置
   */
  public static class Multiple extends CoreLocale {

      private  final  Properties [] props;

      public Multiple(String lang, Properties... props) {
          super(lang);
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
          if (!core.exists(nc))
          try {
              return  CoreLocale.getInstance(conf).translate(text, reps);
          } catch (CruxExemption e ) {
            if (826 == e.getErrno()) {
              core.put ( nc , null );
            } else {
              throw e;
            }
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
