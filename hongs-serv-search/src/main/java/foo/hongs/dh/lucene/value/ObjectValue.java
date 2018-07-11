package foo.hongs.dh.lucene.value;

import foo.hongs.util.Data;
import org.apache.lucene.index.IndexableField;

/**
 *
 * @author Hongs
 */
public class ObjectValue implements IValue {
    @Override
    public Object get(IndexableField f) {
        return Data.toObject(f.stringValue());
    }
}
