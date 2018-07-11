package foo.hongs.dh.lucene.value;

import org.apache.lucene.index.IndexableField;

/**
 *
 * @author Hongs
 */
public class StringValue implements IValue {
    @Override
    public Object get(IndexableField f) {
        return f.stringValue();
    }
}
