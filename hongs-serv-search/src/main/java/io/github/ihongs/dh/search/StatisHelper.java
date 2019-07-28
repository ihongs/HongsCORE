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
public class StatisHelper {

    private final LuceneRecord that;

    public StatisHelper(LuceneRecord that) {
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

        int         topn = Synt.declare(rd.get(Cnst.RN_KEY) , 0);
        Set<String> cntz = Synt.toTerms(rd.get(Cnst.RB_KEY)    );
        Map<String, Map<String, Integer>> counts = new HashMap();
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
                        if (!counts.containsKey(a[0])) {
                            counts.put(a[0], new HashMap());
                        }
                        counts.get(a[0]).put(a[1] , 0);
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
        Map<String, Set<String         >> countx2 = new HashMap();

        Map<String, Map<String, Integer>> counts3 = new HashMap();
        Map<String, Set<String         >> countx3 = new HashMap();

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

        for(String  k  : counts.keySet()) {
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
                    vs.remove( "" );
                    vs.remove(null);
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
                Map<String, Integer> vz = counts.get(k);
                Set<String         > vx = countx.get(k);

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

                /* 如果将参数值加入, 下面会跳过其他值
                for(Object v : vs) {
                    String s = v.toString();
                    if (vx  == null || ! vx.contains(s)) {
                        vz.put( s, 0 );
                    }
                }
                */

                Map xd = new HashMap();
                xd.putAll(rd);
                xd.remove( k);
                acount(xd, counts3, countx3, reader, finder);
            }
        }

        int z = acount(rd, counts2, countx2, reader, finder);

        Map cnts = new HashMap();
        cnts.put("__count__", z);

        //** 排序并截取统计数据 **/

        for(Map.Entry<String, Map<String, Integer>> et : counts.entrySet()) {
            List<Object[]>  a = new ArrayList( );
            for (Map.Entry< String, Integer > e : et.getValue().entrySet()) {
                String  m = e.getKey  ();
                Integer c = e.getValue();
                a.add(new Object[] {
                    m, null, c
                });
            }
            Collections.sort (a, new Counts());
            if (0 < topn && topn < a . size()) {
                a = a.subList(0, topn);
            }
            cnts.put( et.getKey(), a );
        }

        return cnts;
    }

    private int acount(Map rd,
            Map<String, Map<String, Integer>> counts,
            Map<String, Set<String         >> countx,
            IndexReader reader, IndexSearcher finder) throws HongsException {
        // 判断是否需要额外的值
        Set<String> more = new HashSet();
        for(Map.Entry<String, Map<String, Integer>> et : counts.entrySet()) {
            if (et.getValue().isEmpty()) {
                more.add( et . getKey());
            }
        }

        int total = 0;

        try {
            Query q = that.padQry(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
                CoreLogger.debug("SearchRecord.counts: "+q.toString());
            }

            TopDocs docz = finder.search(q,65535);
            while ( docz.totalHits > 0) {
                ScoreDoc[] docs = docz.scoreDocs ;

                if (!counts.isEmpty()) {
                    for(ScoreDoc dox : docs) {
                        Document doc = reader.document(dox.doc);

                        for(Map.Entry<String, Map<String, Integer>> et : counts.entrySet()) {
                            String               k    = et .getKey   ( );
                            Map<String, Integer> cntc = et .getValue ( );
                            IndexableField[]     vals = doc.getFields(k);
                            Set<String>          cntx = countx.get   (k);

                            if (! more.contains( k )) {
                                for(IndexableField x : vals) {
                                    String   v = x.stringValue();
                                             v = getValue( v,k );
                                    if (cntc.containsKey(v)) {
                                        cntc.put(v, 1 + cntc.get(v));
                                    }
                                }
                            } else {
                                for(IndexableField x : vals) {
                                    String   v = x.stringValue();
                                             v = getValue( v,k );
                                    if (cntc.containsKey(v)) {
                                        cntc.put(v, 1 + cntc.get(v));
                                    } else
                                    if (cntx == null
                                    || !cntx.contains   (v)) {
                                        cntc.put(v, 1);
                                    }
                                }
                            }
                        }
                    }
                }

                if (docs.length > 0) {
                    docz = finder.searchAfter(docs[docs.length - 1], q, 65535);
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

        int         topn = Synt.declare(rd.get(Cnst.RN_KEY), 0);
        Set<String> cntz = Synt.toTerms(rd.get(Cnst.RB_KEY)   );
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

        /**
         * 根据请求数据进行综合判断,
         * 如果字段已经作为过滤条件,
         * 则此字段的统计需单独进行,
         * 且需抛开此字段的过滤数据.
         *
         * 例如某数据有一个时间字段且每条记录只能有一个时间,
         * 如果没有以下处理则选某段后其他未选区间数量将为零.
         *
         * 与 acount 的对应逻辑类似
         */

        for(String  k  : counts.keySet()) {
            Set     vs = null;
            Object  vo = rd.get(k);
            if (vo instanceof Map) {
                Map vm = (Map) vo ;
                if (vm.containsKey ( /***/ Cnst.RG_REL)) {
                    vs = Synt.asSet(vm.get(Cnst.RG_REL));
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

                /* 如果将参数值加入, 下面会跳过其他值
                for(Object v : vs) {
                    Minmax m = new Minmax(v.toString( ));
                    if (vx  == null || ! vx.contains(m)) {
                        vz.put( m, new Cntsum() );
                    }
                }
                */

                Map xd = new HashMap();
                xd.putAll(rd);
                xd.remove( k);
                amount(xd, counts3, countx3, reader, finder);
            }
        }

        int z = amount(rd, counts2, countx2, reader, finder);

        Map cnts = new HashMap();
        cnts.put("__count__", z);

        //** 排序统计数据 **/

        for(Map.Entry<String, Map<Minmax, Cntsum>> et : counts.entrySet()) {
            List<Object[]> a = new ArrayList( );
            for(Map.Entry < Minmax, Cntsum > e : et.getValue().entrySet()) {
                Minmax m = e.getKey  ();
                Cntsum c = e.getValue();
                String v = m.toString();
                a.add(new Object[] {
                    v, null, c.cnt, c.sum ,
                    c.cnt != 0 ? c.min : 0, // 最小值
                    c.cnt != 0 ? c.max : 0  // 最大值
                });
            }
            Collections.sort (a, new Mounts());
            if (0 < topn && topn < a . size()) {
                a = a.subList(0, topn);
            }
            cnts.put( et.getKey(), a );
        }

        return cnts;
    }

    private int amount(Map rd,
            Map<String, Map<Minmax , Cntsum>> counts,
            Map<String, Set<Minmax         >> countx,
            IndexReader reader, IndexSearcher finder) throws HongsException {
        int total = 0;

        try {
            Query q = that.padQry(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
                CoreLogger.debug("SearchRecord.statis: " +q.toString());
            }

            TopDocs docz = finder.search(q,65535);
            while ( docz.totalHits > 0) {
                ScoreDoc[] docs = docz.scoreDocs ;

                if (!counts.isEmpty()) {
                    for(ScoreDoc dox : docs) {
                        Document doc = reader.document(dox.doc);

                        for (Map.Entry<String, Map<Minmax, Cntsum>> et : counts.entrySet()) {
                            String              k    = et .getKey   ( );
                            Map<Minmax, Cntsum> cntc = et .getValue ( );
                            Set<Minmax        > cntx = countx.get   (k);
                            IndexableField[   ] vals = doc.getFields(k);

                        F : for (IndexableField x : vals) {
                                double v = x.numericValue()
                                            . doubleValue();
                                       v = getValue( v, k );

                                if (cntx != null) for (Minmax w : cntx) {
                                    if (w.covers(v)) {
                                        continue F ;
                                    }
                                }

                                for (Map.Entry<Minmax, Cntsum> mc : cntc.entrySet()) {
                                    Minmax m = mc.getKey  ( );
                                    Cntsum c = mc.getValue( );

                                    if (m.covers(v)) {
                                        c.add   (v);
                                    }
                                }
                            }
                        }
                    }
                }

                if (docs.length > 0) {
                    docz = finder.searchAfter(docs[docs.length - 1], q, 65535);
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

    private static class Counts implements Comparator<Object[]> {
        @Override
        public int compare(Object[] o1, Object[] o2) {
            int    cnt1 = (int)    o1[2];
            int    cnt2 = (int)    o2[2];
            if (cnt1 != cnt2) {
                return  cnt1>cnt2 ? -1:1;
            }

            return 0;
        }
    }

    private static class Mounts implements Comparator<Object[]> {
        @Override
        public int compare(Object[] o1, Object[] o2) {
            int    cnt1 = (int)    o1[2];
            int    cnt2 = (int)    o2[2];
            if (cnt1 != cnt2) {
                return  cnt1>cnt2 ? -1:1;
            }

            double sum1 = (double) o1[3];
            double sum2 = (double) o2[3];
            if (sum1 != sum2) {
                return  sum1>sum2 ? -1:1;
            }

            return 0;
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
