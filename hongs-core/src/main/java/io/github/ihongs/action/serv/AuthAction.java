package io.github.ihongs.action.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CoreSerial;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.NaviMap;
import io.github.ihongs.util.Dawn;
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
 *   &lt;servlet-class&gt;io.github.ihongs.action.JSAuthAction&lt;/servlet-class&gt;
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
    /*
    // 2020/05/14  通过配置和用户的修改时间来判断是否能有变化
    // 受是否登录、不同用户等影响, 权限经常变化, 必须禁止缓存
    rsp.setHeader("Expires", "0");
    rsp.addHeader("Pragma" , "no-cache");
    rsp.setHeader("Cache-Control", "no-cache");
    */

    Core core = ActionDriver.getActualCore(req);
    ActionHelper helper = core.got(ActionHelper.class);

    String name = req.getPathInfo();
    if (name == null || name.length() == 0) {
      helper.error(400, "Path info required");
      return;
    }
    int p = name.lastIndexOf( '.' );
    if (p < 0) {
      helper.error(400, "File type required");
      return;
    }
    String type = name.substring(1 + p);
           name = name.substring(1 , p);
    if (!"js".equals(type) && !"json".equals(type)) {
      helper.error(400, "Wrong file type: "+ type);
      return;
    }

    String s;
    try {
      NaviMap  sitemap = NaviMap.getInstance(name);
      Set<String> roleset = sitemap.getRoleSet(  );
      Set<String> authset ;

      // 没有设置 rsname 的不公开
      if (null == sitemap.session) {
        helper.error(403, "Auth data for '"+name+"' is not open to the public");
        return;
      }

      // HTTP 304 缓存策略
      if (roleset instanceof CoreSerial.Mtimes) {
        CoreSerial.Mtimes rolemod = (CoreSerial.Mtimes) roleset;
        long l  =  Math.max( sitemap.dataModified(), rolemod.dataModified() );
        long m  =  helper.getRequest(  ).getDateHeader( "If-Modified-Since" );
        if ( l !=  0 ) {
            // HTTP 时间精确到秒
             l  =  l / 1000;
             m  =  m / 1000;
        if ( m >=  l ) {
          helper.getResponse().setStatus(HttpServletResponse.SC_NOT_MODIFIED);
          return;
        } else {
          helper.getResponse().setHeader( "Cache-Control", "no-cache" );
          helper.getResponse().setDateHeader("Last-Modified", l * 1000);
        }}
      }

      Map<String, Boolean> datamap = new HashMap();
      if (null == roleset) authset = new HashSet();
      else  authset  =  sitemap.getRoleAuths(roleset.toArray(new String[]{}));
      for(String  act : sitemap.actions) {
        datamap.put( act , authset.contains(act) );
      }

      s = Dawn.toString(datamap);
    }
    catch (IllegalArgumentException ex ) {
      helper.error(500 , ex.getMessage());
      return;
    }
    catch (HongsException|HongsExemption ex) {
      helper.error(404 , ex.getMessage());
      return;
    }

    // 输出权限信息
    if ("json".equals(type)) {
      helper.write(s, "application/json");
    } else {
      String c = req.getParameter("callback");
      if (c != null && c.length( ) != 0 ) {
        if (!c.matches("^[a-zA-Z_\\$][a-zA-Z0-9_]*$")) {
          helper.error(400, "Illegal callback function name!");
          return;
        }
        helper.write(c+"("+s+");", "text/javascript");
      } else {
        helper.write("if(!self.HsAUTH)self.HsAUTH={};Object.assign(self.HsAUTH,"+s+");", "text/javascript");
      }
    }
  }

}
