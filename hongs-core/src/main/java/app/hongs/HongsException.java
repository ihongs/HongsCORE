package app.hongs;

/**
 * 通用异常类
 *
 * 与 HongsExpedient 不同, 必须 throws
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

    protected final HongsCauze that;

    public HongsException(int errno, String error, Throwable cause) {
        super(cause);

        that = new HongsCauze(errno, error, this);

        if (errno < 0x1000 || errno > 0xFFFFF) {
            throw new HongsError(0x22,
                "Exception code must be from 0x1000(65536) to 0xFFFFF(1048575).");
        }
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

    public HongsExpedient toExpedient() {
        HongsExpedient ex = new HongsExpedient(this.getErrno(), this.getError(), this);
        ex.setLocalizedOptions(this.getLocalizedOptions());
        ex.setLocalizedSection(this.getLocalizedSection());
        return ex;
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
    public String getLocalizedSection() {
        return that.getLocalizedSection();
    }

    @Override
    public String[] getLocalizedOptions() {
        return that.getLocalizedOptions();
    }

    @Override
    public HongsException setLocalizedSection(String lang) {
        that.setLocalizedSection(lang);
        return this;
    }

    @Override
    public HongsException setLocalizedOptions(String... opts) {
        that.setLocalizedOptions(opts);
        return this;
    }

    public static final int COMMON = 0x1000;

    public static final int NOTICE = 0x1001;

    /**
     * 常规错误(无需错误代码)
     */
    public static class Common extends HongsException {
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
    public static class Notice extends HongsException {
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
