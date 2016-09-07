package app.hongs.util.verify;

import app.hongs.HongsException;
import app.hongs.util.Synt;
import java.util.Map;

/**
 * 规则基类
 * @author Hongs
 */
public abstract class Rule {
    /**
     * 返回此对象将被抛弃, 后续处理器会被跳过
     */
    public static final Object BLANK = Synt.LOOP.NEXT;
    public static final Object BREAK = Synt.LOOP.LAST;
    public static final Object EMPTY = null;

    public Map params = null;
    public Map values = null;
    public Map cleans = null;
    public Verify helper;

    public void setParams(Map params) {
        this.params = params;
    }
    public void setValues(Map values) {
        this.values = values;
    }
    public void setCleans(Map cleans) {
        this.cleans = cleans;
    }
    public void setHelper(Verify helper) {
        this.helper = helper;
    }

    public abstract Object verify(Object value) throws Wrong, Wrongs, HongsException;
}
