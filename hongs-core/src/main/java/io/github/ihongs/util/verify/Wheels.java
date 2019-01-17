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
public class Wheels implements Wheel {

    private final Map values;
    private final Map cleans;
    private final boolean update;
    private final boolean prompt;
    private       boolean valued;

    public Wheels(Map values, Map cleans, boolean update, boolean prompt) {
        this.values = values;
        this.cleans = cleans;
        this.update = update;
        this.prompt = prompt;
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

    /**
     * 是否为更新模式
     * @return
     */
    @Override
    public boolean isUpdate() {
        return update;
    }

    /**
     * 是否为速断模式
     * @return
     */
    @Override
    public boolean isPrompt() {
        return prompt;
    }

    /**
     * 是否外部有赋值
     *
     * 因 Java 缺少 undefined,
     * 而 null 无法再细分类型,
     * 像 Optional,Required,Default 等是需要区分的.
     *
     * @return
     */
    @Override
    public boolean isValued() {
        return valued;
    }

    /**
     * 设置是否有赋值
     * @param valued
     */
    public void isValued(boolean valued) {
        this.valued = valued;
    }

}
