package app.hongs.dh.lucene.value;

import java.util.Date;
import java.text.SimpleDateFormat;
import org.apache.lucene.index.IndexableField;

/**
 *
 * @author Hongs
 */
public class DatStrValue implements IValue {
    public int mul = 1;
    public SimpleDateFormat sdf = null;
    @Override
    public Object get(IndexableField f) {
        return sdf.format(new Date(f.numericValue().longValue() * mul));
    }
}
