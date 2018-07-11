package foo.hongs.cmdlet.serv;

import foo.hongs.Core;
import foo.hongs.CoreLogger;
import foo.hongs.HongsException;
import foo.hongs.HongsExemption;
import foo.hongs.cmdlet.CmdletHelper;
import foo.hongs.cmdlet.CmdletRunner;
import foo.hongs.cmdlet.anno.Cmdlet;
import foo.hongs.db.DB;
import foo.hongs.util.Synt;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 管理命令
 * @author Hongs
 */
@Cmdlet("system")
public class SystemCmdlet {

    private static final Pattern filNamPatt = Pattern.compile("^(.+?\\.)?(.*?)\\.(sql|cmd\\.xml)$"); // 文件

    private static final Pattern sqlDnmPatt = Pattern.compile("--\\s*(DB|DT):\\s*(\\S+)");           // 配置
    private static final Pattern sqlCmnPatt = Pattern.compile("--.*?[\r\n]");                        // 注释

    private static final Pattern timVarPatt = Pattern.compile("\\{\\{(.+?)(\\|(.+?))?\\}\\}");
    private static final Pattern timFmtPatt = Pattern.compile("([\\-\\+])(\\d+Y)?(\\d+M)?(\\d+w)?(\\d+d)?(\\d+h)?(\\d+m)?(\\d+s)?$");
    private static final Pattern tinFmtPatt = Pattern.compile("^((\\d{2,4}/\\d{1,2}/\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,2})|(\\d{2,4}/\\d{1,2}/\\d{1,2})|(\\d{1,2}:\\d{1,2}:\\d{1,2})|(\\d{1,2}:\\d{1,2}))$");

    /**
     * 维护命令
     * @param args
     * @throws HongsException
     */
    @Cmdlet( "serve" )
    public static void serve(String[] args) throws HongsException {
        List<String> argz = Synt.listOf((Object[]) args);
                     argz.add( 0, "serve" );
        exec( argz.toArray(new String[0]) );
    }

    /**
     * 设置命令
     * @param args
     * @throws HongsException
     */
    @Cmdlet( "setup" )
    public static void setup(String[] args) throws HongsException {
        List<String> argz = Synt.listOf((Object[]) args);
                     argz.add( 0, "setup" );
        exec( argz.toArray(new String[0]) );
    }

    /**
     * 维护命令
     * @param args
     * @throws HongsException
     */
    @Cmdlet("__main__")
    public static void exec (String[] args) throws HongsException {
        Map<String, Object> opts;
        opts = CmdletHelper.getOpts ( args, "date:s" );
        args = ( String[] ) opts.remove("");
        if ( 0 == args.length ) {
            System.err.println("Serve name required!");
            return;
        }

        String fn = args[0];
        File   fu = new File(fn);
        Date   dt = new Date(  );

        // 日期参数
        if (opts.containsKey("date")) {
            dt = getTin((String) opts.get("date"), dt);
        }

        // 相对且不存在则看作内部目录
        if (! fu.isAbsolute() && ! fu.exists()) {
            fn = Core.CORE_PATH
               + File.separator + "bin"
               + File.separator +  fn ;
            fu = new File(fn);
        }

        // 获取目录下全部待执行的文件
        List<File> fxs = new ArrayList();
        if (! fu.isFile()) {
            File[] fus = fu. listFiles();
            for (File fo : fus) {
                if (! fo.isFile()
                || fo.getName().startsWith(".")) {
                    continue;
                }
                fxs.add (fo);
            }
            Collections.sort(fxs, new Sorter( ));
        } else {
                fxs.add (fu);
        }

        Logger lgr = new Logger();
        String act = Core.ACTION_NAME.get();
        long   now = Core.ACTION_TIME.get();

        // 逐个执行
        for (File fo : fxs) {
            Matcher met = filNamPatt.matcher(fo.getName());
            if (  ! met.matches() ) {
                continue;
            }

            String  ent = met.group(2).trim();
            String  ext = met.group(3).trim();

            try {
                if ("cmd.xml".equals(ext)) {
                    SystemCmdlet.runCmd(dt , fo , lgr);
                } else
                if (/**/"sql".equals(ext)) {
                    runSql(dt , fo , ent);
                }
            } catch (Exception ex) {
                lgr.error(ex);
            } catch (Error/**/ ex) {
                lgr.error(ex);
            } finally {
                // 放回名称和开始时间
                // 避免时间或日志模糊
                Core.ACTION_NAME.set(act);
                Core.ACTION_TIME.set(now);
            }
        }
    }

    private static void runCmd(Date dt, File fo, Logger lg) throws HongsException {
        Document doc;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbn = dbf.newDocumentBuilder();
            doc = dbn.parse(fo);
        } catch (ParserConfigurationException ex) {
            throw new HongsException.Common(ex);
        } catch (SAXException ex) {
            throw new HongsException.Common(ex);
        } catch ( IOException ex) {
            throw new HongsException.Common(ex);
        }

        NodeList l = doc.getChildNodes ();
        for (int i = 0; i < l.getLength(); i ++) {
            Node n = l.item(i);
            if ( n.getNodeType() != Node.ELEMENT_NODE ) {
                continue;
            }

            Element e = ( Element ) n ;
            String  t = e.getTagName();

            if ("cmdlet".equals(t)) {
                runCmd( e, dt, lg );
            } else
            if ("action".equals(t)) {
                runAct( e, dt, lg );
            } else {
                throw new HongsException.Common("Wrong tagName: " + t);
            }
        }
    }

    private static void runSql(Date dt, File fo, String fn)
            throws HongsException {
        // 读取代码
        String sql;
        byte[] buf;
        try {
            FileInputStream in = new FileInputStream(fo);
            buf = new byte [in.available()];
                            in.read( buf ) ;
            sql = new String(buf, "UTF-8") ;
        } catch (FileNotFoundException ex) {
            throw new HongsException.Common(ex);
        } catch (IOException ex) {
            throw new HongsException.Common(ex);
        }

        // 解析配置
        Date    dzt = dt;
        String  dbn = fn;
        Matcher mat = sqlDnmPatt.matcher(sql);
        while ( mat.find() ) {
            String key = mat.group(1);
            if ("DB".equals(key)) {
                dbn = mat.group(2).trim();
            } else
            if ("DT".equals(key)) {
                dzt = getTim(mat.group(2).trim(),dt);
            }
        }

        // 清理注释
        sql = sqlCmnPatt.matcher(sql).replaceAll("");

        // 设置时间
        sql = repTim ( sql, dzt );

        CmdletHelper.println("Run '" + fo.getName() + "' for '" + dbn + "'");

        // 逐条执行
        String[] a = sql.split(";\\s*[\r\n]");
        DB   db = DB.getInstance(dbn);
        long st = System.currentTimeMillis( );
        int  al = a.length;
        int  ok = 0;
        int  er = 0;
        for(String s : a) {
            s = s.trim( );
            try {
                if (0 < s.length()) {
                    db.execute( s );
                }
                CmdletHelper.progres(st, al, ++ok, er);
            } catch (HongsException ex) {
                CmdletHelper.progres(st, al, ok, ++er);
                CmdletHelper.progred( );
                throw ex;
            }
        }
    }

    private static void runCmd(Element e, Date dt, Logger lg) {
        boolean system = Synt.declare(e.getAttribute("system"), false);
        List<String> a = new ArrayList();

        String c, s;
        NodeList  x;
        Element   m;

        // 获取命令
        x = e.getElementsByTagName("cmd");
        if (x.getLength() == 0) {
            throw new Error( "Wrong cmd: " + e );
        }
        m = (Element) x.item(0);
        s = m.getTextContent( );
        a.add(s);

        // 获取参数
        x = e.getChildNodes();
        for (int j = 0; j < x.getLength(); j ++) {
            Node u = x.item(j);
            if ( u.getNodeType() != Node.ELEMENT_NODE ) {
                continue;
            }

            m = (Element) u;
            c = m.getTagName();
            s = m.getTextContent();

            if ("cmd".equals(c)) {
                continue;
            }
            if ("arg".equals(c)) {
                c = m.getAttribute("opt");
            }
            if (c != null) {
                a.add("--" + c + "--");
            }
            if (s != null) {
                a.add(repTim(s, dt ) );
            }
        }

        if (system) {
            runCmd(a.toArray(new String[0]), lg);
        } else {
            runLet(a.toArray(new String[0]), lg);
        }
    }

    private static void runAct(Element e, Date dt, Logger lg) {
        boolean server = Synt.declare(e.getAttribute("server"), false);
        List<String> a = new ArrayList();
        if (server) {
            a.add ("common:call-action"); // 在服务端执行
        } else {
            a.add ("common:exec-action"); // 在命令行执行
        }

        String c, s;
        NodeList  x;
        Element   m;

        // 获取命令
        x = e.getElementsByTagName("act");
        if (x.getLength() == 0) {
            throw new Error( "Wrong act: " + e );
        }
        m = (Element) x.item(0);
        s = m.getTextContent( );
        a.add(s);

        // 获取参数
        x = e.getChildNodes();
        for (int j = 0; j < x.getLength(); j ++) {
            Node u = x.item(j);
            if ( u.getNodeType() != Node.ELEMENT_NODE ) {
                continue;
            }

            m = (Element) u;
            c = m.getTagName();
            s = m.getTextContent();
            if ("act".equals(c)) {
                continue;
            }
            if ("arg".equals(c)) {
                c = m.getAttribute("opt");
            }
            if (c != null) {
                a.add("--" + c /***/ );
            }
            if (s != null) {
                a.add(repTim(s, dt ) );
            }
        }

        runLet(a.toArray(new String[0]), lg);
    }

    private static void runCmd(String[] cs, Logger lg) {
        // 命令路径相对当前 bin 目录
        String  c = cs[0];
        if (!(new File(c).isAbsolute())) {
            c = Core.CORE_PATH
              + File.separator + "bin"
              + File.separator +  c  ;
            if (new File( c ).exists() ) {
                cs[0] = c;
            }
        }

        // 开启独立进程记录 log 输出
        try {
            Process        pp = Runtime.getRuntime().exec(cs);
            BufferedReader bo = new BufferedReader(new InputStreamReader(pp.getInputStream()));
            BufferedReader br = new BufferedReader(new InputStreamReader(pp.getErrorStream()));
            String         ln ;
            while ((ln = bo.readLine()) != null) {
                lg.print(ln);
            }
            while ((ln = br.readLine()) != null) {
                lg.print(ln);
            }
            pp.waitFor();
        }
        catch (InterruptedException ex) {
            lg.error(ex);
        }
        catch (IOException ex) {
            lg.error(ex);
        }
    }

    private static void runLet(String[] cs, Logger lg) {
        try {
            CmdletRunner.main(cs);
        }
        catch (IOException ex) {
            lg.error(ex);
        }
        catch (  Exception ex) {
            lg.error(ex);
        }
        catch (  Error/**/ ex) {
            lg.error(ex);
        }
    }

    private static String repTim(String ss, Date dt) {
        StringBuffer buf = new   StringBuffer(  );
        Matcher      mxt = timVarPatt.matcher(ss);
        String       mxp ;
        Date         dst ;

        while (mxt.find()) {
            // 时间偏移
            mxp = mxt.group(3);
            if (mxp == null) {
                dst =  dt  ;
            } else {
                dst = getTim(mxp, dt );
            }

            // 时间格式
            mxp = mxt.group(1);
            if ("%S".equals(mxp)) {
                mxp = String.valueOf(dst.getTime());
            } else
            if ("%s".equals(mxp)) {
                mxp = String.valueOf(dst.getTime( ) / 1000);
            } else
            {
                mxp = new SimpleDateFormat(mxp).format(dst);
            }

            mxt.appendReplacement(buf, Matcher.quoteReplacement(mxp));
        }
        mxt.appendTail( buf );

        return buf.toString();
    }

    private static Date getTim(String ds, Date dt) {
        Matcher mzt = timFmtPatt.matcher(ds);
        String  mzp ;

        if (mzt.find()) {
            Calendar cal = Calendar.getInstance();
            cal.setTime ( dt );

            // 加减符号
            mzp = mzt.group(1);
            boolean add = !"-".equals(mzp);
            int     num ;

            mzp = mzt.group(2);
            if (null != mzp) {
                mzp = mzp.substring(0  , mzp.length( ) - 1);
                num = Integer.valueOf(mzp);
                cal.add(Calendar.YEAR  , add? num: 0 - num);
            }

            mzp = mzt.group(3);
            if (null != mzp) {
                mzp = mzp.substring(0  , mzp.length( ) - 1);
                num = Integer.valueOf(mzp);
                cal.add(Calendar.MONTH , add? num: 0 - num);
            }

            mzp = mzt.group(4);
            if (null != mzp) {
                mzp = mzp.substring(0  , mzp.length( ) - 1);
                num = Integer.valueOf(mzp);
                cal.add(Calendar.WEEK_OF_MONTH, add? num: 0 - num);
            }

            mzp = mzt.group(5);
            if (null != mzp) {
                mzp = mzp.substring(0  , mzp.length( ) - 1);
                num = Integer.valueOf(mzp);
                cal.add(Calendar.DATE  , add? num: 0 - num);
            }

            mzp = mzt.group(6);
            if (null != mzp) {
                mzp = mzp.substring(0  , mzp.length( ) - 1);
                num = Integer.valueOf(mzp);
                cal.add(Calendar.HOUR  , add? num: 0 - num);
            }

            mzp = mzt.group(7);
            if (null != mzp) {
                mzp = mzp.substring(0  , mzp.length( ) - 1);
                num = Integer.valueOf(mzp);
                cal.add(Calendar.MINUTE, add? num: 0 - num);
            }

            mzp = mzt.group(8);
            if (null != mzp) {
                mzp = mzp.substring(0  , mzp.length( ) - 1);
                num = Integer.valueOf(mzp);
                cal.add(Calendar.SECOND, add? num: 0 - num);
            }

            return cal.getTime();
        }

        return dt;
    }

    private static Date getTin(String ds, Date dt) {
        Matcher dm = tinFmtPatt.matcher(ds);
        if (! dm.matches()) {
            return getTim(ds, dt);
        }

        try {
            String dz;

            dz = dm.group(1);
            if ( dz != null) {
                return new SimpleDateFormat("yyyy/MM/ddTHH:mm:ss").parse(dz);
            }

            dz = dm.group(2);
            if ( dz != null) {
                return new SimpleDateFormat("yyyy/MM/dd" ).parse(dz);
            }

            dz = dm.group(3);
            if ( dz != null) {
                return new SimpleDateFormat("HH:mm:ss").parse(dz);
            }

            dz = dm.group(4);
            if ( dz != null) {
                return new SimpleDateFormat("HH:mm").parse(dz);
            }

            return null;
        } catch (ParseException ex) {
            throw new HongsExemption.Common(ex);
        }
    }

    private static class Logger {
        org.slf4j.Logger lgr;
                  String env;

        Logger() {
            String spc;
            spc = CoreLogger.space("hongs.out");
            lgr = CoreLogger.getLogger(spc);
            env = CoreLogger.envir("");
        }

        void print(String msg) {
            lgr.trace(msg);
        }

        void trace(String msg) {
            lgr.trace(env+" "+msg);
        }

        void error(Throwable  err) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            err.printStackTrace ( new PrintStream(buf));
            lgr.error( buf.toString() );
        }
    }

    private static class Sorter implements Comparator<File> {
        @Override
        public int compare(File f1 , File f2) {
            return f1.getName().compareTo(f2.getName());
        }
    }

}
