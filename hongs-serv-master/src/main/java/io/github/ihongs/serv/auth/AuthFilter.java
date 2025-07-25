package io.github.ihongs.serv.auth;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.verify.Wrong;
import io.github.ihongs.util.verify.Wrongs;
import java.io.IOException;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 认证过滤器
 *
 * 供接口使用, 需置于 AuthFilter 的上面.
 *
 * 注意:
 * 在此过滤之后的会话是临时的,
 * 不具备跨请求存储状态的能力,
 * 故无法正常使用基础验证码等,
 * 除非不使用会话记录验证码等.
 *
 * @author Hongs
 */
public class AuthFilter
  extends  ActionDriver
{

  private String      scheme = null;

  private PathPattern patter = null;

  @Override
  public void init(FilterConfig config)
    throws ServletException
  {
    super.init(config);

    this.scheme = Synt.defoult(config.getInitParameter("auth-type"), "Bearer");

    this.patter = new PathPattern(
        config.getInitParameter("url-include"),
        config.getInitParameter("url-exclude")
    );
  }

  @Override
  public void destroy()
  {
    super.destroy();
    scheme = null;
    patter = null;
  }

  @Override
  public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
    throws IOException, ServletException
  {
    HttpServletResponse rsp = hlpr.getResponse();
    HttpServletRequest  req = hlpr.getRequest( );
    String act = ActionDriver.getRecentPath(req);

    if (patter != null && ! patter.matches(act)) {
        chain.doFilter(req, rsp);
        return;
    }

    String code = req.getHeader("Authorization");
    if (code != null) {
        int p = code.indexOf(" ");
        if (p > 0) {
            String type;
            type = code.substring(0,p);
            code = code.substring(1+p);
            if (type.equalsIgnoreCase(scheme)) {
                // 检查是否已经有登录状态了
                Object uuid = hlpr.getSessibute(Cnst.UID_SES);
                if (uuid != null) {
                    CoreLogger.debug("Authorization unused. Session already exists for user {}", uuid);
                } else {
                    try {
                        authSign(hlpr, "auth", code);
                    } catch (CruxException ex) {
                        hlpr.error(401, ex.getLocalizedMessage());
                        return;
                    }
                }
            }
        }
    }

    chain.doFilter(req, rsp);
  }

  private void authSign(ActionHelper hlpr, String unit, String code) throws CruxException
  {
    DB    db = DB.getInstance("master");
    Table tb = db.getTable("user_sign");
    Table ub = db.getTable("user");
    Map   ud = tb.fetchCase()
                 .from(tb.tableName, "s")
                 .join(ub.tableName, "u", "u.id = s.user_id", FetchCase.LEFT)
                 .filter("s.unit = ? AND s.code = ?" , unit , code)
                 .select("u.id, u.state")
                 .getOne(   );

    if (ud.isEmpty()) {
        throw new Wrongs(Synt.mapOf("state" ,
              new Wrong ("@master:core.sign.oauth.invalid")
        ));
    }

    if (Synt.declare(ud.get("state"), 0) <= 0) {
        throw new Wrongs(Synt.mapOf("state" ,
              new Wrong ("@master:core.sign.state.invalid")
        ));
    }

    // 临时虚拟会话
    hlpr.setSessionData(Synt.mapOf(
        Cnst.UID_SES, ud.get("id")
    ));
  }


}
