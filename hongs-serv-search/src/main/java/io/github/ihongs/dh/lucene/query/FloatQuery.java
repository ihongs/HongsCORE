package io.github.ihongs.dh.lucene.query;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.search.Query;

/**
 *
 * @author Hongs
 */
public class FloatQuery implements IQuery {
    @Override
    public Query gen(String k, Object v) {
        throw new UnsupportedOperationException("Number field "+k+" does not support search");
    }
    @Override
    public Query get(String k, Object v) {
        if (v == null) {
            throw new NullPointerException("Query for "+k+" must be float, but null");
        }
        float   n2 = Synt.asFloat(v);
        Query   q2 = FloatPoint.newExactQuery("@"+k, n2);
        return  q2;
    }
    @Override
    public Query get(String k, Object n, Object x, boolean l, boolean g) {
        if (n == null && x == null) {
            throw new NullPointerException("Range for "+k+" must be float, but null");
        }
        float n2, x2;
        if (n == null || "".equals(n)) {
            n2 = Float.MIN_VALUE;
        } else {
            n2 = Synt.asFloat(n);
            if (!l) {
                n2 = FloatPoint.nextUp  (n2);
            }
        }
        if (x == null || "".equals(x)) {
            x2 = Float.MAX_VALUE;
        } else {
            x2 = Synt.asFloat(x);
            if (!g) {
                x2 = FloatPoint.nextDown(x2);
            }
        }
        Query   q2 = FloatPoint.newRangeQuery("@"+k, n2, x2);
        return  q2;
    }
}
