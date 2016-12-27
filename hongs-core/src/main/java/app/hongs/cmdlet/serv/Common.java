package app.hongs.cmdlet.serv;

import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.cmdlet.CmdletHelper;
import app.hongs.cmdlet.CmdletRunner;
import app.hongs.cmdlet.anno.Cmdlet;
import app.hongs.CoreLoader.Mathod;
import app.hongs.util.Data;
import app.hongs.util.Tool;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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

    @Cmdlet("make-uid")
    public static void makeUID(String[] args) {
        String sid =  args.length > 0 ? args[0] : Core.SERVER_ID;
        String uid =  Core.getUniqueId( sid );
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
            if (i < j && j < 31) {
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
            if (i < j && j < 31) {
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
        Map<String, String> a = new TreeMap(new PropComparator());
        int i = 0, j;

        for (Map.Entry<String, Method> et : CmdletRunner.getCmdlets().entrySet()) {
            String k = et.getKey(  );
            Method v = et.getValue();
            a.put( k, v.getDeclaringClass().getName()+"."+v.getName() );
            j = k.length();
            if (i < j && j < 31) {
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
        Map<String, String> a = new TreeMap(new PropComparator());
        int i = 0, j;

        for (Map.Entry<String, Mathod> et : ActionRunner.getActions().entrySet()) {
            String k = et.getKey(  );
            Mathod v = et.getValue();
            a.put( k, v.toString() );
            j = k.length();
            if (i < j && j < 31) {
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
            System.out.println(
                  "Usage: common:view-serial serial/file/path\r\n\t"
                + "Just for CoreSerial or Collection object.");
        }

        try
        {
            File file = new File( args[ 0 ] );
            FileInputStream fis = new   FileInputStream(file);
          ObjectInputStream ois = new ObjectInputStream(fis );
            Data.dumps((Map)ois.readObject());
             ois.close();
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
        opts = CmdletHelper.getOpts (args ,
            "request:s", "context:s", "session:s", "cookies:s"
        );
        args = ( String[] ) opts.get( "" );

        if (args.length == 0) {
            System.err.println("Action name required!\r\nUsage: ACTION_NAME --request QUERY_STRING --cookies QUERY_STRING --session QUERY_STRING --context QUERY_STRING");
            return;
        }

        ActionHelper helper = Core.getInstance(ActionHelper.class);
        helper.setRequestData(data( (String) opts.get("request")));
        helper.setContextData(data( (String) opts.get("context")));
        helper.setSessionData(data( (String) opts.get("session")));
        helper.setCookiesData(data( (String) opts.get("cookies")));

        new app.hongs.action.ActionRunner(args[0], helper ).doAction();
        app.hongs.util.Data.dumps(helper.getResponseData());
    }

    @Cmdlet("call-action")
    public static void callAction(String[] args) throws HongsException {
        Map<String, Object> opts;
        opts = CmdletHelper.getOpts (args ,
            "request:s", "cookies:s"
        );
        args = ( String[] ) opts.get( "" );

        String act = args[0];
        String req = text((String) opts.get("request"));
        Map<String, String> cok = data((String) opts.get("cookies"));
        String hst = System.getProperty("server.host" , "localhost");
        String pot = System.getProperty("server.port" ,   "8080"   );
        String url = "http://" +hst+":"+pot+Core.BASE_HREF+"/"+act+".act";

        try {
            HttpURLConnection conn = (HttpURLConnection)new URL(url).openConnection();

            conn.setDoInput  ( true );
            conn.setDoOutput ( true );
            conn.setUseCaches( false);
            conn.setConnectTimeout(0);
            conn.setRequestMethod ("POST");

            conn.setRequestProperty("Accept", "application/json,text/html,*/*;q=0.8");
            conn.setRequestProperty("X-Requested-With","HongsCORE/0.3");

            // 放入 cookie
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> et : cok.entrySet()) {
                sb.append( URLEncoder.encode( et.getKey(  ), "UTF-8" ));
                sb.append("=");
                sb.append( URLEncoder.encode( et.getValue(), "UTF-8" ));
                sb.append(";");
            }
            conn.setRequestProperty("Cookie", sb.toString());

            PrintWriter   out = new PrintWriter(conn.getOutputStream());
            out.print(req);
            out.flush(   );

            BufferedReader in = new BufferedReader(
                          new InputStreamReader(conn.getInputStream()));
            String  line;
            while ((line = in.readLine()) != null) {
                System.out.print( line );
            }
        } catch (UnsupportedEncodingException ex ) {
            throw new HongsException.Common(ex);
        } catch (MalformedURLException ex) {
            throw new HongsException.Common(ex);
        } catch (IOException ex) {
            throw new HongsException.Common(ex);
        }
    }

    private static String text(String text) {
        if (text == null ) {
            return  "" ;
        }
        text = text.trim();

        if (text.startsWith("@")) {
            text = text.substring(1);
            if ( ! new File(text).isAbsolute()) {
                text = Core.CORE_PATH  +  text;
            }
            text = Tool.fetchFile (text);
            text = text.replaceAll("//.*?(\\r|\\n|$)", "$1");
            text = text.trim();
        }

        return text;
    }

    private static Map data(String text) {
        text = text(text);
        if (text.length() == 0 ) {
            return new HashMap();
        }

        Map data;
        if (text.startsWith("{") && text.endsWith("}")) {
            data = (  Map  ) Data.toObject(text);
        } else {
            data = ActionHelper.parseQuery(text);
        }

        return data;
    }

    private static class PropComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            return s1.compareTo(s2);
        }
    }

}
