package io.github.ihongs.normal.serv;

import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionDriver.PathPattern;
import io.github.ihongs.util.Synt;
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
 *
 * 设置参数:
 * request-attr 会话请求属性名称
 * request-name 会话请求参数名称
 *  cookie-name 会话 Cookie 键名
 *  cookie-path 会话 Cookie 路径
 *  cookie-max-age 过期 Cookie (秒)
 *  record-max-age 会话过期时间(秒)
 * 支持 url-include 和 url-exclude
 *
 * @author Hongs
 * @deprecated 改用 Jetty 的 SessionManager
 */
public class SessFilter implements Filter {

    protected String SSRA = ".ssid"; // 会话请求属性名称
    protected String SSRN = ".ssid"; // 会话请求参数名称
    protected String SSCN =  "SSID"; // 会话 Cookie 键名
    protected String SSCP =      ""; // 会话 Cookie 路径
    protected int    SSCX =      -1; // 过期 Cookie (秒)
    protected int    SSRX =   86400; // 会话过期时间(秒)

    private String      inside = null; // 过滤器标识
    private PathPattern patter = null; // 待忽略用例

    @Override
    public void init(FilterConfig fc)
    throws ServletException {
        String fn;

        fn = fc.getInitParameter("request-attr");
        if (fn != null) SSRA = fn;

        fn = fc.getInitParameter("request-name");
        if (fn != null) SSRN = fn;

        fn = fc.getInitParameter( "cookie-name");
        if (fn != null) SSCN = fn;

        fn = fc.getInitParameter( "cookie-path");
        if (fn != null) SSCP = fn;

        fn = fc.getInitParameter( "cookie-max-age");
        if (fn != null) SSCX = Integer.parseInt(fn);

        fn = fc.getInitParameter( "record-max-age");
        if (fn != null) SSRX = Integer.parseInt(fn);

        if (! SSCP.startsWith("/")) {
            SSCP = Core.SERV_PATH + "/" + SSCP;
        }

        inside = SessFilter.class.getName()+":"+fc.getFilterName()+":INSIDE";
        patter = new ActionDriver.PathPattern(
            fc.getInitParameter("url-include"),
            fc.getInitParameter("url-exclude")
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
        if ((inside != null &&  Synt  .declare(req.getAttribute(inside), false ))
        ||  (patter != null && !patter.matches(ActionDriver.getRecentPath(raq)))) {
            flt.doFilter(req, rsp);
            return;
        }

        try {
            raq = new SessAccess(raq, rzp, this);
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

/*
    <filter>
        <filter-name>SessFilter</filter-name>
        <filter-class>io.github.ihongs.normal.serv.SessFilter</filter-class>
        <init-param>
            <param-name>record-max-age</param-name>
            <param-value>604800</param-value>
        </init-param>
        <init-param>
            <param-name>cookie-max-age</param-name>
            <param-value>604800</param-value>
        </init-param>
        <init-param>
            <param-name>intend-urls</param-name>
            <param-value>
                /common/auth/*;/common/lang/*
            </param-value>
        </init-param>
        <init-param>
            <param-name>url-exclude</param-name>
            <param-value>
                *.js;*.css;*.png;*.gif;*.jpg;*.bmp
            </param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>SessFilter</filter-name>
        <url-pattern>/centra/*</url-pattern>
        <url-pattern>/centre/*</url-pattern>
        <url-pattern>/common/*</url-pattern>
        <url-pattern>/api/*</url-pattern>
    </filter-mapping>
*/
