package io.github.ihongs;

/**
 * 通用异常类
 *
 * 与 HongsExemption 不同, 必须 throws
 *
 * <h3>取值范围:</h3>
 * <pre>
 * 核心: 0x1000~0xFFFF (4096~65535)
 * 用户: 0x10000~0xFFFFF (65536~1048575)
 * </pre>
 *
 * @author Hongs
 */
public class HongsException
     extends      Exception
  implements HongsCause {

    public static final int COMMON = 0;
    public static final int NOTICE = 1;

    protected final HongsCurse that;

    public HongsException(int code, String desc, Throwable fact) {
        super(fact);

        that = new HongsCurse(code, desc, this);
    }

    public HongsException(String desc, Throwable fact) {
        this(0x0 , null, fact);
    }

    public HongsException(int code, Throwable fact) {
        this(code, null, fact);
    }

    public HongsException(int code, String desc) {
        this(code, desc, null);
    }

    public HongsException(Throwable fact) {
        this(0x0 , null, fact);
    }

    public HongsException(String desc) {
        this(0x0 , desc, null);
    }

    public HongsException(int code) {
        this(code, null, null);
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
    public String[] getLocalizedOptions() {
        return that.getLocalizedOptions();
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
    public HongsException setLocalizedOptions(String... opts) {
        that.setLocalizedOptions(opts);
        return this;
    }

    @Override
    public HongsException toException() {
        return this;
    }

    @Override
    public HongsExemption toExemption() {
        return new HongsExemption(this.getErrno(), this.getError(), this)
             .setLocalizedOptions(this.getLocalizedOptions())
             .setLocalizedContext(this.getLocalizedContext());
    }

    /**
     * 常规错误(无需错误代码)
     * @deprecated 请改用 HongsException(String, Throwable)
     */
    public static final class Common extends HongsException {
        public Common(String error, Throwable cause) {
            super(COMMON, error,cause);
        }
        public Common(Throwable cause) {
            super(COMMON, cause);
        }
        public Common(String error) {
            super(COMMON, error);
        }
    }

    /**
     * 通告错误(无需错误代码)
     * @deprecated 请改用 HongsException(String, Throwable)
     */
    public static final class Notice extends HongsException {
        public Notice(String error, Throwable cause) {
            super(NOTICE, error,cause);
        }
        public Notice(Throwable cause) {
            super(NOTICE, cause);
        }
        public Notice(String error) {
            super(NOTICE, error);
        }
    }

}
