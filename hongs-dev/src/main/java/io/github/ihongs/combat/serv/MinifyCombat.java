package io.github.ihongs.combat.serv;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.util.Map;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

/**
 * 脚本压缩
 *
 * @author Hongs
 */
@Combat("minify")
public class MinifyCombat {

    /**
     * 合并命令
     * @param args
     */
    @Combat("merge")
    public static void merge(String[] args) {
        if (args.length == 0) {
            CombatHelper.println("Usage: minify.merge FILE1 FILE2 ... [--output OUTPUT_FILE]");
            return;
        }

        Map<String, Object> opts;
        opts = CombatHelper.getOpts( args, "output:s", "!A" );
        args = ( String[] ) opts.remove("");
        if (0 == args.length) {
            CombatHelper.println("Files for merge required!");
            return;
        }

        String fn = (String) opts.get("output");
        byte[] ln = "\r\n" . getBytes();
        OutputStream fo;
         InputStream fi;

        if (fn == null) {
            fo = CombatHelper.OUT.get();
        } else {
            try {
                fo = new FileOutputStream(new File(fn));
            } catch (IOException ex) {
                CombatHelper.println("Output file '"+fn+"' can not write. "+ex.getMessage());
                return;
            }
        }

        for(int i = 0; i < args.length; i ++) {
            fn = args [i];
            try {
                fi = new  FileInputStream(new File(fn));
            } catch (IOException ex) {
                CombatHelper.println("Source file '"+fn+"' can not read. " +ex.getMessage());
                continue;
            }

            try {
                byte[] bf = new byte[1024];
                int    bl ;
                while((bl = fi.read(bf)) > 0) {
                    fo.write(bf, 0, bl); // 拷贝
                }   fo.write(ln       ); // 换行
            } catch (IOException ex) {
                CombatHelper.println(ex.getMessage());
                continue;
            } finally {
            try {
                fi.close();
            } catch (IOException er) {
                CombatHelper.println(er.getMessage());
            }}
        }
            try {
                fo.close();
            } catch (IOException er) {
                CombatHelper.println(er.getMessage());
            }
    }

    @Combat("build")
    public static void build(String[] args) {
        if (args.length == 0) {
            CombatHelper.println("Usage: minify.build FILE1 FILE2 ... [--output OUTPUT_FILE]");
            CombatHelper.println("\tOUTPUT_FILE is '*xxx' will compress one by one, with suffix 'xxx'");
            return;
        }

        Map<String, Object> opts;
        opts = CombatHelper.getOpts( args, "output:s", "suffix:s", "!A" );
        args = ( String[] ) opts.remove("");
        if (args == null || args.length == 0) {
            CombatHelper.println("Files for build required!");
            return;
        }

        String fn = (String) opts.get("output");
        char[] ln = "\r\n".toCharArray (  );
        Reader fr ;
        Writer fw ;
        PrintStream eo = CombatHelper.ERR.get();

        if (fn != null && fn.startsWith ("*.")) {
                String fe = fn.substring(1);
            for(String fm : args) {
                fn  =  fm.replaceAll("\\.\\w+$", fe + "$0");

                try {
                    fw = new OutputStreamWriter(new FileOutputStream(fn), "UTF-8");
                } catch (IOException ex) {
                    CombatHelper.println("Output file '"+fn+"' can not write. "+ex.getMessage());
                    continue;
                }

                try {
                    fr = new  InputStreamReader(new  FileInputStream(fm), "UTF-8");
                } catch (IOException ex) {
                    CombatHelper.println("Source file '"+fm+"' is not exists. "+ex.getMessage());
                    continue;
                }

                try {
                    if (fm.endsWith(".js" )) {
                        buildJs (fr, fw, eo, fm);
                    } else
                    if (fm.endsWith(".css")) {
                        buildCss(fr, fw, eo, fm);
                    } else {
                        CombatHelper.println("Source file '"+fm+"' not support, must be '.js' or '.css'");
                        continue;
                    }
                    fw.write(ln); // 结尾换行
                } catch (IOException|EvaluatorException ex) {
                    ex.printStackTrace(eo);
                    continue;
                }

                try {
                    fw.close();
                }
                catch (IOException ex) {
                    ex.printStackTrace(eo);
                }
            }
        } else {
            if (fn == null) {
                fw = new OutputStreamWriter(CombatHelper.OUT.get());
            } else {
                try {
                    fw = new OutputStreamWriter(new FileOutputStream(fn), "UTF-8");
                } catch (IOException ex) {
                    CombatHelper.println("Output file '"+fn+"' can not write. "+ex.getMessage());
                    return;
                }
            }

            for(String fm : args) {
                try {
                    fr = new  InputStreamReader(new  FileInputStream(fm), "UTF-8");
                } catch (IOException ex) {
                    CombatHelper.println("Source file '"+fm+"' is not exists. "+ex.getMessage());
                    continue;
                }

                try {
                    if (fm.endsWith(".js" )) {
                        buildJs (fr, fw, eo, fm);
                    } else
                    if (fm.endsWith(".css")) {
                        buildCss(fr, fw, eo, fm);
                    } else {
                        CombatHelper.println("Output file '"+fn+"' not support, must be '.js' or '.css'");
                        continue;
                    }
                    fw.write(ln); // 结尾换行
                } catch (IOException|EvaluatorException ex) {
                    ex.printStackTrace(eo);
                    continue;
                }
            }

                try {
                    fw.close();
                }
                catch (IOException ex) {
                    ex.printStackTrace(eo);
                }
        }
    }

    private static void buildJs (Reader in, Writer out, final PrintStream err, final String fn) throws IOException, EvaluatorException {
        try {
            JavaScriptCompressor compressor = new JavaScriptCompressor(in, new ErrorReporter() {

                @Override
                public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
                    err.println("\n[WARNING] in " + fn);
                    if (line < 0 ) {
                        err.println("  " + message);
                    } else {
                        err.println("  " + line + ':' + lineOffset + ':' + message);
                    }
                }

                @Override
                public void   error(String message, String sourceName, int line, String lineSource, int lineOffset) {
                    err.println(  "\n[ERROR] in " + fn);
                    if (line < 0 ) {
                        err.println("  " + message);
                    } else {
                        err.println("  " + line + ':' + lineOffset + ':' + message);
                    }
                }

                @Override
                public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
                    error(message, sourceName, line, lineSource, lineOffset);
                    return new EvaluatorException(message);
                }

            });

            in.close();
            in = null ;

            compressor.compress(out, -1, true, false, false, false);
        } catch (EvaluatorException  ee) {
            ee.printStackTrace (err);
        }
    }

    private static void buildCss(Reader in, Writer out, final PrintStream err, final String fn) throws IOException {
        try {
            CssCompressor compressor = new CssCompressor(in);

            in.close();
            in = null ;

            compressor.compress(out, -1);
        } catch (EvaluatorException  ee) {
            ee.printStackTrace (err);
        }
    }

}
