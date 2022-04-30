package io.github.ihongs;

/**
 * 异常基类
 * @author Hongs
 */
public interface HongsCause
{

    public int getErrno();

    public String getError();

    public int getState();

    public String getStage();

    public Object [] getCases();

    public Throwable getCause();

    public String getMessage( );

    public String getLocalizedMessage();

    public HongsException toException();

    public HongsExemption toExemption();

}
