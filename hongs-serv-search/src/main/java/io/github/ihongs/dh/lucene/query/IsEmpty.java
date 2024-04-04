package io.github.ihongs.dh.lucene.query;

import java.io.IOException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.AutomatonQuery;
import org.apache.lucene.util.automaton.Automaton;

/**
 * 非空串查询
 * 类似 SQL 的 `fn` != ''
 * @author Kevin
 */
public class IsEmpty extends Query {

    private final String field;
    private final Query  query;

    public IsEmpty (String field) {
        /**
         * From Lucene 9.x Automata.makeEmptyString()
         */
        Automaton a = new Automaton();
        a.createState();
        a.setAccept(0, true);

        this.query = new AutomatonQuery(new Term(field, ""), a, Integer.MAX_VALUE, true);
        this.field = field;
    }

    @Override
    public String toString(String field) {
        StringBuilder sb = new StringBuilder();
        if (! this.field.equals(field) ) {
          sb.append(this.field);
          sb.append(":");
        } sb.append("[+ TO *]");
        return sb.toString();
    }

    @Override
    public boolean equals (Object other) {
        return query.equals (other );
    }

    @Override
    public Query  rewrite (IndexReader reader) throws IOException {
        return query.rewrite(reader);
    }

    @Override
    public Weight createWeight(IndexSearcher searcher, boolean score, float boost) throws IOException {
        return query.createWeight(searcher, score, boost);
    }

    @Override
    public int hashCode() {
        return query.hashCode();
    }

}
