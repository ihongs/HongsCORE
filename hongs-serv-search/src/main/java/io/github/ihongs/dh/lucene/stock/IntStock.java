package io.github.ihongs.dh.lucene.stock;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.SortedNumericDocValuesField;

/**
 * 数值存储
 * @author Hongs
 */
public class IntStock implements IStock {
    @Override
    public Field get(String k, Object v) {
        return new StoredField(/**/k, Synt.declare(v, 0));
    }
    @Override
    public Field whr(String k, Object v) {
        return new    IntPoint("@"+k, Synt.declare(v, 0));
    }
    @Override
    public Field odr(String k, Object v) {
        return new NumericDocValuesField("#"+k, Synt.declare(v, 0));
    }
    @Override
    public Field ods(String k, Object v) {
        return new SortedNumericDocValuesField("%"+k, Synt.declare(v, 0));
    }
    @Override
    public Field wdr(String k, Object v) {
        return null; // 数字类型无法模糊搜索, 无法增加搜索字段
    }
}
