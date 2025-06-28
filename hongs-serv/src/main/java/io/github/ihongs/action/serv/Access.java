package io.github.ihongs.action.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CruxCause;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.CombatRunner;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 维护任务
 *
 * @author Hongs
 */
@Action("common/access")
public class Access {

    public static final Map<String, Core> JOBS = new HashMap();

    @Action("exec")
    public void exec(ActionHelper helper) throws CruxException {
        CoreConfig          cnf = CoreConfig.getInstance();
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();

        // 许可及IP白名单
        String  tok = cnf.getProperty("core.access.token");
        String  ia  = cnf.getProperty("core.access.allow");
        String  aut = req.getHeader  ("Authorization");
        String  ip  = ActionDriver.getClientAddr (req);
        Set     ias = Synt.toTerms( ia );
        if (aut != null) {
        if (aut.startsWith( "Bearer " )) {
            aut  = aut. substring ( 07 );
        } else {
            aut  = "" ;
        }}
        if (ias == null) {
            ias  = new  HashSet();
            ias.add(       "::1"       );
            ias.add(    "127.0.0.1"    );
            ias.add( "0:0:0:0:0:0:0:1" );
        }
        if (tok == null || tok.isEmpty() || ! tok.equals(aut)) {
            throw new CruxException( 400, "Illegal request!" );
        }
        if ( ! ias.contains(ip) && ! ias.contains("*") ) {
            throw new CruxException( 400, "Illegal request." );
        }

        Map    map = helper.getRequestData();
        String cmd = (String)map.get("cmd" );
        String act = Core.ACTION_NAME.get( );
        Thread job = Thread.currentThread( );
        String jid = Core.newIdentity();
        Core  core = Core.getInstance();

        core.put("!THREAD", job);
        core.put("!TASKID", jid);

        try {
            Core.ACTION_NAME.set(cmd);
            JOBS.put(jid , core);

            exec(helper, jid, cmd, req, rsp);
        } finally {
            Core.ACTION_NAME.set(act);
            JOBS.remove( jid );
        }
    }

    @Action("eval")
    public void eval(ActionHelper helper) throws CruxException {
        CoreConfig          cnf = CoreConfig.getInstance();
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();

        // 许可及IP白名单
        String  tok = cnf.getProperty("core.access.token");
        String  ia  = cnf.getProperty("core.access.allow");
        String  aut = req.getHeader  ("Authorization");
        String  ip  = ActionDriver.getClientAddr (req);
        Set     ias = Synt.toTerms( ia );
        if (aut != null) {
        if (aut.startsWith( "Bearer " )) {
            aut  = aut. substring ( 07 );
        } else {
            aut  = "" ;
        }}
        if (ias == null) {
            ias  = new  HashSet();
            ias.add(       "::1"       );
            ias.add(    "127.0.0.1"    );
            ias.add( "0:0:0:0:0:0:0:1" );
        }
        if (tok == null || tok.isEmpty() || ! tok.equals(aut)) {
            throw new CruxException( 400, "Illegal request!" );
        }
        if ( ! ias.contains(ip) && ! ias.contains("*") ) {
            throw new CruxException( 400, "Illegal request." );
        }

        Map    map = helper.getRequestData();
        String uri = (String)map.get("act" );
        String act = Core.ACTION_NAME.get( );
        Thread job = Thread.currentThread( );
        String jid = Core.newIdentity();
        Core  core = Core.getInstance();

        act = act + Cnst.ACT_EXT;
        core.put("!THREAD", job);
        core.put("!TASKID", jid);

        // 从参数提取参数
        helper.setRequestData(data(map, "request"));
        helper.setContextData(data(map, "context"));
        helper.setSessionData(data(map, "session"));
        helper.setCookiesData(data(map, "cookies"));

        try {
            Core.ACTION_NAME.set(act);
            JOBS.put(jid , core);

            eval(helper, jid, uri, req, rsp);
        } finally {
            Core.ACTION_NAME.set(act);
            JOBS.remove( jid );
        }
    }

    private void exec(ActionHelper helper, String jid, String cmd, HttpServletRequest req, HttpServletResponse rsp) {
        // 组织参数
        String[] args;
        List opts = Synt.asList(helper.getRequestData().get("args"));
        if (opts == null || opts.isEmpty()) {
            args = new String[1 /* no args */ ];
        } else {
            args = new String[1 + opts.size() ];
                int  i = 1 ;
            for(Object opt : opts) {
                args[i ++] = Synt.asString(opt);
            }
        }
        args[0] = cmd;

        // 环境变量
        Map env = Synt.asMap(helper.getRequestData().get("env"));
        if (env == null) {
            env = Synt.mapOf();
        }

        try {
            rsp.setCharacterEncoding("utf-8");
            rsp.setContentType ("text/plain");
            rsp.setHeader("Connection" , "keep-alive");
            rsp.setHeader("Cache-Control", "no-store");
            PrintStream out = new PrintStream(rsp.getOutputStream(), true);

            try {
                CombatHelper.OUT.set(out);
                CombatHelper.ERR.set(out);
                CombatHelper.ENV.set(env);

                CombatHelper.println("TaskID: "+ jid + "\n");

                CombatRunner.exec( args );
            }
            finally {
                CombatHelper.OUT.remove();
                CombatHelper.ERR.remove();
                CombatHelper.ENV.remove();
            }
        }
        catch (IOException e) {
            throw new CruxExemption(e);
        }
    }

    private void eval(ActionHelper helper, String jid, String act, HttpServletRequest req, HttpServletResponse rsp) {
        helper.setAttribute(Cnst.ACTION_ATTR, null);

        try {
            req.getRequestDispatcher("/" + act).include(req, rsp);
        } catch (ServletException | IOException ex) {
            if (ex.getCause() instanceof CruxCause) {
                CruxCause ez = (CruxCause) ex.getCause();
                String en = Integer.toHexString(ez.getErrno());
                Map map = new HashMap(4);
                map.put("ok" ,  false  );
                map.put("ern", "Ex"+en );
                map.put("err", ez.getMessage());
                map.put("msg", ez.getLocalizedMessage());
                helper.reply(map);
            } else {
                Map map = new HashMap(4);
                map.put("ok" ,  false  );
                map.put("ern", "Er500" );
                map.put("err", ex.getMessage());
                map.put("msg", ex.getLocalizedMessage());
                helper.reply(map);
            }
        }
    }

    private Map data(Map map, String key) throws CruxException {
        Object obj = map.get (key);
        if (obj == null) {
            return new HashMap (0);
        }
        try {
            return Synt.toMap(obj);
        }
        catch (ClassCastException e) {
            throw new CruxException(400, "Can not parse "+key);
        }
    }

}
