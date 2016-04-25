package app.hongs.action.serv;

import app.hongs.Core;
import app.hongs.CoreLocale;
import app.hongs.HongsError;
import app.hongs.action.ActionDriver;
import app.hongs.action.ActionHelper;
import app.hongs.util.Tool;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 语言信息输出动作
 *
 * <h3>web.xml配置:</h3>
 * <pre>
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;JsLang&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;app.hongs.action.JSLangAction&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;JsLang&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;*.js-lang&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;<br/>
 * </pre>
 *
 * @author Hongs
 */
public class LangAction
  extends  ActionDriver
{
  private static final Map<String, String> caches = new HashMap<String, String>();
  private static final Map<String, String> lastModified = new HashMap<String, String>();

  /**
   * 服务方法
   * 判断配置和消息有没有生成, 如果没有则生成; 消息按客户语言存放
   * @param req
   * @param rsp
   * @throws java.io.IOException
   * @throws javax.servlet.ServletException
   */
  @Override
  public void service(HttpServletRequest req, HttpServletResponse rsp)
    throws IOException, ServletException
  {
    Core core = ActionDriver.getWorkCore(req);
    ActionHelper helper = core.get(ActionHelper.class);

    String name = req.getPathInfo();
    if (name == null || name.length() == 0) {
      helper.error500("Path info required");
      return;
    }
    int p = name.lastIndexOf( '.' );
    if (p < 0) {
      helper.error500("File type required");
      return;
    }
    String type = name.substring(1 + p);
           name = name.substring(1 , p);
    if ( !"js".equals(type) && !"json".equals(type)) {
      helper.error500("Wrong file type: "+type);
      return;
    }

    /**
     * 如果指定语言的数据并没有改变
     * 则直接返回 304 Not modified
     */
    String m;
    m = helper.getRequest().getHeader("If-Modified-Since");
    if (m != null  &&  m.equals(LangAction.lastModified.get(name)))
    {
      helper.getResponse().setStatus(HttpServletResponse.SC_NOT_MODIFIED );
      return;
    }

    /**
     * 如果没有语言
     * 则调用工厂方法构造 JS 代码
     */
    String s;
    if (!LangAction.caches.containsKey(name))
    {
      try
      {
        s = this.makeLang(name);
      }
      catch (HongsError ex) {
        helper.error500(ex.getMessage());
        return;
      }

      SimpleDateFormat
          sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z",
                                      Locale.ENGLISH );
          sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
      m = sdf.format(new Date());

      LangAction.caches.put(name , s);
      LangAction.lastModified.put(name , m);
    }
    else
    {
      s = LangAction.caches.get(name);
      m = LangAction.lastModified.get(name);
    }

    // 标明修改时间
    helper.getResponse().setHeader("Last-Modified", m);

    // 输出语言信息
    if ("json".equals(type)) {
      helper.print(s, "application/json");
    }
    else {
      helper.print("if(!window.HsLANG)window.HsLANG={};$.extend(window.HsLANG,"+s+");", "application/javascript");
    }
  }

  /**
   * 销毁方法
   * 清空配置信息和消息数据
   */
  @Override
  public void destroy()
  {
    super.destroy();

    // 销毁配置信息
    LangAction.caches.clear();
  }

  /**
   * 按语言构造消息信息
   * 配置类型按后缀划分为:
   * .C 代码
   * 无后缀及其他为字符串
   * @param lang
   */
  private String makeLang(String confName)
  {
    Maker         mk = new Maker(confName);
    StringBuilder sb = new StringBuilder();

    /** 配置代码 **/
    
    sb.append("{\r\n");

    // 公共语言
    if ("default".equals(confName))
    {
      sb.append("\t\"lang\":\"")
        .append(Core.ACTION_LANG.get())
        .append("\",\n")
        .append("\t\"zone\":\"")
        .append(Core.ACTION_ZONE.get())
        .append("\",\n")
        .append("\t\"error.label\":\"")
        .append(mk.lang.getProperty("core.error.label", "ERROR"))
        .append("\",\n")
        .append("\t\"error.unkwn\":\"")
        .append(mk.lang.getProperty("core.error.unkwn", "UNKWN"))
        .append("\",\n")
        .append("\t\"date.format\":\"")
        .append(mk.lang.getProperty("core.default.date.format", "yyyy/MM/dd"))
        .append("\",\n")
        .append("\t\"time.format\":\"")
        .append(mk.lang.getProperty("core.default.time.format",  "HH:mm:ss" ))
        .append("\",\n")
        .append("\t\"datetime.format\":\"")
        .append(mk.lang.getProperty("core.default.datetime.format", "yyyy/MM/dd HH:mm:ss"))
        .append("\",\n");
    }

    // 查找扩展语言信息
    Iterator it = mk.lang.keySet().iterator();
    while (it.hasNext())
    {
      sb.append(mk.make((String) it.next( )));
    }

    sb.append("\t\"\":\"\"\r\n}");
    return sb.toString();
  }

  //** 辅助工具类 **/

  /**
   * 语言信息辅助构造类
   */
  private static class Maker
  {
    private CoreLocale lang;

    public Maker(String name)
    {
      this.lang = new CoreLocale(name);
    }

    public String make(String key)
    {
      /**
       * 后缀 意义
       * [无] 字符串
       * .B   布尔
       * .N   数字
       * .C   代码
       * .L   链接
       */
      if (!key.startsWith("fore.") && !key.startsWith("$")) {
          return "";
      }
      String name = key.replaceFirst("^(fore\\.|\\$)", "")
                       .replaceFirst("\\.[B|N|C|L]$" , "");
      if (key.endsWith(".L"))
      {
        return this.makeLink(name, key);
      }
      else if (key.endsWith(".C"))
      {
        return this.makeCode(name, key);
      }
      else if (key.endsWith(".B"))
      {
        return this.makeLang(name, key, false);
      }
      else if (key.endsWith(".N"))
      {
        return this.makeLang(name, key, 0 );
      }
      else
      {
        return this.makeLang(name, key, "");
      }
    }

    private String makeLang(String name, String key, String def)
    {
      String value = this.lang.getProperty(key, def);
      value = Tool.escape(value);
      return "\t\"" + name + "\":\"" + value + "\",\r\n";
    }

    private String makeLang(String name, String key, double def)
    {
      String value = String.valueOf(this.lang.getProperty(key, def));
      return "\t\"" + name + "\":" + value + ",\r\n";
    }

    private String makeLang(String name, String key, boolean def)
    {
      String value = String.valueOf(this.lang.getProperty(key, def));
      return "\t\"" + name + "\":" + value + ",\r\n";
    }

    private String makeCode(String name, String key)
    {
      String value = this.lang.getProperty(key, "null");
      return "\t\"" + name + "\":" + value + ",\r\n";
    }

    private String makeLink(String name, String key)
    {
      String[] arr = this.lang.getProperty(key, "").split(":", 2);
      if (1 == arr.length)
      {
        name = "default";
        key  = arr[0];
      }
      else
      {
        name = arr[0];
        key  = arr[1];
      }
        this.lang.load(name);
      return this.make(key );
    }
  }
}
