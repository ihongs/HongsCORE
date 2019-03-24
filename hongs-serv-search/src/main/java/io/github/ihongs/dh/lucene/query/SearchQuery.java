package io.github.ihongs.dh.lucene.query;

import io.github.ihongs.HongsExemption;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 *
 * @author Hongs
 */
public class SearchQuery extends StringQuery {
    private Analyzer ana = null;
    private Boolean  des = null;
    private Boolean  dor = null;
    private Integer  phr = null;
    private Integer  fpl = null;
    private Float    fms = null;
    private Boolean  art = null;
    private Boolean  alw = null;
    private Boolean  let = null;
    private Boolean  epi = null;
    private Boolean  agp = null;
    public void  analyzer(Analyzer a) {
        this.ana = a;
    }
    public void  smartParse (Boolean x) {
        this.des = x;
    }
    public void  lightMatch (Boolean x) {
        this.dor = x;
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
    public void  analyzeRangeTerms(Boolean x) {
        this.art = x;
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
    public void  autoGeneratePhraseQueries(Boolean x) {
        this.agp = x;
    }

    @Override
    public Query gen(String k, Object v) {
        if (null == v) {
            throw new NullPointerException("Query for "+k+" must be string, but null");
        }
        
        QueryParser qp = new QueryParser("$" + k, ana != null ? ana : new StandardAnalyzer());

        String s = v.toString( );

        // 是否转义
        if (des == null || !des) {
            s = QueryParser.escape(s);
        }

        // 词间关系
        if (dor == null || !dor) {
            qp.setDefaultOperator(QueryParser.AND_OPERATOR);
        } else {
            qp.setDefaultOperator(QueryParser. OR_OPERATOR);
        }

        // 其他设置
        if (phr != null) qp.setPhraseSlop       (phr);
        if (fms != null) qp.setFuzzyMinSim      (fms);
        if (fpl != null) qp.setFuzzyPrefixLength(fpl);
        if (art != null) qp.setAnalyzeRangeTerms(art);
        if (alw != null) qp.setAllowLeadingWildcard     (alw);
        if (let != null) qp.setLowercaseExpandedTerms   (let);
        if (epi != null) qp.setEnablePositionIncrements (epi);
        if (agp != null) qp.setAutoGeneratePhraseQueries(agp);

        try {
            Query  q2 = qp.parse(s);
            return q2 ;
        } catch (ParseException ex) {
            throw new HongsExemption.Common(ex);
        }
    }
    
}
