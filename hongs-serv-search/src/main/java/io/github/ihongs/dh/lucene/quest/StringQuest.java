package io.github.ihongs.dh.lucene.quest;

import io.github.ihongs.util.Synt;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;

/**
 * 字串查询
 * @author Hongs
 */
public class StringQuest implements IQuest {
    @Override
    public Query wdr(String k, Object v) {
        throw new UnsupportedOperationException("Field "+k+" does not support search");
    }
    @Override
    public Query whr(String k, Object v) {
        String v2;
        try {
            v2 = Synt.asString(v);
        }
        catch (ClassCastException ex) {
            throw new   ClassCastException("Query for "+k+" must be string");
        }
        if (v2 == null) {
            throw new NullPointerException("Query for "+k+" must be string, but null");
        }
        v2 = v2.trim( );

        Query  q2 = new  TermQuery (new Term("@"+k, v2));
        return q2 ;
    }
    @Override
    public Query whr(String k, Object n, Object x, boolean l, boolean g) {
        String n2, x2;
        try {
            n2 = Synt.asString(n);
            x2 = Synt.asString(x);
        }
        catch (ClassCastException ex) {
            throw new   ClassCastException("Range for "+k+" must be string");
        }
        if (n2 == null && x2 == null) {
            throw new NullPointerException("Range for "+k+" must be string");
        }
        n2 = Synt.defoult(n2, "").trim();
        x2 = Synt.defoult(x2, "").trim();

        Query  q2 = TermRangeQuery.newStringRange("@" + k, n2, x2, l, g);
        return q2 ;
    }
}
