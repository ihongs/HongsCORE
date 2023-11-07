package io.github.ihongs;

/**
 * 核心异常
 * @author Hongs
 */
public interface Crux extends HongsCause {

    @Override
    public CruxException toException();

    @Override
    public CruxExemption toExemption();

}
