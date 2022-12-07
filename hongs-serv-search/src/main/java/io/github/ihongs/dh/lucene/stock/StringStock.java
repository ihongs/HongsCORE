package io.github.ihongs.dh.lucene.stock;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.SortedSetDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author Hongs
 */
public class StringStock implements IStock {
    private Analyzer ana = null;
    public void  analyser(Analyzer a) {
        this.ana = a;
    }
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
        return ( ana == null )
               ? new TextField("$"+k, v != null ? v.toString() : "", Field.Store.NO)
               : new TextField("$"+k,ana.tokenStream("$"+k, v != null ? v.toString() : ""));
    }
    @Override
    public Field odr(String k, Object v) {
        return new SortedDocValuesField("#"+k, new BytesRef(v != null ? v.toString() : ""));
    }
    @Override
    public Field ods(String k, Object v) {
        return new SortedSetDocValuesField("%"+k, new BytesRef(v != null ? v.toString() : ""));
    }
}
