package io.github.ihongs;

/**
 * 通用异常类
 *
 * 部分方法说明请参考 HongsCurse
 * 与 HongsExemption 不同, 必须 throws
 *
 * <h3>取值范围:</h3>
 * <pre>
 * 核心: 400~999
 * 框架: 1000~9999
 * 用户: 10000~99999
 * </pre>
 *
 * @author Hongs
 * @deprecated Use CruxException instead
 */
public class HongsException
     extends      Exception
  implements HongsCause {

    protected final HongsCurse that;

    public HongsException(Throwable cause, int errno, String error, Object... cases) {
        super(cause);

        that = new HongsCurse(this, errno, error, cases);
    }

    public HongsException(Throwable cause, int errno, String error) {
        this(cause, errno, error, (Object[]) null);
    }

    public HongsException(Throwable cause, int errno) {
        this(cause, errno, null , (Object[]) null);
    }

    public HongsException(Throwable cause, String error, Object... cases) {
        this(cause,  0x0 , error, cases);
    }

    public HongsException(Throwable cause, String error) {
        this(cause,  0x0 , error, (Object[]) null);
    }

    public HongsException(Throwable cause) {
        this(cause,  0x0 , null , (Object[]) null);
    }

    public HongsException(int errno, String error, Object... cases) {
        this(null , errno, error, cases);
    }

    public HongsException(int errno, String error) {
        this(null , errno, error, (Object[]) null);
    }

    public HongsException(int errno) {
        this(null , errno, null , (Object[]) null);
    }

    public HongsException(String error, Object... cases) {
        this(null ,  0x0 , error, cases);
    }

    public HongsException(String error) {
        this(null ,  0x0 , error, (Object[]) null);
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

    @Override
    public HongsException toException() {
        return this;
    }

    @Override
    public HongsExemption toExemption() {
        return new HongsExemption((Throwable) this.getCause(), this.getErrno(), this.getError(), this.getCases());
    }

}
