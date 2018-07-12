package io.github.ihongs.dh.lucene.field;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;

/**
 *
 * @author Hongs
 */
public class IntField implements IField {
    @Override
    public Field get(String k, Object v, boolean u) {
        return new StoredField( k, Synt.declare(v, 0));
    }
    @Override
    public Field got(String k, Object v) {
        return new IntPoint(":"+k, Synt.declare(v, 0));
    }
    @Override
    public Field srt(String k, Object v) {
        return new NumericDocValuesField("."+k, Synt.declare(v, 0));
    }
}
