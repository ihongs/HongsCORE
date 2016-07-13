package app.hongs.util.verify;

import app.hongs.HongsException;
import java.util.Map;

/**
 * 规则基类
 * @author Hongs
 */
public abstract class Rule {
    /**
     * 返回此对象将被抛弃, 后续处理器会被跳过
     */
    public static final Object BLANK = new Object();
    public static final Object EMPTY = null;

    public Map params = null;
    public Map values = null;
    public Verify helper;

    public void setParams(Map params) {
        this.params = params;
    }
    public void setValues(Map values) {
        this.values = values;
    }
    public void setHelper(Verify helper) {
        this.helper = helper;
    }

    public abstract Object verify(Object value) throws Wrong, Wrongs, HongsException;
}
