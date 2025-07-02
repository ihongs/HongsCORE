package io.github.ihongs.serv.caesar;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
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
 * <pre>
 *  调远程命令
 *    access/exec.act
 *  调远程脚本
 *    access/eval.act
 * </pre>
 *
 * <p>许可配置, 在 default.properties 中:</p>
 * <pre>
 * core.access.token 认证口令, 客户端和服务端设置一致
 * core.access.allow 来源(IP), 服务端设置, 默认限本地
 * </pre>
 *
 * @author Hongs
 */
@Action("caesar")
public class CaesarAction {

    public static final Map<String, Core> JOBS = new HashMap();

    /**
     * 任务线程
     */
    public static final Core.Valuable<Thread> TASK = new Core.Valuable("!THREAD") {
        @Override
        protected Thread initialValue () {
           return Thread.currentThread();
        }
    };

    /**
     * 执行任务命令
     * @param helper
     * @throws CruxException
     */
    @Action("exec")
    public void exec(ActionHelper helper) throws CruxException {
        CoreConfig          cnf = CoreConfig.getInstance();
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();

        // 许可及IP白名单
        String  tok = cnf.getProperty("core.caesar.token");
        String  ia  = cnf.getProperty("core.caesar.allow");
        String  aut = req.getHeader  ("Authorization");
        String  ip  = ActionDriver.getClientAddr (req);
        Set     ias = Synt.toTerms( ia );
        if (aut != null) {
        if (aut.startsWith( "Caesar " )) {
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
            throw new CruxException( 401, "Illegal request!" );
        }
        if ( ! ias.contains(ip) && ! ias.contains("*") ) {
            throw new CruxException( 403, "Illegal request." );
        }

        Thread job = Thread.currentThread();
        String jid = Core.newIdentity();
        Core  core = Core.getInstance();

        core.put(TASK.key( ), job);
        JOBS.put(jid , core);

        try {
            exec(helper,jid , req, rsp);
        } finally {
            JOBS.remove(jid);
        }
    }

    /**
     * 执行任务脚本
     * @param helper
     * @throws CruxException
     */
    @Action("eval")
    public void eval(ActionHelper helper) throws CruxException {
        CoreConfig          cnf = CoreConfig.getInstance();
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();

        // 许可及IP白名单
        String  tok = cnf.getProperty("core.caesar.token");
        String  ia  = cnf.getProperty("core.caesar.allow");
        String  aut = req.getHeader  ("Authorization");
        String  ip  = ActionDriver.getClientAddr (req);
        Set     ias = Synt.toTerms( ia );
        if (aut != null) {
        if (aut.startsWith( "Caesar " )) {
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
            throw new CruxException( 401, "Illegal request!" );
        }
        if ( ! ias.contains(ip) && ! ias.contains("*") ) {
            throw new CruxException( 403, "Illegal request." );
        }

        Thread job = Thread.currentThread();
        String jid = Core.newIdentity();
        Core  core = Core.getInstance();

        core.put(TASK.key( ), job);
        JOBS.put(jid , core);

        try {
            eval(helper,jid , req, rsp);
        } finally {
            JOBS.remove(jid);
        }
    }

    private void exec(ActionHelper helper, String jid, HttpServletRequest req, HttpServletResponse rsp) throws CruxException {
        Map rsd = helper.getRequestData();

        Map env = Synt.asMap(rsd.get("env"));
        if (env == null) {
            env = new HashMap(0);
        }

        List<String> arr = Synt.asList( rsd.get( "args" ) );
        if (arr == null || arr.isEmpty()) {
            throw new CruxException( 400, "args required" );
        }
        String cmd = arr.get (0);
        rsd.put("args", arr.subList( 1, arr.size() ) );
        String[] args = arr.toArray( new String [00] );

        try {
            rsp.setCharacterEncoding("utf-8");
            rsp.setContentType ("text/plain");
            rsp.setHeader("Connection" , "keep-alive");
            rsp.setHeader("Cache-Control", "no-store");

            helper.setAttribute( CaesarAction.class.getName()+":ID", jid );
            PrintStream out = new PrintStream(rsp.getOutputStream(), true);
            String act = Core.ACTION_NAME.get();

            try {
                Core.ACTION_NAME.set(cmd);
                CombatHelper.OUT.set(out);
                CombatHelper.ERR.set(out);
                CombatHelper.ENV.set(env);

                CombatRunner.exec( args );
            }
            finally {
                Core.ACTION_NAME.set(act);
                CombatHelper.OUT.remove();
                CombatHelper.ERR.remove();
                CombatHelper.ENV.remove();
            }
        }
        catch ( IOException ex ) {
            throw new CruxException( ex );
        }
    }

    private void eval(ActionHelper helper, String jid, HttpServletRequest req, HttpServletResponse rsp) throws CruxException {
        Map rsd = helper.getRequestData();

        Map env = Synt.asMap(rsd.get("env"));
        if (env == null) {
            env = new HashMap(0);
        }

        List<String> arr = Synt.asList( rsd.get( "args" ) );
        if (arr == null || arr.isEmpty()) {
            throw new CruxException( 400, "args required" );
        }
        String uri = arr.get (0);
        rsd.put("args", arr.subList( 1, arr.size() ) );

        try {
            rsp.setCharacterEncoding("utf-8");
            rsp.setContentType ("text/plain");
            rsp.setHeader("Connection" , "keep-alive");
            rsp.setHeader("Cache-Control", "no-store");

            helper.setAttribute( CaesarAction.class.getName()+":ID", jid );
            PrintStream out = new PrintStream(rsp.getOutputStream(), true);
            String act = Core.ACTION_NAME.get();

            try {
                Core.ACTION_NAME.set(uri);
                CombatHelper.OUT.set(out);
                CombatHelper.ERR.set(out);
                CombatHelper.ENV.set(env);

                req.getRequestDispatcher("/"+uri).include(req, rsp);
            }
            finally {
                Core.ACTION_NAME.set(act);
                CombatHelper.OUT.remove();
                CombatHelper.ERR.remove();
                CombatHelper.ENV.remove();
            }
        }
        catch ( IOException|ServletException ex ) {
            throw new CruxException( ex );
        }
    }

    /**
     * 中止执行任务
     * @param core  任务容器
     * @param force 中止线程
     */
    public static void kill(Core core, boolean force) {
        if (force) {
            Thread th = (Thread) core.get(TASK.key());
            if (th != null) {
                th.interrupt();
                return;
            }
            CoreLogger.error("Can not kill force: {}", core.get(Core.ACTION_NAME.key()));
        }

        core.put(Core.INTERRUPTED.key(), true);
    }

}
