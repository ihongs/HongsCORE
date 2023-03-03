package io.github.ihongs.dh.lucene.stock;

import io.github.ihongs.util.Dist;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;

/**
 * 对象存储
 * @author Hongs
 */
public class ObjectStock implements IStock {
    @Override
    public Field get(String k, Object v) {
        if (v == null || "".equals(v)) {
            v  = "{}" ;
        } else
        if (! ( v instanceof String )) {
            v  = Dist.toString(v, true );
        }
        return new StoredField(k, v.toString());
    }
    @Override
    public Field whr(String k, Object v) {
        // 对象类型无法进行筛选, 但可判断是否有值
        return new StringField("@"+k, v == null || v.equals("") ? "" : "0", Field.Store.NO);
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
