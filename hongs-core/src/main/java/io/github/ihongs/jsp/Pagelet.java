package io.github.ihongs.jsp;

import io.github.ihongs.CoreConfig;
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
        int eo  = ax instanceof HongsCause ? ((HongsCause)ax).getErrno() : 0;
        String er = ax.getLocalizedMessage();
        String ec ;

        // 代号映射
          ec = Integer.toHexString( eo );
          ec = CoreConfig.getInstance( ).getProperty( "core.ern.map." + ec );
        if (null != ec && !ec.isEmpty())
        {
          eo = Integer.parseInt(ec, 16 );
        }

        // 响应状态, 错误码为 16 进制, 取字面值, 如 0x400 取 400
        if (eo >= 0x400 && eo <= 0x599 ) try
        {
          eo = Integer.parseInt( Integer.toHexString (eo) );
        } catch (NumberFormatException ne)
        {
          eo = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        } else
        {
          eo = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        req.setAttribute("javax.servlet.error.status_code" , eo);
        req.setAttribute("javax.servlet.error.message"     , er);
        req.setAttribute("javax.servlet.error.exception"   , ax);
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
          throw new HongsExemption(ex);
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
          throw new HongsExemption(ex);
      }
  }

}
