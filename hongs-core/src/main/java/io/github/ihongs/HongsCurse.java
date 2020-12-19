package io.github.ihongs;

/**
 * 异常本地化工具
 * @author Hongs
 */
public final class HongsCurse {

    private final       int code;
    private final    String desc;
    private final Throwable that;

    private String   lang;
    private String   term;
    private String[] opts;

    HongsCurse( int errno, String error, Throwable cause ) {
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
     * 接口状态
     * @return
     */
    public int getState() {
        if ( 600 > code ) {
            return code ;
        }
        String     codx = "Ex"+ Integer.toString(code, 10 );
        CoreConfig conf = CoreConfig.getInstance("defects");
        if (null!= lang ) {
            conf = conf.clone();
            conf . load( lang );
        }
        return conf.getProperty(codx, code);
    }

    /**
     * 接口代码
     * @return
     */
    public String getStage() {
        int code  = getState(); // 尝试转换
        if (code >= 600 ) {
            return "Ex" + Integer.toString(code, 10);
        } else
        if (code >= 400 ) {
            return "Er" + Integer.toString(code, 10);
        }

        String codx;
        if (term != null
        && !term.isEmpty()) {
            codx  = term;
        } else
        if (desc != null
        && !desc.isEmpty()) {
            codx  = desc;
        } else
        {
            codx  = "error";
        }

        // 增加模块前缀
        if (lang != null
        && !lang.isEmpty()
        && !lang.equals("default")
        && !lang.equals("defects")) {
            String coda;
            coda  = lang.replace('/' , '.')
                        .replace('\\', '.')
                  + "." ;
            if (!codx.startsWith(coda)) {
                 codx  = (coda + codx);
            }
        }

        return codx;
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
        String codx, desx;

        if (code >= 600 ) {
            codx  = "Ex" + Integer.toString(code, 10);
        } else
        if (code >= 400 ) {
            codx  = "Er" + Integer.toString(code, 10);
        } else {
            codx  = "";
        }

        // 优先错误描述
        if (null != desc) {
            desx  = desc;
        } else
        if (null != term) {
            desx  = term;
        } else {
          Throwable erro = that.getCause();
        if (null != erro) {
            if (code < 2) {
                return erro.getMessage();
            } else {
                desx = erro.getMessage();
            }
        if (desx == null) {
            desx  = "";
        }
        } else {
            desx  = "";
        }}

        return (codx +" "+ desx).trim( );
    }

    /**
     * 获取本地消息
     * @return
     */
    public String getLocalizedMessage() {
        String codx, desx;

        if (code >= 600 ) {
            codx  = "Ex" + Integer.toString(code, 10);
        } else
        if (code >= 400 ) {
            codx  = "Er" + Integer.toString(code, 10);
        } else {
            codx  = "";
        }

        // 优先本地描述
        if (null != term) {
            desx  = term;
        } else
        if (null != desc) {
            desx  = desc;
        } else {
          Throwable erro = that.getCause();
        if (null != erro) {
            if (code < 2) {
                return erro.getLocalizedMessage();
            } else {
                desx = erro.getLocalizedMessage();
            }
        if (desx == null) {
            desx  = "";
        }
        } else {
            desx  = "";
        }}

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

        /**
         * 存在错误解释就按错误作翻译
         * 存在代号语句就按代号来翻译
         */
        CoreLocale trns = CoreLocale.getInstance("defects");
        if (null!= lang) {
            trns = trns.clone();
            trns . load( lang );
        }
        if (trns.getProperty(desx) != null) {
            desx = trns.translate( desx, opts != null ? opts : new String[] {} );
        } else
        if (trns.getProperty(codx) != null) {
            desx = trns.translate( codx, opts != null ? opts : new String[] {} );
        }
        if (code < 600 ) {
            codx = trns.translate("fore.error" , codx);
        } else {
            codx = trns.translate("core.error" , codx);
        }

        return (codx +" "+ desx).trim( );
    }

    /**
     * 获取翻译配置
     * @return
     */
    public String   getLocalizedContext() {
        return this.lang;
    }

    /**
     * 获取翻译短语
     * @return
     */
    public String   getLocalizedContent() {
        return this.term;
    }

    /**
     * 获取翻译选项
     * @return
     */
    public String[] getLocalizedOptions() {
        return this.opts;
    }

    /**
     * 设置翻译配置
     * @param lang
     */
    public void setLocalizedContext(String    lang) {
        this.lang = lang;
    }

    /**
     * 设置翻译短语
     * @param term
     */
    public void setLocalizedContent(String    term) {
        this.term = term;
    }

    /**
     * 设置翻译选项
     * @param opts
     */
    public void setLocalizedOptions(String... opts) {
        this.opts = opts;
    }

}
