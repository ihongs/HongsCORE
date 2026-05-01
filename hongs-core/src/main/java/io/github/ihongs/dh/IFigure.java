package io.github.ihongs.dh;

import java.util.Map;
import java.util.Set;

/**
 * 字段参数
 * @author Hongs
 */
public interface IFigure {

    /**
     * 获取表单字段
     * @return
     */
    public Map getFields();

    /**
     * 获取表单参数
     * @return
     */
    public Map getParams();

    /**
     * 获取可列举的字段
     * @return
     */
    public Set<String> getListable();

    /**
     * 获取可排序的字段
     * @return
     */
    public Set<String> getSortable();

    /**
     * 获取可过滤的字段
     * @return
     */
    public Set<String> getFindable();

    /**
     * 获取可比对的字段 (用于区间查询)
     * @return
     */
    public Set<String> getRankable();

    /**
     * 获取可搜索的字段 (用于模糊查询)
     * @return
     */
    public Set<String> getSrchable();

    /**
     * 获取可搜索的字段 (用于整体搜索, 默认同 getSrchable)
     * @return
     */
    default public Set<String> getRschable() {
        return getSrchable();
    }
}