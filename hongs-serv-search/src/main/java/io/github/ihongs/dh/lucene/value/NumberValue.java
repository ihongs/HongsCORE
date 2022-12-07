package io.github.ihongs.dh.lucene.value;

import org.apache.lucene.index.IndexableField;

/**
 * 数值取值
 * @author Hongs
 */
public class NumberValue implements IValue {
    @Override
    public Object get(IndexableField f) {
        return f.numericValue();
    }
}
