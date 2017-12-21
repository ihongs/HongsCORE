package app.hongs.util.verify;

import app.hongs.CoreLocale;
import app.hongs.HongsException;

/**
 * 单项错误
 * @author Hongs
 */
public class Wrong extends HongsException {
    private String text = null;

    public Wrong(Throwable cause, String desc, String... prms) {
        super(0x1100, desc, cause);
        this.setLocalizedContext("default");
        this.setLocalizedOptions(   prms  );
    }

    public Wrong(String desc, String... prms) {
        super(0x1100, desc  /**/ );
        this.setLocalizedContext("default");
        this.setLocalizedOptions(   prms  );
    }

    @Override
    public Wrong  setLocalizedOptions(String... opts) {
        super.setLocalizedOptions(opts);
        return this;
    }

    @Override
    public Wrong  setLocalizedContext(String name) {
        super.setLocalizedContext(name);
        return this;
    }

    public Wrong  setLocalizedCaption(String text) {
        this.text = text;
        return this;
    }

    public String getLocalizedCaption() {
        return text;
    }

    @Override
    public String getLocalizedMessage() {
        CoreLocale trns = CoreLocale.getInstance(getLocalizedContext());
        if (null!= text && 0 < text.length()) {
            return trns.translate(text)
            +": "+ trns.translate(getError(), getLocalizedOptions());
        } else {
            return trns.translate(getError(), getLocalizedOptions());
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
