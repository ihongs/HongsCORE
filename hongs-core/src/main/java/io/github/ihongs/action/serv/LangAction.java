package io.github.ihongs.action.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Dawn;

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
 *   &lt;servlet-class&gt;io.github.ihongs.action.JSLangAction&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;JsLang&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/common/lang/*&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Hongs
 */
public class LangAction
  extends  ActionDriver
{

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
    Core core = ActionDriver.getActualCore(req);
    ActionHelper helper = core.got(ActionHelper.class);

    String name = req.getPathInfo();
    if (name == null || name.length() == 0) {
      helper.error(400, "Path info required");
      return;
    }
    int p = name.lastIndexOf( '.' );
    if (p < 0) {
      helper.error(400, "File type required");
      return;
    }
    String type = name.substring(1 + p);
           name = name.substring(1 , p);
    if (!"js".equals(type) && !"json".equals(type)) {
      helper.error(400, "Wrong file type: "+ type);
      return;
    }

    // 需要区分语言
    String lang = name+"_"+Core.ACTION_LANG.get( );

    Maker mk;
    try {
      mk = new Maker( name );
    }
    catch (HongsExemption e) {
      helper.error(404, e.getMessage());
      return;
    }

    /**
     * 如果指定语言的数据并没有改变
     * 则直接返回 304 Not modified
     */
    long m  =  helper.getRequest(  ).getDateHeader( "If-Modified-Since" );
    if ( m >=  mk.modified() )
    {
      helper.getResponse().setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return;
    }

    String s;
    m = mk.modified();
    s = mk.toString();

    // 标明修改时间
    helper.getResponse().setDateHeader("Last-Modified", m);

    // 输出语言信息
    if ("json".equals(type))
    {
      helper.write("application/json", s);
    }
    else
    {
      String c = req.getParameter("callback");
      if (c != null && !c.isEmpty())
      {
        if (!c.matches("^[a-zA-Z_\\$][a-zA-Z0-9_]*$"))
        {
          helper.error(400, "Illegal callback function name!");
          return;
        }
        helper.write("text/javascript", c +"("+ s +");");
      }
      else
      {
        c = "self.HsLANG=Object.assign(self.HsLANG||{}" ;
        helper.write("text/javascript", c +","+ s +");");
      }
    }
  }

  //** 辅助工具类 **/

  /**
   * 语言信息辅助构造类
   */
  private static class Maker
  {
    private final     String name;
    private final CoreLocale lang;

    public Maker(String name)
    {
      this.name = name;
      this.lang = CoreLocale.getInstance(name);

      // 未设置 core.fore.keys 不公开, 哪怕设置个空的都行
      if ( this.lang.getProperty("core.fore.keys", null) == null )
      {
        throw new HongsExemption(404, "Lang for "+name+" is non-public");
      }
    }

    public long modified()
    {
      return lang.fileModified();
    }

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder();

      /** 配置代码 **/

      sb.append("{\r\n");

      // 公共语言
      if ("default".equals(name))
      {
        sb.append("\t\"lang\":\"")
          .append(Core.ACTION_LANG.get())
          .append("\",\r\n")
          .append("\t\"zone\":\"")
          .append(Core.ACTION_ZONE.get())
          .append("\",\r\n");
      }

      // 查找扩展语言信息
      for (String nk : lang.stringPropertyNames())
      {
        if (nk.startsWith("fore."))
        {
            sb.append( make(nk.substring(5), nk) );
        }
      }

      // 查找共享语言信息
      String x  = lang.getProperty("core.fore.keys");
      if (null !=  x ) for (String k : x.split(";")) {
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
          sb.append ( make(n , k) );
      }

      sb.append("\t\"\":\"\"\r\n}");
      return sb.toString();
    }

    public String make(String nam, String key)
    {
      String  fmt;
      int p = nam.lastIndexOf (".");
      if (p > 0) {
          fmt = nam.substring (1+p);
      } else {
          fmt = "" ;
      }
      switch (fmt) {
        case "json":
        case "bool":
        case "num" :
          nam = nam.substring (0,p);
          return this.makeCode(nam, key);
        case "str" :
          nam = nam.substring (0,p);
          return this.makeConf(nam, key);
        default:
          return this.makeConf(nam, key);
      }
    }

    private String makeCode(String nam, String key)
    {
      String val = this.getValue(key, "");
      if ( val.isEmpty( ) ) val = "null" ;
      return "\t\""+ nam +"\":"  + val +  ",\r\n";
    }

    private String makeConf(String nam, String key)
    {
      String val = this.getValue(key, "");
             val = Dawn.doEscape(  val  );
      return "\t\""+ nam +"\":\""+ val +"\",\r\n";
    }

    private String getValue(String key, String def)
    {
      int pos = key.indexOf(":");
      if (pos == -1) {
          return this.lang.getProperty (key, def);
      } else {
          return CoreLocale
                .getInstance(key.substring(0,pos)/**/)
                .getProperty(key.substring(1+pos),def);
      }
    }
  }
}
