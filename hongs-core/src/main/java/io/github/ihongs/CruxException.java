package io.github.ihongs;

/**
 * 核心通用异常
 *
 * 部分方法说明请参考 CruxUtil
 * 与 CruxExemption 不同, 必须 throws
 *
 * <h3>取值范围:</h3>
 * <pre>
 * 核心: 600~999
 * 框架: 1000~9999
 * 用户: 10000~99999
 * 另外, 2~255 同 Exit Code, 300~599 同 Http Code
 * 推荐用 desc 参数的 @conf:mark 形式, 在配置和语言资源里定义描述和消息
 * </pre>
 *
 * @author Hongs
 */
public class CruxException
  extends        Exception
  implements CruxCause {

    protected final CruxDemon that;

    public CruxException(Throwable cause, int errno, String error, Object... cases) {
        super(cause);

        that = new CruxDemon(this, errno, error, cases);
    }

    public CruxException(Throwable cause, int errno, String error) {
        this(cause, errno, error, (Object[]) null);
    }

    public CruxException(Throwable cause, int errno) {
        this(cause, errno, null , (Object[]) null);
    }

    public CruxException(Throwable cause, String error, Object... cases) {
        this(cause,  0x0 , error, cases);
    }

    public CruxException(Throwable cause, String error) {
        this(cause,  0x0 , error, (Object[]) null);
    }

    public CruxException(Throwable cause) {
        this(cause,  0x0 , null , (Object[]) null);
    }

    public CruxException(int errno, String error, Object... cases) {
        this(null , errno, error, cases);
    }

    public CruxException(int errno, String error) {
        this(null , errno, error, (Object[]) null);
    }

    public CruxException(int errno) {
        this(null , errno, null , (Object[]) null);
    }

    public CruxException(String error, Object... cases) {
        this(null ,  0x0 , error, cases);
    }

    public CruxException(String error) {
        this(null ,  0x0 , error, (Object[]) null);
    }

    @Override
    public CruxException toException() {
        return this;
    }

    @Override
    public CruxExemption toExemption() {
        return new CruxExemption((Throwable) this.getCause(), this.getErrno(), this.getError(), this.getCases());
    }

    @Override
    public int getErrno() {
        return that.errno;
    }

    @Override
    public String getError() {
        return that.error;
    }

    @Override
    public Object[] getCases() {
        return that.cases;
    }

    @Override
    public int getState() {
        return that.getState();
    }

    @Override
    public String getStage() {
        return that.getStage();
    }

    @Override
    public String toString() {
        return that.toString();
    }

    @Override
    public String getMessage() {
        return that.getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return that.getLocalizedMessage();
    }

}
