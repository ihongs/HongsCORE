package io.github.ihongs;

import io.github.ihongs.util.Syno;

/**
 * 异常本地化工具
 *
 * 当 error 长在 128 以内, 且以 @ 开头,
 * 表示将从对应的配置文件和语言资源里读取,
 * 如 @default:my.term 为默认配置自定语句,
 * 若省略配置名称前缀则会默认设为 default.
 *
 * @author Hongs
 */
public final class HongsCurse {

    public final Throwable cause;
    public final int       errno;
    public final String    error;
    public final Object[ ] cases;

    public final String    conf ;
    public final String    mark ;

    /**
     * @param cause 异常
     * @param errno 代号
     * @param error 描述
     * @param cases 参数
     */
    HongsCurse (Throwable cause, int errno, String error, Object[] cases) {
        // 从描述里提取配置名称
        if (error != null
        &&  error.length( ) >= 2
        &&  error.length( ) <= 128
        &&  error.charAt(0) == '@') {
            int  p = error.indexOf(":");
            if ( p > 0 ) {
                mark = error.substring(1+p);
                conf = error.substring(1,p);
            } else {
                mark = error.substring(1  );
                conf = "default";
            }
        } else {
                conf = "default";
                mark = null ;
        }

        this.cause = cause;
        this.errno = errno;
        this.error = error;
        this.cases = cases;
    }

    /**
     * 接口状态
     * @return
     */
    public int getState() {
        if (errno >= 200
        &&  errno <= 599 ) {
            return errno ;
        } else
        if (errno >= 600 ) {
            String codx = "Ex" + Integer.toString(errno, 10 );
            return getConfig().getProperty(codx+".stat", 500);
        } else
        if (null != mark && ! mark.isEmpty()) {
            return getConfig().getProperty(mark+".stat", 500);
        } else
        {
            return 500;
        }
    }

    /**
     * 接口代码
     * @return
     */
    public String getStage() {
        if (errno >= 200
        &&  errno <= 599 ) {
            return "Er" + Integer.toString(errno, 10);
        } else
        if (errno >= 600 ) {
            return "Ex" + Integer.toString(errno, 10);
        } else
        if (null != mark && ! mark.isEmpty()) {
            return  mark  ;
        } else
        {
            return this.getClass().getName();
        }
    }

    /**
     * 描述文本
     * @return
     */
    @Override
    public String toString()
    {
        Throwable  cauze = cause.getCause();
        if (cauze != null
        &&  error == null
        &&  errno <= 1) {
            return cauze.toString();
        }

        String s  = this.getStage();
        String m  = this.getMessage();
        return m != null ? (s +": "+ m) : s;
    }

    /**
     * 获取消息
     * @return
     */
    public String getMessage()
    {
        String  errox;

        R: {
            CoreConfig conf = getConfig( );

            if ( mark != null && ! mark .isEmpty()) {
                errox  = conf.getProperty(mark);
                if (errox != null) break R;
            }

            if (errno >= 600 ) {
                errox  = conf.getProperty("Ex" + Integer.toString(errno, 10));
                if (errox != null) break R;
            } else
            if (errno >= 400 ) {
                errox  = conf.getProperty("Er" + Integer.toString(errno, 10));
                if (errox != null) break R;
            }

            if (error != null && ! error.isEmpty()) {
                errox  = error;
                break R;
            }

            Throwable  erroo = cause.getCause();
            if (erroo != null) {
                return erroo.getMessage();
            }

            // 参数作为消息
            if (cases != null && cases.length > 0) {
                return Syno . concat (", ", cases);
            }

            return null;
        }

        // 注入消息参数
        if (cases != null && cases.length > 0) {
            errox  = Syno.inject(errox, cases);
        }

        return errox;
    }

    /**
     * 获取本地消息
     * @return
     */
    public String getLocalizedMessage()
    {
        String  errox;

        R: {
            CoreConfig conf = getLocale( );

            if ( mark != null && ! mark .isEmpty()) {
                errox  = conf.getProperty(mark);
                if (errox != null) break R;
            }

            if (errno >= 600 ) {
                errox  = conf.getProperty("Ex" + Integer.toString(errno, 10));
                if (errox != null) break R;
            } else
            if (errno >= 400 ) {
                errox  = conf.getProperty("Er" + Integer.toString(errno, 10));
                if (errox != null) break R;
            }

            if (error != null && ! error.isEmpty()) {
                errox  = error;
                break R;
            }

            Throwable  erroo = cause.getCause();
            if (erroo != null) {
                return erroo.getLocalizedMessage();
            }

            /**
             * 本地消息可能对外展示,
             * 故不会将参数作为消息.
             */

            return null;
        }

        // 注入消息参数
        if (cases != null && cases.length > 0) {
            errox  = Syno.inject(errox, cases);
        }

        return errox;
    }

    public CoreConfig getConfig() {
        try {
            return CoreConfig.getInstance(conf);
        } catch (HongsExemption e) {
            String defs = "default";
        if (826 == e.getErrno()
        && ! defs.equals(conf)) {
            return CoreConfig.getInstance(defs);
        } else {
            throw  e;
        }}
    }

    public CoreLocale getLocale() {
        /**
         * 在例如多线程环境下未初始化 Core 时
         * 出现异常会导致下方因找不到语言类别而提前终止
         * 从而无法获知真实的错误位置
         * 此时设为默认的语言让其继续
         */
        if (Core.ACTION_LANG.get() == null) {
            Core.ACTION_LANG.set(CoreConfig.getInstance().getProperty("core.language.default"));
            CoreLogger.error("ACTION_LANG is not set. Current exception: " + cause.toString( ));
        }

        try {
            return CoreLocale.getInstance(conf);
        } catch (HongsExemption e) {
            String defs = "default";
        if (826 == e.getErrno()
        && ! defs.equals(conf)) {
            return CoreLocale.getInstance(defs);
        } else {
            throw  e;
        }}
    }

}
