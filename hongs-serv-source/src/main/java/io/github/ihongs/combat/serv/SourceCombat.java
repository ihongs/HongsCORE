package io.github.ihongs.combat.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.CombatRunner;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.db.DB;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;

import java.io.File;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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
 * 批量执行命令
 *
 * @author Hongs
 */
@Combat("source")
public class SourceCombat {

    private static final Pattern VAR_PAT = Pattern.compile("\\$(\\w+|\\{.*?\\})");
    private static final Pattern TIM_PAT = Pattern.compile("^(CURR_TIME|EXEC_TIME)(_MS)?([\\-\\+]\\d+)?(\\|.*)?$");

    /**
     * 执行命令
     * @param args
     * @throws HongsException
     */
    @Combat("__main__")
    public static void exec (String[] args) throws HongsException {
        if (0 == args.length) {
            CombatHelper.println ("Usage: source FILE_OR_DIR1 FILE_OR_DIR2 ...");
            return;
        }

        for(String fn : args) {
            File   fu = new File(fn);

            // 相对且不存在则看作内部目录
            if (! fu.isAbsolute() && ! fu.exists()) {
                fn = Core.CORE_PATH
                   + File.separator + "bin"
                   + File.separator +  fn ;
                fu = new File(fn);
            }

            // 获取目录下全部待执行的文件
            List<File> fxs ;
            if (fu.isDirectory()) {
                File[] fus = fu.listFiles ();
                fxs = new ArrayList(fus.length);
                for (File fo : fus) {
                    if (! fo.isFile()) {
                        continue;
                    }
                    String fp = fo.getName();
                    if (fp.startsWith(".")
                    ||  fp.startsWith("!")
                    ||  fp.  endsWith("~")) {
                        continue;
                    }
                    fxs.add (fo);
                }
                Collections.sort(fxs, new Sorter());
            } else
            if (fu.isFile()) {
                fxs = new ArrayList(1);
                fxs.add (fu);
            } else {
                continue;
            }

            Looker lgr = new Looker();
            String act = Core.ACTION_NAME.get();
            Map    env = CombatHelper.ENV.get();

            /**
             * 依次逐个执行脚本
             * 脚本环境变量独立
             */
            for ( File fo : fxs ) {
                String fp = fo.getPath();
                try {
                    if (fp.endsWith(".xml")) {
                        Core.ACTION_NAME.set(act +":"+ fo.getName());
                        CombatHelper.ENV.remove();
                        CombatHelper.println( "Run " + fo.getName());
                        runXml(fo , lgr);
                    } else
                    if (fp.endsWith(".sql")) {
                        Core.ACTION_NAME.set(act +":"+ fo.getName());
                        CombatHelper.ENV.remove();
                        CombatHelper.println( "Run " + fo.getName());
                        runSql(fo);
                    }
                } catch (Exception ex) {
                    lgr.error (ex);
                } catch (Error     ex) {
                    lgr.error (ex);
                } finally {
                    Core.ACTION_NAME.set(act);
                    CombatHelper.ENV.set(env);
                }
            }
        }
    }

    private static void runSql(File fo)
            throws HongsException {
        try (
            Scanner in = new Scanner (fo);
        ) {
            DB db = DB.getInstance("default");
            StringBuilder sb = new StringBuilder();

            long st = System. currentTimeMillis ();
            long al = fo.length();
            long rl = 0; // 处理进度
            long ok = 0; // 成功计数
            long er = 0; // 失败计数
            int  li = 0; // 行号

            while ( in.hasNextLine() ) {
                String  ln = in.nextLine();
                String  tn = ln.trim();
                rl  +=  ln.length();
                li  ++  ;

                if (sb.length() == 0 ) {
                    if (tn.length() == 0 ) {        // 空行
                        continue;
                    }
                    if (tn.startsWith("--DB=")) {   // 切换数据库
                        db = DB.getInstance(tn.substring(5));
                        continue;
                    }
                    if (tn.startsWith("--")) {      // 注释
                        continue;
                    }
                }
                    if (! tn.endsWith(";" )) {      // 换行
                        sb.append(ln);
                        continue;
                    }

                ln = ln.substring(0, ln.lastIndexOf(';'));
                sb.append( ln );
                ln = sb.toString ();
                sb.setLength(0);

                // 进度
               float rp = (float) rl / al;
                long et = System.currentTimeMillis() - st;
                     et = (long ) (  et / rp - et  );

                try {
                    db.execute(ln);
                    CombatHelper.progres(rp, String.format("Ok(%d) Er(%d) ET: %s", ok++, er, Syno.phraseTime(et)));
                }
                catch(HongsException ex) {
                    CombatHelper.progres(rp, String.format("Ok(%d) Er(%d) ET: %s", ok, er++, Syno.phraseTime(et)));
                    if (0 < Core.DEBUG) {
                        CombatHelper.progres();
                        CombatHelper.println("Error in file("+fo.getName()+") at lin("+li+"): "+ex.getMessage() );
                        throw ex;
                    }
                }
            }
            if (sb.length()!=0) {
                String ln = sb.toString();

                // 进度
               float rp = (float) rl / al;
                long et = System.currentTimeMillis() - st;
                     et = (long ) (  et / rp - et  );

                try {
                    db.execute(ln);
                    CombatHelper.progres(rp, String.format("Ok(%d) Er(%d) ET: %s", ok++, er, Syno.phraseTime(et)));
                }
                catch(HongsException ex) {
                    CombatHelper.progres(rp, String.format("Ok(%d) Er(%d) ET: %s", ok, er++, Syno.phraseTime(et)));
                    if (0 < Core.DEBUG) {
                        CombatHelper.progres();
                        CombatHelper.println("Error in file("+fo.getName()+") at lin("+li+"): "+ex.getMessage() );
                        throw ex;
                    }
                }
            }
            CombatHelper.progres(1, String.format("Ok(%d) Er(%d)", ok, er));
            CombatHelper.progres();
        }
        catch (FileNotFoundException ex) {
            throw new HongsException(ex);
        }
    }

    private static void runXml(File fo, Looker lg)
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

            if (null != t) switch ( t ) {
                case "combat":
                    runCmd( e, lg );
                    break ;
                case "action":
                    runAct( e, lg );
                    break ;
                case "sql":
                    runSql( e, lg );
                    break ;
                case "say": {
                    String txt = repVar (e.getTextContent());
                    CombatHelper.println(txt);
                    break ; }
                case "set": {
                    Map    env = CombatHelper.ENV.get();
                    String key = e.getAttribute ("env");
                    String val = repVar (e.getTextContent());
                           env.put (key, val);
                    break ; }
                case "def": {
                    Map    env = CombatHelper.ENV.get();
                    String key = e.getAttribute ("env");
                    if ( ! env.containsKey(key) ) {
                    String val = repVar (e.getTextContent());
                           env.put (key, val);
                    }
                    break ; }
                default:
                    throw new HongsException("Wrong tagName: " + t );
            }
        }
    }

    private static void runSql(Element e, Looker lg) {
        String d = repVar(Synt.declare(e.getAttribute("db"), "defalut"));
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
            s = m.getTextContent().trim();

            if (s != null && s.length() > 0) {
                if ("arg".equals(c)) {
                    a.add(repVar(s));
                } else {
                    b.add(repVar(s));
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

    private static void runCmd(Element e, Looker lg) {
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
                a.add("--" + c );
            }
            if (s != null && s.length() > 0) {
                a.add(repVar(s));
            }
        }

        if (system) {
            runCmd(a.toArray(new String[0]), lg);
        } else {
            runBat(a.toArray(new String[0]), lg);
        }
    }

    private static void runAct(Element e, Looker lg) {
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
                a.add(repVar(s));
            }
        }

        runBat(a.toArray(new String[0]), lg);
    }

    private static void runCmd(String[] cs, Looker lg) {
        if (Core.DEBUG > 1 ) {
            CombatHelper.println("+ " + Syno.concat(" ", (Object[]) cs));
        }

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

    private static void runBat(String[] cs, Looker lg) {
        if (Core.DEBUG > 1 ) {
            CombatHelper.println("- " + Syno.concat(" ", (Object[]) cs));
        }
        try {
            CombatRunner.exec(cs);
        }
        catch (Exception ex) {
            lg.error(ex);
        }
        catch (Error ex) {
            lg.error(ex);
        }
    }

    private static String repVar(String ss) {
        Matcher      mxt = VAR_PAT.matcher (ss);
        StringBuffer buf = new StringBuffer(  );
        Map<String,String> env = CombatHelper.ENV.get( );
        String       key ;
        String       val ;

        while ( mxt.find( ) ) {
            key=mxt.group (1);
            if (key.charAt(0) == '{') {
                key = key.substring(1, key.length() - 1);
            }

            switch (key) {
                case "SERVER_ID": val = Core.SERVER_ID; break;
                case "BASE_PATH": val = Core.BASE_PATH; break;
                case "CORE_PATH": val = Core.CORE_PATH; break;
                case "CONF_PATH": val = Core.CONF_PATH; break;
                case "DATA_PATH": val = Core.DATA_PATH; break;
                default:
                    // 时间处理
                    Matcher mat = TIM_PAT.matcher(key);
                    if (mat.matches()) {
                               key = mat.group(1);
                        String add = mat.group(3);
                        String fmt = mat.group(4);
                        long   mis = mat.group(2) == null ? 1000 : 1;
                        Long   tim ;

                        // 时间获取
                        if ("EXEC_TIME".equals(key)) {
                            tim = Core.ACTION_TIME.get(/**/);
                        } else {
                            tim = System.currentTimeMillis();
                        }

                        // 时间偏移
                        if (add != null ) {
                        if (add.charAt(0) == '+') {
                            add  = add.substring( 1 );
                            tim += Long.valueOf (add) * mis ;
                        } else {
                            add  = add.substring( 1 );
                            tim -= Long.valueOf (add) * mis ;
                        }}

                        // 时间格式
                        if (fmt != null ) {
                            fmt  = fmt.substring( 1 );
                            val  = Syno.formatTime(tim , fmt , Core.getZoneId(), Core.getLocale());
                        } else {
                            val  = Synt.asString(tim  /  mis);
                        }
                    } else {
                            val  = Synt.defoult (env.get(key), "");
                    }
            }

            mxt.appendReplacement(buf, Matcher.quoteReplacement(val));
        }
        mxt.appendTail( buf );

        return buf.toString();
    }

    private static class Looker {
        final Logger lgr;

        Looker() {
            lgr = CoreLogger.getLogger(1 == (1 & Core.DEBUG) ? Cnst.LOG_NAME : Cnst.OUT_NAME);
        }

        void print(String msg) {
            lgr.trace(CoreLogger.mark(msg));
        }

        void error(String msg) {
            lgr.error(CoreLogger.mark(msg));
        }

        void error(Throwable err) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            err.printStackTrace ( new PrintStream(buf));
            error(buf.toString());
        }
    }

    private static class Sorter implements Comparator<File> {
        @Override
        public int compare(File f1 , File f2) {
            return f1.getName().compareTo(f2.getName());
        }
    }

}
