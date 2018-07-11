package foo.hongs.action.serv;

import foo.hongs.Core;
import foo.hongs.CoreConfig;
import foo.hongs.HongsError;
import foo.hongs.action.ActionDriver;
import foo.hongs.action.ActionHelper;
import foo.hongs.util.Tool;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 配置信息输出动作
 *
 * <h3>web.xml配置:</h3>
 * <pre>
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;JsConf&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;foo.hongs.action.JSConfAction&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;JsConf&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/common/conf/*&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Hongs
 */
public class ConfAction
  extends  ActionDriver
{
  private static final Map<String, String> CACHES = new HashMap();
  private static final Map<String, String> MTIMES = new HashMap();

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
    throws ServletException, IOException
  {
    Core core = ActionDriver.getActualCore(req);
    ActionHelper helper = core.get(ActionHelper.class);

    String name = req.getPathInfo();
    if (name == null || name.length() == 0) {
      helper.error400("Path info required");
      return;
    }
    int p = name.lastIndexOf( '.' );
    if (p < 0) {
      helper.error400("File type required");
      return;
    }
    String type = name.substring(1 + p);
           name = name.substring(1 , p);
    if ( !"js".equals(type) && !"json".equals(type)) {
      helper.error400("Wrong file type: "+type);
      return;
    }

    /**
     * 如果指定配置的数据并没有改变
     * 则直接返回 304 Not modified
     */
    String m;
    m = helper.getRequest().getHeader("If-Modified-Since");
    if (m != null && m.equals(ConfAction.MTIMES.get(name)))
    {
      helper.getResponse().setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    /**
     * 如果没有配置
     * 则调用工厂方法构造 JS 代码
     */
    String s;
    if (!ConfAction.CACHES.containsKey(name))
    {
      try {
        s = this.makeConf(name);
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

      ConfAction.CACHES.put(name , s);
      ConfAction.MTIMES.put(name , m);
    }
    else
    {
      s = ConfAction.CACHES.get(name);
      m = ConfAction.MTIMES.get(name);
    }

    // 标明修改时间
    helper.getResponse().setHeader("Last-Modified", m);

    // 输出配置信息
    if ("json".equals(type))
    {
      helper.print(s, "application/json");
    }
    else
    {
      String c = req.getParameter("callback");
      if (c != null && c.length( ) != 0 )
      {
        if (!c.matches("^[a-zA-Z_\\$][a-zA-Z0-9_]*$"))
        {
          helper.error400("Illegal callback function name!");
          return;
        }
        helper.print("function "+c+"() { return "+s+"; }", "text/javascript");
      }
      else
      {
        helper.print("if(!self.HsCONF)self.HsCONF={};Object.assign(self.HsCONF,"+s+");", "text/javascript");
      }
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
    ConfAction.CACHES.clear();
    ConfAction.MTIMES.clear();
  }

  /**
   * 构造配置信息
   * 配置类型按后缀划分为:
   * .N 数字
   * .B 布尔
   * .C 代码
   * 无后缀及其他为字符串
   */
  private String makeConf(String confName)
  {
    Maker         mk = new Maker(confName);
    StringBuilder sb = new StringBuilder();

    /** 配置代码 **/

    sb.append("{\r\n");

    // 公共配置
    if ("default".equals(confName))
    {
      sb.append("\t\"DEBUG\":")
        .append(String.valueOf(Core.DEBUG))
        .append(",\n")
        .append("\t\"SERVER_ID\":\"")
        .append(Core.SERVER_ID)
        .append("\",\n")
        .append("\t\"BASE_HREF\":\"")
        .append(Core.BASE_HREF)
        .append("\",\n");
    }

    // 查找扩展配置信息
    for (String nk : mk.conf.stringPropertyNames())
    {
      if( nk.startsWith ( "fore." ))
      {
          sb.append(mk.make(nk, nk));
      }
    }

    // 查找共享配置信息
    String x  = mk.conf.getProperty("core.fore.keys");
    if (null !=  x ) for ( String k : x.split( ";" )) {
        k = k.trim();
        if (k.length()==0) {
            continue;
        }
        String[] a = k.split("=", 2);
        String   n ;
        if (1  < a.length) {
            n  = a[0];
            k  = a[1];
        } else {
            n  = a[0];
            k  = a[0];
        }
        sb.append(mk.make(n, k) );
    }

    sb.append("\t\"\":\"\"\r\n}");
    return sb.toString();
  }

  //** 辅助工具类 **/

  /**
   * 配置信息辅助构造类
   */
  private static class Maker
  {
    private final CoreConfig conf ;

    public Maker(String name)
    {
      conf = CoreConfig.getInstance(name);
    }

    public String make(String nam, String key)
    {
      /**
       * 后缀 意义
       * [无] 字符串
       * .B   布尔
       * .N   数字
       * .C   代码
       * .L   链接
       */
      String name = nam.replaceFirst( "\\.[B|N|C|L]$" , "")
                       .replaceFirst("^(fore|core)\\.", "");
      if (nam.endsWith(".L"))
      {
        return this.makeLink(name, key);
      }
      else if (nam.endsWith(".C"))
      {
        return this.makeCode(name, key);
      }
      else if (nam.endsWith(".B"))
      {
        return this.makeConf(name, key, false);
      }
      else if (nam.endsWith(".N"))
      {
        return this.makeConf(name, key, 0 );
      }
      else
      {
        return this.makeConf(name, key, "");
      }
    }

    private String makeConf(String name, String key, String def)
    {
      String value = this.conf.getProperty(key, def);
      value = Tool.escape(value);
      return "\t\"" + name + "\":\"" + value + "\",\r\n";
    }

    private String makeConf(String name, String key, double def)
    {
      String value = String.valueOf(this.conf.getProperty(key, def));
      return "\t\"" + name + "\":" + value + ",\r\n";
    }

    private String makeConf(String name, String key, boolean def)
    {
      String value = String.valueOf(this.conf.getProperty(key, def));
      return "\t\"" + name + "\":" + value + ",\r\n";
    }

    private String makeCode(String name, String key)
    {
      String value = this.conf.getProperty(key, "null");
      return "\t\"" + name + "\":" + value + ",\n";
    }

    private String makeLink(String name, String key)
    {
      String[] arr = this.conf.getProperty(key, "" ).split(":", 2);
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
      return new Maker(name).make(key, key);
    }
  }

}
