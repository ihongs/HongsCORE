package io.github.ihongs.dh.lucene.stock;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;
import org.apache.lucene.util.NumericUtils;

/**
 * 数值存储
 * @author Hongs
 */
public class DoubleStock extends NumberStock implements IStock {
    @Override
    public Field get(String k, Object v) {
        return new StoredField(/**/k, Synt.declare(v, 0D));
    }
    @Override
    public Field whr(String k, Object v) {
        return new DoublePoint("@"+k, Synt.declare(v, 0D));
    }
    @Override
    public Field odr(String k, Object v) {
        return new NumericDocValuesField("#"+k, Double.doubleToRawLongBits(Synt.declare(v, 0.0D)));
    }
    @Override
    public Field ods(String k, Object v) {
        return new SortedNumericDocValuesField("%"+k, NumericUtils.doubleToSortableLong(Synt.declare(v, 0.0D)));
    }
}
