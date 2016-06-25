package app.hongs;

import app.hongs.action.ActionHelper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志记录工具
 *
 * @author Hongs
 */
public class CoreLogger
{

    /**
     * 获取 slf4j 的 Logger 实例
     * @param name
     * @return
     */
    public static Logger getLogger(String name) {
        return LoggerFactory.getLogger (name);
    }

    /**
     * 获取动作空间标识
     * @param name
     * @return
     */
    public static String space(String name) {
        String flag = Core.ACTION_NAME.get( );
        if (flag != null && !"".equals(flag)) {
            name += "."+flag.replace('/','.')
                            .replace(':','.');
        }
        return name;
    }

    /**
     * 补充动作环境信息
     * @param text
     * @return
     */
    public static String envir(String text) {
        StringBuilder line = new StringBuilder();

        // add IP Address
        if (Core.ENVIR == 1) {
            Core core = Core.getInstance();
            if ( core.containsKey( ActionHelper.class.getName() ) ) {
                ActionHelper helper = Core.getInstance(ActionHelper.class);
                if (/**/null != helper.getRequest()) {
                    line.append(helper.getRequest( ).getRemoteAddr( )/**/)
                        .append(' ');
                } else {
                    line.append("ACTION ");
                }
            } else {
                line.append("ACTION ");
            }
        } else {
           line.append("CMDLET ");
        }

        // add Action Name
        if (/**/null != Core.ACTION_NAME.get()) {
            line.append(Core.ACTION_NAME.get())
                .append(' ');
        }

        return  line.append( text ).toString( );
    }

    /**
     * 调试
     * @param text
     * @param args
     */
    public static void debug(String text, Object... args) {
        if (8 == (8 & Core.DEBUG)) {
            return; // 禁止调试
        }
        if (1 == (1 & Core.DEBUG)) {
            getLogger(space("hongs.out")).debug(envir(text), args);
        }
        if (2 == (2 & Core.DEBUG)) {
            getLogger(space("hongs.log")).debug(envir(text), args);
        }
    }

    /**
     * 输出
     * @param text
     * @param args
     */
    public static void trace(String text, Object... args) {
        if (4 == (4 & Core.DEBUG)) {
            return; // 禁止跟踪
        }
        if (1 == (1 & Core.DEBUG)) {
            getLogger(space("hongs.out")).trace(envir(text), args);
        }
        if (2 == (2 & Core.DEBUG)) {
            getLogger(space("hongs.log")).trace(envir(text), args);
        }
    }

    /**
     * 错误
     * 既不显示又不记录则显示错误
     * @param text
     * @param args
     */
    public static void error(String text, Object... args) {
        String flag = space("");
        if (1 == (1 & Core.DEBUG )
        ||  2 != (2 & Core.DEBUG)) {
            getLogger("hongs.out" + flag).error(envir(text), args);
        }
        if (2 == (2 & Core.DEBUG)) {
            getLogger("hongs.log" + flag).error(envir(text), args);
        }
    }

    /**
     * 错误
     * 既不显示又不记录则显示概要
     * @param t
     */
    public static void error(Throwable t) {
        String flag = space("");

        if (1 != (1 & Core.DEBUG )
        &&  2 != (2 & Core.DEBUG)) {
            getLogger("hongs.out" + flag).error(envir(t.getMessage()));
            return;
        }

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        t.printStackTrace(new PrintStream ( b ) );
        String s = b.toString().replaceAll("^\\S","  $0");

        if (1 == (1 & Core.DEBUG )
        ||  2 != (2 & Core.DEBUG)) {
            getLogger("hongs.out" + flag).error(envir(s));
        }
        if (2 == (2 & Core.DEBUG)) {
            getLogger("hongs.log" + flag).error(envir(s));
        }
    }

}
