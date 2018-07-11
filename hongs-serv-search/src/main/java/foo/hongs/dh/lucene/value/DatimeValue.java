package foo.hongs.dh.lucene.value;

import foo.hongs.util.Synt;
import org.apache.lucene.index.IndexableField;
import java.util.Map;
import java.util.Date;

/**
 *
 * @author Hongs
 */
public class DatimeValue implements IValue {
    private final long MN;

    public DatimeValue(Map c) {
        String tp = Synt.declare(c.get( "type" ), "");
        if ("datestamp".equals(tp)
        ||  "timestamp".equals(tp)) {
            MN = 1000;
        } else {
            MN = 1;
        }
    }

    @Override
    public  Object  get(IndexableField f) {
        return new Date(f.numericValue( ).longValue() * MN);
    }
}
