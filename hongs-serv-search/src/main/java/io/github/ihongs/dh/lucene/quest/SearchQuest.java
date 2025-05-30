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
    private Integer  phr = null;
    private Integer  fpl = null;
    private Float    fms = null;
    private Boolean  sow = null;
    private Boolean  alw = null;
    private Boolean  epi = null;
    private Boolean  agp = null;

    public void analyser(Analyzer a) {
        this.ana = a;
    }

    public void settings(Map m) {
        Object obj;
        obj = m.get("search-mode");
        if (obj != null) mod = Synt.toSet  (obj);
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
    public  Query  wdr(String k, Object v, Object a) {
        // 提取模式和参数(同 AssocCase.srchs)
        String  sa = Synt.asString (a);
        String  md = "search";
        boolean or =  false  ;
        if (sa != null && ! sa.isEmpty( )) {
            int p  = sa. indexOf (",");
            if (p  < 0) {
                md = sa;
            } else {
                md = sa.substring(0,p);

                int b, l;
                String t;
                l = sa.length();
                do {
                    b = p + 1;
                    p = sa.indexOf("," , b);
                    if (p < 0) {
                        t = sa.substring(b, l);
                    } else {
                        t = sa.substring(b, p);
                    }

                    if ("or".equals(t)) {
                        or = true; break;
                    }
                } while (p > 0);
            }
        }

        if (this.mod != null) {
            if (!this.mod.contains(md)) {
                throw new CruxExemption(1051, "Unsupported search mode for `"+k+"`: "+mod);
            }
        } else {
            if (!"search". equals (md)) {
                throw new CruxExemption(1051, "Unsupported search mode for `"+k+"`: "+mod);
            }
        }

        String s = Synt.asString(v);
        if (s == null) {
            s  =  "" ;
        }

        switch (md) {
            case "search": {
                    s = s.trim( );
                if (s.isEmpty ()) {
                    return all("$" + k);
                }
                return wdr ("$" + k, s, true , or);
            }
            case "lucene": {
                    s = s.trim( );
                if (s.isEmpty ()) {
                    return all("$" + k);
                }
                return wdr ("$" + k, s, false, or);
            }
            case "regexp": {
                if (s.isEmpty ()) {
                    return all("@" + k);
                }
                return new RegexpQuery(new Term("@" + k, s));
            }
            case "prefix": {
                if (s.isEmpty ()) {
                    return all("@" + k);
                }
                return new PrefixQuery(new Term("@" + k, s));
            }
            case "wildcard": {
                if (s.isEmpty ()) {
                    return all("@" + k);
                }
                return new WildcardQuery(new Term("@" + k, s));
            }
            default:
                throw  new CruxExemption(1050, "Wrong search mode for `"+ k +"`: "+ mod );
        }
    }

    private Query  wdr(String k, String s, boolean se, boolean or) {
        QueryParser qp = new QueryParser(k, ana != null ? ana : new StandardAnalyzer());

        // 是否转义
        if (se) {
            s = "\""+ Syno.concat( "\" \"", Synt.toWords(s) ) +"\"" ;
        }

        // 词间关系
        if (or) {
            qp.setDefaultOperator( QueryParser. OR_OPERATOR );
        } else {
            qp.setDefaultOperator( QueryParser.AND_OPERATOR );
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
            QueryParser qp = new QueryParser(k, new StandardAnalyzer());
            String s ="[* TO *]";
            return qp. parse (s);
        }
        catch (ParseException e) {
            throw new CruxExemption(e);
        }
    }

}
