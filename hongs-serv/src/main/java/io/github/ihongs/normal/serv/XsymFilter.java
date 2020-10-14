package io.github.ihongs.normal.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.PasserHelper;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 版本兼容
 *
 * 参数 versions 取值:
 * 20190728 将 :xx 操作符换成 xx
 * 20200906 将 enus 换成 enum 或 menu, create 返回 nid 而非 info 等
 *
 * @deprecated 仅为兼容
 * @author Hongs
 */
public class XsymFilter extends ActionDriver {

    /**
     * 不包含的URL
     */
    private PasserHelper ignoreUrls = null;
    private PasserHelper ignoreRefs = null;
    private byte level = 0;

    private static final Pattern REF_PAT = Pattern.compile("^\\w+://([^/]+)(.*)");

    @Override
    public void init(FilterConfig config)
        throws ServletException
    {
        super.init(config);

        /**
         * 获取不包含的URL
         */
        this.ignoreUrls = new PasserHelper(
            config.getInitParameter("ignore-urls"),
            config.getInitParameter("attend-urls")
        );
        this.ignoreRefs = new PasserHelper(
            config.getInitParameter("ignore-refs"),
            config.getInitParameter("attend-refs")
        );

        /**
         * 适配版本
         */
        Set vs  = Synt.toTerms(config.getInitParameter("versions"));
        if (vs != null) {
            if (vs.contains("20190728")) {
                level += 1;
            }
            if (vs.contains("20200906")) {
                level += 2;
            }
        }
    }

    @Override
    public void destroy()
    {
        super.destroy();

        ignoreUrls = null;
        ignoreRefs = null;
    }

    @Override
    public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
        throws IOException, ServletException
    {
        HttpServletResponse rsp = hlpr.getResponse();
        HttpServletRequest  req = hlpr.getRequest( );

        /**
         * 检查当前动作是否可以忽略
         */
        if (0 == level) {
            chain.doFilter(req, rsp);
            return;
        }
        String act = ActionDriver.getRecentPath(req);
        if (act != null && ignoreUrls.ignore(act)) {
            chain.doFilter(req, rsp);
            return;
        }
        String ref = /* Referer */getRefersPath(req);
        if (ref != null && ignoreRefs.ignore(ref)) {
            chain.doFilter(req, rsp);
            return;
        }

        int rf = 0;
        Map rd = hlpr.getRequestData();

        if (1 == (1 & level)) {
            /**
             * 非 JSON 已在 Dict 中兼容
             */
            String ct = req.getContentType();
            if (ct != null) {
                ct  = ct.split (";" , 2) [0];
                if (ct.endsWith("/json")) {
                    doChange(rd);
                }
            }
        }

        if (2 == (2 & level)) {
            Set ab  = Synt.toTerms(rd.get(Cnst.AB_KEY));
            if (ab != null) {
                rd.put(Cnst.AB_KEY , ab);
                if (rd.containsKey(Cnst.ID_KEY)
                && (ab.contains("!enum")
                ||  ab.contains("!menu")
                ||  ab.contains("!info")) ) {
                    rd.put(Cnst.ID_KEY, "");
                }
                if (ab.contains("!info")) {
                    ab.add(".info");
                }
                if (ab.contains("!enum")
                ||  ab.contains(".enum")) {
                    ab.add(".enus");
                    rf = 1;
                } else
                if (ab.contains("!menu")
                ||  ab.contains(".menu")) {
                    ab.add(".enus");
                    rf = 2;
                } else
                if (ab.contains(".enus") == false) {
                    rf = 3;
                }
            }
        }

        chain.doFilter(req, rsp);

        if (2 == (2 & level)) {
            Map sd  = hlpr.getResponseData();
            if (sd != null) {
                if (sd.containsKey("enus")) {
                    switch (rf) {
                        case 1:
                            sd.put("enum", sd.remove("enus"));
                            break;
                        case 2:
                            sd.put("menu", sd.remove("enus"));
                            break;
                        case 3:
                            sd.put("info", sd.remove("enus"));
                            break;
                    }
                }
                if (sd.containsKey("nid" )
                && !sd.containsKey("info")) {
                    Object id = sd.get("nid");
                    rd.put(Cnst.ID_KEY , id );
                    sd.put("info", rd );
                }
                if (sd.containsKey("cnt" )
                && !sd.containsKey("size")) {
                    Object ct = sd.get("cnt");
                    sd.put("size", ct );
                }
            }
        }
    }

    private String getRefersPath(HttpServletRequest req) {
        String hst = req.getHeader ("Host"   );
        String ref = req.getHeader ("Referer");
        if (hst != null && ref != null) {
            Matcher mat = REF_PAT.matcher(ref);
            if (mat.matches()) {
                String  reh;
                reh  =  mat.group(1);
                ref  =  mat.group(2);
                if (hst.equals(reh)) {
                    // 仅适配当前应用, 且去除应用路径
                    if  (  ref.startsWith(Core.BASE_HREF + "/" ) )
                    return ref.substring (Core.BASE_HREF.length());
                }
            }
        }
        return null;
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