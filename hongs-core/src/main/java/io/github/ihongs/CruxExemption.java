package io.github.ihongs;

/**
 * 核心免检异常
 *
 * 部分方法说明请参考 CruxUtil
 * 与 CruxException 不同, 无需 throws;
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
public class CruxExemption extends HongsExemption implements Crux {

    public CruxExemption(Throwable cause, int errno, String error, Object... cases) {
        super(cause, errno, error, cases);
    }

    public CruxExemption(Throwable cause, int errno, String error) {
        this(cause, errno, error, (Object[]) null);
    }

    public CruxExemption(Throwable cause, int errno) {
        this(cause, errno, null , (Object[]) null);
    }

    public CruxExemption(Throwable cause, String error, Object... cases) {
        this(cause,  0x0 , error, cases);
    }

    public CruxExemption(Throwable cause, String error) {
        this(cause,  0x0 , error, (Object[]) null);
    }

    public CruxExemption(Throwable cause) {
        this(cause,  0x0 , null , (Object[]) null);
    }

    public CruxExemption(int errno, String error, Object... cases) {
        this(null , errno, error, cases);
    }

    public CruxExemption(int errno, String error) {
        this(null , errno, error, (Object[]) null);
    }

    public CruxExemption(int errno) {
        this(null , errno, null , (Object[]) null);
    }

    public CruxExemption(String error, Object... cases) {
        this(null ,  0x0 , error, cases);
    }

    public CruxExemption(String error) {
        this(null ,  0x0 , error, (Object[]) null);
    }

    @Override
    public CruxExemption toExemption() {
        return this;
    }

    @Override
    public CruxException toException() {
        return new CruxException((Throwable) this.getCause(), this.getErrno(), this.getError(), this.getCases());
    }

}
