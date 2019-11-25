package io.github.ihongs.dh;

/**
 * 提交事务
 * 当 Core.getInstance().containsKey(Cnst.REFLUX_MODE) 时启用事务, 否则即时提交
 * @author Hongs
 */
public interface IReflux {

    public void begin( );

    public void commit();

    public void revert();

}
