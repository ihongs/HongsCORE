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

    public HongsExemption(Throwable fact, int code, String desc) {
        super(fact);

        that = new HongsCurse(code, desc, this);
    }

    public HongsExemption(Throwable fact, String desc) {
        this(fact, 0x0 , null);
    }

    public HongsExemption(Throwable fact, int code) {
        this(fact, code, null);
    }

    public HongsExemption(int code, String desc) {
        this(null, code, desc);
    }

    public HongsExemption(Throwable fact) {
        this(fact, 0x0 , null);
    }

    public HongsExemption(String desc) {
        this(null, 0x0 , desc);
    }

    public HongsExemption(int code) {
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
    public HongsExemption setLocalizedContext(String    lang) {
        that.setLocalizedContext(lang);
        return this;
    }

    @Override
    public HongsExemption setLocalizedContent(String    term) {
        that.setLocalizedContent(term);
        return this;
    }

    @Override
    public HongsExemption setFinalizedMessage(String    text) {
        that.setLocalizedContent(text);
        return this;
    }

    @Override
    public HongsExemption setFinalizedOptions(Object... opts) {
        that.setFinalizedOptions(opts);
        return this;
    }

    @Deprecated
    public HongsExemption setLocalizedOptions(Object... opts) {
        that.setFinalizedOptions(opts);
        return this;
    }

    @Override
    public HongsExemption toExemption() {
        return this;
    }

    @Override
    public HongsException toException() {
        return new HongsException(this, this.getErrno(), this.getError())
             .setLocalizedContext(this.getLocalizedContext())
             .setLocalizedContent(this.getLocalizedContent())
             .setFinalizedMessage(this.getFinalizedMessage())
             .setFinalizedOptions(this.getFinalizedOptions());
    }

}
