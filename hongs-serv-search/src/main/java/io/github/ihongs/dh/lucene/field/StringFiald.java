package io.github.ihongs.dh.lucene.field;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author Hongs
 */
public class StringFiald implements IField {
    @Override
    public Field get(String k, Object v) {
        return new StoredField(/**/k, v != null ? v.toString() : "");
    }
    @Override
    public Field whr(String k, Object v) {
        return new StringField("@"+k, v != null ? v.toString() : "", Field.Store.NO);
    }
    @Override
    public Field wdr(String k, Object v) {
        return new   TextField("$"+k, v != null ? v.toString() : "", Field.Store.NO);
    }
    @Override
    public Field odr(String k, Object v) {
        return new SortedDocValuesField("#"+k, new BytesRef(v != null ? v.toString() : ""));
    }
}
