package io.github.ihongs.dh.lucene.stock;

import org.apache.lucene.document.Field;

/**
 *
 * @author Hongs
 */
public interface IStock {
    public Field get(String k, Object v); // 存储字段
    public Field whr(String k, Object v); // 筛选字段
    public Field wdr(String k, Object v); // 搜索字段
    public Field odr(String k, Object v); // 排序字段
    public Field ods(String k, Object v); // 排序字段(多个)
}
