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
 * sr-limit 复合条件数量限定, 如 2
 * sr-level 复合条件层数限定, 如 1
 * illegals 非法参数, 如 or,nr,ar
 *
 * @author Hongs
 */
public class VarsFilter extends ActionDriver {

    private URLPatterns patter = null;
    private Set illegals = null;
    private int sr_level = 0;
    private int sr_limit = 0;
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

        illegals = Synt.toSet  (config.getInitParameter("illegals")   );
        sr_level = Synt.declare(config.getInitParameter("sr-level"), 0);
        sr_limit = Synt.declare(config.getInitParameter("sr-limit"), 0);
        rn_limit = Synt.declare(config.getInitParameter("rn-limit"), 0);
    }

    @Override
    public void destroy()
    {
        super.destroy();

        patter   = null;
        illegals = null;
        sr_level = 0;
        sr_limit = 0;
        rn_limit = 0;
    }


    @Override
    public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
        throws IOException, ServletException
    {
        HttpServletResponse rsp = hlpr.getResponse();
        HttpServletRequest  req = hlpr.getRequest( );

        /**
         * 跳过内部动作代理, 如 AutoFilter
         */
        if (null != req.getAttribute(Cnst.ACTION_ATTR)) {
            chain.doFilter(req, rsp);
            return;
        }

        String act = ActionDriver.getRecentPath(req);
        if (null != act && ! patter.matches(act)) {
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
            int rn = Synt.declare( rd.get(Cnst.RN_KEY) , Cnst.RN_DEF );
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

        if (sr_limit != 0
        ||  sr_level != 0) {
            try {
                srCheck(rd, sr_limit, sr_level, 0,1);
            }
            catch (HongsException|HongsExemption ex) {
                hlpr.fault( ex );
                return;
            }
        }

        chain.doFilter(req, rsp);
    }

    private int srCheck(Object od, int limit, int level, int t, int l) throws HongsException {
        if (od == null) {
            return t;
        }
        if (od instanceof Map) {
            Map rd = (Map) od ;
            t = srCount(rd.get(Cnst.AR_KEY), limit, level, t, l);
            t = srCount(rd.get(Cnst.NR_KEY), limit, level, t, l);
            t = srCount(rd.get(Cnst.OR_KEY), limit, level, t, l);
            return t;
        } else {
            throw new HongsException(400, Cnst.AR_KEY+"/"+Cnst.NR_KEY+"/"+Cnst.OR_KEY+" item must be json object");
        }
    }

    private int srCount(Object od, int limit, int level, int t, int l) throws HongsException {
        if (od == null) {
            return t;
        }
            srLevel (level, l);
        if (od instanceof Map) {
            t = t + ((Map) od).size( );
            srLimit (limit, t);
            for(Object xd : ((Map) od).values()) {
                t = srCheck((Map) xd, limit, level, t, l + 1);
            }
            return t;
        } else
        if (od instanceof Collection) {
            t = t + ((Collection) od).size( );
            srLimit (limit, t);
            for(Object xd : ((Collection) od)) {
                t = srCheck((Map) xd, limit, level, t, l + 1);
            }
            return t;
        } else
        if (od instanceof Object [] ) {
            t = t + ((Object [] ) od).length ;
            srLimit (limit, t);
            for(Object xd : ((Object [] ) od)) {
                t = srCheck((Map) xd, limit, level, t, l + 1);
            }
            return t;
        } else {
            throw new HongsException(400, Cnst.AR_KEY+"/"+Cnst.NR_KEY+"/"+Cnst.OR_KEY+" must be json array or object");
        }
    }

    private void srLimit(int limit, int t) throws HongsException {
        if (limit != 0 && limit < t) {
            throw new HongsException(400, Cnst.AR_KEY+"/"+Cnst.NR_KEY+"/"+Cnst.OR_KEY+" can not exceed "+limit+" groups");
        }
    }

    private void srLevel(int level, int l) throws HongsException {
        if (level != 0 && level < l) {
            throw new HongsException(400, Cnst.AR_KEY+"/"+Cnst.NR_KEY+"/"+Cnst.OR_KEY+" can not exceed "+level+" layers");
        }
    }

}
