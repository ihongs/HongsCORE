package app.hongs.util.verify;

import app.hongs.HongsException;
import java.util.Map;

public abstract class Rule {
    /**
     * 返回此对象将被抛弃
     */
    public static final Object FALSE = new Object();

    public Map  values = null;
    public Map  params = null;
    public Veri helper ;

    public void setValues(Map  values) {
        this.values = values;
    }
    public void setParams(Map  params) {
        this.params = params;
    }
    public void setHelper(Veri helper) {
        this.helper = helper;
    }

    public abstract Object verify(Object value) throws Wrong, Wrongs, HongsException;
}
