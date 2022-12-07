package io.github.ihongs.dh.lucene.value;

import org.apache.lucene.index.IndexableField;

/**
 * 字串取值
 * @author Hongs
 */
public class StringValue implements IValue {
    @Override
    public Object get(IndexableField f) {
        return f.stringValue();
    }
}
