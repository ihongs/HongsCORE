package io.github.ihongs.combat.serv;

import static io.github.ihongs.action.serv.Access.JOBS;
import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.util.Remote;
import io.github.ihongs.util.Synt;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 维护命令
 *
 * <pre>
 *  调本地动作
 *    access.call 动作 --request 请求参数 ...
 *  调远程动作
 *    access.eval 动作 --request 请求参数 ...
 *  调远程命令
 *    access.exec 命令 参数1 参数2 ...
 * </pre>
 *
 * <p>许可配置, 在 default.properties 中:</p>
 * <pre>
 * core.access.token 远程认证口令, 客户端和服务端设置一致
 * core.access.serve 远程服务路径, 客户端设置, 默认为本地
 * core.access.allow 远程地址(IP), 服务端设置, 默认限本地
 * </pre>
 *
 * @author hongs
 */
@Combat("access")
public class Access {

    @Combat("exec")
    public static void exec(String[] args) throws CruxException {
        if (args.length == 0) {
            CombatHelper.ERR.get().println(
                  "Usage: COMBAT_NAME [ARG_0] [ARG_1] ..."
            );
            return;
        }

        CoreConfig cc = CoreConfig.getInstance( );
        String pow = cc.getProperty("core.powered.by", "hs" );
        String tok = cc.getProperty("core.access.token", "" );
        String url = cc.getProperty("core.access.serve", "" );
        if (url.isEmpty()) {
            url = Core.SERV_HREF + Core.SERV_PATH;
        if (url.isEmpty()) {
            url = "http://localhost:8080";
        }}
        url += "/common/access/exec"+Cnst.ACT_EXT;

        // 请求报头
        Map reh = Synt.mapOf(
            "Accept", "application/json,text/plain,*/*;q=0.8",
            "Authorization", "Bearer " + tok,
            "X-Requested-With", pow,
            // 超时
            ":SOCK-TIMEOUT", 0,
            ":WAIT-TIMEOUT", 0
        );

        // 请求参数
        Map rep = new HashMap(3);
        rep.put("cmd" , args[0]);
        if (args.length > 1 ) {
            rep.put("args", Arrays.copyOfRange(args, 1, args.length));
        }

        // 环境变量
        Map env = new HashMap(2);
        rep.put("env" , env );
        String   val  ;
        val = CombatHelper.ENV.get().get("COLUMNS");
        if (val != null && ! val.isEmpty()) {
            env.put("COLUMNS", val );
        }
        val = CombatHelper.ENV.get().get( "LINES" );
        if (val != null && ! val.isEmpty()) {
            env.put( "LINES" , val );
        }

        Remote.request(Remote.METHOD.POST, Remote.FORMAT.JSON, url, rep, reh, CombatHelper.OUT.get());
    }

    @Combat("eval")
    public static void eval(String[] args) throws CruxException {
        Map<String, Object> opts;
        opts = CombatHelper.getOpts(args ,
            "request:s", "context:s", "session:s", "cookies:s", "!A"
        );
        args = (String[ ]) opts.get( "" );

        if (args.length == 0) {
            CombatHelper.ERR.get().println(
                  "Usage: ACTION_NAME [--request DATA] [--cookies DATA] [--session DATA] [--context DATA]\r\n\t"
                + "DATA can be JSON or URL search string."
            );
            return;
        }

        CoreConfig cc = CoreConfig.getInstance( );
        String pow = cc.getProperty("core.powered.by", "hs" );
        String tok = cc.getProperty("core.access.token", "" );
        String url = cc.getProperty("core.access.serve", "" );
        if (url.isEmpty()) {
            url = Core.SERV_HREF + Core.SERV_PATH;
        if (url.isEmpty()) {
            url = "http://localhost:8080";
        }}
        url += "/common/access/eval"+Cnst.ACT_EXT;

        // 请求报头
        Map reh = Synt.mapOf(
            "Accept", "application/json,text/plain,*/*;q=0.8",
            "Authorization", "Bearer " + tok,
            "X-Requested-With", pow,
            // 超时
            ":SOCK-TIMEOUT", 0,
            ":WAIT-TIMEOUT", 0
        );

        // 请求参数
        Map rep = new HashMap(5);
        rep.put("act" , args[0]);
        if (opts.containsKey("request")) {
            rep.put("request", text((String) opts.get("request")));
        }
        if (opts.containsKey("cookies")) {
            rep.put("cookies", text((String) opts.get("cookies")));
        }
        if (opts.containsKey("session")) {
            rep.put("session", text((String) opts.get("session")));
        }
        if (opts.containsKey("context")) {
            rep.put("context", text((String) opts.get("context")));
        }

        Remote.request(Remote.METHOD.POST, Remote.FORMAT.JSON, url, rep, reh, CombatHelper.OUT.get());
    }

    @Combat("call")
    public static void call(String[] args) throws CruxException {
        Map<String, Object> opts;
        opts = CombatHelper.getOpts(args ,
            "request:s", "context:s", "session:s", "cookies:s", "!A"
        );
        args = (String[ ]) opts.get( "" );

        if (args.length == 0) {
            CombatHelper.ERR.get().println(
                  "Usage: ACTION_NAME [--request DATA] [--cookies DATA] [--session DATA] [--context DATA]\r\n\t"
                + "DATA can be JSON or URL search string."
            );
            return;
        }

        // 请求参数
        ActionHelper helper = new ActionHelper(
            data((String) opts.get("request")),
            data((String) opts.get("context")),
            data((String) opts.get("session")),
            data((String) opts.get("cookies"))
        );

        // 输出管道
        PrintStream ps = CombatHelper.OUT.get( );
        PrintWriter pw = new PrintWriter(  ps  );
        helper.updateOutput( ps , pw );

        // 将新动作助手对象放入全局以便跨层读取
        String cn = ActionHelper.class.getName();
        Core   co = Core.getInstance();
        Object ah = co.get(cn);

        try {
            co.set(cn, helper);
            ActionRunner.newInstance(helper, args[0]).doActing();
            helper.flush(/**/);
            ps.println( );
        } finally {
            if ( null  !=  ah) {
                co.set(cn, ah);
            } else {
                co.unset( cn );
            }
        }
    }

    @Combat("list")
    public static void list(String[] args) throws CruxException {
        for(Map.Entry<String, Core> et : JOBS.entrySet()) {
            String id = et.getKey(  );
            Core core = et.getValue();
            CombatHelper.println (id +"\t"+ core.get("!ACTION_NAME"));
        }
    }

    @Combat("kill")
    public static void kill(String[] args) throws CruxException {
        if (args.length == 0) {
            CombatHelper.println(
                  "Usage: access.exec access.kill TASKID"
            );
            return;
        }

        boolean force = args.length > 1 ? "--force".equals(args[1]) : false;

        String id = args[0];
        Core core = JOBS.get(id);
        if (null == core) {
            CombatHelper. println ("Not found");
            return;
        }

        Thread th = (Thread)core.get("!THREAD");
        if (null ==  th ) {
            CombatHelper. println ("No thread");
            core.put("!END", true);
            return;
        }

        // 先通知任务, 再通知线程
        if (! force && ! Synt.declare(core.get("!END"), false)) {
            core.put("!END", true);
        } else {
            th.interrupt( );
        }

        CombatHelper.println("OK");
    }

    @Combat("view")
    public static void view(String[] args) throws CruxException, InterruptedException {
        if (args.length == 0) {
            CombatHelper.println(
                  "Usage: access.exec access.view TASKID"
            );
            return;
        }

        String id = args[0];
        Core core = JOBS.get(id);
        if (null == core) {
            CombatHelper. println ("Not found");
            return;
        }

        // 提示输出切换
        PrintStream out = (PrintStream) core.get("!SYSTEM_OUT");
        PrintStream err = (PrintStream) core.get("!SYSTEM_ERR");
        if (out != null) {
            out.println("Output changed");
        } else
        if (err != null) {
            err.println("Output changed");
        }

        // 接管任务输出
        core.put("!SYSTEM_OUT", CombatHelper.OUT.get());
        core.put("!SYSTEM_ERR", CombatHelper.ERR.get());

        // 等待任务结束
        while (true) {
            Thread.sleep(500L);
            if (!JOBS.containsKey(id)) {
                break;
            }
        }
    }

    @Combat("test")
    public static void test(String[] args) throws CruxException, InterruptedException {
        if (args.length == 0) {
            CombatHelper.println(
                  "Usage: access.exec access.test TEXT SECS"
            );
            return;
        }

        CombatHelper.println (args[0]) ;

        int n = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        if (n > 0) {
            CombatHelper.progres(0, n) ;
        for(int i = 1 ; i <= n ; i ++) {
            Thread.sleep(1000) ;
            CombatHelper.progres(i, n) ;
        }
            CombatHelper.progres(/**/) ;
        }
    }

    private static Map data(String text) throws CruxException {
        return Synt.toMap(text(text));
    }

    private static String text(String text) throws CruxException {
        if (text == null ) {
            return   ""   ;
        }
        text = text.trim();

        if (text.startsWith("@")) {
            text = text.substring(1);
            if ( ! new File(text).isAbsolute()) {
                text = Core.CORE_PATH  +  text;
            }
            text = file(text);
            text = text.replaceAll("//.*?(\\r|\\n|$)", "$1");
            text = text.trim();
        }

        return text;
    }

    private static String file(String path) throws CruxException {
        try (
            BufferedReader br = new BufferedReader(
                new FileReader( new File (path) ) );
        ) {
            int            bn ;
            char[ ]        bs ;
            StringBuilder  sb = new StringBuilder();
            while ( true ) {
                bs = new char [1024];
                if((bn = br.read(bs)) < 0) {
                    break;
                }
                sb.append(bs, 0, bn);
            }
            return sb.toString();
        } catch (FileNotFoundException ex) {
            throw new CruxException(ex, "Can not find " + path);
        } catch (IOException ex) {
            throw new CruxException(ex, "Can not read " + path);
        }
    }

}
