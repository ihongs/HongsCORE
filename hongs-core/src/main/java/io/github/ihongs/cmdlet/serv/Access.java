package io.github.ihongs.cmdlet.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.util.Dawn;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 维护命令
 * @author hongs
 */
@Cmdlet("access")
public class Access {

    @Cmdlet("exec")
    public static void exec(String[] args) throws HongsException {
        if (args.length == 0) {
            CmdletHelper.ERR.get().println(
                  "Usage: CMDLET_NAME [ARG_0] [ARG_1] ..."
            );
            return;
        }

        // 请求参数
        Map rep = new HashMap();
        rep.put("cmd", args[0]);
        if (args.length > 1 ) {
            rep.put("args", Arrays.copyOfRange(args, 1, args.length));
        }
        String req = Dawn.toString (rep, true);

        // 命令接口
        String url = Core.SITE_HREF+Core.BASE_HREF + "/common/more/exec" +Cnst.ACT_EXT;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setDoInput        ( true );
            conn.setDoOutput       ( true );
            conn.setUseCaches      ( false);
            conn.setConnectTimeout (  0   );
            conn.setRequestMethod  ("POST");
            conn.setRequestProperty("Accept" , "application/json,text/html,*/*;q=0.8");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Requested-With", CoreConfig.getInstance().getProperty("core.powered.by"));

            String         ln;
            PrintStream    ps;
            PrintWriter    pw;
            BufferedReader br;

            pw = new PrintWriter(conn.getOutputStream());
            pw.print(req);
            pw.flush();
            pw.close();

            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            ps = CmdletHelper.OUT.get();
            while ( (ln = br.readLine()) != null ) {
                ps.print( ln );
            }   ps.println(  );
        } catch (UnsupportedEncodingException ex ) {
            throw  new  HongsException(ex);
        } catch (MalformedURLException ex) {
            throw  new  HongsException(ex);
        } catch (IOException ex) {
            throw  new  HongsException(ex);
        }
    }

    @Cmdlet("eval")
    public static void eval(String[] args) throws HongsException {
        Map<String, Object> opts;
        opts = CmdletHelper.getOpts(args ,
            "request:s", "context:s", "session:s", "cookies:s", "!A"
        );
        args = (String[ ]) opts.get( "" );

        if (args.length == 0) {
            CmdletHelper.ERR.get().println(
                  "Usage: ACTION_NAME [--request DATA] [--cookies DATA] [--session DATA] [--context DATA]\r\n\t"
                + "DATA can be JSON or URL search string."
            );
            return;
        }

        // 请求参数
        Map rep = new HashMap();
        rep.put("act", args[0]);
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
        String req = Dawn.toString (rep, true);

        // 动作接口
        String url = Core.SITE_HREF+Core.BASE_HREF + "/common/more/call" +Cnst.ACT_EXT;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setDoInput        ( true );
            conn.setDoOutput       ( true );
            conn.setUseCaches      ( false);
            conn.setConnectTimeout (  0   );
            conn.setRequestMethod  ("POST");
            conn.setRequestProperty("Accept" , "application/json,text/html,*/*;q=0.8");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Requested-With", CoreConfig.getInstance().getProperty("core.powered.by"));

            String         ln;
            PrintStream    ps;
            PrintWriter    pw;
            BufferedReader br;

            pw = new PrintWriter(conn.getOutputStream());
            pw.print(req);
            pw.flush();
            pw.close();

            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            ps = CmdletHelper.OUT.get();
            while ( (ln = br.readLine()) != null ) {
                ps.print( ln );
            }   ps.println(  );
        } catch (UnsupportedEncodingException ex ) {
            throw  new  HongsException(ex);
        } catch (MalformedURLException ex) {
            throw  new  HongsException(ex);
        } catch (IOException ex) {
            throw  new  HongsException(ex);
        }
    }

    @Cmdlet("call")
    public static void call(String[] args) throws HongsException {
        Map<String, Object> opts;
        opts = CmdletHelper.getOpts(args ,
            "request:s", "context:s", "session:s", "cookies:s", "!A"
        );
        args = (String[ ]) opts.get( "" );

        if (args.length == 0) {
            CmdletHelper.ERR.get().println(
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
        PrintStream ps = CmdletHelper.OUT.get( );
        PrintWriter pw = new PrintWriter(  ps  );
        helper.updateOutput( ps , pw );

        // 将新动作助手对象放入全局以便跨层读取
        String cn = ActionHelper.class.getName();
        Core   co = Core.getInstance();
        Object ah = co.got(cn);

        try {
            co.put(cn, helper);
            ActionRunner.newInstance(helper, args[0]).doActing();
            helper.responed( );
            ps.println( );
        } finally {
            if ( null  !=  ah) {
                co.put(cn, ah);
            } else {
                co.remove (cn);
            }
        }
    }

    private static Map data(String text) throws HongsException {
        text = text(text);
        if (text.length() == 0 ) {
            return new HashMap();
        }

        if (text.startsWith("<") && text.endsWith(">")) {
            throw  new UnsupportedOperationException("Unsupported xml: "+ text);
        } else
        if (text.startsWith("[") && text.endsWith("]")) {
            throw  new UnsupportedOperationException("Unsupported arr: "+ text);
        } else
        if (text.startsWith("{") && text.endsWith("}")) {
            return (  Map  ) Dawn.toObject(text);
        } else {
            return ActionHelper.parseQuery(text);
        }
    }

    private static String text(String text) throws HongsException {
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

    private static String file(String path) throws HongsException {
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
            throw  new  HongsException("Can not find " + path, ex);
        } catch (IOException ex) {
            throw  new  HongsException("Can not read " + path, ex);
        }
    }

}
