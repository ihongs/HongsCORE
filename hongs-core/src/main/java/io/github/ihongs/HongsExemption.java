package io.github.ihongs;

/**
 * 通用免责类
 *
 * 部分方法说明请参考 HongsCurse
 * 与 HongsException 不同, 无需 throws;
 *
 * <h3>取值范围:</h3>
 * <pre>
 * 核心: 400~999
 * 框架: 1000~9999
 * 用户: 10000~99999
 * </pre>
 *
 * @author Hongs
 * @deprecated Use CruxExemption instead
 */
public class HongsExemption
  extends  RuntimeException
  implements HongsCause {

    protected final HongsFault that;

    public HongsExemption(Throwable cause, int errno, String error, Object... cases) {
        super(cause);

        that = new HongsFault(this, errno, error, cases);
    }

    public HongsExemption(Throwable cause, int errno, String error) {
        this(cause, errno, error, (Object[]) null);
    }

    public HongsExemption(Throwable cause, int errno) {
        this(cause, errno, null , (Object[]) null);
    }

    public HongsExemption(Throwable cause, String error, Object... cases) {
        this(cause,  0x0 , error, cases);
    }

    public HongsExemption(Throwable cause, String error) {
        this(cause,  0x0 , error, (Object[]) null);
    }

    public HongsExemption(Throwable cause) {
        this(cause,  0x0 , null , (Object[]) null);
    }

    public HongsExemption(int errno, String error, Object... cases) {
        this(null , errno, error, cases);
    }

    public HongsExemption(int errno, String error) {
        this(null , errno, error, (Object[]) null);
    }

    public HongsExemption(int errno) {
        this(null , errno, null , (Object[]) null);
    }

    public HongsExemption(String error, Object... cases) {
        this(null ,  0x0 , error, cases);
    }

    public HongsExemption(String error) {
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
    public HongsExemption toExemption() {
        return this;
    }

    @Override
    public HongsException toException() {
        return new HongsException((Throwable) this.getCause(), this.getErrno(), this.getError(), this.getCases());
    }

}
