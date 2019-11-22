package io.github.ihongs.util.verify;

import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;

/**
 * 单项错误
 * @author Hongs
 */
public class Wrong extends HongsException {
    private String text = null;
    private CoreLocale lang = null;

    public Wrong(Throwable cause, String desc, String... prms) {
        super(0x400, desc, cause);
        this.setLocalizedContext("default");
        this.setLocalizedOptions(   prms  );
    }

    public Wrong(String desc, String... prms) {
        super(0x400, desc  /**/ );
        this.setLocalizedContext("default");
        this.setLocalizedOptions(   prms  );
    }

    @Override
    public Wrong  setLocalizedOptions(String...  opts) {
        super.setLocalizedOptions(opts);
        return this;
    }

    @Override
    public Wrong  setLocalizedContext(String     name) {
        super.setLocalizedContext(name);
        this.lang = null;
        return this;
    }

    public Wrong  setLocalizedContext(CoreLocale lang) {
        this.lang = lang;
        return this;
    }

    public Wrong  setLocalizedCaption(String     text) {
        this.text = text;
        return this;
    }

    public String getLocalizedCaption() {
        return text;
    }

    public String getLocalizedMistake() {
        if (null== lang) {
            lang = CoreLocale.getInstance(getLocalizedContext());
        }
        return lang.translate(getError(), getLocalizedOptions());
    }

    @Override
    public String getLocalizedMessage() {
        if (null== lang) {
            lang = CoreLocale.getInstance(getLocalizedContext());
        }
        if (null!= text && 0 < text.length()) {
            return lang.translate(text)
            +": "+ lang.translate(getError(), getLocalizedOptions());
        } else {
            return lang.translate(getError(), getLocalizedOptions());
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
