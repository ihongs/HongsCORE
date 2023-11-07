package io.github.ihongs;

/**
 * 核心通用异常
 *
 * 部分方法说明请参考 CruxUtil
 * 与 CruxExemption 不同, 必须 throws
 *
 * <h3>取值范围:</h3>
 * <pre>
 * 核心: 400~999
 * 框架: 1000~9999
 * 用户: 10000~99999
 * 推荐用 desc 参数的 @conf:mark 形式, 在配置和语言资源里定义描述和消息
 * </pre>
 *
 * @author Hongs
 */
public class CruxException extends HongsException implements Crux {

    public CruxException(Throwable fact, int code, String desc, Object... data) {
        super(fact, code, desc, data);
    }

    public CruxException(Throwable fact, int code, String desc) {
        this(fact, code, desc, (Object[]) null);
    }

    public CruxException(Throwable fact, int code) {
        this(fact, code, null, (Object[]) null);
    }

    public CruxException(Throwable fact, String desc, Object... data) {
        this(fact, 0x0 , desc, data);
    }

    public CruxException(Throwable fact, String desc) {
        this(fact, 0x0 , desc, (Object[]) null);
    }

    public CruxException(Throwable fact) {
        this(fact, 0x0 , null, (Object[]) null);
    }

    public CruxException(int code, String desc, Object... data) {
        this(null, code, desc, data);
    }

    public CruxException(int code, String desc) {
        this(null, code, desc, (Object[]) null);
    }

    public CruxException(int code) {
        this(null, code, null, (Object[]) null);
    }

    public CruxException(String desc, Object... data) {
        this(null, 0x0 , desc, data);
    }

    public CruxException(String desc) {
        this(null, 0x0 , desc, (Object[]) null);
    }

    @Override
    public CruxException toException() {
        return this;
    }

    @Override
    public CruxExemption toExemption() {
        return new CruxExemption((Throwable) this.getCause(), this.getErrno(), this.getError(), this.getCases());
    }

}
