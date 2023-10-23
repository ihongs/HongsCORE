package io.github.ihongs.dh.lucene.quest;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;

/**
 * 数值查询
 * @author Hongs
 */
public class LongQuest implements IQuest {
    @Override
    public Query wdr(String k, Object v) {
        throw new UnsupportedOperationException("Field "+k+" does not support search");
    }
    @Override
    public Query whr(String k, Object v) {
        Long   n2;
        try {
            n2 = Synt.asLong(v);
        }
        catch (ClassCastException ex) {
            throw new   ClassCastException("Query for "+k+" must be number");
        }
        if (n2 == null) {
            throw new NullPointerException("Query for "+k+" must be number");
        }
        Query  q2 = LongPoint.newExactQuery("@"+k, n2);
        return q2;
    }
    @Override
    public Query whr(String k, Object n, Object x, boolean l, boolean g) {
        Long   n2, x2;
        try {
            n2 = Synt.asLong(n);
            x2 = Synt.asLong(x);
        }
        catch (ClassCastException ex) {
            throw new   ClassCastException("Range for "+k+" must be number");
        }
        if (n2 == null && x2 == null) {
            throw new NullPointerException("Range for "+k+" must be number");
        }
        if (n2 == null) {
            n2 = Long.MIN_VALUE;
        } else if (! l) {
            n2 = n2 + 1;
        }
        if (x2 == null) {
            x2 = Long.MAX_VALUE;
        } else if (! g) {
            x2 = x2 - 1;
        }
        Query  q2 = LongPoint.newRangeQuery("@"+k, n2, x2);
        return q2;
    }
}
