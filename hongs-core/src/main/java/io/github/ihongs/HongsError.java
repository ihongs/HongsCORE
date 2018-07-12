package io.github.ihongs;

/**
 * 通用错误类
 *
 * <h3>取值范围:</h3>
 * <pre>
 * 核心: 0x10~0xFF (16~255)
 * 用户: 0x100~0xFFF (256~4095)
 * </pre>
 *
 * @author Hongs
 */
public class HongsError extends Error implements HongsCause {

    protected final HongsCurse that;

    public HongsError(int errno, String error, Throwable cause) {
        super(cause);

        that = new HongsCurse(errno, error, this);

        if (errno > 0xFFF) {
            throw new HongsError(0x21,
                  "Error code must be less than 0x1000(4096).");
        }
    }

    public HongsError(int code, Throwable cause) {
        this(code, cause.getMessage(), cause);
    }

    public HongsError(int code, String desc) {
        this(code, desc, null);
    }

    public HongsError(int code) {
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
    public HongsError setLocalizedContext(String lang) {
        that.setLocalizedContext(lang);
        return this;
    }

    @Override
    public HongsError setLocalizedOptions(String... opts) {
        that.setLocalizedOptions(opts);
        return this;
    }

    public static final int COMMON = 0x10;

    public static final int NOTICE = 0x11;

    /**
     * 常规错误(无需错误代码)
     */
    public static class Common extends HongsError {
        public Common(String error, Throwable cause) {
            super(COMMON, error, cause);
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
    public static class Notice extends HongsError {
        public Notice(String error, Throwable cause) {
            super(NOTICE, error, cause);
        }
        public Notice(Throwable cause) {
            super(NOTICE, cause);
        }
        public Notice(String error) {
            super(NOTICE, error);
        }
    }

}
