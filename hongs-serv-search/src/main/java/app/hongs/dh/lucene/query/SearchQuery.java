package app.hongs.dh.lucene.query;

import app.hongs.HongsExpedient;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermRangeQuery;

/**
 *
 * @author Hongs
 */
public class SearchQuery implements IQuery {
    private Boolean  des = null;
    private Boolean  and = null;
    private Boolean  alw = null;
    private Boolean  let = null;
    private Boolean  epi = null;
    private Integer  phr = null;
    private Integer  fpl = null;
    private Float    fms = null;
    private Analyzer ana = null;
    public void  analyzer(Analyzer a) {
        this.ana = a;
    }
    public void  advanceAnalysisInUse(Boolean x) {
        this.des = x;
    }
    public void  defaultOperatorIsAnd(Boolean x) {
        this.and = x;
    }
    public void  allowLeadingWildcard(Boolean x) {
        this.alw = x;
    }
    public void  lowercaseExpandedTerms(Boolean x) {
        this.let = x;
    }
    public void  enablePositionIncrements(Boolean x) {
        this.epi = x;
    }
    public void  phraseSlop (Integer x) {
        this.phr = x;
    }
    public void  fuzzyPreLen(Integer x) {
        this.fpl = x;
    }
    public void  fuzzyMinSim(Float   x) {
        this.fms = x;
    }

    @Override
    public Query get(String k, Object v) {
        try {
            QueryParser qp = new QueryParser(k, ana != null ? ana : new StandardAnalyzer());

            String s = v.toString ( );
            if (des == null || !des ) {
                s = QueryParser.escape(s);
            }
            if (and != null &&  and ) {
                qp.setDefaultOperator (QueryParser.AND_OPERATOR);
            }
            if (epi != null) qp.setEnablePositionIncrements(epi);
            if (let != null) qp.setLowercaseExpandedTerms(let);
            if (alw != null) qp.setAllowLeadingWildcard(alw);
            if (fpl != null) qp.setFuzzyPrefixLength(fpl);
            if (fms != null) qp.setFuzzyMinSim      (fms);
            if (phr != null) qp.setPhraseSlop       (phr);

            Query  q2 = qp.parse(s);
            return q2 ;
        } catch (ParseException ex) {
            throw new HongsExpedient.Common(ex);
        }
    }
    @Override
    public Query get(String k, Object n, Object x, boolean l, boolean g) {
        throw new HongsExpedient(0x1100, "Field "+k+" does not suported interval queries.");
    }

}
