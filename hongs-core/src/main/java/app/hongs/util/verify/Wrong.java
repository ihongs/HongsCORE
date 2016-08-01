package app.hongs.util.verify;

import app.hongs.CoreLocale;
import app.hongs.HongsException;

/**
 * 单项错误
 * @author Hongs
 */
public class Wrong extends HongsException {
    private String name = null;

    public Wrong(Throwable cause, String desc, String... prms) {
        super(0x1100, desc, cause);
        this.setLocalizedSection("default");
        this.setLocalizedOptions(   prms  );
    }

    public Wrong(String desc, String... prms) {
        super(0x1100, desc  /***/);
        this.setLocalizedSection("default");
        this.setLocalizedOptions(   prms  );
    }

    public Wrong  setLocalizedSegment(String name) {
        this.name = name;
        return this;
    }

    public String getLocalizedSegment() {
        return name;
    }

    @Override
    public String getLocalizedMessage() {
        CoreLocale trns = CoreLocale.getInstance(getLocalizedSection());
        if (null!= name && !"".equals(name)) {
            return trns.translate(name) + " " +
                   trns.translate(getError(), getLocalizedOptions());
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
