package io.github.ihongs.util.verify;

import io.github.ihongs.HongsException;

/**
 * 单项错误
 * @author Hongs
 */
public class Wrong extends HongsException {
    private String name = null;

    public Wrong(Throwable cause, String term, Object... opts) {
        super(cause, 400, term(term), opts);
    }

    public Wrong(String term, Object... opts) {
        super( /***/ 400, term(term), opts);
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
     * 获取错误内容(不含字段名)
     * @return
     */
    public String getLocalizedMistake() {
        String msgs = super.getLocalizedMessage();
        return msgs ;
    }

    /**
     * 获取错误内容(含有字段名)
     * @return
     */
    public String getLocalizedMessage() {
        String msgs = super.getLocalizedMessage();
        return name != null && ! name.isEmpty()
             ? name +": "+ msgs: msgs;
    }

    @Override
    public String getMessage() {
        return getLocalizedMistake( );
    }

    private static String term (String term) {
        if (term != null
        &&  term.length() >= 0x1
        &&  term.length() <= 128
        &&  term.matches("^[0-9A-Za-z_.]$")) {
            term  = "deafult:" + term;
        }
        return term ;
    }
}
