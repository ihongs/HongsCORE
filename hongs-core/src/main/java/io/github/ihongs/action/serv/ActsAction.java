package io.github.ihongs.action.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsCause;
import io.github.ihongs.HongsError;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.ActionDriver;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 动作启动器
 *
 * <h3>处理器编程:</h3>
 * <p>
 * 添加一个类, 给类加注解 @Action(action/path), 不添加或提供一个无参的构造方法;
 * 添加一个方法, 给方法加 @Action(action_name), 提供一个 ActionHelper 类型参数;
 * </p>
 *
 * <h3>web.xml配置:</h3>
 * <pre>
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;ActsAction&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;io.github.ihongs.action.ActsAction&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;ActsAction&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;*.act&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Hongs
 */
public class ActsAction
  extends  ActionDriver
{

  /**
   * 服务方法
   * Servlet Mapping: *.act<br/>
   * 注意: 不支持请求URI的路径中含有"."(句点), 且必须区分大小写;
   * 其目的是为了防止产生多种形式的请求路径, 影响动作过滤, 产生安全隐患.
   *
   * @param req
   * @param rsp
   * @throws javax.servlet.ServletException
   */
  @Override
  public void service(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException
  {
    String act  = ActionDriver.getRecentPath(req);
    Core   core = ActionDriver.getActualCore(req);
    ActionHelper helper = core.get(ActionHelper.class);
    Core.THREAD_CORE.set( core );

    if (act == null || act.length() == 0)
    {
      senderr(helper, 0x1104, null, "Action URI can not be empty.", "");
      return;
    }

    // 去掉根和扩展名
    act = act.substring(1);
    int pos = act.lastIndexOf('.');
    if (pos != -1)
        act = act.substring(0,pos);

    // 获取并执行动作
    try
    {
      new ActionRunner(helper,act).doAction();
    }
    catch (ClassCastException ex )
    {
      senderr(helper, new HongsException(0x1100, ex)); // 类型转换失败按 400 错误处理
    }
    catch (HongsException ex)
    {
      senderr(helper, ex);
    }
    catch (HongsExemption ex)
    {
      senderr(helper, ex);
    }
    catch (HongsError ex)
    {
      senderr(helper, ex);
    }
  }

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

}
