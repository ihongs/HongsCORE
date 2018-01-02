package app.hongs.util.verify;

import app.hongs.HongsException;

/**
 * 规则函数
 * @author Hongs
 */
public interface Ruly {
    
    public Object verify(Object value, Rule rule) throws Wrong, Wrongs, HongsException;

}
