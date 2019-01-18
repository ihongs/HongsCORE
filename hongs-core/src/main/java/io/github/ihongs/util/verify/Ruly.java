package io.github.ihongs.util.verify;

/**
 * 基础规则
 * @author Hongs
 */
public interface Ruly {

    /**
     * 校验
     * @param watch
     * @return
     * @throws Wrong
     * @throws Wrongs
     */
    public Object verify(Value watch) throws Wrong, Wrongs;

}
