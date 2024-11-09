package io.github.ihongs.dh.lucene.value;

import org.apache.lucene.index.IndexableField;
import static io.github.ihongs.dh.lucene.quest.VectorQuest.toVector;

/**
 * 向量取值
 * @author Hongs
 */
public class VectorValue implements IValue {
    @Override
    public Object get(IndexableField f) {
        String v = f.stringValue( );
        try {
            return toVector(v);
        } catch ( Exception e) {
            return v;
        }
    }
}
