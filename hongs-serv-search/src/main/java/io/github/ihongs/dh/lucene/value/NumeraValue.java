package io.github.ihongs.dh.lucene.value;

import io.github.ihongs.util.Synt;
import org.apache.lucene.index.IndexableField;

/**
 *
 * @author Hongs
 */
public class NumeraValue implements IValue {
    @Override
    public Object get(IndexableField f) {
        return Synt.asString(f.numericValue());
    }
}
