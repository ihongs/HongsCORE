package app.hongs.dh.lucene.query;

import app.hongs.util.Synt;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.search.Query;

/**
 *
 * @author Hongs
 */
public class FloatQuery implements IQuery {
    @Override
    public Query get(String k, Object v) {
        if (v == null) {
            throw new ClassCastException("Query for "+k+" must be float, but null");
        }
        float   n2 = Synt.declare(v, Float.class);
        Query   q2 = FloatPoint.newExactQuery(":"+k, n2);
        return  q2;
    }
    @Override
    public Query get(String k, Object n, Object x, boolean l, boolean g) {
        if (n == null && x == null) {
            throw new ClassCastException("Range for "+k+" must be float, but null");
        }
        float n2, x2;
        if (n == null || "".equals(n)) {
            n2 = Float.MIN_VALUE;
        } else {
            n2 = Synt.declare(n, Float.class);
            if (!l) {
                n2 = FloatPoint.nextUp(n2);
            }
        }
        if (x == null || "".equals(x)) {
            x2 = Float.MAX_VALUE;
        } else {
            x2 = Synt.declare(n, Float.class);
            if (!l) {
                x2 = FloatPoint.nextDown(x2);
            }
        }
        Query   q2 = FloatPoint.newRangeQuery(":"+k, n2, x2);
        return  q2;
    }
}
