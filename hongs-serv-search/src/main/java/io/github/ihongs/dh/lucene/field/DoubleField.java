package io.github.ihongs.dh.lucene.field;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.DoubleDocValuesField;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;

/**
 *
 * @author Hongs
 */
public class DoubleField implements IField {
    @Override
    public Field get(String k, Object v) {
        return new StoredField(/**/k, Synt.declare(v, 0D));
    }
    @Override
    public Field whr(String k, Object v) {
        return new DoublePoint("@"+k, Synt.declare(v, 0L));
    }
    @Override
    public Field odr(String k, Object v) {
        return new DoubleDocValuesField("#"+k, Synt.declare(v, 0.0D));
    }
    @Override
    public Field wdr(String k, Object v) {
        return null; // 数字类型无法模糊搜索, 无法增加搜索字段
    }
}
