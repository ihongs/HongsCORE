package app.hongs.dh.lucene.query;

import app.hongs.util.Synt;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.search.Query;

/**
 *
 * @author Hongs
 */
public class IntQuery implements IQuery {
    @Override
    public Query get(String k, Object v) {
        if (v == null) {
            throw new ClassCastException("Query for "+k+" must be int, but null");
        }
        int     n2 = Synt.declare(v, Integer.class);
        Query   q2 = IntPoint.newExactQuery(":"+k, n2);
        return  q2;
    }
    @Override
    public Query get(String k, Object n, Object x, boolean l, boolean g) {
        if (n == null && x == null) {
            throw new ClassCastException("Range for "+k+" must be int, but null");
        }
        int n2, x2;
        if (n == null || "".equals(n)) {
            n2 = Integer.MIN_VALUE;
        } else {
            n2 = Synt.declare(n, Integer.class);
            if (!l) {
                n2 = n2 + 1;
            }
        }
        if (x == null || "".equals(x)) {
            x2 = Integer.MAX_VALUE;
        } else {
            x2 = Synt.declare(n, Integer.class);
            if (!l) {
                x2 = x2 - 1;
            }
        }
        Query   q2 = IntPoint.newRangeQuery(":"+k, n2, x2);
        return  q2;
    }
}
