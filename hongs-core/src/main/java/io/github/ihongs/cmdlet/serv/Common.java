package io.github.ihongs.cmdlet.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.CmdletRunner;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.CoreRoster.Mathod;
import io.github.ihongs.util.Dawn;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;

/**
 * 常规服务
 * @author Hongs
 */
@Cmdlet("common")
public class Common {

    @Cmdlet("drop-dir")
    public static void dropDir(String[] args) {
        new Dirs(args[0]).rmdirs();
    }

    @Cmdlet("make-dir")
    public static void makeDir(String[] args) {
        new Dirs(args[0]).mkdirs();
    }

    @Cmdlet("make-uid")
    public static void makeUID(String[] args) {
        String sid =  args.length > 0 ? args[0] : Core.SERVER_ID;
        String uid =  Core.newIdentity( sid );
        System.out.println(uid);
    }

    @Cmdlet("show-env")
    public static void showENV(String[] args) {
        Map<String, String> a = new TreeMap(new PropComparator());
        Map<String, String> m = new HashMap(    System.getenv ());
        int i = 0, j;

        for (Map.Entry<String, String> et : m.entrySet()) {
            String k = et.getKey(  );
            String v = et.getValue();
            a.put(k, v);
            j = k.length();
            if (i < j && j < 39) {
                i = j;
            }
        }

        for (Map.Entry<String, String> n : a.entrySet()) {
            StringBuilder s = new StringBuilder();
            s.append(n.getKey());
            for (j = n.getKey( ).length( ); j < i; j ++) {
                s.append(" " );
            }   s.append("\t");
            s.append(n.getValue());
            System.out.println (s);
        }
    }

    @Cmdlet("show-properties")
    public static void showProperties(String[] args) {
        Map<String, String> a = new TreeMap( new  PropComparator());
        Map<String, String> m = new HashMap(System.getProperties());
        int i = 0, j;

        for (Map.Entry<String, String> et : m.entrySet()) {
            String k = et.getKey(  );
            String v = et.getValue();
            a.put(k, v);
            j = k.length();
            if (i < j && j < 39) {
                i = j;
            }
        }

        for (Map.Entry<String, String> n : a.entrySet()) {
            StringBuilder s = new StringBuilder();
            s.append(n.getKey());
            for (j = n.getKey( ).length( ); j < i; j ++) {
                s.append(" " );
            }   s.append("\t");
            s.append(n.getValue());
            System.out.println (s);
        }
    }

    @Cmdlet("show-cmdlets")
    public static void showCmdlets(String[] args) {
        Map<String, String> a = new TreeMap(new PathComparator('.'));
        int i = 0, j;

        for (Map.Entry<String, Method> et : CmdletRunner.getCmdlets().entrySet()) {
            String k = et.getKey(  );
            Method v = et.getValue();
            a.put( k, v.getDeclaringClass().getName()+"."+v.getName() );
            j = k.length();
            if (i < j && j < 39) {
                i = j;
            }
        }

        for (Map.Entry<String, String> n : a.entrySet()) {
            StringBuilder s = new StringBuilder();
            s.append(n.getKey());
            for (j = n.getKey( ).length( ); j < i; j ++) {
                s.append(" " );
            }   s.append("\t");
            s.append(n.getValue());
            System.out.println (s);
        }
    }

    @Cmdlet("show-actions")
    public static void showActions(String[] args) {
        Map<String, String> a = new TreeMap(new PathComparator('/'));
        int i = 0, j;

        for (Map.Entry<String, Mathod> et : ActionRunner.getActions().entrySet()) {
            String k = et.getKey(  );
            Mathod v = et.getValue();
            a.put( k, v.toString() );
            j = k.length();
            if (i < j && j < 39) {
                i = j;
            }
        }

        for (Map.Entry<String, String> n : a.entrySet()) {
            StringBuilder s = new StringBuilder();
            s.append(n.getKey());
            for (j = n.getKey( ).length( ); j < i; j ++) {
                s.append(" " );
            }   s.append("\t");
            s.append(n.getValue());
            System.out.println (s);
        }
    }

    @Cmdlet("view-serial")
    public static void viewSerial(String[] args) throws HongsException {
        if (args.length == 0) {
            System.err.println(
                  "Usage: common:view-serial serial/file/path\r\n\t"
                + "Just for CoreSerial or Collection object.");
            return;
        }

        File fio = new File(args[0]);
        try (
            FileInputStream fis = new   FileInputStream(fio);
          ObjectInputStream ois = new ObjectInputStream(fis);
        ) {
          CmdletHelper.preview(ois.readObject());
        }
        catch (ClassNotFoundException e)
        {
          throw new HongsException(0x10d8, e);
        }
        catch ( FileNotFoundException e)
        {
          throw new HongsException(0x10d6, e);
        }
        catch (           IOException e)
        {
          throw new HongsException(0x10d4, e);
        }
    }

    @Cmdlet("exec-action")
    public static void execAction(String[] args) throws HongsException {
        Map<String, Object> opts;
        opts = CmdletHelper.getOpts(args ,
            "request:s", "context:s", "session:s", "cookies:s", "!A"
        );
        args = (String[ ]) opts.get( "" );

        if (args.length == 0) {
            System.err.println(
                  "Usage: ACTION_NAME [--request DATA] [--cookies DATA] [--session DATA] [--context DATA]\r\n\t"
                + "DATA can be JSON or URL search string."
            );
            return;
        }

        ActionHelper helper = new ActionHelper(
            data((String) opts.get("request")),
            data((String) opts.get("context")),
            data((String) opts.get("session")),
            data((String) opts.get("cookies"))
        );

        String  target = args [ 0 ];
        String  action = args [ 0 ];
        int p = action.indexOf('!');
        if (p > 0) {
            target = action.substring( 1 + p );
            action = action.substring( 0 , p );
        if (ActionRunner.getActions().containsKey(target)) {
            action = target;
        }}

        // 放入全局以便跨层读取
        String cn = ActionHelper.class.getName( );
        Core   co = Core.getInstance();
        Object ah = co.got(cn);
        co.put ( cn , helper );

        try {
            helper.setAttribute(Cnst.ACTION_ATTR , target);
            new ActionRunner( helper, action ).doAction( );
            CmdletHelper.preview(helper.getResponseData());
        } finally {
            if (ah  !=  null ) {
                co.put(cn, ah);
            } else {
                co.remove( cn);
            }
        }
    }

    @Cmdlet("call-action")
    public static void callAction(String[] args) throws HongsException {
        Map<String, Object> opts;
        opts = CmdletHelper.getOpts(args ,
            "request:s", "cookies:s", "!A"
        );
        args = (String[ ]) opts.get( "" );

        if (args.length == 0) {
            System.err.println(
                  "Usage: ACTION_NAME [--request DATA] [--cookies DATA]"
                + "DATA can be JSON or URL search string."
            );
            return;
        }

        String req = text((String) opts.get("request"));
        String cok = cook((String) opts.get("cookies"));
        String url = Core.SITE_HREF + Core.BASE_HREF + "/" + args[0] + Cnst.ACT_EXT;

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setDoInput        ( true );
            conn.setDoOutput       ( true );
            conn.setUseCaches      ( false);
            conn.setConnectTimeout (  0   );
            conn.setRequestMethod  ("POST");
            conn.setRequestProperty("Cookie", cok);
            conn.setRequestProperty("Accept", "application/json,text/html,*/*;q=0.8" );
            conn.setRequestProperty("Content-Type", req.startsWith("{") && req.endsWith("}")
                                                    ? "application/json" : "application/x-www-form-urlencoded" );
            conn.setRequestProperty("X-Requested-With", CoreConfig.getInstance().getProperty("core.powered.by"));

            String         ln;
            PrintWriter    pw;
            BufferedReader br;

            pw = new PrintWriter   (                      conn.getOutputStream());
            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            pw.print(req);
            pw.flush(   );
            while ( (ln = br.readLine()) != null ) {
                System.out.print ( ln );
            }
        } catch (UnsupportedEncodingException ex ) {
            throw  new  HongsException(ex);
        } catch (MalformedURLException ex) {
            throw  new  HongsException(ex);
        } catch (IOException ex) {
            throw  new  HongsException(ex);
        }
    }

    private static String cook(String text) throws HongsException {
        try {
            Map<String, String> cd =  data( text );
            StringBuilder ck = new StringBuilder();
            for (Map.Entry<String, String> et : cd.entrySet()) {
                ck.append( URLEncoder.encode( et.getKey(  ), "UTF-8" ));
                ck.append("=");
                ck.append( URLEncoder.encode( et.getValue(), "UTF-8" ));
                ck.append(";");
            }
            return ck.toString();
        } catch (UnsupportedEncodingException ex) {
            throw  new  HongsException(ex);
        }
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

    private static Map data(String text) throws HongsException {
        text = text(text);
        if (text.length() == 0 ) {
            return new HashMap();
        }

        Map data;
        if (text.startsWith("{") && text.endsWith("}")) {
            data = (  Map  ) Dawn.toObject(text);
        } else
        if (text.startsWith("[") && text.endsWith("]")) {
            throw  new UnsupportedOperationException("Unsupported list: "+ text);
        } else
        if (text.startsWith("<") && text.endsWith(">")) {
            throw  new UnsupportedOperationException("Unsupported html: "+ text);
        } else {
            data = ActionHelper.parseQuery(text);
        }

        return data;
    }

    private static class Dirs {
        private final File dir;
        public Dirs(String dir) {
            this.dir = new File(dir);
        }
        public boolean mkdirs() {
            return this.dir.mkdirs();
        }
        public boolean rmdirs() {
            if (dir.exists ( )) {
                delete(dir);
                return true;
            }
            return false;
        }
        public void delete(File dir) {
            if (dir.isDirectory ( )) {
            for(File   one : dir.listFiles()) {
                delete(one);
            }}
            dir.delete(   );
        }
    }

    private static class PropComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            return s1.compareTo ( s2 );
        }
    }

    private static class PathComparator implements Comparator<String> {
        final char SP;
        public PathComparator(char sp) {
            SP  =  sp;
        }
        @Override
        public int compare(String s1, String s2) {
            int p1 = 0;
            int p2 = 0;
            while (true) {
                p1 = s1.indexOf(SP, p1);
                p2 = s2.indexOf(SP, p2);
                if (p1 == p2) {
                    if (p1 == -1) {
                        return s1.compareTo( s2 );
                    }
                  String s3 =  s1.substring(0,p1);
                  String s4 =  s2.substring(0,p2);
                    int cp  =  s3.compareTo( s4 );
                    if (cp !=  0) {
                        return cp;
                    }
                }
                if (p1 == -1) {
                   return -1;
                }
                if (p2 == -1) {
                   return  1;
                }
                p1 ++;
                p2 ++;
            }
        }
    }

}
