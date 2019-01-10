package io.github.ihongs.util.verify;

import io.github.ihongs.HongsException;

/**
 * 规则函数
 * @author Hongs
 */
public interface Ruly {
    
    /**
     * 校验
     * @param value
     * @param watch
     * @return
     * @throws Wrong
     * @throws Wrongs
     * @throws HongsException 
     */
    public Object verify(Object value, Verity watch) throws Wrong, Wrongs, HongsException;

}
