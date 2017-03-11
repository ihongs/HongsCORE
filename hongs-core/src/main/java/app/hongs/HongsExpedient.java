package app.hongs;

/**
 * 通用异常类
 *
 * 与 HongsException 不同, 无需 throws
 *
 * <h3>取值范围:</h3>
 * <pre>
 * 核心: 0x1000~0xFFFF (4096~65535)
 * 用户: 0x10000~0xFFFFF (65536~1048575)
 * </pre>
 *
 * @author Hongs
 */
public class HongsExpedient extends RuntimeException implements HongsCause {

    protected final HongsCauze that;

    public HongsExpedient(int errno, String error, Throwable cause) {
        super(cause);

        that = new HongsCauze(errno, error, this);

        if (errno < 0x1000 || errno > 0xFFFFF) {
            throw new HongsError(0x22,
                "Unchecked code must be from 0x1000(65536) to 0xFFFFF(1048575).");
        }
    }

    public HongsExpedient(int code, Throwable cause) {
        this(code, cause.getMessage(), cause);
    }

    public HongsExpedient(int code, String desc) {
        this(code, desc, null);
    }

    public HongsExpedient(int code) {
        this(code, null, null);
    }

    public HongsException toException() {
        HongsException ex = new HongsException(this.getErrno(), this.getError(), this);
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
    public HongsExpedient setLocalizedSection(String lang) {
        that.setLocalizedSection(lang);
        return this;
    }

    @Override
    public HongsExpedient setLocalizedOptions(String... opts) {
        that.setLocalizedOptions(opts);
        return this;
    }

    public static final int COMMON = 0x1000;

    public static final int NOTICE = 0x1001;

    /**
     * 常规错误(无需错误代码)
     */
    public static class Common extends HongsExpedient {
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
    public static class Notice extends HongsExpedient {
        public Notice(String desc, Throwable cause) {
            super(NOTICE, desc, cause);
        }
        public Notice(Throwable cause) {
            super(NOTICE, cause);
        }
        public Notice(String error) {
            super(NOTICE, error);
        }
    }

}
