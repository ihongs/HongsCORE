package io.github.ihongs.normal.serv;

import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionDriver.PathPattern;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.Set;
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

    private String      inside = null; // 过滤器标识
    private PathPattern patter = null; // 待忽略用例
    private Set<String> allows = null; // 许可的域名

    @Override
    public void init(FilterConfig fc) throws ServletException {
        inside = XsrfFilter.class.getName()+":"+fc.getFilterName()+":INSIDE";
        allows = Synt.toTerms (fc.getInitParameter("allow-hosts"));
        patter = new ActionDriver.PathPattern(
            fc.getInitParameter("url-include"),
            fc.getInitParameter("url-exclude")
        );
        if (allows != null && allows.isEmpty()) {
            allows  = null;
        }
    }

    @Override
    public void doFilter(ServletRequest rxq, ServletResponse rxp, FilterChain fc) throws IOException, ServletException {
        HttpServletRequest  req = (HttpServletRequest ) rxq;
        HttpServletResponse rsp = (HttpServletResponse) rxp;

        /**
         * 对于嵌套相同过滤, 不在内部重复执行;
         * 如外部设置了忽略, 则跳过忽略的路径.
         */
        if ((inside != null &&  Synt  .declare(req.getAttribute(inside), false ))
        ||  (patter != null && !patter.matches(ActionDriver.getRecentPath(req)))) {
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
        if (ref != null) {
            Matcher mat = DOMAIN.matcher(ref);
        if (mat.find( )) {
            String domain = mat.group(1);
            String server = req.getServerName();
        if ((allows != null && allows.contains(domain) )
        || ( server != null && server. equals (domain))) {
            try {
                req.setAttribute(inside,true);
                fc . doFilter   (   rxq, rxp);
            } finally {
                req.removeAttribute( inside );
            }
            return;
        } } }

        rsp.sendError(HttpServletResponse.SC_FORBIDDEN, "What's your problem?");
    }

    @Override
    public void destroy() {
        // Nothing todo.
    }

}
