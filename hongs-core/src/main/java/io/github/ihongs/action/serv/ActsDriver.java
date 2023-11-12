package io.github.ihongs.action.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.ActionDriver;
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
public class ActsDriver
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
    ActionHelper helper = core.got(ActionHelper.class);
    Core.THREAD_CORE.set( core );

    if (act == null || act.length() == 0)
    {
      helper.fault(new CruxException(404, "Action URI can not be empty."));
      return;
    }

    // 去掉根和扩展名
    int pos = act.lastIndexOf('.');
    if (pos > 1) {
        act = act.substring(1,pos);
    } else {
        act = act.substring(1);
    }

    // 获取并执行动作
    try
    {
      new ActionRunner(helper,act).doAction();
    }
    catch (CruxException e)
    {
      helper.fault(e);
    }
    catch (CruxExemption e)
    {
      helper.fault(e);
    }
    catch (RuntimeException e)
    {
      helper.fault(new CruxException(e, 500));
    }
  }

}
