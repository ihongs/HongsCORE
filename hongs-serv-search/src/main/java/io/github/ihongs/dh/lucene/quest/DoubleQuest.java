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
        Double n2;
        try {
            n2 = Synt.asDouble(v);
        }
        catch (ClassCastException ex) {
            throw new   ClassCastException("Query for "+k+" must be number");
        }
        if (n2 == null) {
            throw new NullPointerException("Query for "+k+" must be number");
        }
        Query  q2 = DoublePoint.newExactQuery("@"+k, n2);
        return q2;
    }
    @Override
    public Query whr(String k, Object n, Object x, boolean l, boolean g) {
        Double n2, x2;
        try {
            n2 = Synt.asDouble(n);
            x2 = Synt.asDouble(x);
        }
        catch (ClassCastException ex) {
            throw new   ClassCastException("Range for "+k+" must be number");
        }
        if (n2 == null && x2 == null) {
            throw new NullPointerException("Range for "+k+" must be number");
        }
        if (n2 == null) {
            n2 = Double.NEGATIVE_INFINITY;
        } else if (! l) {
            n2 = DoublePoint.nextUp  (n2);
        }
        if (x2 == null) {
            x2 = Double.POSITIVE_INFINITY;
        } else if (! g) {
            x2 = DoublePoint.nextDown(x2);
        }
        Query  q2 = DoublePoint.newRangeQuery("@"+k, n2, x2);
        return q2;
    }
}
