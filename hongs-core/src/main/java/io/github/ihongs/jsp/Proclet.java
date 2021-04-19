package io.github.ihongs.jsp;

import io.github.ihongs.HongsCause;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.jsp.HttpJspPage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * 动作JSP类
 *
 * <pre>
 * 如果将JSP作为类似框架的动作方法等来使用, 需要从这继承:
 * &lt;%@page extends="io.github.ihongs.action.Proclet"%&gt;
 * </pre>
 *
 * @author Hongs
 */
abstract public class Proclet extends ActionDriver implements HttpJspPage
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
  public void service(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    try
    {
      this._jspService(new Request(req), rsp);
    }
    catch (ServletException ex )
    {
        ActionHelper ah = ActionDriver.getActualCore(req).got(ActionHelper.class);
        Throwable ax = ex.getCause( );
        if (ax == null) { ax = ex ; }
        if (ax instanceof HongsCause) {
            ah.fault((HongsCause) ax);
        } else {
            ah.fault( new HongsException(ax));
        }
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
          &&  !"HEAD".equals(mathod)) {
              return "POST";
          }
          return mathod;
      }

  }

}
