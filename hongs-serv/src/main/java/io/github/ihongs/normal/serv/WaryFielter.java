package io.github.ihongs.normal.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsCause;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 请求参数限定
 *
 * 规避单页读取过多导致单次吞吐过多,
 * 规避使用过量复合条件造成复杂查询.
 *
 * 设置参数:
 * rn-limit 列表查询数量限定, 如 100
 * xr-limit 复合条件数量限定, 如 1
 * xr-level 复合条件层级限定, 如 1
 * illegals 非法参数, 如 or,nr,ar
 *
 * @author Hongs
 */
public class WaryFielter extends ActionDriver {

    private URLPatterns patter = null;
    private Set illegals = null;
    private int xr_level = 0;
    private int xr_limit = 0;
    private int rn_limit = 0;

    @Override
    public void init(FilterConfig config)
        throws ServletException
    {
        super.init(config);

        patter = new URLPatterns(
            config.getInitParameter("url-include"),
            config.getInitParameter("url-exclude")
        );

        illegals = Synt.toSet(config.getInitParameter("illegals"));
        xr_level = Synt.asInt(config.getInitParameter("xr-level"));
        xr_limit = Synt.asInt(config.getInitParameter("xr-limit"));
        rn_limit = Synt.asInt(config.getInitParameter("rn-limit"));
    }

    @Override
    public void destroy()
    {
        super.destroy();

        patter   = null;
        illegals = null;
        xr_level = 2;
        xr_limit = 0;
        rn_limit = 0;
    }


    @Override
    public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
        throws IOException, ServletException
    {
        HttpServletResponse rsp = hlpr.getResponse();
        HttpServletRequest  req = hlpr.getRequest( );

        String act = ActionDriver.getRecentPath(req);
        if (act != null && ! patter.matches(act)) {
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

        if (rn_limit != 0) {
            int rn = Synt.asInt(rd.get(Cnst.RN_KEY));
            if (rn < 1 || rn > rn_limit) {
                rsp.setStatus(400);
                hlpr.fault(Cnst.RN_KEY + " must be 1 to " + rn_limit );
                return;
            }
        }

        if (illegals != null && !illegals.isEmpty()) {
            Set ls = new HashSet(illegals);
            Set ks = rd.keySet();
                ls.retainAll(ks);
            if (! ls.isEmpty() ) {
                rsp.setStatus(400);
                hlpr.fault("Illegal parameters: "+Syno.concat(",",ls));
                return;
            }
        }

        if (xr_limit != 0
        ||  xr_level != 0) {
            try {
                xrCheck(rd, xr_limit, xr_level, 0,1);
            }
            catch (HongsException|HongsExemption ex) {
                hlpr.fault( ex );
            }
        }

        chain.doFilter(req, rsp);
    }

    private int xrCheck(Object od, int limit, int level, int t, int l) throws HongsException {
        if (od == null) {
            return t;
        }
        if (od instanceof Map) {
            Map rd = (Map) od ;
            xrLevel (level, l);
            t = xrCount(rd.get(Cnst.AR_KEY), limit, level, t, l);
            t = xrCount(rd.get(Cnst.NR_KEY), limit, level, t, l);
            t = xrCount(rd.get(Cnst.OR_KEY), limit, level, t, l);
            return t;
        } else {
            throw new HongsException(400, Cnst.AR_KEY+"/"+Cnst.NR_KEY+"/"+Cnst.OR_KEY+" item must be json object");
        }
    }

    private int xrCount(Object od, int limit, int level, int t, int l) throws HongsException {
        if (od == null) {
            return t;
        }
        if (od instanceof Map) {
            t = t + ((Map) od).size( );
            xrLimit (limit, t);
            for(Object xd : ((Map) od).values()) {
                t = xrCheck((Map) xd, limit, level, t, l + 1);
            }
            return t;
        } else
        if (od instanceof Collection) {
            t = t + ((Collection) od).size( );
            xrLimit (limit, t);
            for(Object xd : ((Collection) od)) {
                t = xrCheck((Map) xd, limit, level, t, l + 1);
            }
            return t;
        } else
        if (od instanceof Object [] ) {
            t = t + ((Object [] ) od).length ;
            xrLimit (limit, t);
            for(Object xd : ((Object [] ) od)) {
                t = xrCheck((Map) xd, limit, level, t, l + 1);
            }
            return t;
        } else {
            throw new HongsException(400, Cnst.AR_KEY+"/"+Cnst.NR_KEY+"/"+Cnst.OR_KEY+" must be json array or object");
        }
    }

    private void xrLimit(int limit, int t) throws HongsException {
        if (limit != 0 && limit < t) {
            throw new HongsException(400, Cnst.AR_KEY+"/"+Cnst.NR_KEY+"/"+Cnst.OR_KEY+" can not exceed "+limit+" items" );
        }
    }

    private void xrLevel(int level, int l) throws HongsException {
        if (level != 0 && level < l) {
            throw new HongsException(400, Cnst.AR_KEY+"/"+Cnst.NR_KEY+"/"+Cnst.OR_KEY+" can not exceed "+level+" layers");
        }
    }

}
