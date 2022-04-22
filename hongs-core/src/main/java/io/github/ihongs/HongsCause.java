package io.github.ihongs;

/**
 * 异常基类
 * @author Hongs
 * @param <T> 子类
 */
public interface HongsCause
{

    public int getErrno();

    public String getError();

    public int getState();

    public String getStage();

    public Throwable getCause();

    public String getMessage( );

    public String getLocalizedMessage();

    public String getLocalizedContext();

    public String getLocalizedContent();

    public String getFinalizedMessage();

    public Object[] getFinalizedOptions();

    public HongsCause setLocalizedContext(String    lang);

    public HongsCause setLocalizedContent(String    term);

    public HongsCause setFinalizedMessage(String    text);

    public HongsCause setFinalizedOptions(Object... opts);

    public HongsException toException();

    public HongsExemption toExemption();

}
