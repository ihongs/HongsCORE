package io.github.ihongs.action.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.PasserHelper;
import io.github.ihongs.action.NaviMap;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 动作权限过滤器
 *
 * <h3>初始化参数(init-param):</h3>
 * <pre>
 * config-name  菜单配置, 注: 如存在同名动作, 则全区均需登录
 * expire-time  登录超时(默认为永久)
 * index-page   起始页(为空则不跳转)
 * login-page   登录页(为空则不跳转)
 * ignore-urls  忽略的URL, 可用","分割多个, 可用"*"为前后缀
 * </pre>
 *
 * @author Hongs
 */
public class AuthFilter
  extends  ActionDriver
{

  /**
   * 动作配置
   */
  private NaviMap siteMap;

  /**
   * 载入时间
   */
  private long    mod = 0;

  /**
   * 登录超时
   */
  private long    exp = 0;

  /**
   * 区域验证
   */
  private String  aut = null;

  /**
   * 首页路径
   */
  private String  indexPage = null;

  /**
   * 登录路径
   */
  private String  loginPage = null;

  /**
   * 不包含的URL
   */
  private PasserHelper ignore = null;

  /**
   * 去主机名正则
   */
  private final Pattern RM_HOST = Pattern.compile("^\\w+://([^/]+)" );

  /**
   * 环境检测正则
   */
  private final Pattern IS_HTML = Pattern.compile( "text/(html|plain)" );
  private final Pattern IS_JSON = Pattern.compile("(text|application)/(x-)?(json|javascript)");

  @Override
  public void init(FilterConfig config)
    throws ServletException
  {
    super.init(config);

    String s;

    /**
     * 获取登录超时
     */
    this.exp = Synt.declare(config.getInitParameter("expire-time"), 0L);

    /**
     * 获取权限配置名
     */
    s = config.getInitParameter("config-name");
    if ( null != s)
    {
      this.aut = s;
      try
      {
        this.siteMap = NaviMap.getInstance(s);
      }
      catch (HongsException ex)
      {
        throw new ServletException(ex);
      }
    }
    else
    {
      try
      {
        this.siteMap = NaviMap.getInstance( );
      }
      catch (HongsException ex)
      {
        throw new ServletException(ex);
      }
    }
    this.mod = System.currentTimeMillis();

    /**
     * 获取首页URL
     */
    s = config.getInitParameter("index-page");
    if (s != null)
    {
      this.indexPage = Core.BASE_HREF + s;
    }

    /**
     * 获取登录URL
     */
    s = config.getInitParameter("login-page");
    if (s != null)
    {
      this.loginPage = Core.BASE_HREF + s;
    }

    /**
     * 获取不包含的URL
     */
    this.ignore = new PasserHelper(
        config.getInitParameter("ignore-urls"),
        config.getInitParameter("attend-urls")
    );
  }

  @Override
  public void destroy()
  {
    super.destroy( );

    siteMap   = null;
    indexPage = null;
    loginPage = null;
    ignore    = null;
  }

  @Override
  public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
    throws IOException, ServletException
  {
    HttpServletResponse rsp = hlpr.getResponse();
    HttpServletRequest  req = hlpr.getRequest( );
    String act = ActionDriver.getRecentPath(req);

    /**
     * 标记当前登录区域, 以便区分不同权限
     */
    req.setAttribute(AuthFilter.class.getName( ) + ":config", aut);

    /**
     * 检查当前动作是否可以忽略
     */
    if (ignore != null && ignore.ignore( act ) ) {
        chain.doFilter(req, rsp);
        return;
    }

    /**
     * 登记的动作权限串无前斜杠
     */
    if (act . startsWith ("/") ) {
        act = act.substring( 1 );
    }

    /**
     * 自动重载导航对象(权限表)
     */
    long las = siteMap.fileModified();
    if ( las == 0 || las > mod ) {
        try {
            String name = siteMap.getName( /**/ );
            siteMap = NaviMap.getInstance( name );
            mod     = System .currentTimeMillis();
        } catch (HongsException e) {
            throw new ServletException(e);
        }
    }

    /**
     * 判断当前用户是否登录超时
     * 未超时且是调试模式
     * 对超级管理员无限制
     */
    Set <String> authset = null;
    long ust = Synt.declare ( hlpr.getSessibute ( Cnst.UST_SES ) , 0L );
    long now = System.currentTimeMillis() / 1000;
    if ( exp == 0 || exp > now - ust ) {
        if ( 0 < Core.DEBUG && 8 != (8 & Core.DEBUG) ) {
            String uid = Synt.asString(hlpr.getSessibute(Cnst.UID_SES));
            if ( Cnst.ADM_UID.equals(uid)) {
                chain.doFilter( req, rsp );
                return;
            }
        }

        try {
            authset = siteMap.getAuthSet();
        } catch (HongsException ex) {
            throw new ServletException(ex);
        }
        ust = 0;
    }

    if (authset != null) {
        if (siteMap.actions.contains(aut)
            &&  !   authset.contains(aut)) {
            doFailed(core, hlpr, (byte) 2);
            return;
        }
        if (siteMap.actions.contains(act)
            &&  !   authset.contains(act)) {
            doFailed(core, hlpr, (byte) 3);
            return;
        }
    } else {
        if (siteMap.actions.contains(aut)) {
            doFailed(core, hlpr, (byte) (ust > 0 ? 0 : 1));
            return;
        }
        if (siteMap.actions.contains(act)) {
            doFailed(core, hlpr, (byte) (ust > 0 ? 0 : 1));
            return;
        }
    }

    chain.doFilter(req, rsp);
  }

  private void doFailed(Core core, ActionHelper hlpr, byte type)
  {
    HttpServletResponse rsp = hlpr.getResponse();
    HttpServletRequest  req = hlpr.getRequest( );
    CoreLocale lang = core.get(CoreLocale.class);
    String  uri ;
    String  msg ;

    switch (type) {
    case 3 :
        uri = this.indexPage;
        if (uri == null || uri.length() == 0) {
            uri =  null;
            msg =  lang.translate("core.error.no.power");
        } else {
            msg =  lang.translate("core.error.no.power.redirect");
        }
    break  ;
    case 2 :
        uri = this.loginPage;
        if (uri == null || uri.length() == 0) {
            uri =  null;
            msg =  lang.translate("core.error.no.place");
        } else {
            msg =  lang.translate("core.error.no.place.redirect");
        }
    break  ;
    default:
        uri = this.loginPage;
        if (uri == null || uri.length() == 0) {
            if ( 0 == type) {
                type  =  1;
                msg = lang.translate("core.error.un.login");
            } else {
                msg = lang.translate("core.error.no.login");
            }
        } else {
            if ( 0 == type) {
                type  =  1;
                msg = lang.translate("core.error.un.login.redirect");
            } else {
                msg = lang.translate("core.error.no.login.redirect");
            }

            /**
             * 追加来源路径, 登录后跳回.
             *
             * WEB 正常发起的异步请求等,
             * Referer 总是当前页面 URL.
             * APP 和小程序可能不带这个.
             */

            String src = null;
            String oth ;

            if (isAjax(req)) {
                src =  req.getHeader("Referer");
                oth =  req.getHeader("Host"   );
                if (src != null && src.length() != 0
                &&  oth != null && oth.length() != 0 ) {
                    Matcher mat = RM_HOST.matcher(src);
                    if (mat.find ( )
                    &&  mat.group(1).equals(oth) ) {
                        src = src.substring(mat.end());
                    }
                }
            } else
            if (isHtml(req)) {
                src =  req.getRequestURI( );
                oth =  req.getQueryString();
                if (oth != null && oth.length() != 0 ) {
                    src += "?"  +  oth;
                }
            }

            if (src != null) {
                try {
                    src = URLEncoder.encode(src, "UTF-8");
                } catch ( UnsupportedEncodingException e) {
                    src = "";
                }
                if (uri.contains("?")) {
                    uri += "&r=" + src;
                } else {
                    uri += "?r=" + src;
                }
            }
        }
    }

    if (isAjax(req) || isJson(req) || isJsop(req)) {
        Map rep = new HashMap();
        rep.put( "ok"  , false);
        rep.put( "msg" ,  msg );
        rep.put( "ern" , "Er40" + type);
        if (uri != null && uri.length() != 0) {
        rep.put( "err" , "Goto "+ uri );
        }

        // 错误状态
        if (type == 3) {
            rsp.setStatus(HttpServletResponse.SC_FORBIDDEN   );
        } else {
            rsp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }

        // forward 后可能会失效
        // 必须立即输出错误信息
        hlpr.reply(rep);
        hlpr.responed();
    } else {
        /**
         * 如果从收藏或历史打开一个页面
         * 而此区域并没有提供跳转的路径
         * 则从全局错误跳转构建响应代码
         */
        if (uri == null || uri.length() == 0) {
            uri =  core.get(CoreConfig.class)
                       .getProperty("fore.Er40"+ type +".redirect");
        if (uri == null || uri.length() == 0) {
            uri =  Core.BASE_HREF +"/";
        }}

        Map rep = new HashMap();
        rep.put( "msg" ,  msg );
        rep.put( "uri" ,  uri );
        rep.put( "urt" , Core.BASE_HREF );
        String err = lang.translate("core.redirect.html", rep /**/);

        // 禁止缓存
        rsp.addHeader("Cache-Control", "no-cache");
        rsp.setHeader(    "Pragma"   , "no-cache");
        rsp.setDateHeader("Max-Age"  , 0);
        rsp.setDateHeader("Expires"  , 0);

        // 错误消息
        if (type == 3) {
            hlpr.error403(err);
        } else {
            hlpr.error401(err);
        }
    }
  }

  private boolean isAjax(HttpServletRequest req) {
      if (Synt.declare(req.getParameter(".ajax") , false)) {
          return  true ; // 使用 iframe 提交通过此参数标识.
      }
      String x  = req.getHeader("X-Requested-With");
      return x != null && 0 != x.length();
  }

  private boolean isHtml(HttpServletRequest req) {
      String a  = req.getHeader("Accept");
      return a == null ? false : IS_HTML.matcher(a).find();
  }

  private boolean isJson(HttpServletRequest req) {
      String a  = req.getHeader("Accept");
      return a == null ? false : IS_JSON.matcher(a).find();
  }

  private boolean isJsop(HttpServletRequest req) {
      String c = Cnst.CB_KEY ;
      c = req.getParameter(c);
      if (c != null && c.length() != 0) {
          return true;
      }
      c = CoreConfig
          .getInstance()
          .getProperty("core.callback", "callback");
      c = req.getParameter(c);
      if (c != null && c.length() != 0) {
          return true;
      }
      return false;
  }

}
