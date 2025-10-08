package io.github.ihongs.serv.matrix.util;

import java.util.Set;

/**
 * 数据级联
 * @author Hongs
 */
public interface DataCascader {

    /**
     * 级联更新
     * @param aq 关联代码
     * @param id 主键取值
     */
    public void update(Set<String> aq, Object id);

    /**
     * 级联删除
     * @param aq 关联代码
     * @param id 主键取值
     */
    public void delete(Set<String> aq, Object id);

}
