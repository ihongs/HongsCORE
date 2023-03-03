package io.github.ihongs.dh.lucene.value;

import io.github.ihongs.util.Dist;
import org.apache.lucene.index.IndexableField;

/**
 * 对象取值
 * @author Hongs
 */
public class ObjectValue implements IValue {
    @Override
    public Object get(IndexableField f) {
        return Dist.toObject(f.stringValue());
    }
}
