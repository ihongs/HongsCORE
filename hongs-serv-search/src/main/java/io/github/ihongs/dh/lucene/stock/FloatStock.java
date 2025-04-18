package io.github.ihongs.dh.lucene.stock;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.util.NumericUtils;

/**
 * 数值存储
 * @author Hongs
 */
public class FloatStock extends NumberStock implements IStock {
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
        return new SortedNumericDocValuesField("%"+k, NumericUtils.floatToSortableInt(Synt.declare(v, 0.0F)));
    }
}
