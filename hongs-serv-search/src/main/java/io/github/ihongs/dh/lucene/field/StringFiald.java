package io.github.ihongs.dh.lucene.field;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author Hongs
 */
public class StringFiald implements IField {
    @Override
    public Field whr(String k, Object v) {
        return null; // 字符类型本身即可用于过滤, 无需额外增加过滤字段
    }
    @Override
    public Field odr(String k, Object v) {
        return new SortedDocValuesField("."+k, new BytesRef(v.toString()));
    }
    @Override
    public Field get(String k, Object v, boolean u) {
        return new StringField(k, Synt.declare(v, ""), u ? Field.Store.NO : Field.Store.YES);
    }
}
