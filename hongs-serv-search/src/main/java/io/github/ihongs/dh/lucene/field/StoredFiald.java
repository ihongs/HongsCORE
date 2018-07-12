package io.github.ihongs.dh.lucene.field;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;

/**
 *
 * @author Hongs
 */
public class StoredFiald implements IField {
    @Override
    public Field got(String k, Object v) {
        return null; // 存储类型无法用于过滤, 无法增加过滤字段
    }
    @Override
    public Field srt(String k, Object v) {
        return null; // 文本类型无法用于排序, 无法增加排序字段
    }
    @Override
    public Field get(String k, Object v, boolean u) {
        return new StoredField(k, v.toString());
    }
}
