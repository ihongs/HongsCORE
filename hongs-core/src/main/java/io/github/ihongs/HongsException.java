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
 */
public class HongsException
     extends      Exception
  implements HongsCause {

    protected final HongsCurse that;

    public HongsException(Throwable fact, int code, String desc, Object... data) {
        super(fact);

        that = new HongsCurse(this, code, desc, data);
    }

    public HongsException(Throwable fact, int code, String desc) {
        this(fact, code, desc, (Object[]) null);
    }

    public HongsException(Throwable fact, int code) {
        this(fact, code, null, (Object[]) null);
    }

    public HongsException(Throwable fact, String desc, Object... data) {
        this(fact, 0x0 , desc, data);
    }

    public HongsException(Throwable fact, String desc) {
        this(fact, 0x0 , desc, (Object[]) null);
    }

    public HongsException(Throwable fact) {
        this(fact, 0x0 , null, (Object[]) null);
    }

    public HongsException(int code, String desc, Object... data) {
        this(null, code, desc, data);
    }

    public HongsException(int code, String desc) {
        this(null, code, desc, (Object[]) null);
    }

    public HongsException(int code) {
        this(null, code, null, (Object[]) null);
    }

    public HongsException(String desc, Object... data) {
        this(null, 0x0 , desc, data);
    }

    public HongsException(String desc) {
        this(null, 0x0 , desc, (Object[]) null);
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
