package app.hongs.dh.lucene;

import app.hongs.Core;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.util.Synt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

/**
 * 搜索助手
 * @author Hongs
 */
public class SearchHelper {

    private final LuceneRecord that;
    private Map<String, Map<String, String>> enums = null;

    public SearchHelper(LuceneRecord that) {
        this.that = that;
    }

    public LuceneRecord getRecord() {
        return that;
    }

    /**
     * 通过表单配置设置枚举数据
     * @param conf
     * @param form
     * @throws HongsException
     */
    public void setEnums(String conf, String form) throws HongsException {
        Map<String, Map> fs = FormSet.getInstance(conf).getForm(form);
        enums = new HashMap();
        for (Map.Entry<String, Map> et : fs.entrySet()) {
            String fn = et.getKey(  );
            Map    fc = et.getValue();
            if (! "enum".equals( fc.get( "__type__" ))) {
                continue;
            }

            String xn = (String) fc.get("enum");
            String xc = (String) fc.get("conf");
            if (xn == null || "".equals(xn)) xn = fn  ;
            if (xc == null || "".equals(xc)) xc = conf;
            Map xe = FormSet.getInstance(xc).getEnumTranslated ( xn );

            enums.put(fn, xe);
        }
    }

    /**
     * 设置枚举
     * @param dict
     */
    public void setEnums(Map dict) {
        enums = dict;
    }

    /**
     * 追加枚举
     * @param info 通过 counts 得到的 info
     * @throws HongsException
     */
    public void addEnums(Map info) throws HongsException {
        Iterator<Map.Entry> it = info.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry  et = it.next();
            String     fn = Synt.asserts( et.getKey(), "" );
            Map        es = enums.get( fn );
            List<List<String>> ls =  (List) et.getValue(  );

            /**
             * 加入枚举代号对应的标签文本
             * 如果不是一个枚举字段
             * 则将值复制一份反末尾
             * 已保持相同长度方便前端处理
             */
            if (es == null || es.isEmpty()) {
                for (List<String> x : ls) {
                    String val =  x.get (1);
                    x.add( val );
                }
            } else {
                for (List<String> x : ls) {
                    String val =  x.get (1);
                    if (val != null) {
                        val  = (String) es.get(val); // 得到标签
                    }
                    if (val == null) {
                        val  = (String) es.get("*"); // 其他类型
                    }
                    if (val == null) {
                        continue;
                    }
                    x.add( val );
                }
            }
        }
    }

    /**
     * 统计数量
     * @param rd
     * @return
     * @throws HongsException
     */
    public Map counts(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();
        IndexReader   reader = that.getReader();

        Map  resp = new HashMap();
        Map  cnts = new HashMap();
        resp.put( "info" , cnts );

        int         topz = Synt.declare(rd.get("top"), 0);
        Set<String> cntz = Synt.declare(rd.get("cnt"), Set.class);
        Map<String, Map<String, Integer>> counts = new HashMap( );
        Map<String, Map<String, Integer>> countz = new HashMap( );
        Map<String, Set<String>> countx  =  new HashMap();

        //** 整理代统计的数据 **/

        if (cntz != null && !cntz.isEmpty()) {
            Map<String, Map> fields = that.getFields();
            for(String   x : cntz) {
                String[] a = x.split(":", 2);
                if (a[0].startsWith ("-")) {
                    a[0] = a[0].substring(1);
                    if (!fields.containsKey(a[0])) {
                        throw new HongsException.Common("Field "+a[0]+" not exists");
                    }
                    if (a.length > 1) {
                        if (!countx.containsKey(a[0])) {
                            countx.put(a[0], new HashSet());
                        }
                        countx.get(a[0]).add(a[1]/**/);
                    }
                } else {
                    if (!fields.containsKey(a[0])) {
                        throw new HongsException.Common("Field "+a[0]+" not exists");
                    }
                    if (a.length > 1) {
                        if (!countz.containsKey(a[0])) {
                            countz.put(a[0], new HashMap());
                        }
                        countz.get(a[0]).put(a[1] , 0);
                    } else {
                        if (!counts.containsKey(a[0])) {
                            counts.put(a[0], new HashMap());
                        }
                    }
                }
            }
        }

        //** 分块统计数据 **/

        Map<String, Map<String, Integer>> counts2 = new HashMap();
        Map<String, Map<String, Integer>> countz2 = new HashMap();
        Map<String, Set<String>> countx2 = new HashMap();

        Map<String, Map<String, Integer>> counts3 = new HashMap();
        Map<String, Map<String, Integer>> countz3 = new HashMap();
        Map<String, Set<String>> countx3 = new HashMap();

        Set<String> cxts = new HashSet();
        cxts.addAll(counts.keySet());
        cxts.addAll(countz.keySet());

        // 条件中的统计值也要作为统计项
        for(String k : cxts) {
            Set vs  =  Synt.declare(rd.get(k), Set.class);
            if (vs !=  null) {
                Map<String, Integer> vz = countz.get( k );
                if (vz == null) {
                    vz = new HashMap();
                    countz.put(k , vz);
                }
                Set vx = countx.get(k);
                for(Object v : vs) {
                    if (vx == null || ! vx.contains( v )) {
                        vz.put(v.toString(), 0);
                    }
                }
            }
        }

        for(String k : cxts) {
            if (! rd.containsKey(k)) {
                if (counts.containsKey(k)) {
                    counts2.put(k, counts.get(k));
                }
                if (countz.containsKey(k)) {
                    countz2.put(k, countz.get(k));
                }
                if (countx.containsKey(k)) {
                    countx2.put(k, countx.get(k));
                }
            } else {
                countz3.clear();
                counts3.clear();
                countx3.clear();

                if (counts.containsKey(k)) {
                    counts3.put(k, counts.get(k));
                }
                if (countz.containsKey(k)) {
                    countz3.put(k, countz.get(k));
                }
                if (countx.containsKey(k)) {
                    countx3.put(k, countx.get(k));
                }

                Map xd = new HashMap();
                xd.putAll(rd);
                xd.remove(k );
                counts(xd, counts3, countz3, countx3, reader, finder);
            }
        }

        int z = counts(rd, counts2, countz2, countx2, reader, finder);
        cnts.put("__total__", z);

        //** 排序并截取统计数据 **/

        Map<String, List<Map.Entry<String, Integer>>> cntlst = new HashMap();

        for (Map.Entry<String, Map<String, Integer>>  et : counts.entrySet()) {
            String k = et.getKey();
            int t  = topz;
            if (t != 0) {
                Map c  = countz.get(k);
                if (c != null) {
                    t  = t - c.size( );
                }
                if (t <=  0  ) {
                    continue;
                }
            }
            List<Map.Entry<String, Integer>> l = new ArrayList(et.getValue().entrySet());
            Collections.sort( l, new Sorted());
            if (t != 0 && t < l.size()) {
                l  = l.subList( 0, t );
            }
            cntlst.put(k, l);
        }

        for (Map.Entry<String, Map<String, Integer>> et : countz.entrySet()) {
            String k = et.getKey();
            List<Map.Entry<String, Integer>> l = new ArrayList(et.getValue().entrySet());
            List<Map.Entry<String, Integer>> a = cntlst.get(k);
            if ( null != a ) {
                l.addAll(a );
            }
            Collections.sort( l, new Sorted());
            cntlst.put(k, l);
        }

        for (Map.Entry<String, List<Map.Entry<String, Integer>>> et : cntlst.entrySet()) {
            List<List> a = new ArrayList();
            for (Map.Entry<String, Integer> e : et.getValue()) {
                 List  b = new ArrayList();
                b.add(e.getValue() );
                b.add(e.getKey(  ) );
                a.add(b);
            }
            cnts.put(et.getKey(), a);
        }

        return resp;
    }

    private int counts(Map rd, Map<String, Map<String, Integer>> counts, Map<String, Map<String, Integer>> countz, Map<String, Set<String>> countx,
    IndexReader reader, IndexSearcher finder) throws HongsException {
        int total = 0;

        try {
            Query q = that.getQuery(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
                CoreLogger.debug("SearchRecord.counts: "+q.toString());
            }

            TopDocs docz = finder.search(q, 500);
            while ( docz.totalHits > 0) {
                ScoreDoc[] docs = docz.scoreDocs;

                if (!countz.isEmpty() || !counts.isEmpty()) {
                    for(ScoreDoc dox : docs) {
                        Document doc = reader.document(dox.doc);

                        for (Map.Entry<String, Map<String, Integer>> et : countz.entrySet()) {
                            String k = et.getKey();
                            Map<String, Integer> cntc = et.getValue();
                            String[] vals = doc.getValues(k);

                            for (String val: vals) {
                                if (cntc.containsKey(val)) {
                                    cntc.put(val , cntc.get(val) + 1);
                                }
                            }
                        }

                        for (Map.Entry<String, Map<String, Integer>> et : counts.entrySet()) {
                            String k = et.getKey();
                            Map<String, Integer> cntc = et.getValue();
                            String[] vals = doc.getValues(k);
                            Set<String> cntx = countx.get(k);
                            Map<String, Integer> cntu = countz.get(k);
                            Set<String> cntv = cntu != null ? cntu.keySet() : null;

                            for (String val: vals) {
                                if (cntc.containsKey(val)) {
                                    cntc.put(val , cntc.get(val) + 1);
                                } else
                                if ((cntv == null || !cntv.contains(val) )
                                && ( cntx == null || !cntx.contains(val))) {
                                    cntc.put(val , 1);
                                }
                            }
                        }
                    }
                }

                if (docs.length > 0) {
                    docz = finder.searchAfter(docs[docs.length - 1], q, 500);
                    total += docs.length;
                } else {
                    break;
                }
            }
        } catch (IOException ex) {
            throw new HongsException.Common(ex);
        }

        return total;
    }

    private static class Sorted implements Comparator<Map.Entry<String, Integer>> {
        @Override
        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
            int i1 = o1.getValue(), i2 = o2.getValue();
            return i1 == i2 ? 0 : ( i2 > i1 ? 1 : -1 );
        }
    }

    static {
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true"); // jdk1.7加上这个后排序不会报错
    }

}
