package io.github.ihongs.util.verify;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.Syno;

/**
 * 单项错误
 * @author Hongs
 */
public class Wrong extends HongsException {
    private String name = null;

    public Wrong(Throwable cause, String term, Object... opts) {
        super(cause, term, opts);
    }

    public Wrong(String term, Object... opts) {
        super(term , opts);
    }

    /**
     * 设置字段标题
     * @param name
     */
    public  void  setLocalizedCaption(String name) {
        this.name = name;
    }

    /**
     * 获取字段标题
     * @return
     */
    public String getLocalizedCaption() {
        return this.name;
    }

    /**
     * 获取错误内容(包含字段标题)
     * @return
     */
    public String getLocalizedMistake() {
        String msgs  = this.getLocalizedMessage();
        return name != null && ! name.isEmpty()
             ? name +": "+ msgs: msgs;
    }

    @Override
    public String getMessage() {
        return getLocalizedMessage( );
    }

    @Override
    public String toString() {
        return getLocalizedMistake( );
    }

    @Override
    public int getState() {
        return 400;
    }
}
