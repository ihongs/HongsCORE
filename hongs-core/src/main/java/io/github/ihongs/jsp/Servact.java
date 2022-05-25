package io.github.ihongs.jsp;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 动作代理
 * 需要覆盖 service(HttpServletRequest, HttpServletResponse)
 * 建议采用 Filter 而非 Servlet, 避免同级和下级目录也被接管.
 * @author Kevin
 */
abstract public class Servact extends ActionDriver {

    protected String ACT_DIR = null;
    protected String ACT_EXT = null;

    private void init() {
        if (ACT_EXT == null) {
            ACT_EXT  = Cnst.ACT_EXT;
        }

        // 从注解提取资源前缀
        if (ACT_DIR == null) {
            WebFilter  fil  = this.getClass().getAnnotation(WebFilter.class );
            if (fil != null) {
                String [ ] val;
                    val  = fil.value();
                if (val == null || val.length == 0) {
                    val  = fil.urlPatterns();
                }
                if (val != null && val.length != 0) {
                    if (val [0] . endsWith( "/*" )) {
                        ACT_DIR = val[0].substring( 0, val[0].length() - 1 );
                    }
                }
            }

            WebServlet srv = this.getClass().getAnnotation(WebServlet.class);
            if (srv != null) {
                String [ ] val;
                    val  = srv.value();
                if (val == null || val.length == 0) {
                    val  = srv.urlPatterns();
                }
                if (val != null && val.length != 0) {
                    if (val [0] . endsWith( "/*" )) {
                        ACT_DIR = val[0].substring( 0, val[0].length() - 1 );
                    }
                }
            }
        }
    }

    @Override
    public void init( FilterConfig conf) throws ServletException {
        super.init(conf);
         this.init();
    }

    @Override
    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);
         this.init();
    }

    @Override
    protected void doFilter(Core core, ActionHelper hlpr, FilterChain next)
    throws ServletException, IOException {
        HttpServletRequest  req = hlpr.getRequest ();
        HttpServletResponse rsp = hlpr.getResponse();
        String url = ActionDriver.getRecentPath(req);
        if (url.  endsWith(ACT_EXT)
        &&  url.startsWith(ACT_DIR)
        && !getHandle(url).contains("/")) {
            this.service (req, rsp);
        } else {
            next.doFilter(req, rsp);
        }
    }

    @Override
    protected void doAction(Core core, ActionHelper hlpr)
    throws ServletException, IOException {
        HttpServletRequest  req = hlpr.getRequest ();
        HttpServletResponse rsp = hlpr.getResponse();
        String url = ActionDriver.getRecentPath(req);
        if (url.  endsWith(ACT_EXT)
        &&  url.startsWith(ACT_DIR)
        && !getHandle(url).contains("/")) {
            this.service (req, rsp);
        } else {
            rsp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Unsupported action url: " + url);
        }
    }

    @Override
    abstract public void service(HttpServletRequest req, HttpServletResponse rsp);

    public String getSource() {
       return ACT_DIR.substring(1 , ACT_DIR.length() - 1);
    }

    public String getHandle(String url) {
       return url.substring( ACT_DIR.length()
            , url.length() - ACT_EXT.length() );
    }

    public String getHandle(HttpServletRequest req) {
       return getHandle(ActionDriver.getRecentPath(req));
    }

}
