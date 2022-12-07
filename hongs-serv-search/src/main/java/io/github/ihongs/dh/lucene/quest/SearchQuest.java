package io.github.ihongs.dh.lucene.quest;

import io.github.ihongs.HongsExemption;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import java.util.Map;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * 全文检索
 * @author Hongs
 */
public class SearchQuest extends StringQuest {
    private Analyzer ana = null;
    private Boolean  des = null;
    private Boolean  dor = null;
    private Integer  phr = null;
    private Integer  fpl = null;
    private Float    fms = null;
//  private Boolean  art = null;
    private Boolean  sow = null;
    private Boolean  alw = null;
//  private Boolean  let = null;
    private Boolean  epi = null;
    private Boolean  agp = null;

    public void  smartParse(Boolean x) {
        this.des = x;
    }
    public void  lightMatch(Boolean x) {
        this.dor = x;
    }

    public void  analyser(Analyzer a) {
        this.ana = a;
    }

    /**
     * 快捷设置 lightMatch,smartParse 等, 但不包含 analyser
     * @param m
     */
    public void  settings(Map m) {
        Object obj;
        obj = m.get("lucene-smart-parse");
        if (obj != null) des = Synt.asBool (obj);
        obj = m.get("lucene-light-match");
        if (obj != null) dor = Synt.asBool (obj);
        obj = m.get("lucene-phrase-slop");
        if (obj != null) phr = Synt.asInt  (obj);
        obj = m.get("lucene-fuzzy-pre-len");
        if (obj != null) fpl = Synt.asInt  (obj);
        obj = m.get("lucene-fuzzy-min-sim");
        if (obj != null) fms = Synt.asFloat(obj);
        obj = m.get("lucene-parser-split-on-whitespace");
        if (obj != null) sow = Synt.asBool (obj);
//      obj = m.whr("lucene-parser-analyze-range-terms");
//      if (obj != null) art = Synt.asBool (obj);
        obj = m.get("lucene-parser-allow-leading-wildcard");
        if (obj != null) alw = Synt.asBool (obj);
//      obj = m.whr("lucene-parser-lowercase-expanded-terms");
//      if (obj != null) let = Synt.asBool (obj);
        obj = m.get("lucene-parser-enable-position-increments");
        if (obj != null) epi = Synt.asBool (obj);
        obj = m.get("lucene-parser-auto-generate-phrase-queries");
        if (obj != null) agp = Synt.asBool (obj);
    }

    @Override
    public Query wdr(String k, Object v) {
        if (null == v) {
            throw new NullPointerException("Query for "+k+" must be string, but null");
        }

        String  s = v.toString().trim();

        // 是否转义
        if (des == null || !des) {
            if (s . isEmpty( ) ) {
                // 空串查全部
                s = "[* TO *]" ;
            } else {
                // 拆分关键词
                s = "\""+Syno.concat("\" \"", Synt.toWords(s))+"\"" ;
            }
        }

        QueryParser qp = new QueryParser("$" + k, ana != null ? ana : new StandardAnalyzer());

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
        if (sow != null) qp.setSplitOnWhitespace(sow);
//      if (art != null) qp.setAnalyzeRangeTerms(art);
        if (alw != null) qp.setAllowLeadingWildcard     (alw);
//      if (let != null) qp.setLowercaseExpandedTerms   (let);
        if (epi != null) qp.setEnablePositionIncrements (epi);
        if (agp != null) qp.setAutoGeneratePhraseQueries(agp);

        try {
            Query  q2 = qp.parse(s);
            return q2 ;
        } catch ( ParseException e) {
            throw new HongsExemption(e);
        }
    }

}
