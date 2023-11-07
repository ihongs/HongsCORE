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

    public CruxExemption(Throwable fact, int code, String desc, Object... data) {
        super(fact, code, desc, data);
    }

    public CruxExemption(Throwable fact, int code, String desc) {
        this(fact, code, desc, (Object[]) null);
    }

    public CruxExemption(Throwable fact, int code) {
        this(fact, code, null, (Object[]) null);
    }

    public CruxExemption(Throwable fact, String desc, Object... data) {
        this(fact, 0x1 , desc, data);
    }

    public CruxExemption(Throwable fact, String desc) {
        this(fact, 0x1 , desc, (Object[]) null);
    }

    public CruxExemption(Throwable fact) {
        this(fact, 0x1 , null, (Object[]) null);
    }

    public CruxExemption(int code, String desc, Object... data) {
        this(null, code, desc, data);
    }

    public CruxExemption(int code, String desc) {
        this(null, code, desc, (Object[]) null);
    }

    public CruxExemption(int code) {
        this(null, code, null, (Object[]) null);
    }

    public CruxExemption(String desc, Object... data) {
        this(null, 0x1 , desc, data);
    }

    public CruxExemption(String desc) {
        this(null, 0x1 , desc, (Object[]) null);
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
