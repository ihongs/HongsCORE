package io.github.ihongs.util.verify;

/**
 * 基础规则
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
     */
    public Object verify(Object value, Wheel watch) throws Wrong, Wrongs;

}
