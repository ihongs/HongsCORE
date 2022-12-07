package io.github.ihongs.dh.lucene.value;

import org.apache.lucene.index.IndexableField;

/**
 * 取值接口
 * @author Hongs
 */
public interface IValue {
    public Object get(IndexableField f);
}
