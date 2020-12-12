package io.github.ihongs.normal.serv;

import io.github.ihongs.Core;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionDriver.URLPatterns;
import io.github.ihongs.util.Synt;
import java.io.File;
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
 * 反射脚本拦截
 * @author Hongs
 */
public class XrshFilter implements Filter {

    private String      inside = null; // 过滤器标识
    private URLPatterns ignore = null; // 待忽略用例
    private boolean     blocks = false ; // 屏蔽全部

    @Override
    public void init(FilterConfig fc) throws ServletException {
        inside = XsrfFilter.class.getName()+":"+fc.getFilterName()+":INSIDE";
        blocks = Synt.declare (fc.getInitParameter ("block-every") , false );
        ignore = new ActionDriver.URLPatterns(
            fc.getInitParameter("url-exclude"),
            fc.getInitParameter("url-include")
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
        if ((inside != null &&  Synt .declare(req.getAttribute(inside), false ))
        ||  (ignore != null && ignore.matches(ActionDriver.getRecentPath(req)))) {
            fc.doFilter(req, rsp);
            return;
        }

        /**
         * 文件缺失无需理会，将会自动转入 404
         */
        if (! blocks
        &&  ! new File(Core.BASE_PATH+ActionDriver.getRecentPath(req)).exists()) {
            fc.doFilter(req, rsp);
            return;
        }

        rsp.sendError(HttpServletResponse.SC_FORBIDDEN, "What's your problem?");
    }

    @Override
    public void destroy() {
        // Nothing todo.
    }

}
