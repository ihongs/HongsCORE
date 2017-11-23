package app.hongs.jsp;

import app.hongs.HongsCause;
import app.hongs.HongsExpedient;
import app.hongs.action.ActionDriver;
import app.hongs.action.ActionHelper;
import app.hongs.util.Data;
import java.net.URLEncoder;
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
      this._jspService(new HsRequest( req ), rsp);
    }
    catch (ServletException ex)
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
            if (  (  ax instanceof HongsCause))
            switch(((HongsCause)ax).getErrno()) {
                case 0x1105:
                    rsp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, er);
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
   * 转义XML/HTML文本
   * @param str
   * @return
   */
  public static String escapeXML(String str) {
      StringBuilder b = new StringBuilder( );
      int  l = str.length();
      int  i = 0;
      char c ;
      while ( i < l) {
          c = str.charAt(i);
          switch (c) {
            case '<': b.append("&lt;" ); break;
            case '>': b.append("&gt;" ); break;
            case '&': b.append("&amp;"); break;
            case 10 : b.append("&#10;"); break; // 换行
            case 13 : b.append("&#13;"); break; // 回车
            case 34 : b.append("&#34;"); break; // 双引号
            case 39 : b.append("&#39;"); break; // 单引号
            default : b.append(c);
          }
          i ++;
      }
      return b.toString();
  }

  /**
   * 转义URL文本
   * @param str
   * @return
   */
  public static String escapeURL(String str) {
      try {
          return URLEncoder.encode(str, "UTF-8");
      } catch (UnsupportedEncodingException ex ) {
          throw  new HongsExpedient.Common( ex );
      }
  }

  /**
   * 转义JS文本
   * @param str
   * @return
   */
  public static String escapeJSS(String str) {
      return Data.doEscape(str);
  }

  /**
   * 转为JS对象
   * @param obj
   * @return
   */
  public static String encodeJSO(Object obj) {
      return Data.toString(obj);
  }

  /**
   * 将非 GET,HEAD 转为 POST
   * 规避 JSP 不接受其他方法
   */
  private static class HsRequest extends HttpServletRequestWrapper {

      public HsRequest(HttpServletRequest req) {
          super(req);
      }

      public String getMathod() {
          return    super.getMethod();
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
