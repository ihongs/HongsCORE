package io.github.ihongs.jsp;

import io.github.ihongs.Core;
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
    return req.getMethod();
  }

}
