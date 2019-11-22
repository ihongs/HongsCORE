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
public class HongsException extends Exception implements HongsCause {

    public static final int COMMON = HongsCurse.COMMON;
    public static final int NOTICE = HongsCurse.NOTICE;

    protected final HongsCurse that;

    public HongsException(int errno, String error, Throwable cause) {
        super(cause);

        that = new HongsCurse(errno, error, this);
    }

    public HongsException(int code, Throwable cause) {
        this(code, cause.getMessage(), cause);
    }

    public HongsException(int code, String desc) {
        this(code, desc, null);
    }

    public HongsException(int code) {
        this(code, null, null);
    }

    public HongsExemption toExemption() {
        return new HongsExemption(this.getErrno(), this.getError(), this)
             .setLocalizedOptions(this.getLocalizedOptions())
             .setLocalizedContext(this.getLocalizedContext());
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
    public String toString() {
        return this.getMessage();
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
    public String[] getLocalizedOptions() {
        return that.getLocalizedOptions();
    }

    @Override
    public HongsException setLocalizedContext(String    lang) {
        that.setLocalizedContext(lang);
        return this;
    }

    @Override
    public HongsException setLocalizedOptions(String... opts) {
        that.setLocalizedOptions(opts);
        return this;
    }

    /**
     * 常规错误(无需错误代码)
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
