package io.github.ihongs.normal.serv;

import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.PasserHelper;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 跨站攻击拦截
 * @author Hongs
 */
public class XsrfFilter implements Filter {

    private static final Pattern DOMAIN = Pattern.compile("^(?:\\w+\\:)?//(.+?)(?:\\:\\d+)?/");

    private String       inside = null; // 过滤器标识
    private PasserHelper ignore = null; // 待忽略用例

    @Override
    public void init(FilterConfig fc) throws ServletException {
        inside = XsrfFilter.class.getName()+":"+fc.getFilterName()+":INSIDE";
        ignore = new PasserHelper(
            fc.getInitParameter("ignore-urls"),
            fc.getInitParameter("attend-urls")
        );
    }

    @Override
    public void doFilter(ServletRequest rxq, ServletResponse rxp, FilterChain fc) throws IOException, ServletException {
        HttpServletRequest  req = (HttpServletRequest ) rxq;
        HttpServletResponse rsp = (HttpServletResponse) rxp;

        /**
         * 对于嵌套相同过滤, 不在内部重复执行;
         * 如外部设置了忽略, 则跳过忽略的路径.
         */
        if ((inside != null &&  Synt.declare(req.getAttribute(inside), false ))
        ||  (ignore != null && ignore.ignore(ActionDriver.getRecentPath(req)))) {
            fc.doFilter(req, rsp);
            return;
        }

        // 如果有 X-Requested-With 表示请求来自 AJAX 或 APP
        String ret = req.getHeader("X-Requested-With" );
        if (ret != null && ret.length() != 0) {
            try {
                req.setAttribute(inside,true);
                fc . doFilter   (   rxq, rxp);
            } finally {
                req.removeAttribute( inside );
            }
            return;
        }

        // 提取到 Referer 并与当前请求的 URL 比对来判断同域
        String ref = req.getHeader("Referer");
        String dmn = req.getServerName( );
        if (ref != null && dmn != null) {
            Matcher mat = DOMAIN.matcher(ref);
        if (mat.find( ) && mat.group(1).equals(dmn)) {
            try {
                req.setAttribute(inside,true);
                fc . doFilter   (   rxq, rxp);
            } finally {
                req.removeAttribute( inside );
            }
            return;
        }}

        rsp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        rsp.getWriter().print("XSRF Access Forbidden!");
    }

    @Override
    public void destroy() {
        // Nothing todo.
    }

}
