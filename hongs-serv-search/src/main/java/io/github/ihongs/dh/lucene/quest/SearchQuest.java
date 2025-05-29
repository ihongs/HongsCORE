package io.github.ihongs.dh.lucene.quest;

import io.github.ihongs.CruxExemption;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.Term;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.WildcardQuery;

/**
 * 全文检索
 * @author Hongs
 */
public class SearchQuest extends StringQuest {
    private Set      mod = null;
    private Analyzer ana = null;
    private Boolean  des = null;
    private Boolean  dor = null;
    private Integer  phr = null;
    private Integer  fpl = null;
    private Float    fms = null;
    private Boolean  sow = null;
    private Boolean  alw = null;
    private Boolean  epi = null;
    private Boolean  agp = null;

    public void smartParse(Boolean x) {
        this.des = x;
    }
    public void lightMatch(Boolean x) {
        this.dor = x;
    }

    public void analyser(Analyzer a) {
        this.ana = a;
    }

    /**
     * 快捷设置 lightMatch,smartParse 等, 但不包含 analyser
     * @param m
     */
    public void settings(Map m) {
        Object obj;
        obj = m.get("search-mode");
        if (obj != null) mod = Synt.toSet  (obj);
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
        obj = m.get("lucene-split-on-whitespace");
        if (obj != null) sow = Synt.asBool (obj);
        obj = m.get("lucene-allow-leading-wildcard");
        if (obj != null) alw = Synt.asBool (obj);
        obj = m.get("lucene-enable-position-increments");
        if (obj != null) epi = Synt.asBool (obj);
        obj = m.get("lucene-auto-generate-phrase-queries");
        if (obj != null) agp = Synt.asBool (obj);
    }

    @Override
    public  Query  wdr(String k, Object v) {
        Boolean dor = this.dor;
        Boolean des = this.des;

        // 查询参数
        if (v instanceof Map ) {
            Map m =  ( Map ) v;
            v = m.get("value");
            String mod;
            mod = Synt.declare(m.get("mode"), "" );
            dor = Synt.declare(m.get( "or" ), dor != null ? dor : false);

            if (this.mod != null) {
                if (!this.mod.contains(mod)) {
                    throw new CruxExemption(1051, "Unsupported search mode for `"+k+"`: "+mod);
                }
            } else {
                if (!"search". equals (mod)) {
                    throw new CruxExemption(1051, "Unsupported search mode for `"+k+"`: "+mod);
                }
            }

            switch (mod) {
                case "search": {
                    des = false;
                } break;
                case "lucene": {
                    des = true ;
                } break;
                case "regexp": {
                    String s = str(v);
                    if (s.isEmpty ()) {
                        return all(k);
                    }
                    return new RegexpQuery(new Term("@" + k, v.toString()));
                }
                case "prefix": {
                    String s = str(v);
                    if (s.isEmpty ()) {
                        return all(k);
                    }
                    return new PrefixQuery(new Term("@" + k, v.toString()));
                }
                case "wildcard": {
                    String s = str(v);
                    if (s.isEmpty ()) {
                        return all(k);
                    }
                    return new WildcardQuery(new Term("@" + k, v.toString()));
                }
                default:
                    throw  new CruxExemption(1051, "Wrong search mode for `"+ k +"`: "+ mod );
            }
        }

        String s = str(v).trim();
        if (s.isEmpty ()) {
            return all(k);
        }

        QueryParser qp = new QueryParser("$" + k, ana != null ? ana : new StandardAnalyzer());

        // 是否转义
        if (des == null || !des) {
            s = "\""+ Syno.concat( "\" \"", Synt.toWords(s) ) +"\"" ;
        }

        // 词间关系
        if (dor == null || !dor) {
            qp.setDefaultOperator( QueryParser.AND_OPERATOR );
        } else {
            qp.setDefaultOperator( QueryParser. OR_OPERATOR );
        }

        // 其他设置
        if (phr != null) qp.setPhraseSlop               (phr);
        if (fms != null) qp.setFuzzyMinSim              (fms);
        if (fpl != null) qp.setFuzzyPrefixLength        (fpl);
        if (sow != null) qp.setSplitOnWhitespace        (sow);
        if (alw != null) qp.setAllowLeadingWildcard     (alw);
        if (epi != null) qp.setEnablePositionIncrements (epi);
        if (agp != null) qp.setAutoGeneratePhraseQueries(agp);

        try {
            return qp. parse (s);
        }
        catch (ParseException e) {
            throw new CruxExemption(e);
        }
    }

    private Query  all(String k) {
        try {
            QueryParser qp = new QueryParser("$" + k, new StandardAnalyzer());
            String s ="[* TO *]";
            return qp. parse (s);
        }
        catch (ParseException e) {
            throw new CruxExemption(e);
        }
    }

    private String str(Object v) {
        try {
            String s =Synt.asString(v);
            return s == null ? "" : s ;
        }
        catch (ClassCastException e) {
            throw new CruxExemption(e , 1050);
        }
    }

}
