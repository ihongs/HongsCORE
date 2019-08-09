package io.github.ihongs.dh.lucene.value;

import io.github.ihongs.util.Dawn;
import org.apache.lucene.index.IndexableField;

/**
 *
 * @author Hongs
 */
public class ObjectValue implements IValue {
    @Override
    public Object get(IndexableField f) {
        return Dawn.toObject(f.stringValue());
    }
}
