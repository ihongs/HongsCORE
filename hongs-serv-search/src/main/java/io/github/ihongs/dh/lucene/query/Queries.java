package io.github.ihongs.dh.lucene.query;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

/**
 * 查询序列
 *
 * 不同于 BooleanQuery.Builder
 * 如果所有的条件都是 MUST_NOT
 * 则会在末尾追加一个 MatchAllDocsQuery
 *
 * @author Hongs
 */
public class Queries extends BooleanQuery.Builder {

    private int size = 0;
    private int nots = 0;

    public Queries() {
        super();
    }

    @Override
    public Queries add(BooleanClause clause) {
        super.add(clause);
        if (clause.getOccur() == BooleanClause.Occur.MUST_NOT) {
            nots ++;
        }   size ++;
        return this;
    }

    @Override
    public Queries add(Query query, BooleanClause.Occur occur) {
        return add(new BooleanClause(query, occur));
    }

    @Override
    public BooleanQuery build() {
        if (size > 0 && size == nots) {
            add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
        }
        return super.build();
    }

}
