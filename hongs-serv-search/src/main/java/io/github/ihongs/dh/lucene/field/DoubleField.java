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
    public Field get(String k, Object v, boolean u) {
        return new StoredField(    k, Synt.declare(v, 0D));
    }
    @Override
    public Field got(String k, Object v) {
        return new DoublePoint(":"+k, Synt.declare(v, 0L));
    }
    @Override
    public Field srt(String k, Object v) {
        return new DoubleDocValuesField("."+k, Synt.declare(v, 0.0D));
    }
}
