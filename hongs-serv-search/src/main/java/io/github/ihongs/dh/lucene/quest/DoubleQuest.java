package io.github.ihongs.dh.lucene.quest;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.DoublePoint;
import org.apache.lucene.search.Query;

/**
 * 数值查询
 * @author Hongs
 */
public class DoubleQuest implements IQuest {
    @Override
    public Query wdr(String k, Object v) {
        throw new UnsupportedOperationException("Field "+k+" does not support search");
    }
    @Override
    public Query whr(String k, Object v) {
        if (v == null || "".equals(v)) {
            throw new NullPointerException("Query for "+k+" must be number, but null");
        }
        double  n2 = Synt.asDouble(v);
        Query   q2 = DoublePoint.newExactQuery("@"+k, n2);
        return  q2;
    }
    @Override
    public Query whr(String k, Object n, Object x, boolean l, boolean g) {
        if (n == null && x == null) {
            throw new NullPointerException("Range for "+k+" must be number, but null");
        }
        double n2, x2;
        if (n == null || "".equals(n)) {
            n2 = Double.NEGATIVE_INFINITY;
        } else {
            n2 = Synt.asDouble(n);
            if (!l) {
                n2 = DoublePoint.nextUp  (n2);
            }
        }
        if (x == null || "".equals(x)) {
            x2 = Double.POSITIVE_INFINITY;
        } else {
            x2 = Synt.asDouble(x);
            if (!g) {
                x2 = DoublePoint.nextDown(x2);
            }
        }
        Query   q2 = DoublePoint.newRangeQuery("@"+k, n2, x2);
        return  q2;
    }
}