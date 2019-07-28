package io.github.ihongs.normal.serv;

import io.github.ihongs.Core;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.PasserHelper;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 去除关系符的冒号前缀
 * @deprecated 仅为兼容
 * @author Hongs
 */
public class XsymFilter extends ActionDriver {

    /**
     * 不包含的URL
     */
    private PasserHelper ignore = null;

    @Override
    public void init(FilterConfig config)
        throws ServletException
    {
        super.init(config);

        /**
         * 获取不包含的URL
         */
        this.ignore = new PasserHelper(
            config.getInitParameter("ignore-urls"),
            config.getInitParameter("attend-urls")
        );
    }

    @Override
    public void destroy()
    {
        super.destroy();

        ignore = null;
    }

    @Override
    public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
        throws IOException, ServletException
    {
        HttpServletResponse rsp = hlpr.getResponse();
        HttpServletRequest  req = hlpr.getRequest( );
        String act = ActionDriver.getRecentPath(req);
        String ctt = req.getContentType();

        do {
            if (ctt == null) {
                break;
            }

            /**
             * 非 JSON 已在 Dict 中兼容
             */
            ctt = ctt.split( ";", 2 )[0];
            if (! ctt.endsWith("/json")) {
                break;
            }

            /**
             * 检查当前动作是否可以忽略
             */
            if (ignore != null && ignore.ignore(act)) {
                break;
            }

            doChange(hlpr.getRequestData());
        }
        while (false);

        chain.doFilter(req , rsp);
    }

    private void doChange(Map rd) {
        Iterator<Map.Entry> it = rd.entrySet().iterator();
        Map rp = new HashMap();

        while ( it.hasNext() ) {
            Map.Entry  et = it.next();
            Object v = et.getValue( );
            Object k = et.getKey  ( );

            if (v instanceof Map) {
                doChange((Map) v);
            } else
            if (v instanceof Collection) {
                doChange((Collection) v);
            } else
            if (v instanceof Object [ ]) {
                doChange((Object [ ]) v);
            }

            // 去掉键的冒号前缀
            if (k instanceof String) {
                String  n = (String) k;
                if (n.length( )  ==  3
                &&  n.startsWith(":")) {
                    n = n.substring(1);
                    rp.put(n,v);
                    it.remove();
                }
            }
        }

        // 写入清理后的数据
        if (! rp.isEmpty()) {
            rd.putAll( rp );
        }
    }

    private void doChange(Collection rc) {
        for (Object v : rc) {
            if (v instanceof Map) {
                doChange((Map) v);
            } else
            if (v instanceof Collection) {
                doChange((Collection) v);
            } else
            if (v instanceof Object [ ]) {
                doChange((Object [ ]) v);
            }
        }
    }

    private void doChange(Object [ ] ra) {
        for (Object v : ra) {
            if (v instanceof Map) {
                doChange((Map) v);
            } else
            if (v instanceof Collection) {
                doChange((Collection) v);
            } else
            if (v instanceof Object [ ]) {
                doChange((Object [ ]) v);
            }
        }
    }

}
