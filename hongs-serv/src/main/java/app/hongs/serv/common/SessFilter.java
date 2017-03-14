package app.hongs.serv.common;

import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionDriver.FilterCheck;
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

    protected String SSRN = "SSID"; // 会话请求参数名称
    protected String SSCN = "SSID"; // 会话 Cookie 键名
    protected String SSCP =     ""; // 会话 Cookie 路径
    protected int    SEXP =  86400; // 会话过期时间(秒)
    protected int    CEXP =     -1; // 过期 Cookie (秒)

    private String      inside = null; // 过滤器标识
    private FilterCheck ignore = null; // 待忽略用例

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

        if (! SSCP.startsWith("/")) {
            SSCP = Core.BASE_HREF + "/" + SSCP;
        }

        inside = SessFilter.class.getName()+":"+fc.getFilterName()+":INSIDE";
        ignore = new FilterCheck(
            fc.getInitParameter("ignore-urls"),
            fc.getInitParameter("attend-urls")
        );
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse rsp, FilterChain flt)
    throws ServletException, IOException {
        HttpServletRequest  raq = (HttpServletRequest ) req;
        HttpServletResponse rzp = (HttpServletResponse) rsp;

        /**
         * 对于嵌套相同过滤, 不在内部重复执行;
         * 如外部设置了忽略, 则跳过忽略的路径.
         */
        if (inside != null &&  Synt.declare(raq.getAttribute(inside), false)
        ||  ignore != null && ignore.ignore(raq.getServletPath() ) ) {
            flt.doFilter(req, rsp);
            return;
        }

        try {
            raq = new SessFiller(raq, rzp, this);
            raq.setAttribute    ( inside , true);
            flt.doFilter        (raq, rzp);
        } finally {
            raq.removeAttribute ( inside );

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
        // Nothing todo.
    }

}
