package io.github.ihongs.dh.lucene.value;

import java.util.List;
import io.github.ihongs.util.Synt;
import org.apache.lucene.index.IndexableField;

/**
 * 向量取值
 * @author Hongs
 */
public class VectorValue implements IValue {
    @Override
    public Object get(IndexableField f) {
        String v = f.stringValue( );
        try {
            List w = Synt.toList(v);
            for(int i = 0; i < w.size(); i ++) {
                w.set(i, Synt.asFloat(w.get(i)));
            }
            return w;
        } catch (Exception e) {
            return v;
        }
    }
}
