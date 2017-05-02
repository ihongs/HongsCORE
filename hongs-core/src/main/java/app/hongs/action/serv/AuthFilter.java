package app.hongs.action.serv;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionDriver;
import app.hongs.action.ActionHelper;
import app.hongs.action.NaviMap;
import app.hongs.util.Synt;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 动作权限过滤器
 *
 * <h3>初始化参数(init-param):</h3>
 * <pre>
 * config-name  菜单配置
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
  private FilterCheck ignoreCase = null;

  /**
   * 环境检测正则
   */
  private final Pattern IS_HTML = Pattern.compile( "text/(html|plain)" );
  private final Pattern IS_JSON = Pattern.compile("(text|application)/(x-)?(json|javascript)");
  private final Pattern IS_AJAX = Pattern.compile("(AJAX|XMLHTTP)" , Pattern.CASE_INSENSITIVE);

  @Override
  public void init(FilterConfig config)
    throws ServletException
  {
    super.init(config);

    String s;

    /**
     * 获取登录超时
     */
    this.exp = Synt.asserts(config.getInitParameter("expire-time"), 0L);

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
    this.ignoreCase = new FilterCheck(
        config.getInitParameter("ignore-urls"),
        config.getInitParameter("attend-urls")
    );
  }

  @Override
  public void destroy()
  {
    super.destroy();

    siteMap    = null;
    indexPage  = null;
    loginPage  = null;
    ignoreCase = null;
  }

  @Override
  public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
    throws IOException, ServletException
  {
    ServletRequest  req = hlpr.getRequest( );
    ServletResponse rsp = hlpr.getResponse();
    String act = ActionDriver.getCurrPath((HttpServletRequest) req);

    /**
     * 检查当前动作是否可以忽略
     */
    if (ignoreCase != null && ignoreCase.ignore(act)) {
        chain.doFilter(req, rsp);
        return;
    }

    /**
     * 判断当前用户是否登录超时
     */
    if (exp != 0) {
        long tim = Synt.asserts(hlpr.getSessibute(Cnst.UST_SES), 0L);
        long now = System.currentTimeMillis() / 1000;
        if ( now - tim < exp) {
            hlpr.setSessibute ( Cnst.UST_SES , now );
        } else {
            doFailed ( core, hlpr, (byte) 0 );
            return;
        }
    }

    /**
     * 调试模式超级管理员无限制
     */
    if (Core.DEBUG > 0) {
      String uid = Synt.asserts(hlpr.getSessibute(Cnst.UID_SES), "");
        if ( uid!= null && uid.equals(Cnst.ADM_UID)) {
            chain.doFilter(req , rsp );
            return;
        }
    }

    // 获取详细会话集合
    Set<String> authset;
    try {
        authset = siteMap.getAuthSet();
    } catch (HongsException e) {
        throw new ServletException(e );
    }

    // 权限动作无前导/
    if (act.startsWith( "/" )) {
        act = act.substring(1);
    }
    // 附带上协议方法
    String amt = act + "|" + hlpr.getRequest().getMethod();

    if (null == authset) {
        if (null != loginPage) {
            doFailed(core, hlpr, (byte) 1); // 没有登录
            return;
        }
        if (siteMap.actions.contains(act)) {
            doFailed(core, hlpr, (byte) 3); // 需要权限
            return;
        }
        if (siteMap.actions.contains(amt)) {
            doFailed(core, hlpr, (byte) 3); // 需要权限(带方法)
            return;
        }
    } else {
        if (siteMap.actions.contains(act)) {
            if (  ! authset.contains(act)) {
                doFailed(core, hlpr, (byte) 3); // 缺少权限
                return;
            }
        }
        if (siteMap.actions.contains(amt)) {
            if (  ! authset.contains(amt)) {
                doFailed(core, hlpr, (byte) 3); // 缺少权限(带方法)
                return;
            }
        }
        if (siteMap.actions.contains(aut)) {
            if (  ! authset.contains(aut)) {
                doFailed(core, hlpr, (byte) 2); // 禁入区域
                return;
            }

            /**
             * 权限配置中有指定区域的
             * 必须在指定区域再次登录
             * 登录时会将区域写入会话
             */
            Set usp = (Set) hlpr.getSessibute(Cnst.USL_SES);
            if (usp == null || ! usp.contains(aut)) {
                doFailed(core, hlpr, (byte) 1); // 登录区域
                return;
            }
        }
    }

    chain.doFilter(req, rsp);
  }

  private void doFailed(Core core, ActionHelper hlpr, byte type)
  {
    CoreLocale lang = core.get(CoreLocale.class);
    HttpServletRequest req = hlpr.getRequest(  );
    String uri;
    String msg;

    if ( 2 == type) {
        uri = Core.BASE_HREF;
        if (uri == null || uri.length() == 0) {
            msg = lang.translate("core.error.wr.place");
        } else {
            msg = lang.translate("core.error.wr.place.redirect");
        }
    } else
    if ( 3 == type) {
        uri = this.indexPage;
        if (uri == null || uri.length() == 0) {
            msg = lang.translate("core.error.no.power");
        } else {
            msg = lang.translate("core.error.no.power.redirect");
        }
    } else
    {
        uri = this.loginPage;
        if (uri == null || uri.length() == 0) {
            if ( 0 == type) {
                msg = lang.translate("core.error.to.login");
            } else {
                msg = lang.translate("core.error.no.login");
            }
        } else {
            if ( 0 == type) {
                msg = lang.translate("core.error.to.login.redirect");
            } else {
                msg = lang.translate("core.error.no.login.redirect");
            }

            /** 追加来源路径, 以便登录后跳回 **/

            String src = null;
            String qry ;

            if (isApi (req)) {
                // API 模式无需返回地址
            } else
            if (isAjax(req)) {
                src =  req.getHeader("Referer")
                    .replaceFirst("^\\w+://[^/]+","");
            } else
            if (isHtml(req)) {
                src =  req.getRequestURI( );
                qry =  req.getQueryString();
                if (qry != null && qry.length() != 0) {
                    src += "?"  +  qry;
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

    if (isApi(req) || isAjax(req) || isJson(req)) {
        Map rsp = new HashMap();
            rsp.put("ok",false);
            rsp.put("msg", msg);
            rsp.put("ern", "Er40"+type);
        if (uri != null && uri.length() != 0) {
            rsp.put("err", "Goto "+uri);
        }

        if (type == 1 ) {
            hlpr.getResponse().setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            hlpr.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN   );
        }

        /**
         * 必须调用 responed 立即输出
         * 用以应对 forward  后输出失败的问题
         */
        hlpr.reply(rsp);
        hlpr.responed();
    } else {
        /**
         * 有时候如果客户从收藏夹或历史记录打开了一个没有权限的页
         * 如果此区域没有提供跳转的路径
         * 则从全局错误跳转构建响应代码
         */

        if (uri == null && uri.length() == 0) {
            uri  = core.get(CoreConfig.class)
                       .getProperty("fore.Er40"+ type +".redirect");
        if (uri == null || uri.length() == 0) {
            uri  = Core.BASE_HREF +"/";
        }}

        Map<String, String> rep = new HashMap();
        rep.put("urt", Core.BASE_HREF);
        rep.put("uri", uri);
        rep.put("msg", msg);
        String err = lang.translate("core.redirect.html", rep /**/);

        if (type == 1 ) {
            hlpr.error401(err);
        } else {
            hlpr.error403(err);
        }

        // 禁止缓存
        HttpServletResponse rsp = hlpr.getResponse();
        rsp.addHeader("Cache-Control", "no-cache");
        rsp.setHeader("Pragma", "no-cache");
        rsp.setDateHeader("Expires", 0);
    }
  }

  private boolean isApi (HttpServletRequest req) {
      return ActionDriver.getRealPath(req).endsWith(".api");
  }

  private boolean isAjax(HttpServletRequest req) {
      if (Synt.declare(req.getParameter(".ajax") , false )) {
          return  true; // 为方便特殊情况下使用 POST, 可显式申明 AJAX
      }
      String x  = req.getHeader("X-Requested-With");
      return x == null ? false : IS_AJAX.matcher(x).find( );
  }

  private boolean isJson(HttpServletRequest req) {
      String a  = req.getHeader("Accept");
      return a == null ? false : IS_JSON.matcher(a).find( );
  }

  private boolean isHtml(HttpServletRequest req) {
      String a  = req.getHeader("Accept");
      return a == null ? false : IS_HTML.matcher(a).find( );
  }

}
