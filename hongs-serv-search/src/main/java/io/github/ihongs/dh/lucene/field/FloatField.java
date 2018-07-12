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
    public Field get(String k, Object v, boolean u) {
        return new StoredField (  k, Synt.declare(v, 0F));
    }
    @Override
    public Field got(String k, Object v) {
        return new FloatPoint(":"+k, Synt.declare(v, 0L));
    }
    @Override
    public Field srt(String k, Object v) {
        return new FloatDocValuesField("."+k, Synt.declare(v, 0.0F));
    }
}
