package io.github.ihongs.jsp;

import io.github.ihongs.HongsCause;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Dawn;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

  ActionHelper helper = null;

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
    try
    {
      this._jspService(req, rsp);
    }
    catch (ServletException ex )
    {
        Throwable ax = ex.getCause( );
        if (ax == null) { ax = ex ; }
        int eo = ax instanceof HongsCause ? ((HongsCause) ax).getErrno() : 0;
        switch (eo) {
            case 0x1100:
                eo = HttpServletResponse.SC_BAD_REQUEST ;
                break;
            case 0x1101:
                eo = HttpServletResponse.SC_UNAUTHORIZED;
                break;
            case 0x1102:
                eo = HttpServletResponse.SC_FORBIDDEN;
                break;
            case 0x1103:
                eo = HttpServletResponse.SC_FORBIDDEN;
                break;
            case 0x1104:
                eo = HttpServletResponse.SC_NOT_FOUND;
                break;
            case 0x1106:
                eo = HttpServletResponse.SC_NOT_ACCEPTABLE;
                break;
            case 0x1105:
                eo = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
                break;
            default:
                eo = HttpServletResponse.SC_INTERNAL_SERVER_ERROR ;
        }
        String  er = ax.getLocalizedMessage();
        req.setAttribute("javax.servlet.error.status_code"   , eo);
        req.setAttribute("javax.servlet.error.message"       , er);
        req.setAttribute("javax.servlet.error.exception"     , ax);
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
          throw  new HongsExemption.Common( ex );
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
          throw  new HongsExemption.Common( ex );
      }
  }

}
