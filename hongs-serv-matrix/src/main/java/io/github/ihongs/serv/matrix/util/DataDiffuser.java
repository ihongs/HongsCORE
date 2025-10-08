package io.github.ihongs.serv.matrix.util;

/**
 * 数据广播
 * @author Hongs
 */
public interface DataDiffuser {

    /**
     * 更新广播
     * @param conf 配置
     * @param form 表单
     * @param id   数据ID
     */
    public void update(String conf, String form, String id);

    /**
     * 删除广播
     * @param conf 配置
     * @param form 表单
     * @param id   数据ID
     */
    public void delete(String conf, String form, String id);

}
