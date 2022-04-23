package io.github.ihongs;

/**
 * 通用异常类
 *
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

    public HongsException(Throwable fact, int code, String desc) {
        super(fact);

        that = new HongsCurse(code, desc, this);
    }

    public HongsException(Throwable fact, String desc) {
        this(fact, 0x0 , null);
    }

    public HongsException(Throwable fact, int code) {
        this(fact, code, null);
    }

    public HongsException(int code, String desc) {
        this(null, code, desc);
    }

    public HongsException(Throwable fact) {
        this(fact, 0x0 , null);
    }

    public HongsException(String desc) {
        this(null, 0x0 , desc);
    }

    public HongsException(int code) {
        this(null, code, null);
    }

    @Override
    public int getErrno() {
        return that.getErrno();
    }

    @Override
    public String getError() {
        return that.getError();
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
    public String getLocalizedContext() {
        return that.getLocalizedContext();
    }

    @Override
    public String getLocalizedContent() {
        return that.getLocalizedContent();
    }

    @Override
    public String getFinalizedMessage() {
        return that.getFinalizedMessage();
    }

    @Override
    public Object[] getFinalizedOptions() {
        return that.getFinalizedOptions();
    }

    @Deprecated
    public Object[] getLocalizedOptions() {
        return that.getFinalizedOptions();
    }

    @Override
    public HongsException setLocalizedContext(String    lang) {
        that.setLocalizedContext(lang);
        return this;
    }

    @Override
    public HongsException setLocalizedContent(String    term) {
        that.setLocalizedContent(term);
        return this;
    }

    @Override
    public HongsException setFinalizedMessage(String    text) {
        that.setFinalizedMessage(text);
        return this;
    }

    @Override
    public HongsException setFinalizedOptions(Object... opts) {
        that.setFinalizedOptions(opts);
        return this;
    }

    @Deprecated
    public HongsException setLocalizedOptions(Object... opts) {
        that.setFinalizedOptions(opts);
        return this;
    }

    @Override
    public HongsException toException() {
        return this;
    }

    @Override
    public HongsExemption toExemption() {
        return new HongsExemption(this, this.getErrno(), this.getError())
             .setLocalizedContext(this.getLocalizedContext())
             .setLocalizedContent(this.getLocalizedContent())
             .setFinalizedMessage(this.getFinalizedMessage())
             .setFinalizedOptions(this.getFinalizedOptions());
    }

}
