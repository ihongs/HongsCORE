package io.github.ihongs.dh.lucene.quest;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.search.Query;

/**
 * 数值查询
 * @author Hongs
 */
public class IntQuest implements IQuest {
    @Override
    public Query wdr(String k, Object v) {
        throw new UnsupportedOperationException("Field "+k+" does not support search");
    }
    @Override
    public Query whr(String k, Object v) {
       Integer n2;
        try {
            n2 = Synt.asInt(v);
        }
        catch (ClassCastException ex) {
            throw new   ClassCastException("Query for "+k+" must be number");
        }
        if (n2 == null) {
            throw new NullPointerException("Query for "+k+" must be number");
        }
        Query  q2 = IntPoint.newExactQuery("@"+k, n2);
        return q2;
    }
    @Override
    public Query whr(String k, Object n, Object x, boolean l, boolean g) {
       Integer n2, x2;
        try {
            n2 = Synt.asInt(n);
            x2 = Synt.asInt(x);
        }
        catch (ClassCastException ex) {
            throw new   ClassCastException("Range for "+k+" must be number");
        }
        if (n2 == null && x2 == null) {
            throw new NullPointerException("Range for "+k+" must be number");
        }
        if (n2 == null) {
            n2 = Integer.MIN_VALUE;
        } else if (! l) {
            n2 = n2 + 1;
        }
        if (x2 == null) {
            x2 = Integer.MAX_VALUE;
        } else if (! g) {
            x2 = x2 - 1;
        }
        Query  q2 = IntPoint.newRangeQuery("@"+k, n2, x2);
        return q2;
    }
}
