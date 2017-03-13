package app.hongs.serv.common;

import app.hongs.HongsException;
import app.hongs.util.Synt;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 会话状态过滤
 * @author Hongs
 */
public class SessFilter implements Filter {

    private   String ATT  =  null ; // 过滤器名称
    protected String SSRN = "SSID"; // 会话请求参数名称
    protected String SSCN = "SSID"; // 会话 Cookie 键名
    protected String SSCP =    "/"; // 会话 Cookie 路径
    protected int    SEXP =  86400; // 会话过期时间(秒)
    protected int    CEXP =     -1; // 过期 Cookie (秒)


    @Override
    public void init(FilterConfig fc)
    throws ServletException {
        String fn;

        fn = fc.getInitParameter("request-name");
        if (fn != null) SSRN = fn;

        fn = fc.getInitParameter( "cookie-name");
        if (fn != null) SSCN = fn;

        fn = fc.getInitParameter( "cookie-path");
        if (fn != null) SSCP = fn;

        fn = fc.getInitParameter("session-max-age");
        if (fn != null) SEXP = Integer.parseInt(fn);

        fn = fc.getInitParameter( "cookie-max-age");
        if (fn != null) CEXP = Integer.parseInt(fn);

        ATT = SessFilter.class.getName( ) +":"+ fc.getFilterName( ) +":INSIDE";
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse rsp, FilterChain flt)
    throws ServletException, IOException {
        if (Synt.declare(req.getAttribute(ATT), false)) {
            return; // 路径嵌套则不必再执行
        }

        HttpServletRequest  raq = (HttpServletRequest ) req;
        HttpServletResponse rzp = (HttpServletResponse) rsp;

        try {
            raq = new SessFiller(raq, rzp, this);
            raq.setAttribute    (ATT,      true);
            flt.doFilter        (raq, rzp);
        } finally {
            raq.removeAttribute (ATT);

            // 最终任务完成后对会话进行保存
            HttpSession ses  = raq.getSession (false);
            if (null != ses && ses instanceof Sesion) {
                try {
                    ( (Sesion) ses).close();
                } catch (HongsException ex) {
                    throw new ServletException(ex);
                }
            }
        }
    }

    @Override
    public void destroy() {

    }

}
