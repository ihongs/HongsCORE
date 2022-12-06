package io.github.ihongs.util.verify;

import java.util.Map;
import static io.github.ihongs.util.verify.Rule.PASS;
import static io.github.ihongs.util.verify.Rule.QUIT;

/**
 * 校验过程助手
 *
 * 此对象用于记录那些规则之外的数据,
 * 如正在校验的进出数据和校验器状态,
 * 有此辅助才使校验过程是线程安全的.
 *
 * @author Hongs
 */
public class Values implements Value {

    private final Map     values;
    private final Map     cleans;
    private final boolean update;
    private final boolean prompt;
    private       boolean valued;
    private       Object  value ;

    public Values(Map values, Map cleans, boolean update, boolean prompt) {
        this.values = values;
        this.cleans = cleans;
        this.update = update;
        this.prompt = prompt;
    }

    /**
     * 设置数据
     * @param value 
     */
    public void set(Object value) {
        if (value == PASS || value == QUIT) {
            this.value  = null ;
            this.valued = false;
        } else {
            this.value  = value;
            this.valued = true ;
        }
    }
    
    /**
     * 当前取值
     * @return 
     */
    @Override
    public Object get() {
        return value;
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
    public boolean isDefined() {
        return valued;
    }

    /**
     * 是否赋值且非空
     * @return 
     */
    @Override
    public boolean isPresent() {
        return value != null;
    }

}
