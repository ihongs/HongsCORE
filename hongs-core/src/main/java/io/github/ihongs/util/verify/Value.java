package io.github.ihongs.util.verify;

import java.util.Map;

/**
 * 校验状态
 * @author Hongs
 */
public interface Value {

    /**
     * @return 当前取值
     */
    public Object get();
    
    /**
     * @return 待验证的数据
     */
    public Map getValues();

    /**
     * @return 清洁后的数据
     */
    public Map getCleans();

    /**
     * @return 更新时为 true
     */
    public boolean isUpdate();

    /**
     * @return 速断则为 true
     */
    public boolean isPrompt();

    /**
     * @return 有给值为 true
     */
    public boolean isValued();

}
