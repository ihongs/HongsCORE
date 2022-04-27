package io.github.ihongs;

/**
 * 通用免责类
 *
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
 */
public class HongsExemption
  extends  RuntimeException
  implements HongsCause {

    protected final HongsCurse that;

    public HongsExemption(Throwable fact, int code, String desc, Object... data) {
        super(fact);

        that = new HongsCurse(this, code, desc, data);
    }

    public HongsExemption(Throwable fact, String desc, Object... data) {
        this(fact, 0x1 , desc, data);
    }

    public HongsExemption(int code, String desc, Object... data) {
        this(null, code, desc, data);
    }

    public HongsExemption(String desc, Object... data) {
        this(null, 0x1 , desc, data);
    }

    public HongsExemption(Throwable fact, int code) {
        this(fact, code, null, (Object[]) null);
    }

    public HongsExemption(Throwable fact) {
        this(fact, 0x1 , null, (Object[]) null);
    }

    public HongsExemption(int code) {
        this(null, code, null, (Object[]) null);
    }

    public HongsExemption(Throwable fact, String desc) {
        this(fact, 0x1 , desc, (Object[]) null);
    }

    public HongsExemption(int code, String desc) {
        this(null, code, desc, (Object[]) null);
    }

    public HongsExemption(String desc) {
        this(null, 0x1 , desc, (Object[]) null);
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
