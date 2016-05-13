package app.hongs.action.serv;

import app.hongs.Core;
import app.hongs.CoreLocale;
import app.hongs.CoreLogger;
import app.hongs.HongsCause;
import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.HongsUnchecked;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.ActionDriver;
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
 添加一个包, 将包名加到 default.properties 里的 core.action.pacakges 值中;
 添加一个类, 给类加上注解 @Action(action/path), 不添加或提供一个无参构造方法;
 添加一个方法, 给方法加上 @Action(action_name), 提供一个 ActionHelper 参数;
 </p>
 *
 * <h3>web.xml配置:</h3>
 * <pre>
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;ActsAction&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;app.hongs.action.ActsAction&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;ActsAction&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;***.act&lt;/url-pattern&gt;
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
    String act  = ActionDriver.getCurrPath(req);
    Core   core = ActionDriver.getWorkCore(req);
    ActionHelper helper = core.get(ActionHelper.class);
    Core.THREAD_CORE.set( core );

    if (act == null || act.length() == 0)
    {
      senderr(helper, 0x1104, null, "Action URI can not be empty.");
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
      ActionRunner runner = new ActionRunner(act, helper);
      runner.doAction(  );
    }
    catch (ClassCastException ex)
    {
      senderr(helper, new HongsError(0x43, ex)); // 类型转换异常
    }
    catch (HongsException ex)
    {
      senderr(helper, ex);
    }
    catch (HongsUnchecked ex)
    {
      senderr(helper, ex);
    }
    catch (HongsError ex)
    {
      senderr(helper, ex);
    }
  }

  private void senderr(ActionHelper helper, HongsCause ex)
    throws ServletException
  {
    Throwable ta = (Throwable)ex;
    Throwable te = ta.getCause();
    int    errno = ex.getErrno();
    String errsn = ex.getError();
    String error ;

    // 一般异常, 不记录日志
    if (errno >= 0x1100 && errno <= 0x1109 )
    {
      String[] ls = ex.getLocalizedOptions();
      if (/**/ ls == null || ls.length == 0)
      {
        HttpServletRequest rq = helper.getRequest();
        String rp = ActionDriver.getRealPath  (rq );
        ex.setLocalizedOptions(rp, ex.getError( ) );
      }
    }
    else
    // 服务异常, 交换且记录
    if (errno >= 0x110a && errno <= 0x110e )
    {
        Throwable  tx  = ta ;
                   ta  = te ;
                   te  = tx ;
        CoreLogger.error(tx);
    }
    else
    // 其他异常, 记录到日志
    {
        CoreLogger.error(ta);
    }

    // 错误消息
      error = ta.getLocalizedMessage( );
    if (null != te
    && (null == error || error.length() == 0))
    {
      error = te.getLocalizedMessage( );
    }
    if (null == error || error.length() == 0 )
    {
      error = CoreLocale.getInstance( ).translate("core.error.unkwn");
    }

    senderr(helper, errno, errsn, error);
  }

  private void senderr(ActionHelper helper, int errno, String errsn, String error )
    throws ServletException
  {
    switch(errno)
    {
      case 0x1100:
        errsn = "Er400";
        helper.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST );
        break;
      case 0x1101:
        errsn = "Er401";
        helper.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        break;
      case 0x1102:
        errsn = "Er402";
        helper.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
        break;
      case 0x1103:
        errsn = "Er403";
        helper.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
        break;
      case 0x1104:
        errsn = "Er404";
        helper.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
        break;
      case 0x1105:
        errsn = "Er405";
        helper.getResponse().setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        break;
      case 0x110e:
      case HongsException.COMMON:
      case     HongsError.COMMON:
        errsn = "Er500";
        helper.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        break;
      case HongsException.NOTICE:
      case     HongsError.NOTICE:
        helper.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        break;
      default:
        errsn = "Ex"+Integer.toHexString(errno);
        helper.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    Map data = new HashMap();
    data.put( "ok" , false );
    data.put("err", errsn );
    data.put( "msg", error );
    helper.reply(data);
  }

}
