package io.github.ihongs.dh.lucene.query;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.search.Query;

/**
 *
 * @author Hongs
 */
public class LongQuery implements IQuery {
    @Override
    public Query gen(String k, Object v) {
        throw new UnsupportedOperationException("Field "+k+" does not support search");
    }
    @Override
    public Query get(String k, Object v) {
        if (v == null) {
            throw new NullPointerException("Query for "+k+" must be number, but null");
        }
        long    n2 = Synt.asLong(v);
        Query   q2 = LongPoint.newExactQuery("@"+k, n2);
        return  q2;
    }
    @Override
    public Query get(String k, Object n, Object x, boolean l, boolean g) {
        if (n == null && x == null) {
            throw new NullPointerException("Range for "+k+" must be number, but null");
        }
        long n2, x2;
        if (n == null || "".equals(n)) {
            n2 = Long.MIN_VALUE;
        } else {
            n2 = Synt.asLong(n);
            if (!l) {
                n2 = n2 + 1;
            }
        }
        if (x == null || "".equals(x)) {
            x2 = Long.MAX_VALUE;
        } else {
            x2 = Synt.asLong(x);
            if (!g) {
                x2 = x2 - 1;
            }
        }
        Query   q2 = LongPoint.newRangeQuery("@"+k, n2, x2);
        return  q2;
    }
}
