package io.github.ihongs.util.verify;

import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;

/**
 * 单项错误
 * @author Hongs
 */
public class Wrong extends HongsException {
    private String     name = null;
    private CoreLocale lang = null;

    public Wrong(Throwable cause, String term, String... opts) {
        super(400 , term , cause);
        this.setLocalizedContext("default");
        this.setLocalizedContent(term);
        this.setFinalizedOptions(opts);
    }

    public Wrong(String term, String... opts) {
        super(400 , term );
        this.setLocalizedContext("default");
        this.setLocalizedContent(term);
        this.setLocalizedOptions(opts);
    }

    /**
     * 设置错误参数
     * @param opts
     * @return
     */
    @Override
    @Deprecated
    public Wrong  setLocalizedOptions(Object...  opts) {
        super.setLocalizedOptions(opts);
        return this;
    }

    /**
     * 设置错误参数
     * @param opts
     * @return
     */
    @Override
    public Wrong  setFinalizedOptions(Object...  opts) {
        super.setFinalizedOptions(opts);
        return this;
    }

    /**
     * 设置错误消息
     * @param text
     * @return
     */
    @Override
    public Wrong  setFinalizedMessage(String     text) {
        super.setFinalizedMessage(text);
        return this;
    }

    /**
     * 设置错误短语
     * @param term
     * @return
     */
    @Override
    public Wrong  setLocalizedContent(String     term) {
        super.setLocalizedContent(term);
        return this;
    }

    /**
     * 设置语言对象 (以便复用)
     * @param lang
     * @return
     */
    public Wrong  setLocalizedContext(CoreLocale lang) {
        super.setLocalizedContext(null);
        this.lang = lang;
        return this;
    }

    /**
     * 设置区块名称
     * @param name
     * @return
     */
    @Override
    public Wrong  setLocalizedContext(String     name) {
        super.setLocalizedContext(name);
        this.lang = null;
        return this;
    }

    /**
     * 设置字段标签
     * @param name
     * @return
     */
    public Wrong  setLocalizedCaption(String     name) {
        this.name = name;
        return this;
    }

    /**
     * 获取字段标签
     * @return
     */
    public String getLocalizedCaption() {
        return name;
    }

    /**
     * 获取错误内容
     * @return 
     */
    public String getLocalizedMistake() {
        if (null== lang) {
            lang = CoreLocale.getInstance(getLocalizedContext());
        }
        return lang.translate(getError(), getFinalizedOptions());
    }

    /**
     * 获取错误消息 (含字段名)
     * @return 
     */
    @Override
    public String getLocalizedMessage() {
        if (null== lang) {
            lang = CoreLocale.getInstance(getLocalizedContext());
        }
        if (null!= name && 0 < name.length()) {
            return lang.translate(name)
            +": "+ lang.translate(getLocalizedContent(), getFinalizedOptions());
        } else {
            return lang.translate(getLocalizedContent(), getFinalizedOptions());
        }
    }

    @Override
    public String getMessage() {
        return getLocalizedMessage();
    }

    @Override
    public String toString() {
        return getLocalizedMessage();
    }
}
