package foo.hongs.dh.lucene.value;

import org.apache.lucene.index.IndexableField;

/**
 *
 * @author Hongs
 */
public interface IValue {
    public Object get(IndexableField f);
}
