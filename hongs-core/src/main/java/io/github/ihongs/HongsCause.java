package io.github.ihongs;

/**
 * 异常基类
 * @author Hongs
 */
public interface HongsCause
{

    public int getErrno();

    public String getError();

    public Throwable getCause();

    public String getMessage( );

    public String getLocalizedMessage();

    public String getLocalizedContext();

    public String[] getLocalizedOptions();

    public HongsCause setLocalizedContext(String    lang);

    public HongsCause setLocalizedOptions(String... opts);

}
