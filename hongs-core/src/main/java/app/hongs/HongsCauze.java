package app.hongs;

/**
 * 异常本地化工具
 * @author Hongs
 */
public final class HongsCauze {

    private final int       code;
    private final String    desc;
    private final Throwable that;

    private String   lang;
    private String[] opts;

    HongsCauze( int errno, String error, Throwable cause ) {
        this.code = errno;
        this.desc = error;
        this.that = cause;
    }

    /**
     * 获取代号
     * @return
     */
    public int getErrno() {
        return this.code;
    }

    /**
     * 获取描述
     * @return
     */
    public String getError() {
        return this.desc;
    }

    /**
     * 获取消息
     * @return
     */
    public String getMessage()
    {
        String codx = "Ex" + Integer.toHexString(code);
        String desx = desc != null ? desc : "";
        if (null  !=  lang) {
            codx   =  lang
                .replaceAll  ("[/\\\\]", ".")
                .replaceFirst("_error$", "" )
                +"."+ codx ;
        }
        if (null  ==  desc) {
            Throwable erro = this.that.getCause();
            if (null != erro) {
                if (  erro instanceof HongsCause) {
                    return erro.getMessage();
                }
                desx  = erro.getClass().getName()
                     +" "+ erro.getMessage();
            }
        }
        return  codx +" "+ desx;
    }

    /**
     * 获取本地消息
     * @return
     */
    public String getLocalizedMessage() {
        CoreLocale trns;
        String ckey, dkey;
        String codx, desx;
        String [ ] optx;

        /**
         * 在例如多线程环境下未初始化 Core 时
         * 出现异常会导致下方因找不到语言类别而提前终止
         * 从而无法获知真实的错误位置
         * 此时设为默认的语言让其继续
         */
        if (Core.ACTION_LANG.get() == null) {
            Core.ACTION_LANG.set(CoreConfig.getInstance().getProperty("core.language.default"));
            CoreLogger.error("ACTION_LANG is null in error or exception: " + that.getMessage());
        }

        trns = CoreLocale.getInstance("_error_").clone();
        codx = "Ex" +  Integer.toHexString(code);
        desx = desc != null ? desc : "";
        optx = opts != null ? opts : new String[0];

        // 0x10,0x1000 为通用一般异常代号
        // 0x11,0x1001 使用消息作为语言键
        switch (code) {
            case HongsException.COMMON:
            case     HongsError.COMMON:
                // 包裹其他异常和错误
                if (null == desc) {
                  Throwable them  =  that.getCause(  );
                if (null != them) {
                    return  them.getLocalizedMessage();
                }
                }
                ckey = "fore.error";
                dkey =  "" ;
                break;
            case HongsException.NOTICE:
            case     HongsError.NOTICE:
                ckey = "fore.error";
                dkey = desx;
                break;
            default:
                ckey = "core.error";
                dkey = "error." + codx;
        }

        if (null  !=  lang) {
            trns.load(lang);
            codx   =  lang
                .replaceAll  ("[/\\\\]", ".")
                .replaceFirst("_error$", "" )
                +"."+ codx ;
        }
        if (trns.containsKey(ckey)) {
            codx = trns.translate(ckey, codx);
        }
        if (trns.containsKey(dkey)) {
            desx = trns.translate(dkey, optx);
        } else {
            Throwable cause = this.that.getCause( );
            if (null != cause && cause instanceof HongsCause) {
                return  cause.getLocalizedMessage();
            }
        }

        return  codx +" "+ desx;
    }

    /**
     * 获取翻译章节(模块语言)
     * @return
     */
    public String getLocalizedSection() {
        return this.lang;
    }

    /**
     * 获取翻译选项(填充参数)
     * @return
     */
    public String[] getLocalizedOptions() {
        return this.opts;
    }

    /**
     * 设置翻译章节(模块语言)
     * @param lang
     */
    public void setLocalizedSection(String lang) {
        this.lang = lang;
    }

    /**
     * 设置翻译选项(填充参数)
     * @param opts
     */
    public void setLocalizedOptions(String... opts) {
        this.opts = opts;
    }

}
