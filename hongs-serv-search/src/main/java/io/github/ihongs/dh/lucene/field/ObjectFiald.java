package io.github.ihongs.dh.lucene.field;

import io.github.ihongs.util.Dawn;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;

/**
 *
 * @author Hongs
 */
public class ObjectFiald implements IField {
    @Override
    public Field get(String k, Object v) {
        if (v == null || "".equals(v)) {
            v  = "{}" ;
        } else
        if (! ( v instanceof String )) {
            v  = Dawn.toString(v, true );
        }
        return new StoredField(k, v.toString());
    }
    @Override
    public Field whr(String k, Object v) {
        return null; // 对象类型无法用于排序, 无法增加排序字段
    }
    @Override
    public Field wdr(String k, Object v) {
        return null; // 对象类型无法模糊搜索, 无法增加搜索字段
    }
    @Override
    public Field odr(String k, Object v) {
        return null; // 对象类型无法用于排序, 无法增加排序字段
    }
    @Override
    public Field ods(String k, Object v) {
        return null; // 对象类型无法用于排序, 无法增加排序字段
    }
}
