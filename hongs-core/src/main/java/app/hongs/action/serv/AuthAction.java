package app.hongs.action.serv;

import app.hongs.Core;
import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionDriver;
import app.hongs.action.NaviMap;
import app.hongs.util.Data;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 权限信息输出动作
 *
 * <h3>web.xml配置:</h3>
 * <pre>
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;JsAuth&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;app.hongs.action.JSAuthAction&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;JsAuth&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;/common/auth/*&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Hongs
 */
public class AuthAction
  extends  ActionDriver
{

  /**
   * 服务方法
   * 判断配置和消息有没有生成, 如果没有则生成; 消息按客户语言存放
   * @param req
   * @param rsp
   * @throws java.io.IOException
   * @throws javax.servlet.ServletException
   */
  @Override
  public void service(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    // 受是否登录、不同用户等影响, 权限经常变化，必须禁止缓存
    rsp.setHeader("Expires", "0");
    rsp.addHeader("Pragma" , "no-cache");
    rsp.setHeader("Cache-Control", "no-cache");

    Core core = ActionDriver.getWorkCore(req );
    ActionHelper helper = core.get(ActionHelper.class);

    String name = req.getPathInfo();
    if (name == null || name.length() == 0) {
      helper.error500("Path info required");
      return;
    }
    int p = name.lastIndexOf( '.' );
    if (p < 0) {
      helper.error500("File type required");
      return;
    }
    String type = name.substring(1 + p);
           name = name.substring(1 , p);
    if ( !"js".equals(type) && !"json".equals(type)) {
      helper.error500("Wrong file type: "+type);
      return;
    }

    String data;
    try {
      NaviMap  sitemap = NaviMap.getInstance(name);
      Set<String> authset = sitemap.getAuthSet(  );
      if (null == authset) authset = new HashSet();
      Map<String, Boolean> datamap = new HashMap();
      for(String  act : sitemap.actions) {
        datamap.put( act , authset.contains(act) );
      }

      data = Data.toString(datamap);
    }
    catch (HongsException ex) {
      helper.error500(ex.getMessage());
      return;
    }
    catch (HongsError ex) {
      helper.error500(ex.getMessage());
      return;
    }

    // 输出配置信息
    if ( "json".equals(type)) {
      helper.print(data, "application/json");
    }
    else {
      helper.print("if(!window.HsAUTH)window.HsAUTH={};$.extend(window.HsAUTH,"+data+");", "application/javascript");
    }
  }

}
