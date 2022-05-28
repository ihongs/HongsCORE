package io.github.ihongs.jsp;

import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import javax.servlet.http.HttpServletRequest;

/**
 * 动作代理
 *
 * 建议精确匹配具体动作路径(包含扩展名)
 *
 * @author Kevin
 */
abstract public class Servlet extends ActionDriver
{

  /**
   * 获取协议方法名称
   * @param hlp
   * @return
   */
  public String getMethod(ActionHelper hlp)
  {
    return getMethod(hlp.getRequest());
  }

  /**
   * 获取动作方法名称
   * @param hlp
   * @return
   */
  public String getHandle(ActionHelper hlp)
  {
    return getHandle(hlp.getRequest());
  }

  /**
   * 获取协议方法名称
   * @param req
   * @return
   */
  public String getMethod(HttpServletRequest req)
  {
    return req. getMethod();
  }

  /**
   * 获取动作方法名称
   * @param req
   * @return
   */
  public String getHandle(HttpServletRequest req)
  {
    String rec = ActionDriver.getRecentPath (req)
                          .substring(1  );
    int p  = rec.lastIndexOf("/");
    if (p != -1) rec = rec.substring(0+p);
        p  = rec.lastIndexOf(".");
    if (p != -1) rec = rec.substring(0,p);
    return rec;
  }

}
