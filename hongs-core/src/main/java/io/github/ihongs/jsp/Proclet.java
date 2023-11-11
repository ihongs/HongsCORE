package io.github.ihongs.jsp;

import io.github.ihongs.Core;
import io.github.ihongs.CruxException;
import io.github.ihongs.HongsCause;
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
  public void doAction(Core core, ActionHelper ah)
    throws ServletException, IOException
  {
    HttpServletRequest  req = ah.getRequest ();
    HttpServletResponse rsp = ah.getResponse();

    try
    {
      this._jspService (new Request(req), rsp);
    }
    catch (ServletException ex )
    {
        Throwable ax = ex.getCause( );
        if (ax == null) { ax = ex ; }
        if (ax instanceof HongsCause) {
            ah.fault((HongsCause) ax);
        } else {
            ah.fault( new CruxException(ax) );
        }
    }
    catch (RuntimeException ax )
    {
        if (ax instanceof HongsCause) {
            ah.fault((HongsCause) ax);
        } else {
            ah.fault( new CruxException(ax) );
        }
    }
  }

  /**
   * 获取动作方法名称
   * @return
   */
  public String getHandle()
  {
    String rec = Core.ACTION_NAME.get();
    int p  = rec.lastIndexOf("/");
    if (p != -1) rec = rec.substring(0+p);
        p  = rec.lastIndexOf(".");
    if (p != -1) rec = rec.substring(0,p);
    return rec;
  }

  /**
   * 获取协议方法名称
   * @return
   */
  public String getMethod()
  {
    HttpServletRequest req = ActionHelper
           .getInstance( )
           .getRequest ( );
    if (req == null) {
        return null;
    }
    if (req instanceof Request) {
        Request raq = (Request) req;
        return  raq.getMathod();
    } else {
        return  req.getMethod();
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
