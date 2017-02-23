package app.hongs.serv.record;

import app.hongs.Core;
import app.hongs.HongsException;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 会话状态过滤
 * @author Hongs
 */
public class StatusFilter implements Filter {

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
    public void doFilter(ServletRequest req, ServletResponse rsp, FilterChain chn) throws IOException, ServletException {
        HttpServletRequest  raq = (HttpServletRequest ) req;
        HttpServletResponse rap = (HttpServletResponse) rsp;

        // 提取会话 ID
        String sid = raq.getParameter ( SSPN );
        if (sid == null) {
            for(Cookie cok : raq.getCookies( )) {
                if (SSCN.equals(cok.getName())) {
                    sid = cok.getValue();
                    break;
                }
            }
        }

        // 包裹请求
        // 使得其后可通过特别方法提取会话
        try {
            Status ses;

            raq = new StatusWraper(raq, sid);

            /* RUN */ chn.doFilter(raq, rsp);

            ses = (Status) raq.getSession(false);
            if (ses != null) {
                // 设置 Cookie 过期
                Cookie cok = new Cookie(SSCN, ses.getId());
                cok.setPath(Core.BASE_HREF+ "/");
                cok.setMaxAge(CEXP);
                rap.addCookie(cok );

                // 设置会话过期时间
                int exp = ses.getMaxInactiveInterval();
                if (exp > SEXP && 0 < SEXP) {
                    ses.setMaxInactiveInterval( SEXP );
                }

                // 持久存储会话数据
                ses.store( );
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
