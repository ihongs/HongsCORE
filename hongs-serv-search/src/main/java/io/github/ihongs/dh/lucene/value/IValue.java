package io.github.ihongs.dh.lucene.value;

import org.apache.lucene.index.IndexableField;

/**
 *
 * @author Hongs
 */
public interface IValue {
    public Object get(IndexableField f);
}
