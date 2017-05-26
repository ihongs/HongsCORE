package app.hongs.dh.lucene.value;

import java.util.Date;
import org.apache.lucene.index.IndexableField;

/**
 *
 * @author Hongs
 */
public class DatimeValue implements IValue {
    public int mul = 1;
    @Override
    public Object get(IndexableField f) {
        return new Date(f.numericValue().longValue() * mul);
    }
}
