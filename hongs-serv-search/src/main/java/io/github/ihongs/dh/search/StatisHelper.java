package io.github.ihongs.dh.search;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.lucene.LuceneRecord;
import io.github.ihongs.util.Synt;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.NumericUtils;

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
     * 统计枚举
     * @param rd
     * @return
     * @throws HongsException
     * @deprecated 查询次数随统计列和值一同增多, 效率不够理想
     */
    public Map ecount(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();

        int         topn = Synt.declare(rd.get(Cnst.RN_KEY) , 0);
        Map         incs = Synt.asMap  (rd.get(Cnst.IN_REL)    );
        Map         excs = Synt.asMap  (rd.get(Cnst.NI_REL)    );
        Set<String> cntz = Synt.toTerms(rd.get(Cnst.RB_KEY)    );
        Map<String, Map<String, Integer>> counts = new HashMap();

        //** 整理待统计的数据 **/

        if (incs != null && ! incs.isEmpty( )
        &&  cntz != null && ! cntz.isEmpty()) {
        //  if (incs == null) {
        //      incs  = new HashMap();
        //  }
            if (excs == null) {
                excs  = new HashMap();
            }

            for(String x : cntz) {
                Map cnt; Set inc, exc;
                    inc  = Synt.asSet(incs.get(x));
                if (inc != null && !inc.isEmpty()) {
                    exc  = Synt.asSet(excs.get(x));
                if (exc == null) {
                    exc  = new HashSet( );
                }
                    cnt  = new HashMap( );
                    counts.put( x , cnt );
                    for( Object v : inc ) {
                    if (!exc.contains(v)) {
                        cnt.put(v.toString() , 0 );
                    }}
                }
            }
        }

        //** 分块统计数据 **/

        Map<String, Map<String, Integer>> counts2 = new HashMap();

        Map<String, Map<String, Integer>> counts3 = new HashMap();

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
            Map     vd = null;
            Set     vs = null;
            Object  vo = rd.get(k);
            if (vo instanceof Map) {
                Map vm = (Map) vo ;
                if (vm.containsKey ( /***/ Cnst.IN_REL)) {
                    vs = Synt.asSet(vm.get(Cnst.IN_REL));
                    vd = new HashMap( rd );
                    vm = new HashMap( vm );
                    vm.remove(Cnst.IN_REL);
                    vd.put(k , vm);
                }
            }

            if (vs == null || vs.isEmpty()) {
                if (counts .containsKey(k)) {
                    counts2.put(k, counts.get(k));
                }
            } else {
                Map<String, Integer> vz = counts.get(k);

                counts3.clear();

                if (vz != null) {
                    counts3.put(k, vz);
                } else {
                    vz = new HashMap();
                    counts3.put(k, vz);
                }

                ecount(vd, finder, counts3);
            }
        }

        int z = ecount(rd, finder, counts2);

        // 以上仅对枚举值计数, 以下计算全部
        try {
            Query q ;
            q = that.padQry (rd);
            z = finder.count( q);
        } catch (IOException  e) {
            throw new HongsException(e);
        }

        Map cnts = new HashMap();
        cnts.put("__count__", z);

        //** 排序并截取统计数据 **/

        for(Map.Entry<String, Map<String, Integer>> et : counts.entrySet()) {
            List<Object[]> a = new ArrayList ( et.getValue().size(  )  );
            for(Map.Entry<String, Integer> e : et.getValue().entrySet()) {
                String m = e.getKey ();
                int c  = e.getValue ();
                if (c != 0) {
                    a.add(new Object[] {m, null, c});
                }
            }
            Collections.sort (a, new Counts());
            if (0 < topn && topn < a . size()) {
                a = a.subList(0, topn);
            }
            cnts.put( et.getKey(), a );
        }

        return cnts;
    }

    private int ecount( Map rd, IndexSearcher finder,
            Map<String, Map<String, Integer>> counts) throws HongsException {
        int total = 0 ;

        Query q = that.padQry(rd);

        if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG) ) {
            CoreLogger.debug("StatisHelper.ecount: "+ q.toString());
        }

        Map fs = getRecord().getFields();
        Map ts = FormSet.getInstance().getEnum("__types__");
        Set ks = Synt.setOf("int", "long", "float", "double", "number");

        for(Map.Entry<String, Map<String, Integer>> et : counts.entrySet()) {
            Map<String, Integer> fo = et.getValue();
                String fn = et.getKey();

                // 数值类型采用区间查法
                Map    fc = ( Map ) fs.get(fn);
                Object ft = fc.get("__type__");
                Object fk = fc.get(  "type"  );
                       ft = ts.containsKey(ft) ? ts.get(ft) : ft ;
                String fr = "date" .equals(ft)
                       ||  "number".equals(ft)
                       || ( "enum" .equals(ft) && ks.contains(fk))
                       || ("hidden".equals(ft) && ks.contains(fk))
                          ? Cnst.RG_REL
                          : Cnst.IN_REL;

            for(Map.Entry<String, Integer> xt : fo.entrySet()) {
                String fv = xt.getKey();

                // 增加查询条件
                Query b = that.padQry(Synt.mapOf(fn, Synt.mapOf(fr, fv)));
                BooleanQuery.Builder bq = new BooleanQuery.Builder();
                bq.add(q, BooleanClause.Occur.MUST);
                bq.add(b, BooleanClause.Occur.MUST);

                // 计算命中数量
                int c ;
                try {
                    c = finder.count(bq.build());
                } catch (IOException ex) {
                    throw new HongsException(ex);
                }

                xt.setValue(c);
                if (total < c) {
                    total = c;
                }
            }
        }

        return total;
    }

    /**
     * 统计数量
     * @param rd
     * @return
     * @throws HongsException
     */
    public Map acount(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();

        int         topn = Synt.declare(rd.get(Cnst.RN_KEY) , 0);
        Map         incs = Synt.asMap  (rd.get(Cnst.IN_REL)    );
        Map         excs = Synt.asMap  (rd.get(Cnst.NI_REL)    );
        Set<String> cntz = Synt.toTerms(rd.get(Cnst.RB_KEY)    );
        Map<String, Map<String, Integer>> counts = new HashMap();
        Map<String, Set<String         >> countx = new HashMap();

        //** 整理待统计的数据 **/

        if (cntz != null && !cntz.isEmpty()) {
            if (incs == null) {
                incs  = new HashMap();
            }
            if (excs == null) {
                excs  = new HashMap();
            }

            for(String x : cntz) {
                Map cnt; Set inc, exc, cnx;

                    cnt  = new  HashMap( );
                    inc  = Synt.asSet(incs.get(x));
                if (inc != null && !inc.isEmpty()) {
                    for(Object v:inc) {
                        String s = v.toString ( );
                       cnt.put(s, 0 );
                    }
                }   counts.put(x,cnt);

                    cnx  = new  HashSet( );
                    exc  = Synt.asSet(excs.get(x));
                if (exc != null && !exc.isEmpty()) {
                    for(Object v:inc) {
                        String s = v.toString ( );
                       cnx.add(s/**/);
                    }
                }   countx.put(x,cnx);
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
            Map     vd = null;
            Set     vs = null;
            Object  vo = rd.get(k);
            if (vo instanceof Map) {
                Map vm = (Map) vo ;
                if (vm.containsKey ( /***/ Cnst.IN_REL)) {
                    vs = Synt.asSet(vm.get(Cnst.IN_REL));
                    vd = new HashMap( rd );
                    vm = new HashMap( vm );
                    vm.remove(Cnst.IN_REL);
                    vd.put(k , vm);
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

                acount(vd, finder, counts3, countx3);
            }
        }

        int z = acount(rd, finder, counts2, countx2);

        Map cnts = new HashMap();
        cnts.put("__count__", z);

        //** 排序并截取统计数据 **/

        for(Map.Entry<String, Map<String, Integer>> et : counts.entrySet()) {
            List<Object[]> a = new ArrayList ( et.getValue().size(  )  );
            for(Map.Entry<String, Integer> e : et.getValue().entrySet()) {
                String m = e.getKey ();
                int c  = e.getValue ();
                if (c != 0) {
                    a.add(new Object[] {m, null, c});
                }
            }
            Collections.sort (a, new Counts());
            if (0 < topn && topn < a . size()) {
                a = a.subList(0, topn);
            }
            cnts.put( et.getKey(), a );
        }

        return cnts;
    }

    private int acount( Map rd, IndexSearcher finder,
            Map<String, Map<String, Integer>> counts,
            Map<String, Set<String         >> countx) throws HongsException {
        Set<Map.Entry<String, Map<String, Integer>>> es = counts.entrySet();

        // 判断是否需要额外的值
        Set<String> more = new HashSet();
        for(Map.Entry<String, Map<String, Integer>>  et : es) {
            if (et.getValue().isEmpty()) {
                more.add( et . getKey());
            }
        }

        int t = 0;

        try {
            Query q = that.padQry(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG) ) {
                CoreLogger.debug("StatisHelper.acount: "+ q.toString());
            }

            if (counts.isEmpty()) return finder.count (q);

            Collec c = new AcountCollec((String k, String[] a) -> {
                Map<String, Integer> cntc = counts.get(k);
                Set<String         > cntx = countx.get(k);

                if (! more.contains( k )) {
                    for ( String v : a  ) {
                        if (cntx.contains   (v)) {
                            continue;
                        }
                        if (cntc.containsKey(v)) {
                            cntc.put(v, 1 + cntc.get(v));
                        }
                    }
                } else {
                    for ( String v : a  ) {
                        if (cntx.contains   (v)) {
                            continue;
                        }
                        if (cntc.containsKey(v)) {
                            cntc.put(v, 1 + cntc.get(v));
                        } else {
                            cntc.put(v, 1);
                        }
                    }
                }
            } , counts.keySet().toArray(new String[counts.size()]), that);

            finder.search(q, c);
            t = c.countTotals();

            /*
              TopDocs  docz = finder.search(q , 65536);
            ScoreDoc[] docs = docz.scoreDocs;
            t   =   (  int  ) docz.totalHits;

            if (!counts.isEmpty()) while (docs.length > 0) {
                for(ScoreDoc dox : docs) {
                    Document doc = finder.doc(dox.doc);

                    // 逐个字段读值并计数
                    for(Map.Entry<String, Map<String, Integer>>  et : es) {
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

                docz = finder.searchAfter(docs[docs.length - 1], q, 65536);
                docs = docz.scoreDocs;
            }
            */
        } catch (IOException ex) {
            throw new HongsException(ex);
        }

        return t;
    }

    public Map amount(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();

        int         topn = Synt.declare(rd.get(Cnst.RN_KEY) , 0);
        Map         incs = Synt.asMap  (rd.get(Cnst.IN_REL)    );
        Map         excs = Synt.asMap  (rd.get(Cnst.NI_REL)    );
        Set<String> cntz = Synt.toTerms(rd.get(Cnst.RB_KEY)    );
        Map<String, Map<Minmax, Cntsum >> counts = new HashMap();
        Map<String, Set<Minmax         >> countx = new HashMap();

        //** 整理待统计的数据 **/

        if (cntz != null && !cntz.isEmpty()) {
            if (incs == null) {
                incs  = new HashMap();
            }
            if (excs == null) {
                excs  = new HashMap();
            }

            for(String x : cntz) {
                Map cnt; Set inc, exc, cnx;

                    inc  = Synt.asSet(incs.get(x));
                if (inc != null && !inc.isEmpty()) {
                    cnt  = new  HashMap();
                    for(Object v:inc) {
                        String s = v.toString ( );
                        Minmax m = new Minmax (s);
                        Cntsum c = new Cntsum ( );
                       cnt.put(m, c );
                    }
                    counts.put(x,cnt);
                }

                    exc  = Synt.asSet(excs.get(x));
                if (exc != null && !exc.isEmpty()) {
                    cnx  = new  HashSet( );
                    for(Object v:inc) {
                        String s = v.toString ( );
                        Minmax m = new Minmax (s);
                    //  Cntsum c = new Cntsum ( );
                       cnx.add(m/**/);
                    }
                    countx.put(x,exc);
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
            Map     vd = null;
            Set     vs = null;
            Object  vo = rd.get(k);
            if (vo instanceof Map) {
                Map vm = (Map) vo ;
                if (vm.containsKey ( /***/ Cnst.RG_REL)) {
                    vs = Synt.asSet(vm.get(Cnst.RG_REL));
                    vd = new HashMap( rd );
                    vm = new HashMap( vm );
                    vm.remove(Cnst.RG_REL);
                    vd.put(k , vm);
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

                amount(vd, finder, counts3, countx3);
            }
        }

        int z = amount(rd, finder, counts2, countx2);

        Map cnts = new HashMap();
        cnts.put("__count__", z);

        //** 排序统计数据 **/

        for(Map.Entry<String, Map<Minmax, Cntsum>> et : counts.entrySet()) {
            List<Object[]> a = new ArrayList ( et.getValue().size(  )  );
            for(Map.Entry<Minmax, Cntsum> e : et.getValue().entrySet()) {
                Minmax m = e.getKey  ();
                Cntsum c = e.getValue();
                String v = m.toString();
                if (0 < c.cnt) {
                    a.add( new Object[] {
                        v, null,
                        c.cnt, c.sum,
                        c.min, c.max
                    } );
                }
            }
            Collections.sort (a, new Mounts());
            if (0 < topn && topn < a . size()) {
                a = a.subList(0, topn);
            }
            cnts.put( et.getKey(), a );
        }

        return cnts;
    }

    private int amount( Map rd, IndexSearcher finder,
            Map<String, Map<Minmax , Cntsum>> counts,
            Map<String, Set<Minmax         >> countx) throws HongsException {
        Set<Map.Entry<String, Map<Minmax , Cntsum>>> es = counts.entrySet();

        int t = 0;

        try {
            Query q = that.padQry(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG) ) {
                CoreLogger.debug("StatisHelper.amount: "+ q.toString());
            }

            if (counts.isEmpty()) return finder.count (q);

            Collec c = new AmountCollec((String k, double[] a) -> {
                Map<Minmax, Cntsum > cntc = counts.get(k);
                Set<Minmax         > cntx = countx.get(k);

                F : for(double v :  a  ) {
                    if (  null  != cntx)
                    for(Minmax w : cntx) {
                        if (w.covers(v)) {
                            continue F ;
                        }
                    }

                    for(Map.Entry<Minmax, Cntsum> mc : cntc.entrySet()) {
                        Cntsum w = mc.getValue( );
                        Minmax m = mc.getKey  ( );

                        if (m.covers(v)) {
                            w.add   (v);
                        }
                    }
                }
            } , counts.keySet().toArray(new String[counts.size()]), that);

            finder.search(q, c);
            t = c.countTotals();

            /*
              TopDocs  docz = finder.search(q , 65536);
            ScoreDoc[] docs = docz.scoreDocs;
            t   =   (  int  ) docz.totalHits;
            if (!counts.isEmpty()) while (docs.length > 0) {
                for(ScoreDoc dox : docs) {
                    Document doc = finder.doc(dox.doc);

                    // 逐个字段读值并计数
                    for(Map.Entry<String, Map<Minmax, Cntsum>>  et : es) {
                        String              k    = et .getKey   ( );
                        Map<Minmax, Cntsum> cntc = et .getValue ( );
                        Set<Minmax        > cntx = countx.get   (k);
                        IndexableField[   ] vals = doc.getFields(k);

                    F : for(IndexableField x : vals) {
                            double v = x.numericValue()
                                        . doubleValue();
                                   v = getValue( v, k );

                            if (  null  != cntx)
                            for(Minmax w : cntx) {
                                if (w.covers(v)) {
                                    continue F ;
                                }
                            }

                            for(Map.Entry<Minmax, Cntsum> mc : cntc.entrySet()) {
                                Cntsum c = mc.getValue( );
                                Minmax m = mc.getKey  ( );

                                if (m.covers(v)) {
                                    c.add   (v);
                                }
                            }
                        }
                    }
                }

                docz = finder.searchAfter(docs[docs.length - 1], q, 65536);
                docs = docz.scoreDocs;
            }
            */
        } catch (IOException ex) {
            throw new HongsException(ex);
        }

        return t;
    }

    /**
     * 为可能的特殊数据分组留有余地(废弃)
     * @param v
     * @param k
     * @return
    protected String getValue(String v, String k) {
        return v;
    }
     */

    /**
     * 为可能的特殊数据处理留有余地(废弃)
     * @param v
     * @param k
     * @return
    protected double getValue(double v, String k) {
        return v;
    }
     */

    private static class AcountCollec extends Collec<String[]> {

        public AcountCollec(Coller<String[]> coller, String[] fields, LuceneRecord record) {
            super(coller, fields, record);
        }

        @Override
        public LeafCollector getLeafCollector(LeafReaderContext lrc) throws IOException {
            LeafReader reader = lrc.reader( );

            for (int i = 0; i < fields.length; i ++) {
                if (groups[i][0] >= 1) {
                if (groups[i][1] == 1) {
                    values[i] = reader.getSortedNumericDocValues("%"+fields[i]);
                } else {
                    values[i] = reader.      getNumericDocValues("#"+fields[i]);
                }
                } else {
                if (groups[i][1] == 1) {
                    values[i] = reader.getSortedSetDocValues("%"+fields[i]);
                } else {
                    values[i] = reader.   getSortedDocValues("#"+fields[i]);
                }
                }
            }

            return this;
        }

        @Override
        public void collect(int doc) throws IOException {
            super.collect(doc);

            for (int i = 0; i < fields.length; i ++) {
                String n = fields[i];
                Object d = values[i];
                if (d == null) {
                    continue ;
                }

                if (groups[i][0] == 0 && groups[i][1] == 0) {
                    SortedDocValues b = (SortedDocValues) d;
                    if (!b.advanceExact(doc)) {
                        continue;
                    }

                    String[] v = new String[1];
                    v[0] = b.binaryValue().utf8ToString();
                    coller.collect(n, v);
                } else
                if (groups[i][0] == 0 && groups[i][1] == 1) {
                    SortedSetDocValues b = (SortedSetDocValues) d;
                    if (!b.advanceExact(doc)) {
                        continue;
                    }

                    List<String> u;
                    String[]     v;
                    long         j;
                    u = new LinkedList();
                    while ((j = b.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
                        u.add(b.lookupOrd(j).utf8ToString() );
                    }
                    v = u.toArray ( new String [ u.size() ] );
                    coller.collect(n, v);
                } else
                if (groups[i][0] >= 1 && groups[i][1] == 0) {
                    NumericDocValues b = (NumericDocValues) d;
                    if (!b.advanceExact(doc)) {
                        continue;
                    }

                    String[] v = new String[1];
                    if (groups[i][0] == 4) {
                        v[0] = Synt.asString(NumericUtils.sortableLongToDouble((long) b.longValue()));
                    }  else
                    if (groups[i][0] == 3) {
                        v[0] = Synt.asString(NumericUtils. sortableIntToFloat (( int) b.longValue()));
                    }  else
                    {
                        v[0] = Synt.asString(b.longValue());
                    }
                    coller.collect(n, v);
                } else
                if (groups[i][0] >= 1 && groups[i][1] == 1) {
                    SortedNumericDocValues b = (SortedNumericDocValues) d;
                    if (!b.advanceExact(doc)) {
                        continue;
                    }

                    String[] v = new String[b.docValueCount()];
                    if (groups[i][0] == 4) {
                    for( int j = 0; j < v.length; j ++ ) {
                        v[j] = Synt.asString(NumericUtils.sortableLongToDouble((long) b.nextValue()));
                    }} else
                    if (groups[i][0] == 3) {
                    for( int j = 0; j < v.length; j ++ ) {
                        v[j] = Synt.asString(NumericUtils. sortableIntToFloat (( int) b.nextValue()));
                    }} else
                    {
                    for( int j = 0; j < v.length; j ++ ) {
                        v[j] = Synt.asString(b.nextValue());
                    }}
                    coller.collect(n, v);
                }
            }
        }

    }

    private static class AmountCollec extends Collec<double[]> {

        public AmountCollec(Coller<double[]> coller, String[] fields, LuceneRecord record) {
            super(coller, fields, record);
        }

        @Override
        public LeafCollector getLeafCollector(LeafReaderContext lrc) throws IOException {
            LeafReader reader = lrc.reader( );

            for (int i = 0; i < fields.length; i ++) {
                if (groups[i][1] == 1) {
                    values[i] = reader.getSortedNumericDocValues("%"+fields[i]);
                } else {
                    values[i] = reader.      getNumericDocValues("#"+fields[i]);
                }
            }

            return this;
        }

        @Override
        public void collect(int doc) throws IOException {
            super.collect(doc);

            for (int i = 0; i < fields.length; i ++) {
                String n = fields[i];
                Object d = values[i];
                if (d == null) {
                    continue ;
                }

                if (groups[i][0] >= 1 && groups[i][1] == 0) {
                    NumericDocValues b = (NumericDocValues) d;
                    if (!b.advanceExact(doc)) {
                        continue;
                    }

                    double[] v = new double[1];
                    if (groups[i][0] == 4) {
                        v[0] = NumericUtils.sortableLongToDouble((long) b.longValue());
                    }  else
                    if (groups[i][0] == 3) {
                        v[0] = NumericUtils. sortableIntToFloat (( int) b.longValue());
                    }  else
                    {
                        v[0] = (double) b.longValue();
                    }
                    coller.collect(n, v);
                } else
                if (groups[i][0] >= 1 && groups[i][1] == 1) {
                    SortedNumericDocValues b = (SortedNumericDocValues) d;
                    if (!b.advanceExact(doc)) {
                        continue;
                    }

                    double[] v = new double[(int) b.docValueCount()];
                    if (groups[i][0] == 4) {
                    for( int j = 0; j < v.length; j ++ ) {
                        v[j] = NumericUtils.sortableLongToDouble((long) b.nextValue());
                    }} else
                    if (groups[i][0] == 3) {
                    for( int j = 0; j < v.length; j ++ ) {
                        v[j] = NumericUtils. sortableIntToFloat (( int) b.nextValue());
                    }} else
                    {
                    for( int j = 0; j < v.length; j ++ ) {
                        v[j] = (double) b.nextValue();
                    }}
                    coller.collect(n, v);
                }
            }
        }

    }

    private static interface Coller<V> {
        public void collect(String k, V v);
    }

    private static abstract class Collec<V> implements Collector, LeafCollector {

        protected final Coller<V> coller;
        protected final String[ ] fields;
        protected final Object[ ] values;
        protected final int[ ][ ] groups; // 类型
        private Scorer  scorer;
        private boolean scores;
        private int     totals;

        public Collec(Coller<V> coller, String[ ] fields, LuceneRecord record) {
            this.coller = coller;
            this.fields = fields;
            this.values = new Object[this.fields.length];
            this.groups = new int[this.fields.length][2];

                int  i  = -1;
                Map fs  = record.getFields();
            for(String fn : fields) { i ++  ;
                Map fc  = (Map) fs.get( fn );
                if (fc == null) {
                    continue;
                }

                String  t = (String) fc.get("__type__");
                if ( t == null) {
                    continue;
                }

                if (Synt.declare(fc.get("__repeated__"), false)) {
                    groups[i][1] = 1;
                } else {
                    groups[i][1] = 0;
                }

                switch (t) {
                    case "string":
                    case "search":
                    case "sorted":
                    case "stored":
                    case "object":
                        groups[i][0] = 0;
                        continue ;
                }

                // 基准类型
                try {
                    String k  = (String) FormSet
                          .getInstance ( /***/ )
                          .getEnum ("__types__")
                          .get (t);
                    if (null != k) {
                           t  = k;
                    }
                } catch (HongsException e) {
                    throw e.toExemption( );
                }

                Object  k = fc.get("type");
                switch (t) {
                    case "number":
                        if (   "int".equals(k)) {
                            groups[i][0] = 1;
                        } else
                        if (  "long".equals(k)) {
                            groups[i][0] = 2;
                        } else
                        if ("float".equals(k)) {
                            groups[i][0] = 3;
                        } else
                        {
                            groups[i][0] = 4;
                        }
                        break ;
                    case "hidden":
                    case  "enum" :
                        if (   "int".equals(k)) {
                            groups[i][0] = 1;
                        } else
                        if (  "long".equals(k)) {
                            groups[i][0] = 2;
                        } else
                        if ( "float".equals(k)) {
                            groups[i][0] = 3;
                        } else
                        if ("double".equals(k)
                        ||  "number".equals(k)) {
                            groups[i][0] = 4;
                        }
                        break ;
                    case  "date" :
                        groups[i][0] = 2;
                        break ;
                    default:
                        groups[i][0] = 0;
                }
            }
        }

        @Override
        public void collect(int doc) throws IOException {
            this.totals ++;
        }

        @Override
        public void setScorer(Scorer scorer) throws IOException {
            this.scorer = scorer;
        }

        public void needsScores(boolean scores) {
            this.scores = scores;
        }

        @Override
        public boolean needsScores() {
            return this . scores;
        }

        public int     countTotals() {
            return this . totals;
        }

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
            sb.append(min != Double.NEGATIVE_INFINITY ? Synt.asString(min) : "");
            sb.append(",");
            sb.append(max != Double.POSITIVE_INFINITY ? Synt.asString(max) : "");
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
            if (le) { if (n >  max) {
                return false;
            }} else { if (n >= max) {
                return false;
            }}
            if (ge) { if (n <  min) {
                return false;
            }} else { if (n <= min) {
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
