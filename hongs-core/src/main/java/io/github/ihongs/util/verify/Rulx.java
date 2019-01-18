package io.github.ihongs.util.verify;

import java.util.Collection;

/**
 * 集合规则
 * @author hong
 */
public interface Rulx extends Ruly {
    
    /**
     * 校准集合
     * @param watch
     * @param value
     * @return
     * @throws Wrong
     * @throws Wrongs 
     */
    public Object remedy(Value watch, Collection value) throws Wrong, Wrongs;
    
    /**
     * 获取容器
     * @return 
     */
    public Collection getContext();
    
    /**
     * 忽略集合
     * @return 
     */
    public Collection getDefiant();
    
}
