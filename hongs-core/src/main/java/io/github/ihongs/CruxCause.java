package io.github.ihongs;

/**
 * 异常基类
 * @author Hongs
 */
public interface CruxCause
{

    public int getErrno();

    public String getError();

    public int getState();

    public String getStage();

    public Object [] getCases();

    public Throwable getCause();

    public String getMessage( );

    public String getLocalizedMessage();

    public CruxException toException();

    public CruxExemption toExemption();

}
