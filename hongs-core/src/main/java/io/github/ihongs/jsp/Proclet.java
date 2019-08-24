package io.github.ihongs.jsp;

import io.github.ihongs.CoreLocale;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsCause;
import io.github.ihongs.HongsError;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
  public void service(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    try
    {
      this._jspService(new Request(req), rsp);
    }
    catch (ServletException ex )
    {
        ActionHelper ah = ActionDriver.getActualCore(req).get(ActionHelper.class);
        Throwable ax = ex.getCause( );
        if (ax == null) { ax = ex ; }
        if (ax instanceof HongsCause) {
            senderr(ah , (HongsCause) ax);
        } else {
            senderr(ah , 0x110e, "" , ax.getMessage(), ax.getLocalizedMessage( ));
        }
    }
  }

  //* 以下两个方法来自 ActsAction */

  private void senderr(ActionHelper helper, HongsCause ex)
  {
    Throwable ta = (Throwable)ex;
    Throwable te = ta.getCause();
    int    ern = ex.getErrno();
    String ers = ex.getError();
    String err ;
    String msg ;

    // 外部异常, 不记录日志
    if (ern >= 0x1100 && ern <= 0x1109)
    {
      String[] ls = ex.getLocalizedOptions();
      if (/**/ ls == null || ls.length == 0)
      {
        HttpServletRequest rq = helper.getRequest();
        String rp = ActionDriver.getOriginPath(rq );
        String er = ex.getError(/****/);
        ex.setLocalizedOptions (er, rp);
      }
    }
    else
    // 服务异常, 仅记录起因
    if (ern >= 0x110a && ern <= 0x110e)
    {
      if (null != te)
      {
        CoreLogger.error(ta);
      }
    }
    else
    // 通用错误, 仅记录起因
    if (ern == 0x1000 || ern == 0x1001)
    {
      if (null != te)
      {
        CoreLogger.error(te);
      }
    }
    else
    // 其他异常, 记录到日志
    {
        CoreLogger.error(ta);
    }

    // 错误消息
      err = ta.getMessage( );
      msg = ta.getLocalizedMessage();
    if (null != te
    && (null == msg || msg.length() == 0))
    {
      msg = te.getLocalizedMessage();
    }
    if (null == msg || msg.length() == 0 )
    {
      msg = CoreLocale.getInstance().translate("core.error.unkwn");
    }

    // 401,402.403,404 异常可选本地化参数依次为: 错误,当前URL,跳转URL
    if (ern >= 0x1101 && ern <= 0x1104)
    {
      String[] arr;
      arr = ex.getLocalizedOptions();
      err = arr!=null && arr.length>2
          ? "Goto " + arr[2]
          : "";
    }

    senderr(helper, ern, ers, err, msg);
  }

  private void senderr(ActionHelper helper, int ern, String ers, String err, String msg)
  {
    switch(ern)
    {
      case 0x1100:
        ers = "Er400";
        helper.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST );
        break;
      case 0x1101:
        ers = "Er401";
        helper.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        break;
      case 0x1102:
        ers = "Er402";
        helper.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
        break;
      case 0x1103:
        ers = "Er403";
        helper.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
        break;
      case 0x1104:
        ers = "Er404";
        helper.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
        break;
      case 0x1106:
        ers = "Er406";
        helper.getResponse().setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
        break;
      case 0x1105:
        ers = "Er405";
        helper.getResponse().setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        break;
      case 0x110e:
      case 0x110f:
      case HongsException.COMMON:
      case     HongsError.COMMON:
        ers = "Er500";
        helper.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        break;
      case HongsException.NOTICE:
      case     HongsError.NOTICE:
        // 错误即代号;
        helper.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        break;
      default:
        ers = "Ex"+Integer.toHexString(ern);
        helper.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    Map data = new HashMap();
    data.put( "ok" , false );
    data.put("ern", ers );
    data.put("err", err );
    data.put("msg", msg );
    helper.reply(data);
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
