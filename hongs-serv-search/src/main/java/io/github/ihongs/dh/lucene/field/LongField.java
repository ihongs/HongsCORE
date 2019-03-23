package io.github.ihongs.dh.lucene.field;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;

/**
 *
 * @author Hongs
 */
public class LongField implements IField {
    @Override
    public Field get(String k, Object v) {
        return new StoredField ( k, Synt.declare(v, 0L));
    }
    @Override
    public Field whr(String k, Object v) {
        return new LongPoint(":"+k, Synt.declare(v, 0L));
    }
    @Override
    public Field odr(String k, Object v) {
        return new NumericDocValuesField("."+k, Synt.declare(v, 0L));
    }
    @Override
    public Field wdr(String k, Object v) {
        return null; // 数字类型无法模糊搜索, 无法增加搜索字段
    }
}
