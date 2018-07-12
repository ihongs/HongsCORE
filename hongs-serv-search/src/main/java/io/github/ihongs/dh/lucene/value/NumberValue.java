package io.github.ihongs.dh.lucene.value;

import org.apache.lucene.index.IndexableField;

/**
 *
 * @author Hongs
 */
public class NumberValue implements IValue {
    @Override
    public Object get(IndexableField f) {
        return f.numericValue();
    }
}
