package io.github.ihongs.normal.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsCause;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
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
 * 版本兼容转换
 *
 * 参数 versions 取值:
 * 0.9.0 将 :xx 操作符换成 xx
 * 1.0.0 将 enum/menu 换成 enfo, create 增加 info, update 返回 size
 *
 * @deprecated 仅为兼容
 * @author Hongs
 */
public class VersFilter extends ActionDriver {

    /**
     * 不包含的URL
     */
    private URLPatterns ignoreUrls = null;
    private URLPatterns ignoreRefs = null;
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
        this.ignoreUrls = new URLPatterns(
            config.getInitParameter("url-exclude"),
            config.getInitParameter("url-include")
        );
        this.ignoreRefs = new URLPatterns(
            config.getInitParameter("ref-exclude"),
            config.getInitParameter("ref-include")
        );

        /**
         * 适配版本
         */
        Set vs  = Synt.toTerms(config.getInitParameter("versions"));
        if (vs != null) {
            if (vs.contains("0.9.0")) {
                level += 1;
            }
            if (vs.contains("1.0.0")) {
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
        if (act != null && ignoreUrls.matches(act)) {
            chain.doFilter(req, rsp);
            return;
        }
        String ref = /* Referer */getRefersPath(req);
        if (ref != null && ignoreRefs.matches(ref)) {
            chain.doFilter(req, rsp);
            return;
        }

        /**
         * 上传文件时可能会发生异常
         */
        Map rd;
        try {
            rd = hlpr.getRequestData();
        } catch (Throwable e) {
            if (e instanceof HongsCause) {
                hlpr.fault( (HongsCause) e );
            } else {
                hlpr.fault( e.getMessage() );
            }
            return;
        }

        int rf = 0;
        int nf = 0;

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
                if (ab.contains(".form")) {
                    ab.add(".fall");
                }
                if (ab.contains(".enfo")) {
                    rf =-1;
                } else
                if (ab.contains("!enum")
                ||  ab.contains(".enum")) {
                    ab.add(".enfo");
                    rf = 1;
                } else
                if (ab.contains("!menu")
                ||  ab.contains(".menu")) {
                    ab.add(".enfo");
                    rf = 2;
                }
                if (ab.contains(".nid" )) {
                    nf+= 1;
                }
                if (ab.contains(".cnt" )) {
                    nf+= 2;
                }
            }
        }

        chain.doFilter(req, rsp);

        if (2 == (2 & level)) {
            Map sd  = hlpr.getResponseData();
            if (sd != null) {
                if (sd.containsKey("enfo")) {
                    switch (rf) {
                        case 1:
                            sd.put("enum", sd.remove("enfo"));
                            break;
                        case 2:
                            sd.put("menu", sd.remove("enfo"));
                            break;
                        case 0:
                            sd.put("info", sd.remove("enfo"));
                            break;
                    }
                }
                if (1 != (1 & nf)
                &&  sd.containsKey("nid" )
                && !sd.containsKey("info")) {
                    Object id = sd.get("nid");
                    rd.put(Cnst.ID_KEY , id );
                    sd.put("info", rd );
                }
                if (2 != (2 & nf)
                &&  sd.containsKey("cnt" )
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