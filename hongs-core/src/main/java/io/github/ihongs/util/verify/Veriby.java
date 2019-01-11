package io.github.ihongs.util.verify;

import java.util.Map;

/**
 * 校验过程助手
 *
 * 此对象用于记录那些规则之外的数据,
 * 如正在校验的进出数据和校验器状态,
 * 有此辅助才使校验过程是线程安全的.
 *
 * @author Hongs
 */
public class Veriby implements Veri {

    private final Verify ver;
    private final Map values;
    private final Map cleans;

    public Veriby(Verify ver, Map values, Map cleans) {
        this.ver    =    ver;
        this.values = values;
        this.cleans = cleans;
    }

    /**
     * 是否为速断模式, 同 Verify.isPrompt
     * @return
     */
    @Override
    public boolean isPrompt() {
        return ver.isPrompt();
    }

    /**
     * 是否为更新模式, 同 Verify.isUpdate
     * @return
     */
    @Override
    public boolean isUpdate() {
        return ver.isUpdate();
    }

    /**
     * 原始数据
     * @return
     */
    @Override
    public Map getValues() {
        return values;
    }

    /**
     * 清洁数据
     * @return
     */
    @Override
    public Map getCleans() {
        return cleans;
    }

}
