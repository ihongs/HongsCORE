package io.github.ihongs.dh;

/**
 * 提交事务
 * @author Hongs
 */
public interface IReflux {

    public void begin( );

    public void commit();

    public void cancel();

}
