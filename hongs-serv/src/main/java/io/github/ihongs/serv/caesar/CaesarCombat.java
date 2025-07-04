package io.github.ihongs.serv.caesar;

import static io.github.ihongs.serv.caesar.CaesarAction.JOBS;
import static io.github.ihongs.serv.caesar.CaesarAction.TASK;
import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CruxException;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.util.Inst;
import io.github.ihongs.util.Remote;
import io.github.ihongs.util.Synt;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * 维护命令
 *
 * <pre>
 *  调远程命令
 *    access.exec 命令 参数1 参数2 ...
 *  调远程脚本
 *    access.eval 命令 参数1 参数2 ...
 * </pre>
 *
 * <p>许可配置, 在 default.properties 中:</p>
 * <pre>
 * core.access.token 认证口令, 客户端和服务端设置一致
 * core.access.altar 接口地址, 客户端设置, 默认为本地
 * </pre>
 *
 * @author Hongs
 */
@Combat("caesar")
public class CaesarCombat {

    @Combat("exec")
    public static void exec(String[] args) throws CruxException {
        Map opts = CombatHelper.getOpts(
            args ,
            "TOKEN:s",
            "ALTAR:s",
            "!A","!U",
            "?Usage: caesar.exec COMBAT_NAME [ARG_0] [ARG_1] ..."
        );
        args = (String[]) opts.get ("");

        CoreConfig cc = CoreConfig.getInstance( );
        String pow = cc.getProperty("core.powered.by", "hs");
        String tok = Synt.declare(opts.get("TOKEN"), cc.getProperty("core.caesar.token", ""));
        String url = Synt.declare(opts.get("ALTAR"), cc.getProperty("core.caesar.altar", ""));
        if (url.isEmpty()) {
            url = Core.SERV_HREF + Core.SERV_PATH;
        if (url.isEmpty()) {
            url = "http://localhost:8080";
        }}
        url += "/caesar/exec" + Cnst.ACT_EXT;

        // 请求报头
        Map reh = Synt.mapOf(
            "Authorization"   , "Caesar " + tok ,
            "Accept"          , "text/plain,*/*;q=0.8",
            "Accept-Language" , Core.ACTION_LANG.get(),
            "X-Timezone"      , Core.ACTION_ZONE.get(),
            "X-Requested-With", pow ,
            // 超时
            ":SOCK-TIMEOUT", 0,
            ":WAIT-TIMEOUT", 0
        );

        // 请求参数
        Map rep = new HashMap(3);
        rep.put("args", args);

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
        Map opts = CombatHelper.getOpts(
            args ,
            "TOKEN:s",
            "ALTAR:s",
            "!A","!U",
            "?Usage: caesar.eval EVALET_PATH [ARG_0] [ARG_1] ..."
        );
        args = (String[]) opts.get ("");

        CoreConfig cc = CoreConfig.getInstance( );
        String pow = cc.getProperty("core.powered.by", "hs");
        String tok = Synt.declare(opts.get("TOKEN"), cc.getProperty("core.caesar.token", ""));
        String url = Synt.declare(opts.get("ALTAR"), cc.getProperty("core.caesar.altar", ""));
        if (url.isEmpty()) {
            url = Core.SERV_HREF + Core.SERV_PATH;
        if (url.isEmpty()) {
            url = "http://localhost:8080";
        }}
        url += "/caesar/eval" + Cnst.ACT_EXT;

        // 请求报头
        Map reh = Synt.mapOf(
            "Authorization"   , "Caesar " + tok ,
            "Accept"          , "text/plain,*/*;q=0.8",
            "Accept-Language" , Core.ACTION_LANG.get(),
            "X-Timezone"      , Core.ACTION_ZONE.get(),
            "X-Requested-With", pow ,
            // 超时
            ":SOCK-TIMEOUT", 0,
            ":WAIT-TIMEOUT", 0
        );

        // 请求参数
        Map rep = new HashMap(3);
        rep.put("args", args);

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

    @Combat("list")
    public static void list(String[] args) throws CruxException {
        int i = 0, j = 0, k = 0;
        String[][] list = new String[JOBS.size()][3];

        for(Map.Entry<String, Core> et : JOBS.entrySet()) {
            Core core = et.getValue();
            String id = et.getKey(  );
            String tn = (String) core.get(Core.ACTION_NAME.key());
            Long   tm = (Long  ) core.get(Core.ACTION_TIME.key());
            String ts = Inst.format(tm, "H:mm:ss");
            list[i++] = new String[] {id, ts, tn };

            j = Math.max(j, id.length());
            k = Math.max(k, ts.length());
        }

        // 按 ID 排序
        Arrays.sort(list, new Comparator<String[]>() {
            @Override
            public int compare(String[] a0, String[] a1) {
                return a0[0].compareTo(a1[0]);
            }
        });

        for(i = 0; i < list.length; i++) {
            StringBuilder sb = new StringBuilder();

            sb.append(list[i][0]);
            j = j - list[i][0].length( );
            if (j > 0) {
                sb.append(" ".repeat(j));
            }   sb.append(" ");

            sb.append(list[i][1]);
            k = k - list[i][1].length( );
            if (k > 0) {
                sb.append(" ".repeat(k));
            }   sb.append(" ");

            sb.append(list[i][2]);

            CombatHelper. println ( sb );
        }
    }

    @Combat("kill")
    public static void kill(String[] args) throws CruxException {
        if (args.length == 0) {
            CombatHelper.println(
                  "Usage: caesar.exec caesar.kill TASKID"
            );
            return;
        }

        String id = args[0];
        Core core = JOBS.get(id);
        if (null == core) {
            CombatHelper.println ( "Not found" );
            return;
        }

        String ek = Core.INTERRUPTED.key();
        Thread th = (Thread)core.get(TASK.key());
        if (null ==  th ) {
            CombatHelper.println ( "No thread" );
            core.put(ek , true);
            return;
        }

        if (! "--force".equals(args.length > 1 ? args[1] : null)
        &&  ! Synt.declare(core.get(ek), false)) {
            core.put(ek , true);
        } else {
            th.interrupt( );
        }

        CombatHelper.println("OK");
    }

    @Combat("view")
    public static void view(String[] args) throws CruxException, InterruptedException {
        if (args.length == 0) {
            CombatHelper.println(
                  "Usage: caesar.exec caesar.view TASKID"
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
        PrintStream out = (PrintStream) core.get(CombatHelper.OUT.key());
        PrintStream err = (PrintStream) core.get(CombatHelper.ERR.key());
        if (out != null) {
            out.println("Output changed");
        } else
        if (err != null) {
            err.println("Output changed");
        }

        // 接管任务输出
        core.put(CombatHelper.OUT.key(), CombatHelper.OUT.get());
        core.put(CombatHelper.ERR.key(), CombatHelper.ERR.get());

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
                  "Usage: caesar.exec caesar.test TEXT SECS"
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
            CombatHelper.printed(/**/) ;
        }
    }

}
