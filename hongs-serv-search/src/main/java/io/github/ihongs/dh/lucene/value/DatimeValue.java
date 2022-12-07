package io.github.ihongs.dh.lucene.value;

import io.github.ihongs.util.Synt;
import org.apache.lucene.index.IndexableField;
import java.util.Map;
import java.util.Date;

/**
 * 日期时间戳值
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
