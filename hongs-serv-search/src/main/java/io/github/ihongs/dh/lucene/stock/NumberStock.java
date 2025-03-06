package io.github.ihongs.dh.lucene.stock;

import org.apache.lucene.document.Field;

/**
 * 数值存储
 * @author Hongs
 */
abstract public class NumberStock implements IStock {
    @Override
    public Field wdr(String k, Object v) {
        return null; // 数字类型无法模糊搜索, 无法增加搜索字段
    }
}
