package io.github.ihongs.dh.lucene.quest;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.search.Query;

/**
 * 数值查询
 * @author Hongs
 */
public class FloatQuest implements IQuest {
    @Override
    public Query wdr(String k, Object v) {
        throw new UnsupportedOperationException("Field "+k+" does not support search");
    }
    @Override
    public Query whr(String k, Object v) {
        Float  n2;
        try {
            n2 = Synt.asFloat(v);
        }
        catch (ClassCastException ex) {
            throw new   ClassCastException("Query for "+k+" must be number");
        }
        if (n2 == null) {
            throw new NullPointerException("Query for "+k+" must be number");
        }
        Query  q2 = FloatPoint.newExactQuery("@"+k, n2);
        return q2;
    }
    @Override
    public Query whr(String k, Object n, Object x, boolean l, boolean g) {
        Float  n2, x2;
        try {
            n2 = Synt.asFloat(n);
            x2 = Synt.asFloat(x);
        }
        catch (ClassCastException ex) {
            throw new   ClassCastException("Range for "+k+" must be number");
        }
        if (n2 == null && x2 == null) {
            throw new NullPointerException("Range for "+k+" must be number");
        }
        if (n2 == null) {
            n2 = Float.NEGATIVE_INFINITY;
        } else if (! l) {
            n2 = FloatPoint.nextUp  (n2);
        }
        if (x2 == null) {
            x2 = Float.POSITIVE_INFINITY;
        } else if (! g) {
            x2 = FloatPoint.nextDown(x2);
        }
        Query  q2 = FloatPoint.newRangeQuery("@"+k, n2, x2);
        return q2;
    }
}
