package io.github.ihongs.dh.lucene.field;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.util.NumericUtils;

/**
 *
 * @author Hongs
 */
public class FloatField implements IField {
    @Override
    public Field get(String k, Object v) {
        return new StoredField(/**/k, Synt.declare(v, 0F));
    }
    @Override
    public Field whr(String k, Object v) {
        return new  FloatPoint("@"+k, Synt.declare(v, 0F));
    }
    @Override
    public Field odr(String k, Object v) {
        return new NumericDocValuesField("#"+k, Float.floatToRawIntBits(Synt.declare(v, 0.0F)));
    }
    @Override
    public Field ods(String k, Object v) {
        return new SortedNumericDocValuesField("#"+k, NumericUtils.floatToSortableInt(Synt.declare(v, 0.0F)));
    }
    @Override
    public Field wdr(String k, Object v) {
        return null; // 数字类型无法模糊搜索, 无法增加搜索字段
    }
}
