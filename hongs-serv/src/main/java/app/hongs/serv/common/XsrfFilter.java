package app.hongs.serv.common;

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

    private static final Pattern DMN_PAT = Pattern.compile("^\\w+://([^/:]+)");

    @Override
    public void init(FilterConfig fc) throws ServletException {
        // Nothing todo.
    }

    @Override
    public void doFilter(ServletRequest rxq, ServletResponse rxp, FilterChain fc) throws IOException, ServletException {
        HttpServletRequest  req = (HttpServletRequest ) rxq;
        HttpServletResponse rsp = (HttpServletResponse) rxp;

        // 如果有 X-Requested-With 表示请求来自 AJAX 或 APP
        String ret = req.getHeader("X-Requested-With" );
        if (ret != null && ret.length() != 0) {
            fc.doFilter(rxq, rxp);
            return;
        }

        // 提取到 Referer 并与当前请求的 URL 比对来判断同域
        String ref = req.getHeader("Referer");
        String dmn = req.getServerName( );
        Matcher ma = DMN_PAT.matcher(ref);
        if (ma.find( )  &&  ma.group( 1 ).equals(dmn) ) {
            fc.doFilter(rxq, rxp);
            return;
        }

        rsp.setStatus(HttpServletResponse.SC_FORBIDDEN);
        rsp.getWriter().print("XSRF Access Forbidden!");
    }

    @Override
    public void destroy() {
        // Nothing todo.
    }

}
