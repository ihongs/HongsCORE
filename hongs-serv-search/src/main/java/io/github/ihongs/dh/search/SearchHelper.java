package io.github.ihongs.dh.search;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.dh.lucene.LuceneRecord;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.Tool;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;

/**
 * 搜索助手
 * @author Hongs
 */
public class SearchHelper {

    private final LuceneRecord that;

    public SearchHelper(LuceneRecord that) {
        this.that = that;
    }

    public final LuceneRecord getRecord( ) {
        return that;
    }

    /**
     * 统计数量
     * @param rd
     * @return
     * @throws HongsException
     */
    public Map acount(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();
        IndexReader   reader = that.getReader();

        Map  cnts = new HashMap();

        int         topz = Synt.declare(rd.get(Cnst.RN_KEY), 0);
        Set<String> cntz = Synt.toTerms(rd.get(Cnst.RB_KEY)   );
        Map<String, Map<String, Integer>> counts = new HashMap( );
        Map<String, Map<String, Integer>> countz = new HashMap( );
        Map<String, Set<String         >> countx = new HashMap();

        //** 整理待统计的数据 **/

        if (cntz != null && !cntz.isEmpty()) {
            for(String   x : cntz) {
                String[] a = x.split(":", 2);
                if (a[0].startsWith ("-")) {
                    a[0] = a[0].substring(1);
                    if (a.length > 1) {
                        if (!countx.containsKey(a[0])) {
                            countx.put(a[0], new HashSet());
                        }
                        countx.get(a[0]).add(a[1]/**/);
                    }
                } else {
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

        /**
         * 根据请求数据进行综合判断,
         * 如果字段已经作为过滤条件,
         * 则此字段的统计需单独进行,
         * 且需抛开此字段的过滤数据.
         *
         * 例如某数据有一个地区字段且每条记录只能有一个地区,
         * 如果没有以下处理则选某地后其他未选地区数量将为零.
         *
         * 与 LinkedIn 左侧筛选类似.
         */

        for(String  k  : cxts) {
            Set     vs = null;
            Object  vo = rd.get(k);
            if (vo instanceof Map) {
                Map vm = (Map) vo ;
                if (vm.containsKey(Cnst.EQ_REL)) {
                    vs = Synt.asSet(vm.get(Cnst.EQ_REL));
                } else
                if (vm.containsKey(Cnst.IN_REL)) {
                    vs = Synt.asSet(vm.get(Cnst.IN_REL));
                }
            } else {
                if (vo!= null && !"".equals(vo)) {
                    vs = Synt.asSet(rd.get( k ));
                }
            }

            if (vs == null) {
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
                Map<String, Integer> va = counts.get(k);
                Map<String, Integer> vz = countz.get(k);
                Set<String         > vx = countx.get(k);

                counts3.clear();
                countz3.clear();
                countx3.clear();

                if (va != null) {
                    counts3.put(k, va);
                }
                if (vx != null) {
                    countx3.put(k, vx);
                }
                if (vz != null) {
                    countz3.put(k, vz);
                } else {
                    vz = new HashMap();
                    countz3.put(k, vz);
                    countz .put(k, vz);
                }

                for(Object v : vs) {
                    String s = v.toString();
                    if (vx  == null || ! vx.contains(s)) {
                        vz.put( s, 0 );
                    }
                }

                Map xd = new HashMap();
                xd.putAll(rd);
                xd.remove( k);
                acount(xd, counts3, countz3, countx3, reader, finder);
            }
        }

        int z = acount(rd, counts2, countz2, countx2, reader, finder);
        cnts.put("__count__", z);

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

        for (Map.Entry<String, Map<String, Integer>>  et : countz.entrySet()) {
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
            List<Object[]> a = new ArrayList();
            for (Map.Entry<String, Integer> e : et.getValue()) {
                a.add(new Object[] {
                    e.getKey(), null, e.getValue()
                });
            }
            cnts.put(et.getKey(), a);
        }

        return cnts;
    }

    private int acount(Map rd,
            Map<String, Map<String, Integer>> counts,
            Map<String, Map<String, Integer>> countz,
            Map<String, Set<String         >> countx,
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
                            String               k    = et .getKey   ( );
                            Map<String, Integer> cntc = et .getValue ( );
                            IndexableField[]     vals = doc.getFields(k);

                            for(IndexableField vol : vals) {
                                String   val = vol.stringValue();
                                         val = getValue( val,k );
                                if (cntc.containsKey(val)) {
                                    cntc.put(val , cntc.get(val) + 1);
                                }
                            }
                        }

                        for (Map.Entry<String, Map<String, Integer>> et : counts.entrySet()) {
                            String               k    = et .getKey   ( );
                            Map<String, Integer> cntc = et .getValue ( );
                            IndexableField[]     vals = doc.getFields(k);
                            Map<String, Integer> cntu = countz.get   (k);
                            Set<String>          cntx = countx.get   (k);
                            Set<String>          cntv = cntu != null ? cntu.keySet() : null;

                            for(IndexableField vol : vals) {
                                String   val = vol.stringValue();
                                         val = getValue( val,k );
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

    public Map amount(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();
        IndexReader   reader = that.getReader();

        Map  cnts = new HashMap();

        Set<String> cntz = Synt.toTerms(rd.get(Cnst.RB_KEY));
        Map<String, Map<Minmax, Cntsum>> counts = new HashMap();
        Map<String, Set<Minmax        >> countx = new HashMap();

        //** 整理待统计的数据 **/

        if (cntz != null && !cntz.isEmpty()) {
            for(String   x : cntz) {
                String[] a = x.split(":", 2);
                if (a[0].startsWith ("-")) {
                    a[0] = a[0].substring(1);
                    if (a.length > 1) {
                        if (!countx.containsKey(a[0])) {
                            countx.put(a[0], new HashSet());
                        }

                        Minmax mm = new Minmax(a[1]);
                        countx.get( a[0] ).add( mm );
                    }
                } else {
                    if (a.length > 1) {
                        if (!counts.containsKey(a[0])) {
                            counts.put(a[0], new HashMap());
                        }

                        Minmax mm = new Minmax(a[1]);
                        Cntsum cs = new Cntsum(    );
                        counts.get(a[0]).put(mm, cs);
                    } else {
                        if (!counts.containsKey(a[0])) {
                            counts.put(a[0], new HashMap());
                        }

                        Minmax mm = new Minmax( "" );
                        Cntsum cs = new Cntsum(    );
                        counts.get(a[0]).put(mm, cs);
                    }
                }
            }
        }

        //** 分块统计数据 **/

        Map<String, Map<Minmax , Cntsum>> counts2 = new HashMap();
        Map<String, Set<Minmax         >> countx2 = new HashMap();

        Map<String, Map<Minmax , Cntsum>> counts3 = new HashMap();
        Map<String, Set<Minmax         >> countx3 = new HashMap();

        Set<String> cxts = counts.keySet();

        /**
         * 类似 counts 对应部分的逻辑
         */

        for(String  k  : cxts) {
            Set     vs = null;
            Object  vo = rd.get(k);
            if (vo instanceof Map) {
                Map vm = (Map) vo ;
                if (vm.containsKey (Cnst.RN_REL)) {
                    vs = Synt.setOf(vm.get(Cnst.RN_REL));
                } else
                if (vm.containsKey (Cnst.ON_REL)) {
                    vs = Synt.asSet(vm.get(Cnst.ON_REL));
                }
            }

            if (vs == null || vs.isEmpty()) {
                if (counts .containsKey(k)) {
                    counts2.put(k, counts.get(k));
                }
                if (countx .containsKey(k)) {
                    countx2.put(k, countx.get(k));
                }
            } else {
                Map<Minmax , Cntsum> vz = counts.get(k);
                Set<Minmax         > vx = countx.get(k);

                counts3.clear();
                countx3.clear();

                if (vx != null) {
                    countx3.put(k, vx);
                }
                if (vz != null) {
                    counts3.put(k, vz);
                } else {
                    vz = new HashMap();
                    counts3.put(k, vz);
                }

                for(Object v : vs) {
                    Minmax m = new Minmax(v.toString( ));
                    if (vx  == null || ! vx.contains(m)) {
                        vz.put( m, new Cntsum() );
                    }
                }

                Map xd = new HashMap();
                xd.putAll(rd);
                xd.remove( k);
                amount(xd, counts3, countx3, reader, finder);
            }
        }

        int z = amount(rd, counts2, countx2, reader, finder);
        cnts.put("__count__", z);

        //** 排序统计数据 **/

        Map<String, List<Map.Entry<Minmax, Cntsum>>> cntlst = new HashMap();

        for (Map.Entry<String, Map<Minmax, Cntsum>>  et : counts.entrySet()) {
            String k = et.getKey();
            List<Map.Entry<Minmax, Cntsum>> l = new ArrayList(et.getValue().entrySet());
            List<Map.Entry<Minmax, Cntsum>> a = cntlst.get(k);
            if ( null != a ) {
                l.addAll(a );
            }
            Collections.sort( l, new Sortes());
            cntlst.put(k, l);
        }

        for (Map.Entry<String, List<Map.Entry<Minmax, Cntsum>>> et : cntlst.entrySet()) {
            List<Object[]> a = new ArrayList();
            for (Map.Entry<Minmax, Cntsum> e : et.getValue()) {
                Cntsum c = e.getValue( );
                Minmax m = e.getKey(   );
//              if (c.cnt == 0) continue; // 数值统计都是外部指定的区间, 故不能去掉空值
                String k = m != null ? m.toString() : null;
                a.add(new Object[] {
                    k, null, c.cnt, c.sum ,
                    c.cnt != 0 ? c.min : 0,
                    c.cnt != 0 ? c.max : 0
                });
            }
            cnts.put(et.getKey(), a );
        }

        return cnts;
    }

    private int amount(Map rd,
            Map<String, Map<Minmax , Cntsum>> counts,
            Map<String, Set<Minmax         >> countx,
            IndexReader reader, IndexSearcher finder) throws HongsException {
        int total = 0;

        try {
            Query q = that.getQuery(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
                CoreLogger.debug("SearchRecord.statis: " +q.toString());
            }

            TopDocs docz = finder.search(q, 500);
            while ( docz.totalHits > 0) {
                ScoreDoc[] docs = docz.scoreDocs;

                if (!counts.isEmpty()) {
                    for(ScoreDoc dox : docs) {
                        Document doc = reader.document(dox.doc);

                        for (Map.Entry<String, Map<Minmax, Cntsum>> et : counts.entrySet()) {
                            String              k    = et .getKey   ( );
                            Map<Minmax, Cntsum> cntc = et .getValue ( );
                            Set<Minmax        > cntx = countx.get   (k);
                            IndexableField[   ] vals = doc.getFields(k);

                            F : for (IndexableField x: vals) {
                                double v = x.numericValue( )
                                            . doubleValue( );
                                       v =    getValue(v, k);
                                for (Map.Entry<Minmax, Cntsum> mc : cntc.entrySet()) {
                                    Minmax m = mc.getKey ( );

                                    /*
                                     * 注意:
                                     * 总计并没有跳过需忽略的值,
                                     * 忽略的值仅表示不进行统计;
                                     * 如果需要从结果中排除记录,
                                     * 应在wr或ar参数中进行指定.
                                     */
                                    if (! m.covers( )) {
                                        if (! m.covers(v)) {
                                            continue;
                                        }

                                        if (  cntx  != null)
                                        for(Minmax w : cntx) {
                                            if (w.covers(v)) {
                                                continue F ;
                                            }
                                        }
                                    }

                                    mc.getValue().add(v);
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

    /**
     * 为可能的特殊数据分组留有余地
     * @param v
     * @param k
     * @return
     */
    protected String getValue(String v, String k) {
        return v;
    }

    /**
     * 为可能的特殊数据处理留有余地
     * @param v
     * @param k
     * @return
     */
    protected double getValue(double v, String k) {
        return v;
    }

    // jdk 1.7 加上这个后排序不会报错
    static {
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
    }

    private static class Sorted implements Comparator<Map.Entry<String, Integer>> {
        @Override
        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
            int i1 = o1.getValue(), i2 = o2.getValue();
            if (i1 > i2) return -1;
            if (i1 < i2) return  1;
            // 数量一致时按取值排, 避免限定后顺序变化
            return o1.getKey().compareTo(o2.getKey() );
        }
    }

    private static class Sortes implements Comparator<Map.Entry<Minmax, Cntsum >> {
        @Override
        public int compare(Map.Entry<Minmax, Cntsum > o1, Map.Entry<Minmax, Cntsum > o2) {
            // 区间为空的表示总计, 确保它总是在第一个
            Minmax k1 = o1.getKey(  ), k2 = o2.getKey(  );
            if  (  k1.covers()  ) return -1;
            if  (  k2.covers()  ) return  1;

            Cntsum x1 = o1.getValue(), x2 = o2.getValue();
            return x1.cnt != x2.cnt ? (x2.cnt > x1.cnt ? 1 : -1)
                : (x1.sum != x2.sum ? (x2.sum > x1.sum ? 1 : -1) : 0 );
        }
    }

    private static class Cntsum {
        public int    cnt = 0;
        public double sum = 0;
        public double min = Double.NEGATIVE_INFINITY;
        public double max = Double.POSITIVE_INFINITY;

        public void add(double v) {
            cnt += 1;
            sum += v;
            if (min > v || min == Double.NEGATIVE_INFINITY) {
                min = v;
            }
            if (max < v || max == Double.POSITIVE_INFINITY) {
                max = v;
            }
        }
    }

    private static class Minmax {
        public double min = Double.NEGATIVE_INFINITY;
        public double max = Double.POSITIVE_INFINITY;
        public boolean le = true;
        public boolean ge = true;

        public Minmax(double n) {
            min = max = n;
        }

        public Minmax(String s) {
            Object[] a = Synt.toRange(s);
            if (a == null) {
                return;
            }
            if (a[0] != null) {
                min = Synt.declare(a[0], min);
            }
            if (a[1] != null) {
                max = Synt.declare(a[1], max);
            }
            le = (boolean) a[2];
            ge = (boolean) a[3];
        }

        @Override
        public String toString() {
            // 不限则为空
            if (le && min == Double.NEGATIVE_INFINITY
            &&  ge && max == Double.POSITIVE_INFINITY) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(le ? "[" : "(");
            sb.append(min != Double.NEGATIVE_INFINITY ? Tool.toNumStr(min) : "");
            sb.append(",");
            sb.append(max != Double.POSITIVE_INFINITY ? Tool.toNumStr(max) : "");
            sb.append(ge ? "]" : ")");
            return sb.toString();
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Minmax)) {
                return false;
            }
            Minmax m = (Minmax) o;
            return m.le  == le  && m.ge  == ge
                && m.min == min && m.max == max;
        }

        public boolean covers(double n) {
            if (le) { if (n >= max) {
                return false;
            }} else { if (n >  max) {
                return false;
            }}
            if (ge) { if (n <= min) {
                return false;
            }} else { if (n <  min) {
                return false;
            }}
            return true;
        }

        public boolean covers() {
            return le && ge
            && min == Double.NEGATIVE_INFINITY
            && max == Double.POSITIVE_INFINITY;
        }
    }

}
