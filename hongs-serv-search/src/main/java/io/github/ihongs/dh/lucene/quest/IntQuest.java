package io.github.ihongs.dh.lucene.quest;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.search.Query;

/**
 *
 * @author Hongs
 */
public class IntQuest implements IQuest {
    @Override
    public Query wdr(String k, Object v) {
        throw new UnsupportedOperationException("Field "+k+" does not support search");
    }
    @Override
    public Query whr(String k, Object v) {
        if (v == null || "".equals(v)) {
            throw new NullPointerException("Query for "+k+" must be number, but null");
        }
        int     n2 = Synt.asInt(v);
        Query   q2 = IntPoint.newExactQuery("@"+k, n2);
        return  q2;
    }
    @Override
    public Query whr(String k, Object n, Object x, boolean l, boolean g) {
        if (n == null && x == null) {
            throw new NullPointerException("Range for "+k+" must be number, but null");
        }
        int n2, x2;
        if (n == null || "".equals(n)) {
            n2 = Integer.MIN_VALUE;
        } else {
            n2 = Synt.asInt(n);
            if (!l) {
                n2 = n2 + 1;
            }
        }
        if (x == null || "".equals(x)) {
            x2 = Integer.MAX_VALUE;
        } else {
            x2 = Synt.asInt(x);
            if (!g) {
                x2 = x2 - 1;
            }
        }
        Query   q2 = IntPoint.newRangeQuery("@"+k, n2, x2);
        return  q2;
    }
}
