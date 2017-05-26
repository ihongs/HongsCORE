package app.hongs.dh.lucene.value;

import app.hongs.util.Tool;
import org.apache.lucene.index.IndexableField;

/**
 *
 * @author Hongs
 */
public class NumStrValue implements IValue {
    @Override
    public Object get(IndexableField f) {
        return Tool.toNumStr(f.numericValue());
    }
}
