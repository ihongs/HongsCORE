package io.github.ihongs.dh.lucene.field;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.StoredField;

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
        return new  FloatPoint("@"+k, Synt.declare(v, 0L));
    }
    @Override
    public Field odr(String k, Object v) {
        return new  FloatDocValuesField("#"+k, Synt.declare(v, 0.0F));
    }
    @Override
    public Field wdr(String k, Object v) {
        return null; // 数字类型无法模糊搜索, 无法增加搜索字段
    }
}
