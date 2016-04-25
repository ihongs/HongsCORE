package app.hongs;

/**
 * 异常基类
 * @author Hongs
 */
public interface HongsCause
{

    public int getCode();

    public String getDesc();

    public Throwable getCause();

    public String getMessage( );

    public String getLocalizedMessage();

    public String getLocalizedSection();

    public String[] getLocalizedOptions();

    public HongsCause setLocalizedSection(String lang);

    public HongsCause setLocalizedOptions(String... opts);

}
