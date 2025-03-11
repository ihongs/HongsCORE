package io.github.ihongs.util.verify;

import io.github.ihongs.CruxException;

/**
 * 单项错误
 * @author Hongs
 */
public class Wrong extends CruxException {
    private Object name = null;

    public Wrong(Throwable cause, String term, Object ... opts) {
        super(cause, term, opts);
    }

    public Wrong(String term, Object ... opts) {
        super( null, term, opts);
    }

    public Wrong(Throwable cause, String term) {
        super(cause, term, (Object[]) null);
    }

    public Wrong(String term) {
        super( null, term, (Object[]) null);
    }

    /**
     * 设置字段标签
     * @param name
     * @return
     */
    public Wrong withLabel (Object name) {
        this.name = name;
        return this;
    }

    /**
     * 获取字段标签
     * @return
     */
    public Object getLabel () {
        return this.name;
    }

    /**
     * 获取错误内容(不含字段标签)
     * @return
     */
    public String getLocalizedMistake() {
        return super.getLocalizedMessage();
    }

    /**
     * 获取错误内容(包含字段标签)
     * @return
     */
    @Override
    public String getLocalizedMessage() {
        String msgs = this.getLocalizedMistake();
        if (name  ==  null) return msgs;
        String nams = name. toString ();
        if (nams.isEmpty()) return msgs;
        return nams + ": "+ msgs;
    }

    @Override
    public String getMessage() {
        return getLocalizedMessage( );
    }

    @Override
    public String toString() {
        return getLocalizedMessage( );
    }

    @Override
    public int getState() {
        return 400;
    }
}
