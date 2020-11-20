package io.github.ihongs.dh.search;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.lucene.LuceneRecord;
import io.github.ihongs.dh.search.StatisGrader.Range;
import io.github.ihongs.dh.search.StatisGrader.Ratio;
import io.github.ihongs.util.Synt;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

/**
 * 搜索助手
 * @author Hongs
 */
public class StatisHandle {

    private final LuceneRecord that;

    public StatisHandle(LuceneRecord that) {
        this.that = that;
    }

    public final LuceneRecord getRecord( ) {
        return that;
    }

    /**
     * 分类计数
     * @param rd
     * @return
     * @throws HongsException
     */
    public Map acount(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();

        int         topn = Synt.declare(rd.get(Cnst.RN_KEY), 0);
        Map         incs = Synt.asMap  (rd.get(Cnst.IN_REL) );
        Map         excs = Synt.asMap  (rd.get(Cnst.NI_REL) );
        Set<String> cntz = Synt.toTerms(rd.get(Cnst.RB_KEY) );
        Map<String, Map<Object, Long>> counts = new HashMap();
        Map<String, Set<Object      >> countx = new HashMap();

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

        Map<String, Map<Object, Long>> counts2 = new HashMap();
        Map<String, Set<Object      >> countx2 = new HashMap();

        Map<String, Map<Object, Long>> counts3 = new HashMap();
        Map<String, Set<Object      >> countx3 = new HashMap();

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
                Map<Object, Long> vz = counts.get(k);
                Set<Object      > vx = countx.get(k);

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

        for(Map.Entry<String, Map<Object, Long>> et : counts.entrySet()) {
            List<Object[]> a = new ArrayList(et.getValue().size(  )  );
            for(Map.Entry<Object , Long> e : et.getValue().entrySet()) {
                Object m = e.getKey  ();
                long   c = e.getValue();
                if (0 != c) {
                    a.add(new Object [] {m, null, c});
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
            Map<String, Map<Object , Long > > counts,
            Map<String, Set<Object        > > countx) throws HongsException {
        int t = 0;

        try {
            Query q = that.padQry(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
                CoreLogger.debug("StatisHandle.acount: "+q.toString());
            }

            if (counts.isEmpty()) return finder.count(q);

            StatisGrader.Field[] f = getGraderFields (counts.keySet());
            StatisGrader.Collec  c = new StatisGrader.Collec(new StatisGrader.Acount(counts, countx), f);

            finder.search(q, c);

            t = (int) c.countTotals ( );
        } catch (IOException e) {
            throw new HongsException(e);
        }

        return t;
    }

    /**
     * 分类计算
     * @param rd
     * @return
     * @throws HongsException
     */
    public Map amount(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();

        int         topn = Synt.declare(rd.get(Cnst.RN_KEY), 0);
        Map         incs = Synt.asMap  (rd.get(Cnst.IN_REL) );
        Map         excs = Synt.asMap  (rd.get(Cnst.NI_REL) );
        Set<String> cntz = Synt.toTerms(rd.get(Cnst.RB_KEY) );
        Map<String, Map<Range, Ratio>> counts = new HashMap();
        Map<String, Set<Range       >> countx = new HashMap();

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
                        Range  m = new Range  (s);
                        Ratio  c = new Ratio  ( );
                       cnt.put(m, c );
                    }
                    counts.put(x,cnt);
                }

                    exc  = Synt.asSet(excs.get(x));
                if (exc != null && !exc.isEmpty()) {
                    cnx  = new  HashSet( );
                    for(Object v:inc) {
                        String s = v.toString ( );
                        Range  m = new Range  (s);
                    //  Ratio  c = new Ratio  ( );
                       cnx.add(m/**/);
                    }
                    countx.put(x,exc);
                }
            }
        }

        //** 分块统计数据 **/

        Map<String, Map<Range, Ratio>> counts2 = new HashMap();
        Map<String, Set<Range       >> countx2 = new HashMap();

        Map<String, Map<Range, Ratio>> counts3 = new HashMap();
        Map<String, Set<Range       >> countx3 = new HashMap();

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
                Map<Range, Ratio> vz = counts.get(k);
                Set<Range       > vx = countx.get(k);

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

        for(Map.Entry<String, Map<Range, Ratio>> et : counts.entrySet()) {
            List<Object[]> a = new ArrayList(et.getValue().size(  )  );
            for(Map.Entry<Range , Ratio> e : et.getValue().entrySet()) {
                Range  m = e.getKey  ();
                Ratio  c = e.getValue();
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
            Map<String, Map<Range , Ratio > > counts,
            Map<String, Set<Range         > > countx) throws HongsException {
        int t = 0;

        try {
            Query q = that.padQry(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
                CoreLogger.debug("StatisHandle.amount: "+q.toString());
            }

            if (counts.isEmpty()) return finder.count(q);

            StatisGrader.Field[] f = getGraderFields (counts.keySet());
            StatisGrader.Collec  c = new StatisGrader.Collec(new StatisGrader.Amount(counts, countx), f);

            finder.search(q, c);

            t = (int) c.countTotals ( );
        } catch (IOException e) {
            throw new HongsException(e);
        }

        return t;
    }

    /**
     * 聚合统计
     */
    public Collection<Map> assort (Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();

        Set<String> rb = Synt.toTerms (rd.get(Cnst.RB_KEY));
        if (rb == null || rb.isEmpty()) {
            throw new NullPointerException("Assort fields required.");
        }
        Fields fs = getGatherFields(rb);
        if (fs.fields.length == 0 || fs.indics.length == 0) {
            throw new NullPointerException("Assort fields required!");
        }

        try {
            Query q = that.padQry(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
                CoreLogger.debug("StatisHandle.assort: " + q.toString());
            }

            return new StatisGather(finder)
                .group(fs.fields)
                .count(fs.indics)
                .where(q)
                .fetch( );
        } catch (IOException ex) {
            throw new HongsException ( ex );
        }
    }

    private StatisGrader.Field[] getGraderFields(Set<String> names) {
        StatisGrader.Field[] fields = new StatisGrader.Field[names.size()];
        Map <String, Map   > items  = that.getFields();
        Map <String, String> types  ;

        try {
            types = FormSet.getInstance().getEnum("__types__");
        } catch (HongsException e) {
            throw e.toExemption( );
        }

        int i = 0;
        for(String n : names) {
            String f = n;
            StatisGrader.TYPE g ;

            Map c = items.get(f);
            if (c == null) {
                continue ;
            }

            String t = (String) c.get ("__type__");
            Object k = (Object) c.get (  "type"  );

            // 使用基准类型
            if (types.containsKey(t)) {
                   t = types.get (t);
            }

            if (Synt.declare(c.get("__repeated__"), false) == false) {
                f = "#" + f;
                if (null != t) switch(t) {
                    case "number":
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = StatisGrader.TYPE.INT;
                        } else
                        if (  "long".equals(k)) {
                            g = StatisGrader.TYPE.LONG;
                        } else
                        if ( "float".equals(k)) {
                            g = StatisGrader.TYPE.FLOAT;
                        } else
                        {
                            g = StatisGrader.TYPE.DOUBLE;
                        }
                        break ;
                    case "hidden":
                    case  "enum" :
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = StatisGrader.TYPE.INT;
                        } else
                        if (  "long".equals(k)) {
                            g = StatisGrader.TYPE.LONG;
                        } else
                        if ( "float".equals(k)) {
                            g = StatisGrader.TYPE.FLOAT;
                        } else
                        if ("double".equals(k)
                        ||  "number".equals(k)) {
                            g = StatisGrader.TYPE.DOUBLE;
                        } else
                        {
                            g = StatisGrader.TYPE.STRING;
                        }
                        break ;
                    case  "date" :
                        g = StatisGrader.TYPE.LONG;
                        break ;
                    default:
                        g = StatisGrader.TYPE.STRING;
                } else {
                        g = StatisGrader.TYPE.STRING;
                }
            } else {
                f = "%" + f;
                if (null != t) switch(t) {
                    case "number":
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = StatisGrader.TYPE.INTS;
                        } else
                        if (  "long".equals(k)) {
                            g = StatisGrader.TYPE.LONGS;
                        } else
                        if ( "float".equals(k)) {
                            g = StatisGrader.TYPE.FLOATS;
                        } else
                        {
                            g = StatisGrader.TYPE.DOUBLES;
                        }
                        break ;
                    case "hidden":
                    case  "enum" :
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = StatisGrader.TYPE.INT;
                        } else
                        if (  "long".equals(k)) {
                            g = StatisGrader.TYPE.LONG;
                        } else
                        if ( "float".equals(k)) {
                            g = StatisGrader.TYPE.FLOAT;
                        } else
                        if ("double".equals(k)
                        ||  "number".equals(k)) {
                            g = StatisGrader.TYPE.DOUBLE;
                        } else
                        {
                            g = StatisGrader.TYPE.STRING;
                        }
                        break ;
                    case  "date" :
                        g = StatisGrader.TYPE.LONGS;
                        break ;
                    default:
                        g = StatisGrader.TYPE.STRINGS;
                } else {
                        g = StatisGrader.TYPE.STRINGS;
                }
            }

            fields[i++] = new StatisGrader.Field(g, f, n);
        }

        return fields ;
    }

    private Fields getGatherFields(Set<String> names) {
        List<StatisGather.Diman> dimans = new ArrayList();
        List<StatisGather.Index> indics = new ArrayList();
        Map <String, Map   > items = that.getFields();
        Map <String, String> types ;

        try {
            types = FormSet.getInstance().getEnum("__types__");
        } catch (HongsException e) {
            throw e.toExemption( );
        }

        for(String n : names) {
            String f = n;
            String m = null ;
            StatisGather.TYPE g ;

            // 拆出统计方法
            int j = n.indexOf  ('|');
            if (j > -1) {
                m = n.substring(1+j);
                f = n.substring(0,j);
            }

            Map c = items.get(f);
            if (c == null) {
                continue ;
            }

            String t = (String) c.get ("__type__");
            Object k = (Object) c.get (  "type"  );

            // 使用基准类型
            if (types.containsKey(t)) {
                   t = types.get (t);
            }

            if (Synt.declare(c.get("__repeated__"), false) == false) {
                f = "#" + f;
                if (null != t) switch(t) {
                    case "number":
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = StatisGather.TYPE.INT;
                        } else
                        if (  "long".equals(k)) {
                            g = StatisGather.TYPE.LONG;
                        } else
                        if ( "float".equals(k)) {
                            g = StatisGather.TYPE.FLOAT;
                        } else
                        {
                            g = StatisGather.TYPE.DOUBLE;
                        }
                        break ;
                    case "hidden":
                    case  "enum" :
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = StatisGather.TYPE.INT;
                        } else
                        if (  "long".equals(k)) {
                            g = StatisGather.TYPE.LONG;
                        } else
                        if ( "float".equals(k)) {
                            g = StatisGather.TYPE.FLOAT;
                        } else
                        if ("double".equals(k)
                        ||  "number".equals(k)) {
                            g = StatisGather.TYPE.DOUBLE;
                        } else
                        {
                            g = StatisGather.TYPE.STRING;
                        }
                        break ;
                    case  "date" :
                        g = StatisGather.TYPE.LONG;
                        break ;
                    default:
                        g = StatisGather.TYPE.STRING;
                } else {
                        g = StatisGather.TYPE.STRING;
                }
            } else {
                f = "%" + f;
                if (null != t) switch(t) {
                    case "number":
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = StatisGather.TYPE.INTS;
                        } else
                        if (  "long".equals(k)) {
                            g = StatisGather.TYPE.LONGS;
                        } else
                        if ( "float".equals(k)) {
                            g = StatisGather.TYPE.FLOATS;
                        } else
                        {
                            g = StatisGather.TYPE.DOUBLES;
                        }
                        break ;
                    case "hidden":
                    case  "enum" :
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = StatisGather.TYPE.INT;
                        } else
                        if (  "long".equals(k)) {
                            g = StatisGather.TYPE.LONG;
                        } else
                        if ( "float".equals(k)) {
                            g = StatisGather.TYPE.FLOAT;
                        } else
                        if ("double".equals(k)
                        ||  "number".equals(k)) {
                            g = StatisGather.TYPE.DOUBLE;
                        } else
                        {
                            g = StatisGather.TYPE.STRING;
                        }
                        break ;
                    case  "date" :
                        g = StatisGather.TYPE.LONGS;
                        break ;
                    default:
                        g = StatisGather.TYPE.STRINGS;
                } else {
                        g = StatisGather.TYPE.STRINGS;
                }
            }

            Object o = getGatherField ( g, f, n, m );
            if (o instanceof StatisGather.Index) {
                indics.add( (StatisGather.Index) o );
            } else {
                dimans.add( (StatisGather.Diman) o );
            }
        }

        return new Fields (
            dimans.toArray(new StatisGather.Diman[dimans.size()]),
            indics.toArray(new StatisGather.Index[indics.size()])
        );
    }

    protected Object getGatherField(StatisGather.TYPE type, String field, String alias, String mode) {
        if (null != mode) switch (mode) {
            case "max"  :
                return new StatisGather.Max  (type, field, alias);
            case "min"  :
                return new StatisGather.Min  (type, field, alias);
            case "sum"  :
                return new StatisGather.Sum  (type, field, alias);
            case "count":
                return new StatisGather.Count(type, field, alias);
            case "first":
                return new StatisGather.First(type, field, alias);
        } else {
                return new StatisGather.Datum(type, field, alias);
        }
        throw new UnsupportedOperationException("Unsupported method " + mode + " for gather field");
    }

    // jdk 1.7 加上这个后排序不会报错
    static {
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
    }

    private static class Fields {
        public final  StatisGather.Diman[] fields;
        public final  StatisGather.Index[] indics;
        public Fields(StatisGather.Diman[] fields,
                      StatisGather.Index[] indics) {
            this.fields = fields;
            this.indics = indics;
        }
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

}
