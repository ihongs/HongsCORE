package io.github.ihongs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

/**
 * 日志记录工具
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
        return LoggerFactory.getLogger(name);
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
        String a = Core.CLIENT_ADDR.get();
        if (a != null) {
            line.append( a )
                .append(' ');
        } else {
            line.append('#')
                .append(Thread.currentThread().getId(  ))
                .append(' ');
        }

        // add Action Name
        String n = Core.ACTION_NAME.get();
        if (n != null) {
            line.append( n )
                .append(' ');
        } else {
            line.append('@')
                .append(Thread.currentThread().getName())
                .append(' ');
        }

        return line.append(text).toString();
    }

    /**
     * 信息
     * @param text
     * @param args 
     */
    public static void info (String text, Object... args) {
        if (2 != (2 & Core.DEBUG)) {
            return;
        }
        if (1 == (1 & Core.DEBUG)) {
            getLogger(space("hongs.log")).info (envir(text), args);
        } else {
            getLogger(space("hongs.out")).info (envir(text), args);
        }
    }

    /**
     * 警告
     * @param text
     * @param args 
     */
    public static void warn (String text, Object... args) {
        if (2 != (2 & Core.DEBUG)) {
            return;
        }
        if (1 == (1 & Core.DEBUG)) {
            getLogger(space("hongs.log")).warn (envir(text), args);
        } else {
            getLogger(space("hongs.out")).warn (envir(text), args);
        }
    }

    /**
     * 跟踪
     * @param text
     * @param args
     */
    public static void trace(String text, Object... args) {
        if (4 != (4 & Core.DEBUG)) {
            return;
        }
        if (1 == (1 & Core.DEBUG)) {
            getLogger(space("hongs.log")).trace(envir(text), args);
        } else {
            getLogger(space("hongs.out")).trace(envir(text), args);
        }
    }

    /**
     * 调试
     * @param text
     * @param args
     */
    public static void debug(String text, Object... args) {
        if (4 != (4 & Core.DEBUG)) {
            return;
        }
        if (1 == (1 & Core.DEBUG)) {
            getLogger(space("hongs.log")).debug(envir(text), args);
        } else {
            getLogger(space("hongs.out")).debug(envir(text), args);
        }
    }

    /**
     * 错误(总是记录)
     * @param text
     * @param args
     */
    public static void error(String text, Object... args) {
        if (1 == (1 & Core.DEBUG)) {
            getLogger(space("hongs.out")).error(envir(text), args);
        } else {
            getLogger(space("hongs.log")).error(envir(text), args);
        }
    }

    /**
     * 错误(总是记录)
     * @param flaw
     */
    public static void error(Throwable flaw) {
        // 调试模式下取完整错误栈
        String text ;
        if (4 == (4 & Core.DEBUG)) {
            ByteArrayOutputStream buff = new ByteArrayOutputStream();
            flaw.printStackTrace( new PrintStream( buff ) ) ;
            text = buff.toString().replaceAll("(\r\n|\r|\n)","$1\t");
        } else {
            text = flaw.toString().replaceAll("(\r\n|\r|\n)","$1\t");
        }

        if (1 == (1 & Core.DEBUG)) {
            getLogger(space("hongs.log")).error(envir(text));
        } else {
            getLogger(space("hongs.out")).error(envir(text));
        }
    }

}
