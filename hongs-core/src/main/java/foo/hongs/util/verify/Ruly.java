package foo.hongs.util.verify;

import foo.hongs.HongsException;

/**
 * 规则函数
 * @author Hongs
 */
public interface Ruly {
    
    public Object verify(Object value, Rule rule) throws Wrong, Wrongs, HongsException;

}
