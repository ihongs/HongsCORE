package app.hongs.jsp;

import app.hongs.action.ActionDriver;
import app.hongs.action.ActionHelper;
import java.io.IOException;
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

}
