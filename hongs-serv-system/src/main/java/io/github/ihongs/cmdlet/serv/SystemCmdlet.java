package io.github.ihongs.cmdlet.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.CmdletRunner;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.db.DB;
import io.github.ihongs.util.Synt;

import java.io.File;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * 管理命令
 *
 * <p>
 * 除了通过命令执行内部命令和 SQL,
 * 还可添加以下启动项启用计划任务:<br/>
 * io.github.ihongs.cmdlet.serv.SystemCmdlet.Schedu<br/>
 * bin/crond 下以下文件为任务设置:<br/>
 * </p>
 * <pre>
 * min.cmd.xml 每分钟0秒执行
 * hur.cmd.xml 每小时0分执行
 * day.cmd.xml 每天的0时执行
 * </pre>
 *
 * @author Hongs
 */
@Cmdlet("system")
public class SystemCmdlet {

    private static final Pattern SQL_CMN_PAT = Pattern.compile("--.*?[\r\n]");
    private static final Pattern SQL_SET_PAT = Pattern.compile("--\\s*(\\S+):\\s*(\\S+)");
    private static final Pattern TIM_VAR_PAT = Pattern.compile("\\{\\{(.+?)(\\|(.+?))?\\}\\}");
    private static final Pattern TIM_FMT_PAT = Pattern.compile("([\\-\\+])(\\d+Y)?(\\d+M)?(\\d+w)?(\\d+d)?(\\d+h)?(\\d+m)?(\\d+s)?$");
    private static final Pattern TIN_FMT_PAT = Pattern.compile("^((\\d{2,4}/\\d{1,2}/\\d{1,2}T\\d{1,2}:\\d{1,2}:\\d{1,2})|(\\d{2,4}/\\d{1,2}/\\d{1,2})|(\\d{1,2}:\\d{1,2}:\\d{1,2})|(\\d{1,2}:\\d{1,2}))$");

    private static final Map<String, String> CORE_PATH_REPS = Synt.mapOf(
        "SERVER_ID", Core.SERVER_ID,
        "BASE_PATH", Core.BASE_PATH,
        "CORE_PATH", Core.CORE_PATH,
        "CONF_PATH", Core.CONF_PATH,
        "DATA_PATH", Core.DATA_PATH
    );

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
        opts = CmdletHelper.getOpts(args, "date:s", "!A");
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
                || fo.getName().startsWith(".")
                || fo.getName().startsWith("!")) {
                    continue;
                }
                fxs.add (fo);
            }
            Collections.sort(fxs, new Sorter());
        } else {
                fxs.add (fu);
        }

        Looker lgr = new Looker();
        String act = Core.ACTION_NAME.get();
        long   now = Core.ACTION_TIME.get();

        // 逐个执行
        for ( File fo : fxs ) {
            String fm = fo.getName();
            try {
                if (fm.endsWith(".sql"/**/)) {
                    runSql(dt, fo /**/);
                } else
                if (fm.endsWith(".cmd.xml")) {
                    runCmd(dt, fo, lgr);
                }
            } catch (Exception ex) {
                lgr.error(ex);
            } catch (Error     ex) {
                lgr.error(ex);
            } finally {
                // 放回名称和开始时间
                // 避免时间或日志模糊
                Core.ACTION_NAME.set(act);
                Core.ACTION_TIME.set(now);
            }
        }
    }

    private static void runSql(Date dt, File fo)
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
            throw new HongsException ( ex);
        } catch (IOException ex) {
            throw new HongsException ( ex);
        }

        // 解析配置
        Date    dzt = null;
        String  dbn = null;
        Matcher mat = SQL_SET_PAT.matcher(sql);
        while ( mat.find() ) {
            String key = mat.group(1);
            if ("DB".equals(key)) {
                dbn = /****/ mat.group(2).trim(/***/);
            } else
            if ("DT".equals(key)) {
                dzt = getTim(mat.group(2).trim(), dt);
            }
        }
        if (dzt == null) {
            dzt  = dt;
        }
        if (dbn == null) {
            dbn  = fo.getName()
                     .replaceFirst("\\.[^\\.]+$", "")  // 去掉扩展名
                     .replaceFirst("^[^\\.]+\\.", ""); // 去掉前缀名
        }

        // 清理注释
        sql = SQL_CMN_PAT.matcher(sql).replaceAll("");

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
                CmdletHelper.progres(st, al, ++ok,er);
            } catch ( HongsException ex) {
                CmdletHelper.progres(st, al, ok,++er);
                if (0 < Core.DEBUG) {
                    CmdletHelper.progres( );
                    throw ex;
                }
            }
        }
        CmdletHelper.progres( );
    }

    private static void runCmd(Date dt, File fo, Looker lg)
            throws HongsException {
        Document doc;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder  dbn = dbf.newDocumentBuilder();
            doc = dbn.parse( fo );
        } catch (ParserConfigurationException ex) {
            throw new HongsException(ex);
        } catch (SAXException ex) {
            throw new HongsException(ex);
        } catch ( IOException ex) {
            throw new HongsException(ex);
        }

        NodeList l = doc.getDocumentElement()
                        .getChildNodes ();
        for (int i = 0; i < l.getLength(); i ++ ) {
            Node n = l.item(i);
            if ( n.getNodeType() != Node.ELEMENT_NODE ) {
                continue;
            }

            Element e = ( Element ) n ;
            String  t = e.getTagName();

            if ("runsql".equals(t)) {
                runSql( e, dt, lg );
            } else
            if ("cmdlet".equals(t)) {
                runCmd( e, dt, lg );
            } else
            if ("action".equals(t)) {
                runAct( e, dt, lg );
            } else {
                throw new HongsException("Wrong tagName: " + t );
            }
        }
    }

    private static void runSql(Element e, Date dt, Looker lg) {
        String d = Synt.declare(e.getAttribute("db"), "defalut");
        List<String> a = new ArrayList();
        List<String> b = new ArrayList();

        String c, s;
        NodeList  x;
        Element   m;

        // 获取参数
        x = e.getChildNodes( );
        for (int j = 0; j < x.getLength(); j ++) {
            Node u = x.item(j);
            if ( u.getNodeType() != Node.ELEMENT_NODE ) {
                continue;
            }

            m = (Element) u;
            c = m.getTagName();
            s = m.getTextContent();

            if (s != null && s.length() > 0) {
                if ("arg".equals(c)) {
                    a.add(repTim(s, dt));
                } else {
                    b.add(repTim(s, dt));
                }
            }
        }

        try {
            DB db = DB.getInstance(d);
            Object[] p = a.toArray( );
            for (  String  q : b) {
                db.execute(q , p);
            }
        } catch (HongsException ex) {
            lg.error(ex);
        }
    }

    private static void runCmd(Element e, Date dt, Looker lg) {
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
        x = e.getChildNodes( );
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
            if (c != null && c.length() > 0) {
                a.add("--" + c /**/);
            }
            if (s != null && s.length() > 0) {
                a.add(repTim(s, dt));
            }
        }

        if (system) {
            runCmd(a.toArray(new String[0]), lg);
        } else {
            runLet(a.toArray(new String[0]), lg);
        }
    }

    private static void runAct(Element e, Date dt, Looker lg) {
        boolean server = Synt.declare(e.getAttribute("server"), false);
        List<String> a = new ArrayList();

        if (server) {
            a.add ("access.eval"); // 在服务端执行
        } else {
            a.add ("access.call"); // 在命令行执行
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
        x = e.getChildNodes( );
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
            if (c != null && c.length() > 0) {
                a.add("--" + c /**/);
            }
            if (s != null && s.length() > 0) {
                a.add(repTim(s, dt));
            }
        }

        runLet(a.toArray(new String[0]), lg);
    }

    private static void runCmd(String[] cs, Looker lg) {
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

    private static void runLet(String[] cs, Looker lg) {
        try {
            CmdletRunner.exec(cs);
        }
        catch (Exception ex) {
            lg.error(ex);
        }
        catch (Error ex) {
            lg.error(ex);
        }
    }

    private static String repTim(String ss, Date dt) {
        StringBuffer buf = new   StringBuffer(  );
        Matcher      mxt = TIM_VAR_PAT.matcher(ss);
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
            if (mxp.startsWith("$")) {
                mxp = CORE_PATH_REPS.get (mxp.substring(1));
            } else
            if (mxp.startsWith("%")) {
                mxp = System.getProperty (mxp.substring(1));
            } else
            if ("T".equals(mxp)) {
                mxp = String.valueOf(dst.getTime( ) /***/ );
            } else
            if ("t".equals(mxp)) {
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
        Matcher mzt = TIM_FMT_PAT.matcher(ds);
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
        Matcher dm = TIN_FMT_PAT.matcher(ds);
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
            throw new HongsExemption(ex);
        }
    }

    public  static class Schedu implements Runnable {

        @Override
        public void run() {
            ScheduledExecutorService ses = Executors.newScheduledThreadPool(3);
            Calendar cal = Calendar.getInstance();
            long now = System.currentTimeMillis();

            cal.setTimeInMillis   (  now  );
            cal.set(Calendar.MILLISECOND,1);

            // 每分钟执行
                cal.set(Calendar.SECOND, 0);
            if (new File(Core.CONF_PATH+"/bin/crond/min.cmd.xml").exists()) {
                cal.add(Calendar.MINUTE, 1);
                ses.scheduleAtFixedRate(new CmdletRunner(new String[] {
                    "system", "crond/min.cmd.xml"
                }), cal.getTimeInMillis() - now, 1000 * 60          , TimeUnit.MILLISECONDS);
            }

            // 每小时执行
                cal.set(Calendar.MINUTE, 0);
            if (new File(Core.CONF_PATH+"/bin/crond/hur.cmd.xml").exists()) {
                cal.add(Calendar.HOUR  , 1);
                ses.scheduleAtFixedRate(new CmdletRunner(new String[] {
                    "system", "crond/hur.cmd.xml"
                }), cal.getTimeInMillis() - now, 1000 * 60 * 60     , TimeUnit.MILLISECONDS);
            }

            // 每天都执行
                cal.set(Calendar.HOUR  , 0);
            if (new File(Core.CONF_PATH+"/bin/crond/day.cmd.xml").exists()) {
                cal.add(Calendar.DATE  , 1);
                ses.scheduleAtFixedRate(new CmdletRunner(new String[] {
                    "system", "crond/day.cmd.xml"
                }), cal.getTimeInMillis() - now, 1000 * 60 * 60 * 24, TimeUnit.MILLISECONDS);
            }
        }

    }

    private static class Looker {
        final Logger lgr;

        Looker() {
              String spc;
            spc = CoreLogger.space("hongs.out");
            lgr = CoreLogger.getLogger(  spc  );
        }

        void print(String msg) {
            lgr.trace(msg);
        }

        void error(Throwable  err) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            err.printStackTrace ( new PrintStream(buf));
            lgr.error(buf.toString());
        }
    }

    private static class Sorter implements Comparator<File> {
        @Override
        public int compare(File f1 , File f2) {
            return f1.getName().compareTo(f2.getName());
        }
    }

}
