package foo.hongs.dh.lucene.value;

import foo.hongs.Core;
import foo.hongs.CoreLocale;
import foo.hongs.util.Synt;
import org.apache.lucene.index.IndexableField;
import java.util.Map;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 *
 * @author Hongs
 */
public class DatextValue implements IValue {
    private final long MN;
    private final DateFormat DF;

    public DatextValue(Map c) {
        String  tp = Synt.declare(c.get(  "type"  ), "");
        if ("datestamp".equals(tp)
        ||  "timestamp".equals(tp)) {
            MN = 1000;
        } else {
            MN = 1;
        }

        String  fm = Synt.declare(c.get( "format" ), "");
        if ( "".equals(fm)) {
                fm = Synt.declare(c.get("__type__"), "datetime");
                fm = CoreLocale
                    .getInstance( )
                    .getProperty("core.default."+ fm +".format");
            if (fm == null) {
                fm = "yyyy/MM/dd HH:mm:ss";
            }
        }
        DF  =  new  SimpleDateFormat( fm );
        DF.setTimeZone(Core.getTimezone());
    }

    @Override
    public Object get(IndexableField f) {
        return DF.format(new Date(f.numericValue().longValue() * MN));
    }
}
