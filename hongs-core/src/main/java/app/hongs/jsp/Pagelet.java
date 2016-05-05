package app.hongs.jsp;

import app.hongs.HongsError;
import app.hongs.action.ActionDriver;
import app.hongs.action.ActionHelper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.json.simple.JSONValue;
import javax.servlet.ServletException;
import javax.servlet.jsp.HttpJspPage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 抽象JSP类
 *
 * <pre>
 * 如果在JSP中需要使用框架提供的方法和标签, 需要从这继承:<br/>
 * &lt;%@page extends="app.hongs.action.Pagelet"%&gt;
 * </pre>
 *
 * @author Hongs
 */
public class Pagelet extends ActionDriver implements HttpJspPage
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
  public void _jspService(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    rsp.getWriter( ).close( );
  }

  @Override
  public void /**/service(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    this._jspService(req,rsp);
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
            case  '<': b.append("&lt;"  ); break;
            case  '>': b.append("&gt;"  ); break;
            case  '&': b.append("&amp;" ); break;
            case  '"': b.append("&quot;"); break;
            case '\'': b.append("&apos;"); break;
            case  10 : case  13 :          break;
            default  : b.append(c);
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
          return URLEncoder.encode(str, "utf-8");
      } catch (UnsupportedEncodingException ex ) {
          throw   new   HongsError.Common ( ex );
      }
  }

  /**
   * 转义JS文本
   * @param str
   * @return
   */
  public static String escapeJSS(String str) {
      return JSONValue.escape(str);
  }

}
