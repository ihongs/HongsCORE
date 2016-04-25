package app.hongs;

/**
 * 通用异常类
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

    protected HongsLocalized that;

    public HongsException(int code, String desc, Throwable cause) {
        super(cause);

        that = new HongsLocalized(code, desc, this);

        if (code < 0x1000 || code > 0xFFFFF) {
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

    @Override
    public int getCode() {
        return that.getCode();
    }

    @Override
    public String getDesc() {
        return that.getDesc();
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
    public static class Common extends HongsError {
        public Common(String desc, Throwable cause) {
            super(COMMON, desc, cause);
        }
        public Common(Throwable cause) {
            super(COMMON, cause);
        }
        public Common(String desc) {
            super(COMMON, desc );
        }
    }

    /**
     * 通告错误(无需错误代码)
     */
    public static class Notice extends HongsError {
        public Notice(String desc, Throwable cause) {
            super(NOTICE, desc, cause);
        }
        public Notice(Throwable cause) {
            super(NOTICE, cause);
        }
        public Notice(String desc) {
            super(NOTICE, desc );
        }
    }

}
