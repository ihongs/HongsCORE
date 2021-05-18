package io.github.ihongs.action.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Syno;

import java.io.IOException;
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
 *   &lt;servlet-class&gt;io.github.ihongs.action.JSConfAction&lt;/servlet-class&gt;
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
  private static final Map<String, Long  > MTIMES = new HashMap();

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
    ActionHelper helper = core.got(ActionHelper.class);

    String name = req.getPathInfo();
    if (name == null || name.length() == 0) {
      helper.indicate(400, "Path info required");
      return;
    }
    int p = name.lastIndexOf( '.' );
    if (p < 0) {
      helper.indicate(400, "File type required");
      return;
    }
    String type = name.substring(1 + p);
           name = name.substring(1 , p);
    if (!"js".equals(type) && !"json".equals(type)) {
      helper.indicate(400, "Wrong file type: " + type);
      return;
    }

    /**
     * 如果指定配置的数据并没有改变
     * 则直接返回 304 Not modified
     */
    long m  =  helper.getRequest(  ).getDateHeader( "If-Modified-Since" );
    if ( ConfAction.MTIMES.containsKey(name) && MTIMES.get( name ) <= m )
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
      catch (HongsExemption ex) {
        helper.indicate(404,ex.getMessage());
        return;
      }

      m = System.currentTimeMillis( ) / 1000L * 1000L; // HTTP 时间精确到秒

      ConfAction.CACHES.put(name , s);
      ConfAction.MTIMES.put(name , m);
    }
    else
    {
      s = ConfAction.CACHES.get(name);
      m = ConfAction.MTIMES.get(name);
    }

    // 标明修改时间
    helper.getResponse().setDateHeader("Last-Modified", m);

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
          helper.indicate(400, "Illegal callback function name!");
          return;
        }
        helper.print(c+"("+s+");", "text/javascript");
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
        .append(",\r\n")
        .append("\t\"SERVER_ID\":\"")
        .append(Core.SERVER_ID)
        .append("\",\r\n")
        .append("\t\"BASE_HREF\":\"")
        .append(Core.SERV_PATH)
        .append("\",\r\n");
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
    private final CoreConfig conf;

    public Maker(String name)
    {
      conf = CoreConfig.getInstance(name);

      // 未设置 core.fore.keys 不公开, 哪怕设置个空的都行
      if ( conf.getProperty("core.fore.keys", null) == null )
      {
        throw new HongsExemption(404, "Conf for "+name+" is non-public");
      }
    }

    public String make(String nam, String key)
    {
      /**
       * 后缀 意义
       * [无] 字符串
       * .N   数字
       * .B   布尔
       * .C   代码
       * .D   指向
       */
      String name = nam.replaceFirst( "\\.[N|B|C|D]$" , "")
                       .replaceFirst("^(fore|core)\\.", "");
      if (nam.endsWith(".D"))
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
      value = Syno.escape(value);
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
