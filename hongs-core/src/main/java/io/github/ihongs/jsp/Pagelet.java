package io.github.ihongs.jsp;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsCause;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Synt;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.jsp.HttpJspPage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 抽象JSP类
 *
 * <pre>
 * 如果在JSP中需要使用框架提供的方法和标签, 需要从这继承:
 * &lt;%@page extends="io.github.ihongs.action.Pagelet"%&gt;
 * </pre>
 *
 * @author Hongs
 */
abstract public class Pagelet extends ActionDriver implements HttpJspPage
{

  @Override
  public void jspInit()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void jspDestroy()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void /**/service(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    /**
     * 错误页但要的是 JSON
     * 则不必构建页面 HTML
     */
    if (req.getDispatcherType() == DispatcherType.ERROR)
    {
        Integer ern = (Integer) req.getAttribute("javax.servlet.error.status_code");
        if (ern != null && inAjax(req)) {
            Object err = req.getAttribute("javax.servlet.error.exception");
            Object msg = req.getAttribute("javax.servlet.error.message"  );
            ActionHelper ah = ActionHelper.getInstance();

            if (err != null) {
                Throwable ex = (Throwable)err;
                Throwable ax = ex.getCause( );
                if (ex instanceof HongsCause) {
                    ah.fault((HongsCause) ax);
                } else
                if (ax instanceof HongsCause) {
                    ah.fault((HongsCause) ex);
                } else {
                    ah.fault( new HongsException(
                        ern , ( String ) msg ,
                        ax != null ? ax : ex
                    ));
                }
            } else {
                Map dat = new HashMap( );
                dat.put("ok" ,  false  );
                dat.put("ern", "Er"+ern);
                dat.put("msg", /**/ msg);
                ah .  reply  (dat);
                rsp.setStatus(ern);
            }
            return;
        }
    }

    try
    {
      this._jspService(req, rsp);
    }
    catch (ServletException ex )
    {
      Throwable ax = ex.getCause( );
      if (ax == null) { ax = ex ; }

      String er = ax.getLocalizedMessage();
      int eo  = ax instanceof HongsCause ? ( (HongsCause) ax).getState() : 0;
      if (eo >= 600 || eo < 400)
      {
          eo  = HttpServletResponse.SC_INTERNAL_SERVER_ERROR ;
      }

      req.setAttribute("javax.servlet.error.status_code", eo);
      req.setAttribute("javax.servlet.error.message"    , er);
      req.setAttribute("javax.servlet.error.exception"  , ax);
      req.setAttribute("javax.servlet.error.exception_type", ax.getClass().getName());
      rsp.sendError(eo, er);
    }
  }

  /**
   * @see escapeXML 的别名
   * @param str
   * @return
   */
  public static String escape(String str) {
      return escapeXML(str);
  }

  /**
   * 转义XML/HTML文本
   * @param str
   * @return
   */
  public static String escapeXML(String str) {
      if (str == null) return "";
      StringBuilder b = new StringBuilder( );
      char c ;
      int  i = 0;
      int  l = str.length(/**/);
      while( l >  i) {
           c = str.charAt(i ++);
          switch (c) {
            case '<': b.append("&lt;" ); break;
            case '>': b.append("&gt;" ); break;
            case '&': b.append("&amp;"); break;
            case 34 : b.append("&#34;"); break; // 双引号
            case 39 : b.append("&#39;"); break; // 单引号
            default :
                if (c < 32) {
                    b.append("&#")
                     .append((int) c);
                } else {
                    b.append(/***/ c);
                }
          }
      }
      return b.toString();
  }

  /**
   * 转义JS文本
   * @param str
   * @return
   */
  public static String escapeJSS(String str) {
      return Dawn.doEscape (str);
  }

  /**
   * 编码JS对象
   * @param obj
   * @return
   */
  public static String encodeJSO(Object obj) {
      return Dawn.toString (obj);
  }

  /**
   * 编码URL文本
   * @param str
   * @return
   */
  public static String encodeURL(String str) {
      if (str == null) return "";
      try {
          return URLEncoder.encode(str, "UTF-8");
      } catch (UnsupportedEncodingException ex ) {
          throw new HongsExemption(1111, ex);
      }
  }

  /**
   * 解码URL文本
   * @param str
   * @return
   */
  public static String decodeURL(String str) {
      if (str == null) return "";
      try {
          return URLDecoder.decode(str, "UTF-8");
      } catch (UnsupportedEncodingException ex ) {
          throw new HongsExemption(1111, ex);
      }
  }

  private boolean inAjax(HttpServletRequest req) {
      // Accept
      String a = req.getHeader("Accept");
      if (a != null) {
          if (IS_JSON.matcher(a).find()) {
              return true ;
          }
          if (IS_HTML.matcher(a).find()) {
              return false;
          }
      }

      // Ajax
      String x = req.getHeader("X-Requested-With");
      if (x != null && ! x.isEmpty()) {
          return true;
      }
      // 标识 iframe 内的 ajax 方法
      if (Synt.declare(req.getParameter(".ajax"), false)) {
          return true;
      }

      // JSONP
      String c = Cnst.CB_KEY ;
      c = req.getParameter(c);
      if (c != null && ! c.isEmpty()) {
          return true;
      }
      CoreConfig cnf = CoreConfig.getInstance("default");
      c = cnf.getProperty ("core.callback" , "callback");
      c = req.getParameter(c);
      if (c != null && ! c.isEmpty()) {
          return true;
      }

      return false;
  }

  /**
   * 环境检测正则
   */
  private static final Pattern IS_HTML = Pattern.compile("(text|application)/(x?html|plain)");
  private static final Pattern IS_JSON = Pattern.compile("(text|application)/(x-)?(json|javascript)");

}
