package io.github.ihongs;

import io.github.ihongs.util.Syno;

/**
 * 异常本地化工具
 *
 * 当 error 长在 128 以内,
 * 且由字母、数字、点、冒号、下划线组成时,
 * 表示将从对应的配置文件和语言资源里读取,
 * 如 default:my.error 为默认配置自定语句,
 * 若省略配置名称前缀则会默认设为 defects.
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
        &&  error.length() >= 0x1
        &&  error.length() <= 128
        &&  error.matches("^[0-9A-Za-z_.:]$")) {
            int  p  =  error. indexOf (":");
            if ( p  >  0 ) {
                conf = error.substring(0,p);
                mark = error.substring(1+p);
            } else {
                conf = "defects";
                mark = error;
            }
        } else {
                conf = "defects";
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
            String codx = "Ex" + Integer.toString(errno , 10 );
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
            return "Error";
        }
    }

    /**
     * 描述文本
     * @return
     */
    @Override
    public String toString()
    {
        return  getMessage();
    }

    /**
     * 获取消息
     * @return
     */
    public String getMessage()
    {
        String codx, mrkx;

        CoreConfig conf = getConfig();

        if ( 600 <= errno ) {
            codx  = "Ex" + Integer.toString(errno, 10);
        } else
        if ( 400 <= errno ) {
            codx  = "Er" + Integer.toString(errno, 10);
        } else {
            codx  = "" ;
        }

        R: {
            if (mark != null && ! mark.isEmpty()) {
                mrkx  = conf.getProperty(mark);
                if (mrkx != null) break R;
            }

            if (codx != null && ! codx.isEmpty()) {
                mrkx  = conf.getProperty(codx);
                if (mrkx != null) break R;
            }

            if (error != null && ! error.isEmpty()) {
                mrkx  = error;
                break R;
            }

            Throwable erro = cause.getCause();
            if (erro != null) {
                if (errno < 400) {
                    return erro.getMessage();
                } else {
                    mrkx = erro.getMessage();
                    return (codx +" "+ mrkx).trim ();
                }
            }

            mrkx  = "" ;
        }

        // 注入消息参数
        if (null == mrkx  ||  mrkx.isEmpty( )) {
            mrkx  = "" ;
        } else
        if (null != cases && cases.length > 0) {
            mrkx  = Syno.inject(mrkx, cases );
        }

        return (codx +" "+ mrkx).trim();
    }

    /**
     * 获取本地消息
     * @return
     */
    public String getLocalizedMessage() {
        String codx, mrkx;

        CoreLocale conf = getLocale();

        if ( 600 <= errno ) {
            codx  = "Ex" + Integer.toString(errno, 10);
            codx  = conf.translate("core.error", codx);
        } else
        if ( 400 <= errno ) {
            codx  = "Er" + Integer.toString(errno, 10);
            codx  = conf.translate("fore.error", codx);
        } else {
            codx  = "" ;
        }

        R: {
            if (mark != null && ! mark.isEmpty()) {
                mrkx  = conf.getProperty(mark);
                if (mrkx != null) break R;
            }

            if (codx != null && ! codx.isEmpty()) {
                mrkx  = conf.getProperty(codx);
                if (mrkx != null) break R;
            }

            if (error != null && ! error.isEmpty()) {
                mrkx  = error;
                break R;
            }

            Throwable erro = cause.getCause();
            if (erro != null) {
                if (errno < 400) {
                    return erro.getLocalizedMessage();
                } else {
                    mrkx = erro.getLocalizedMessage();
                    return (codx + " " + mrkx).trim();
                }
            }

            mrkx  = "" ;
        }

        // 注入消息参数
        if (null == mrkx  ||  mrkx.isEmpty( )) {
            mrkx  = "" ;
        } else
        if (null != cases && cases.length > 0) {
            mrkx  = Syno.inject(mrkx, cases );
        }

        return (codx +" "+ mrkx).trim();
    }

    private CoreConfig getConfig() {
        try {
            return CoreConfig.getInstance(conf);
        } catch (HongsExemption e) {
            String defs = "defetcs";
        if (826 == e.getErrno()
        && ! defs.equals(conf)) {
            return CoreConfig.getInstance(defs);
        } else {
            throw  e;
        }}
    }

    private CoreLocale getLocale() {
        /**
         * 在例如多线程环境下未初始化 Core 时
         * 出现异常会导致下方因找不到语言类别而提前终止
         * 从而无法获知真实的错误位置
         * 此时设为默认的语言让其继续
         */
        if (Core.ACTION_LANG.get() == null) {
            Core.ACTION_LANG.set(CoreConfig.getInstance().getProperty("core.language.default"));
            CoreLogger.error("ACTION_LANG is null in error or exception: " + cause.getMessage());
        }

        try {
            return CoreLocale.getInstance(conf);
        } catch (HongsExemption e) {
            String defs = "defetcs";
        if (826 == e.getErrno()
        && ! defs.equals(conf)) {
            return CoreLocale.getInstance(defs);
        } else {
            throw  e;
        }}
    }

}
