package io.github.ihongs.serv.filter;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CruxCause;
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
 * 0.9.0 将 :xx 操作符换成 xx, ni 换成 no, rg 换成 at, cq 和 sp 换成 se
 * 1.0.0 将 enum/menu 换成 enfo, create 增加 info, update 返回 size
 * 1.0.5 在 acount/amount 统计接口默认添加逆序 rb=!
 * 1.0.7 将 amount 转向 acount
 * 1.0.8 将 select 转向 recipe, 将单 id 的 search 转向 recite
 * 支持 url-include 和 url-exclude
 *
 * @deprecated 仅为兼容
 * @author Hongs
 */
public class VersFilter extends ActionDriver {

    /**
     * 不包含的URL
     */
    private PathPattern urlPatter = null;
    private PathPattern refPatter = null;
    private byte level = 0;

    private static final Pattern REF_PAT = Pattern.compile("^\\w+://([^/]+)(.*)");

    private static final int V090 = 1;
    private static final int V100 = 2;
    private static final int V105 = 4;
    private static final int V107 = 8;
    private static final int V108 = 16;

    @Override
    public void init(FilterConfig config)
        throws ServletException
    {
        super.init(config);

        /**
         * 获取不包含的URL
         */
        this.urlPatter = new PathPattern(
            config.getInitParameter("url-include"),
            config.getInitParameter("url-exclude")
        );
        this.refPatter = new PathPattern(
            config.getInitParameter("ref-include"),
            config.getInitParameter("ref-exclude")
        );

        /**
         * 适配版本
         */
        Set vs  = Synt.toTerms(config.getInitParameter("versions"));
        if (vs != null) {
            if (vs.contains("0.9.0")) {
                level += V090;
            }
            if (vs.contains("1.0.0")) {
                level += V100;
            }
            if (vs.contains("1.0.5")) {
                level += V105;
            }
            if (vs.contains("1.0.7")) {
                level += V107;
            }
            if (vs.contains("1.0.8")) {
                level += V108;
            }
        }
    }

    @Override
    public void destroy()
    {
        super.destroy();

        urlPatter = null;
        refPatter = null;
    }

    @Override
    public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
        throws IOException, ServletException
    {
        HttpServletResponse rsp = hlpr.getResponse();
        HttpServletRequest  req = hlpr.getRequest( );

        if (level == 0) {
            chain.doFilter(req, rsp);
            return;
        }

        /**
         * 跳过内部动作代理, 如 AutoFilter
         */
        if (null != req.getAttribute(Cnst.ACTION_ATTR)) {
            chain.doFilter(req, rsp);
            return;
        }

        String act = ActionDriver.getRecentPath(req);
        if (null != act && ! urlPatter.matches (act)) {
            chain.doFilter(req, rsp);
            return;
        }
        String ref = /* Referer */getRefersPath(req);
        if (null != ref && ! refPatter.matches (ref)) {
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
            if (e instanceof CruxCause) {
                hlpr.fault( (CruxCause) e);
            } else {
                hlpr.fault(e.getMessage());
            }
            return;
        }

        if (V090 == (V090 & level)) {
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

        if (V108 == (V108 & level)) {
            if (act != null ) {
                int p = act.lastIndexOf(".");
                if (p > 0) {
                    String c = act.substring(0 , p);
                    if (c.endsWith("/search")) {
                           c = act.substring(0 + p);
                        act  = act.substring(0 , p - 7) + "/recite" + c;
                        Object id = rd.get(Cnst.ID_KEY);
                        if (id != null && ! "".equals(id)
                        && (id instanceof String || id instanceof Number) ) {
                            Core.ACTION_NAME.set(act.substring( 1 ));
                            req.setAttribute(Cnst.ACTION_ATTR, null);
                            req.getRequestDispatcher(act).include(req, rsp);
                            return;
                        }
                    } else
                    if (c.endsWith("/select")) {
                           c = act.substring(0 + p);
                        act  = act.substring(0 , p - 7) + "/recipe" + c;
                        Core.ACTION_NAME.set(act.substring( 1 ));
                        req.setAttribute(Cnst.ACTION_ATTR, null);
                        req.getRequestDispatcher(act).include(req, rsp);
                        return;
                    }
                }
            }
        }

        if (V107 == (V107 & level)) {
            if (act != null ) {
                int p = act.lastIndexOf(".");
                if (p > 0) {
                    String c = act.substring(0 , p);
                    if (c.endsWith("/amount")) {
                           c = act.substring(0 + p);
                        act  = act.substring(0 , p - 7) + "/acount" + c;
                        Core.ACTION_NAME.set(act.substring( 1 ));
                        req.setAttribute(Cnst.ACTION_ATTR, null);
                        req.getRequestDispatcher(act).include(req, rsp);
                        return;
                    }
                }
            }
        }

        if (V105 == (V105 & level)) {
            if (act != null ) {
                int p = act.lastIndexOf(".");
                if (p > 0) {
                    String c = act.substring(0 , p);
                    if (c.endsWith("/acount" )
                    ||  c.endsWith("/amount")) {
                        if (rd.containsKey(Cnst.OB_KEY) == false) {
                            rd.put(Cnst.OB_KEY , Synt.setOf("!"));
                        }
                    }
                }
            }
        }

        int rf = 0;
        int nf = 0;

        if (V100 == (V100 & level)) {
            Set ab  = Synt.toTerms(rd.get(Cnst.AB_KEY));
            if (ab != null) {
                rd.put(Cnst.AB_KEY , ab);
                if (rd.containsKey(Cnst.ID_KEY)
                && (ab.contains("!enum")
                ||  ab.contains("!menu")
                ||  ab.contains("!info")) ) {
                    rd.put(Cnst.ID_KEY,"-");
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
                if (ab.contains(".id")) {
                    nf+= 1;
                }
                if (ab.contains(".rn")) {
                    nf+= 2;
                }
                if (ab.contains(".total")) {
                    nf+= 4;
                }
            }
        }

        chain.doFilter(req, rsp);

        if (V100 == (V100 & level)) {
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
                && !sd.containsKey("info")
                &&  sd.containsKey(Cnst.ID_KEY)) {
                    Object id = sd.get(Cnst.ID_KEY);
                    rd.put(Cnst.ID_KEY, id);
                    sd.put("info", rd);
                }
                if (2 != (2 & nf)
                && !sd.containsKey("size")
                &&  sd.containsKey(Cnst.RN_KEY)) {
                    Object ct = sd.get(Cnst.RN_KEY);
                    sd.put("size", ct);
                }
                if (4 != (4 & nf)
                &&  sd.containsKey("page")) {
                    Map page = (Map) sd.get("page");
                    if (page.containsKey("total")
                    && !page.containsKey("pages") ) {
                        page.put("pages", page.get("total"));
                    }
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
                    if  (  ref.startsWith(Core.SERV_PATH + "/" ) )
                    return ref.substring (Core.SERV_PATH.length());
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

            // 去掉键的冒号前缀, 更换 ni,rg,cq,sp
            if (k instanceof String) {
                String  n = (String) k;
                if (n.length() == 3
                &&  n.startsWith(":")) {
                    n = n.substring(1);
                    rp.put(n,v);
                    it.remove();
                } else
                if ("ni".equals(n)) {
                    n = Cnst.NO_REL;
                    rp.put(n,v);
                    it.remove();
                } else
                if ("rg".equals(n)) {
                    n = Cnst.AT_REL;
                    rp.put(n,v);
                    it.remove();
                } else
                if ("cq".equals(n)
                ||  "sp".equals(n)) {
                    n = Cnst.SE_REL;
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