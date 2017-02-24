package app.hongs.serv.common;

import app.hongs.Core;
import java.io.IOException;
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

    @Override
    public void init(FilterConfig fc) throws ServletException {
        // Nothing todo.
    }

    @Override
    public void doFilter(ServletRequest rxq, ServletResponse rxp, FilterChain fc) throws IOException, ServletException {
        HttpServletRequest  req = (HttpServletRequest ) rxq;
        HttpServletResponse rsp = (HttpServletResponse) rxp;
        String ref  = req.getHeader("Referer");
        String sche = req.getScheme( ) + "://";
        String host = req.getServerName();
        long   port = req.getServerPort();
        if (port != 80 && port != 443) {
            host += ":" + port;
        }
        if (null == ref || !ref.startsWith(sche + host + Core.BASE_HREF + "/")) {
            rsp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            rsp.getWriter().print("XSRF Access Forbidden!");
            return;
        }
        fc.doFilter(req, rsp);
    }

    @Override
    public void destroy() {
        // Nothing todo.
    }

}
