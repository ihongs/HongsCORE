package io.github.ihongs;

/**
 * 异常基类
 * @author Hongs
 */
public interface CruxCause
{

    /**
     * 获取异常代码
     * @return
     */
    public int getErrno();

    /**
     * 获取异常描述
     * @return
     */
    public String getError();

    /**
     * 获取接口状态
     * @return
     */
    public int getState();

    /**
     * 获取接口代码
     * @return
     */
    public String getStage();

    /**
     * 获取异常参数
     * @return
     */
    public Object [] getCases();

    /**
     * 获取异常原因
     * @return
     */
    public Throwable getCause();

    /**
     * 获取异常消息
     * @return
     */
    public String getMessage( );

    /**
     * 获取本地消息
     * @return
     */
    public String getLocalizedMessage();

    /**
     * 转需检查异常
     * @return
     */
    public CruxException toException();

    /**
     * 转无检查异常
     * @return
     */
    public CruxExemption toExemption();

    /**
     * 判断异常类别
     * @param ern
     * @return
     */
    public default boolean like (int ern) {
        return getErrno() == ern;
    }

    /**
     * 判断异常类别
     * @param err
     * @return
     */
    public default boolean like (String err) {
        return getError().equals(err);
    }

}
