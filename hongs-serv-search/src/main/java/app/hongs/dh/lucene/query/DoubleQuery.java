package app.hongs.dh.lucene.query;

import app.hongs.util.Synt;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.search.Query;

/**
 *
 * @author Hongs
 */
public class DoubleQuery implements IQuery {
    @Override
    public Query get(String k, Object v) {
        if (v == null) {
            throw new ClassCastException("Query for "+k+" must be double, but null");
        }
        double  n2 = Synt.declare(v, Double.class);
        Query   q2 = DoublePoint.newExactQuery(":"+k, n2);
        return  q2;
    }
    @Override
    public Query get(String k, Object n, Object x, boolean l, boolean g) {
        if (n == null && x == null) {
            throw new ClassCastException("Range for "+k+" must be double, but null");
        }
        double n2, x2;
        if (n == null || "".equals(n)) {
            n2 = Double.MIN_VALUE;
        } else {
            n2 = Synt.declare(n, Float.class);
            if (!l) {
                n2 = DoublePoint.nextUp(n2);
            }
        }
        if (x == null || "".equals(x)) {
            x2 = Double.MAX_VALUE;
        } else {
            x2 = Synt.declare(x, Float.class);
            if (!g) {
                x2 = DoublePoint.nextDown(x2);
            }
        }
        Query   q2 = DoublePoint.newRangeQuery(":"+k, n2, x2);
        return  q2;
    }
}
