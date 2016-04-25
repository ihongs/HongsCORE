package app.hongs;

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

    protected HongsLocalized that;

    public HongsError(int code, String desc, Throwable cause) {
        super(cause);

        that = new HongsLocalized(code, desc, this);

        if (code < 0x10 || code > 0xFFF) {
            throw new HongsError(0x21,
                "Error code must be from 0x10(16) to 0xFFF(4095).");
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
    public HongsError setLocalizedSection(String lang) {
        that.setLocalizedSection(lang);
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
