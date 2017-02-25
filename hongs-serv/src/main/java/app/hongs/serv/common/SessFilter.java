package app.hongs.serv.common;

import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionDriver;
import app.hongs.action.ActionHelper;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;

/**
 * 会话状态过滤
 * @author Hongs
 */
public class SessFilter extends ActionDriver implements Filter {

    private String SSPN = "SSID"; // 会话请求参数名称
    private String SSCN = "SSID"; // 会话 Cookie 键名
    private int    SEXP =  86400; // 会话过期时间(秒)
    private int    CEXP =     -1; // 过期 Cookie (秒)

    @Override
    public void init(FilterConfig fc) throws ServletException {
        String fn;

        fn = fc.getInitParameter("request-name");
        if (fn != null) SSPN = fn;

        fn = fc.getInitParameter( "cookie-name");
        if (fn != null) SSCN = fn;

        fn = fc.getInitParameter("session-max-age");
        if (fn != null) SEXP = Integer.parseInt(fn);

        fn = fc.getInitParameter( "cookie-max-age");
        if (fn != null) CEXP = Integer.parseInt(fn);
    }

    @Override
    public void doFilter(Core core, ActionHelper hlpr, FilterChain chin) throws IOException, ServletException {
        HttpServletRequest  req = hlpr.getRequest( );
        HttpServletResponse rsp = hlpr.getResponse();

        // 提取会话 ID
        String sid = req.getParameter ( SSPN );
        if (sid == null) {
            for(Cookie cok : req.getCookies( )) {
                if (SSCN.equals(cok.getName())) {
                    sid = cok.getValue();
                    break;
                }
            }
        }

        // 包裹请求
        // 使得其后可通过特别方法提取会话
        try {
            req = new SessFiller(req, sid , SEXP);
            hlpr.updateHelper   (req, rsp);
            chin.doFilter       (req, rsp);

            HttpSession sez =  req.getSession (false);
            if (null != sez && sez instanceof Sesion) {
                // 持久存储会话数据
                Sesion  ses = ( Sesion ) sez ; ses.store( );

                // 重设 Cookie 期限
                Cookie  cok = new Cookie(SSCN, ses.getId());
                cok.setPath(Core.BASE_HREF + "/");
                cok.setHttpOnly(true);
                cok.setMaxAge  (CEXP);
                rsp.addCookie  (cok );
            }
        }
        catch (HongsException ex) {
            throw new ServletException(ex);
        }
    }

    @Override
    public void destroy() {

    }

}
