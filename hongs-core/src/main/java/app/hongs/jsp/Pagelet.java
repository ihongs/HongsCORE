package app.hongs.jsp;

import app.hongs.HongsCause;
import app.hongs.HongsExpedient;
import app.hongs.action.ActionDriver;
import app.hongs.action.ActionHelper;
import app.hongs.util.Data;
import app.hongs.util.Tool;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletException;
import javax.servlet.jsp.HttpJspPage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * 抽象JSP类
 *
 * <pre>
 * 如果在JSP中需要使用框架提供的方法和标签, 需要从这继承:
 * &lt;%@page extends="app.hongs.action.Pagelet"%&gt;
 * </pre>
 *
 * @author Hongs
 */
public class Pagelet extends ActionDriver implements HttpJspPage
{

  ActionHelper helper = null;

  @Override
  public void  jspInit()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void  jspDestroy()
  {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void _jspService(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    rsp.getWriter().close();
  }

  @Override
  public void /**/service(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    try
    {
      this._jspService( new Request(req) , rsp );
    }
    catch (ServletException|RuntimeException ex)
    {
        /**
         * 异常处理
         */
        Throwable  ax = ex.getCause();
        String     er ;
        if (ax == null) {
            er = ex.getLocalizedMessage();
            req.setAttribute("javax.servlet.error.message"  , er);
            req.setAttribute("javax.servlet.error.exception", ex);
            req.setAttribute("javax.servlet.error.exception_type", ex.getClass().getName());
        } else {
            er = ax.getLocalizedMessage();
            req.setAttribute("javax.servlet.error.message"  , er);
            req.setAttribute("javax.servlet.error.exception", ax);
            req.setAttribute("javax.servlet.error.exception_type", ax.getClass().getName());
            if (  (  ax instanceof HongsCause ))
            switch(((HongsCause) ax).getErrno()) {
                case 0x1105:
                    rsp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, er);
                    return ;
                case 0x1106:
                    rsp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE , er);
                    return ;
                case 0x1100:
                    rsp.sendError(HttpServletResponse.SC_BAD_REQUEST , er);
                    return ;
                case 0x1101:
                    rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED, er);
                    return ;
                case 0x1102:
                    rsp.sendError(HttpServletResponse.SC_FORBIDDEN, er);
                    return ;
                case 0x1103:
                    rsp.sendError(HttpServletResponse.SC_FORBIDDEN, er);
                    return ;
                case 0x1104:
                    rsp.sendError(HttpServletResponse.SC_NOT_FOUND, er);
                    return ;
            }
        }
        rsp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, er);
        throw ex;
    }
  }

  /**
   * @see Tool.escape 引用
   * @param str
   * @return
   */
  public static String quotes(String str) {
      return Tool.escape(str);
  }

  /**
   * @see escapeXML 的别名
   * @param str
   * @return
   */
  public static String escape(String str) {
      return   escapeXML(str);
  }

  /**
   * 转义XML/HTML文本
   * @param str
   * @return
   */
  public static String escapeXML(String str) {
      if (str == null) return "";
      StringBuilder b = new StringBuilder( );
      int  l = str.length(     );
      int  i = 0;
      char c ;
      while( l >  i) {
           c = str.charAt(i ++ );
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
      return Data.doEscape (str);
  }

  /**
   * 编码JS对象
   * @param obj
   * @return
   */
  public static String encodeJSO(Object obj) {
      return Data.toString (obj);
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
          throw  new HongsExpedient.Common( ex );
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
          throw  new HongsExpedient.Common( ex );
      }
  }

  /**
   * 将非 GET,HEAD 转为 POST
   * 规避 JSP 拒绝处理的问题
   */
  public static class Request extends HttpServletRequestWrapper {

      public Request(HttpServletRequest req) {
          super(req);
      }

      /**
       * 获取真实的方法名
       * @return
       */
      public String getMathod() {
          return super.getMethod();
      }

      @Override
      public String getMethod() {
          // 使 JSP 可处理 REST 所有方法
          String mathod = getMathod();
          if (! "GET".equals(mathod )
          ||  !"HEAD".equals(mathod)) {
              return "POST";
          }
          return mathod;
      }

  }

}
