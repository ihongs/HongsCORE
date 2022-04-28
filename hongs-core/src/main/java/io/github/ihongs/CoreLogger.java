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
     * 获取当前客户端IP
     * 没有则返回线程ID
     * @return
     */
    public static String client() {
        String a = Core.CLIENT_ADDR.get();
        if (a == null || a.isEmpty()) {
            a = "#"+String.valueOf(Thread.currentThread().getId(  ));
        }
        return a;
    }

    /**
     * 获取当前请求路径
     * 无则返回线程名称
     * @return
     */
    public static String action() {
        String a = Core.ACTION_NAME.get();
        if (a == null || a.isEmpty()) {
            a = "$"+String.valueOf(Thread.currentThread().getName());
        }
        return a;
    }

    /**
     * 补充当前状态信息
     * 等同 client()+" "+action()+" "+text
     * 下列 info,warn,trace,debug,error 均已调此, 无需再加
     * @param text
     * @return
     */
    public static String mark (String text) {
        String addr = client();
        String path = action();
        if (text == null) {
            text  =  "" ;
        }
        return new StringBuilder(addr.length() + path.length() + text.length() + 2)
            .append(addr).append(' ')
            .append(path).append(' ')
            .append(text).toString( );
    }

    /**
     * 信息
     * @param text
     */
    public static void info (String text) {
        if (2 != (2 & Core.DEBUG)) {
            return;
        }
        if (1 == (1 & Core.DEBUG)) {
            getLogger(Cnst.LOG_NAME).info (mark(text));
        } else {
            getLogger(Cnst.OUT_NAME).info (mark(text));
        }
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
            getLogger(Cnst.LOG_NAME).info (mark(text), args);
        } else {
            getLogger(Cnst.OUT_NAME).info (mark(text), args);
        }
    }

    /**
     * 警告
     * @param text
     */
    public static void warn (String text) {
        if (2 != (2 & Core.DEBUG)) {
            return;
        }
        if (1 == (1 & Core.DEBUG)) {
            getLogger(Cnst.LOG_NAME).warn (mark(text));
        } else {
            getLogger(Cnst.OUT_NAME).warn (mark(text));
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
            getLogger(Cnst.LOG_NAME).warn (mark(text), args);
        } else {
            getLogger(Cnst.OUT_NAME).warn (mark(text), args);
        }
    }

    /**
     * 跟踪
     * @param text
     */
    public static void trace(String text) {
        if (4 != (4 & Core.DEBUG)) {
            return;
        }
        if (1 == (1 & Core.DEBUG)) {
            getLogger(Cnst.LOG_NAME).trace(mark(text));
        } else {
            getLogger(Cnst.OUT_NAME).trace(mark(text));
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
            getLogger(Cnst.LOG_NAME).trace(mark(text), args);
        } else {
            getLogger(Cnst.OUT_NAME).trace(mark(text), args);
        }
    }

    /**
     * 调试
     * @param text
     */
    public static void debug(String text) {
        if (4 != (4 & Core.DEBUG)) {
            return;
        }
        if (1 == (1 & Core.DEBUG)) {
            getLogger(Cnst.LOG_NAME).debug(mark(text));
        } else {
            getLogger(Cnst.OUT_NAME).debug(mark(text));
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
            getLogger(Cnst.LOG_NAME).debug(mark(text), args);
        } else {
            getLogger(Cnst.OUT_NAME).debug(mark(text), args);
        }
    }

    /**
     * 错误(总是记录)
     * @param text
     */
    public static void error(String text) {
        if (1 == (1 & Core.DEBUG)) {
            getLogger(Cnst.LOG_NAME).error(mark(text));
        } else {
            getLogger(Cnst.OUT_NAME).error(mark(text));
        }
    }

    /**
     * 错误(总是记录)
     * @param text
     * @param args
     */
    public static void error(String text, Object... args) {
        if (1 == (1 & Core.DEBUG)) {
            getLogger(Cnst.LOG_NAME).error(mark(text), args);
        } else {
            getLogger(Cnst.OUT_NAME).error(mark(text), args);
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
            getLogger(Cnst.LOG_NAME).error(mark(text));
        } else {
            getLogger(Cnst.OUT_NAME).error(mark(text));
        }
    }

}
