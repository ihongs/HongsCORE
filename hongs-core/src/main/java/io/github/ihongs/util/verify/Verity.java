package io.github.ihongs.util.verify;

import java.util.Map;

/**
 * 规则助手
 *
 * 此对象用于记录那些规则之外的数据,
 * 如正在校验的进出数据和校验器状态,
 * 有此辅助才使校验过程是线程安全的.
 *
 * @author Hongs
 */
public class Verity {

    private final Veri veri ;
    private final Map values;
    private final Map cleans;
    private final boolean valued;

    public Verity(Veri veri , Map values, Map cleans, boolean valued) {
        this.veri   =  veri ;
        this.values = values;
        this.cleans = cleans;
        this.valued = valued;
    }

    /**
     * 是否为速断模式, 同 Veri.isPrompt
     * @return
     */
    public boolean  isPrompt() {
        return veri.isPrompt();
    }

    /**
     * 是否为更新模式, 同 Veri.isUpdate
     * @return
     */
    public boolean  isUpdate() {
        return veri.isUpdate();
    }

    /**
     * 是否有值
     * @return
     */
    public boolean  isValued() {
        return valued;
    }

    /**
     * 原始数据
     * @return
     */
    public Map getValues() {
        return values;
    }

    /**
     * 清洁数据
     * @return
     */
    public Map getCleans() {
        return cleans;
    }

}
