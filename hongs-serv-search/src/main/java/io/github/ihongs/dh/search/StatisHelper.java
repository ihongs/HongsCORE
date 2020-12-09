package io.github.ihongs.dh.search;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.lucene.LuceneRecord;
import io.github.ihongs.dh.search.StatisHandle.TYPE;
import io.github.ihongs.dh.search.StatisHandle.Field;
import io.github.ihongs.dh.search.StatisGather.Diman;
import io.github.ihongs.dh.search.StatisGather.Index;
import io.github.ihongs.dh.search.StatisGrader.Range;
import io.github.ihongs.dh.search.StatisGrader.Ratio;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

/**
 * 统计助手
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

        Map         incs = Synt.asMap  (rd.get(Cnst.IN_REL));
        Map         excs = Synt.asMap  (rd.get(Cnst.NI_REL));
        Set<String> cntz = Synt.toTerms(rd.get(Cnst.RB_KEY));
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

        int t = Synt.declare(rd.get(Cnst.RN_KEY), 0); // Top N

        for(Map.Entry<String, Map<String, Integer>> et : counts.entrySet()) {
            List<Object[]> a = new ArrayList ( et.getValue().size(  )  );
            for(Map.Entry<String, Integer> e : et.getValue().entrySet()) {
                String m = e.getKey  ();
                int    c = e.getValue();
                if (c != 0) {
                    a.add( new Object[] {m, null, c} );
                }
            }
            Collections.sort(a, Counts);

            int n = Synt.declare(rd.get(Cnst.RN_KEY +"-"+ et.getKey()), t);
            if (0 < n && n < a.size()) {
                a = a.subList (0, n );
            }

            cnts.put(et.getKey(), a );
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
     * 分类计数
     * @param rd
     * @return
     * @throws HongsException
     */
    public Map acount(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();

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

            try{
            for(String x : cntz) {
                Map cnt; Set inc, exc, cnx;

                // 将参数转换成字段对应类型
                Function f = getGraderFormat(x);
                if (null == f) {
                    continue ;
                }

                    cnt  = new  HashMap( );
                    inc  = Synt.asSet(incs.get(x));
                if (inc != null && !inc.isEmpty()) {
                    for(Object v:inc) {
                        Object s = f.apply(v);
                       cnt.put(s, 0 );
                    }
                }   counts.put(x,cnt);

                    cnx  = new  HashSet( );
                    exc  = Synt.asSet(excs.get(x));
                if (exc != null && !exc.isEmpty()) {
                    for(Object v:inc) {
                        Object s = f.apply(v);
                       cnx.add(s/**/);
                    }
                }   countx.put(x,cnx);
            }
            } catch ( ClassCastException ex ) {
                throw new HongsException(400, ex ); // 值与字段不符
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

        int t = Synt.declare(rd.get(Cnst.RN_KEY), 0); // Top N

        for(Map.Entry<String, Map<Object, Long>> et : counts.entrySet()) {
            List<Object[]> a = new ArrayList(et.getValue().size(  )  );
            for(Map.Entry<Object , Long> e : et.getValue().entrySet()) {
                Object m = e.getKey  ();
                long   c = e.getValue();
                if (0 != c) {
                    a.add( new Object[] {m, null, c} );
                }
            }
            Collections.sort(a, Counts);

            int n = Synt.declare(rd.get(Cnst.RN_KEY +"-"+ et.getKey()), t);
            if (n > 0 && n < a.size()) {
                a = a.subList (0, n );
            }

            cnts.put(et.getKey(), a );
        }

        return cnts;
    }

    private int acount( Map rd, IndexSearcher finder,
            Map<String, Map<Object , Long > > counts,
            Map<String, Set<Object        > > countx) throws HongsException {
        Field[] fields = getGraderFields(counts.keySet(), rd);

        try {
            Query q = that.padQry(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
                CoreLogger.debug("StatisHelper.acount: " + q.toString());
            }

            if (counts.isEmpty()) {
                return finder.count (q);
            }

            StatisGrader.Acount c = new StatisGrader.Acount(fields, counts, countx);

            finder.search(q, c);
            return c . count( );
        } catch (IOException e) {
            throw new HongsException(e);
        }
    }

    /**
     * 分类计算
     * @param rd
     * @return
     * @throws HongsException
     */
    public Map amount(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();

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

            try{
            for(String x : cntz) {
                Map cnt; Set inc, exc, cnx;

                    inc  = Synt.asSet(incs.get(x));
                if (inc != null && !inc.isEmpty()) {
                    cnt  = new  HashMap();
                    for(Object v:inc) {
                        Range  m  = new Range (v);
                        Ratio  c  = new Ratio ( );
                       cnt.put(m, c );
                    }
                    counts.put(x,cnt);
                }

                    exc  = Synt.asSet(excs.get(x));
                if (exc != null && !exc.isEmpty()) {
                    cnx  = new  HashSet( );
                    for(Object v:inc) {
                        Range  m  = new Range (v);
                    //  Ratio  c  = new Ratio ( );
                       cnx.add(m/**/);
                    }
                    countx.put(x,exc);
                }
            }
            } catch ( ClassCastException ex ) {
                throw new HongsException(400, ex ); // 区间格式不对
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

        int t = Synt.declare(rd.get(Cnst.RN_KEY), 0); // Top N

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
            Collections.sort(a, Mounts);

            int n = Synt.declare(rd.get(Cnst.RN_KEY +"-"+ et.getKey()), t);
            if (n > 0 && n < a.size()) {
                a = a.subList (0, n );
            }

            cnts.put(et.getKey(), a );
        }

        return cnts;
    }

    private int amount( Map rd, IndexSearcher finder,
            Map<String, Map<Range , Ratio > > counts,
            Map<String, Set<Range         > > countx) throws HongsException {
        Field[] fields = getGraderFields(counts.keySet(), rd);

        try {
            Query q = that.padQry(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
                CoreLogger.debug("StatisHelper.amount: " + q.toString());
            }

            if (counts.isEmpty()) {
                return finder.count (q);
            }

            StatisGrader.Amount c = new StatisGrader.Amount(fields, counts, countx);

            finder.search(q, c);
            return c . count( );
        } catch (IOException e) {
            throw new HongsException(e);
        }
    }

    /**
     * 聚合统计
     * @param rd
     * @return
     * @throws HongsException
     */
    public List<Map> assort (Map rd) throws HongsException {
        Set<String> rb = Synt.toTerms(rd.get(Cnst.RB_KEY));
            Fields  fs = this.getGatherFields ( rb , rd ) ;
        if (null == fs ) {
            throw new NullPointerException("Assort fields required");
        }

        // 查询
        List<Map> list;
        try {
            Query q = that.padQry(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
                CoreLogger.debug("StatisHelper.assort: " + q.toString());
            }

            IndexSearcher finder = that.getFinder();
            StatisGather.Fetch c = new StatisGather.Fetch(fs.dimans, fs.indics);

            finder.search( q, c );
            list = c.getResult( );
        } catch ( IOException e ) {
            throw new HongsException(e);
        }

        Set<String> ob = Synt.toTerms(rd.get(Cnst.OB_KEY));
        if (ob == null || ob.isEmpty()) {
            return  list ;
        }

        // 排序
        String[] sb = ob.toArray(new String[ob.size()]);
        Orders sort = new Orders( sb );
        Collections . sort(list, sort);

        return list ;
    }

    /**
     * 聚合统计(分页)
     * @param rd
     * @param rn 条数
     * @param pn 页码
     * @return
     */
    public Map assort (Map rd, int rn, int pn) throws HongsException {
        List list = assort ( rd );

        Map  page = new HashMap();
        Map  data = new HashMap();

        data.put( "list" , list );
        data.put( "page" , page );
        page.put(Cnst.RN_KEY, rn);
        page.put(Cnst.PN_KEY, pn);

        int rc = list.size();
        if (rc == 0) {
            page.put("count", 0 );
            page.put("pages", 0 );
            page.put("state", 0 );
        } else
        if (rn <= 0) { // rn 为 0 不要分页只要列表
            data.remove( "page" );
        } else
        if (pn <= 0) { // pn 为 0 不要列表只要分页
            data.remove( "list" );

            int p = (int) Math.ceil(((double) rc) / ((double) rn));
            page.put("count", rc);
            page.put("pages", p );
            page.put("state", 1 );
        } else
        {
            int p = (int) Math.ceil(((double) rc) / ((double) rn));
            page.put("count", rc);
            page.put("pages", p );

            int b = rn * (pn - 1);
            int d = rn +  b ;
            if (d > rc) { // 数量不够, 取到最后
                d = rc;
            }
            if (b > rc) { // 页码超出, 返回错误
                page.put("state", 0 );
            } else {
                page.put("state", 1 );

                // 截取列表
                list = list.subList(b, d);
                data . put ("list", list);
            }
        }

        return  data ;
    }

    /**
     * 统计查询
     * @param rd
     * @param fx
     */
    public void search(Map rd, final Consumer<Field[]> fx) throws HongsException {
        Set<String> rb = Synt.toTerms(rd.get(Cnst.RB_KEY));
        if (rb == null || rb.isEmpty()) {
            throw new NullPointerException("Search fields required.");
        }
        Field[] fs = getGraderFields(rb, rd);
        if (fs == null || fs.length==0) {
            throw new NullPointerException("Search fields required!");
        }

        try {
            Query q = that.padQry(rd);

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
                CoreLogger.debug("StatisHelper.search: " + q.toString());
            }

            IndexSearcher finder = that.getFinder();
            StatisHandle.Fetch c = new StatisHandle.Fetch(fx, fs);

            finder.search( q, c );
        } catch ( IOException e ) {
            throw new HongsException(e);
        }
    }

    private Function getGraderFormat(String x) throws HongsException {
        Map<String, String> types = FormSet.getInstance().getEnum("__types__");
        Map<String, Map   > items = that.getFields();
        Map  c  =  (Map)    items.get(x);
        if ( c  == null)  {
            return null;
        }

        String t = (String) c.get ("__type__");
        Object k = (Object) c.get (  "type"  );

        // 使用基准类型
        if (types.containsKey(t)) {
               t = types.get (t);
        }

        if (null != t) switch(t) {
            case "number":
                if (   "int".equals(k)
                ||    "byte".equals(k)
                ||   "short".equals(k)) {
                    return asInt;
                } else
                if (  "long".equals(k)) {
                    return asLong;
                } else
                if ( "float".equals(k)) {
                    return asFloat;
                } else
                {
                    return asDouble;
                }
            case "hidden":
            case  "enum" :
                if (   "int".equals(k)
                ||    "byte".equals(k)
                ||   "short".equals(k)) {
                    return asInt;
                } else
                if (  "long".equals(k)) {
                    return asLong;
                } else
                if ( "float".equals(k)) {
                    return asFloat;
                } else
                if ("double".equals(k)
                ||  "number".equals(k)) {
                    return asDouble;
                } else
                {
                    return asString;
                }
            case  "date" :
                return asLong;
            default:
                return asString;
        } else {
                return asString;
        }
    }

    private Field[] getGraderFields(Set<String> names, Map rd) {
        if (names == null) {
            return   null;
        }
        
        List<Field> fields = new ArrayList(names.size());
        Map <String, Map   > items = that.getFields (  );
        Map <String, String> types ;

        try {
            types = FormSet.getInstance().getEnum("__types__");
        } catch (HongsException e) {
            throw e.toExemption( );
        }

        for(String n : names) {
            TYPE   g ;
            String f = n ;

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
                            g = TYPE.INT;
                        } else
                        if (  "long".equals(k)) {
                            g = TYPE.LONG;
                        } else
                        if ( "float".equals(k)) {
                            g = TYPE.FLOAT;
                        } else
                        {
                            g = TYPE.DOUBLE;
                        }
                        break ;
                    case "hidden":
                    case  "enum" :
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = TYPE.INT;
                        } else
                        if (  "long".equals(k)) {
                            g = TYPE.LONG;
                        } else
                        if ( "float".equals(k)) {
                            g = TYPE.FLOAT;
                        } else
                        if ("double".equals(k)
                        ||  "number".equals(k)) {
                            g = TYPE.DOUBLE;
                        } else
                        {
                            g = TYPE.STRING;
                        }
                        break ;
                    case  "date" :
                        g = TYPE.LONG;
                        break ;
                    default:
                        g = TYPE.STRING;
                } else {
                        g = TYPE.STRING;
                }
            } else {
                f = "%" + f;
                if (null != t) switch(t) {
                    case "number":
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = TYPE.INTS;
                        } else
                        if (  "long".equals(k)) {
                            g = TYPE.LONGS;
                        } else
                        if ( "float".equals(k)) {
                            g = TYPE.FLOATS;
                        } else
                        {
                            g = TYPE.DOUBLES;
                        }
                        break ;
                    case "hidden":
                    case  "enum" :
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = TYPE.INTS;
                        } else
                        if (  "long".equals(k)) {
                            g = TYPE.LONGS;
                        } else
                        if ( "float".equals(k)) {
                            g = TYPE.FLOATS;
                        } else
                        if ("double".equals(k)
                        ||  "number".equals(k)) {
                            g = TYPE.DOUBLES;
                        } else
                        {
                            g = TYPE.STRINGS;
                        }
                        break ;
                    case  "date" :
                        g = TYPE.LONGS;
                        break ;
                    default:
                        g = TYPE.STRINGS;
                } else {
                        g = TYPE.STRINGS;
                }
            }

            fields.add(new Field(g, f, n));
        }

        return fields.toArray(new Field[fields.size()]);
    }

    private Fields getGatherFields(Set<String> names, Map rd ) {
        if (names == null) {
            return   null;
        }

        List<Diman> dimans = new ArrayList();
        List<Index> indics = new ArrayList();
        Map <String, Map   > items = that.getFields();
        Map <String, String> types ;

        try {
            types = FormSet.getInstance().getEnum("__types__");
        } catch (HongsException e) {
            throw e.toExemption( );
        }

        for(String n : names) {
            TYPE   g ;
            String f = n ;
            String m = null ;

            // 拆出统计方法
            int j = n.indexOf  ('|');
            if (j > -1) {
                m = n.substring(1+j);
                f = n.substring(0,j);

                // 统计行数
                if (m.equals("count")
                &&  f.equals( "*" ) ) {
                    f = Cnst.ID_KEY ;
                }
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
                            g = TYPE.INT;
                        } else
                        if (  "long".equals(k)) {
                            g = TYPE.LONG;
                        } else
                        if ( "float".equals(k)) {
                            g = TYPE.FLOAT;
                        } else
                        {
                            g = TYPE.DOUBLE;
                        }
                        break ;
                    case "hidden":
                    case  "enum" :
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = TYPE.INT;
                        } else
                        if (  "long".equals(k)) {
                            g = TYPE.LONG;
                        } else
                        if ( "float".equals(k)) {
                            g = TYPE.FLOAT;
                        } else
                        if ("double".equals(k)
                        ||  "number".equals(k)) {
                            g = TYPE.DOUBLE;
                        } else
                        {
                            g = TYPE.STRING;
                        }
                        break ;
                    case  "date" :
                        g = TYPE.LONG;
                        break ;
                    default:
                        g = TYPE.STRING;
                } else {
                        g = TYPE.STRING;
                }
            } else {
                f = "%" + f;
                if (null != t) switch(t) {
                    case "number":
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = TYPE.INTS;
                        } else
                        if (  "long".equals(k)) {
                            g = TYPE.LONGS;
                        } else
                        if ( "float".equals(k)) {
                            g = TYPE.FLOATS;
                        } else
                        {
                            g = TYPE.DOUBLES;
                        }
                        break ;
                    case "hidden":
                    case  "enum" :
                        if (   "int".equals(k)
                        ||    "byte".equals(k)
                        ||   "short".equals(k)) {
                            g = TYPE.INTS;
                        } else
                        if (  "long".equals(k)) {
                            g = TYPE.LONGS;
                        } else
                        if ( "float".equals(k)) {
                            g = TYPE.FLOATS;
                        } else
                        if ("double".equals(k)
                        ||  "number".equals(k)) {
                            g = TYPE.DOUBLES;
                        } else
                        {
                            g = TYPE.STRINGS;
                        }
                        break ;
                    case  "date" :
                        g = TYPE.LONGS;
                        break ;
                    default:
                        g = TYPE.STRINGS;
                } else {
                        g = TYPE.STRINGS;
                }
            }

            Object o = getGatherField(g, f,n, m, rd);
            if (o instanceof Index) {
                indics.add( (Index) o );
            } else {
                dimans.add( (Diman) o );
            }
        }

        return new Fields (
            dimans.toArray(new Diman[dimans.size()]),
            indics.toArray(new Index[indics.size()])
        );
    }

    protected Object getGatherField(TYPE type, String field, String alias, String mode, Map rd) {
        // 维度
        if (null == mode) {
            return new StatisGather.Datum(type, field, alias);
        }

        // 指标
        switch (mode) {
        case "scope":
        case "range": {
            /**
             * 区间维度:
             * 从请求数据中
             * 提取区间列表
             */
            Set<String> rz = Dict.getValue (rd, Set.class, Cnst.IN_REL, alias);
            if (null == rz ) rz = new LinkedHashSet();
            String [  ] rs = rz . toArray(new String[rz.size()] );
            return new StatisGather.Scope(type, field, alias, rs);
        }
        case "first":
            return new StatisGather.First(type, field, alias);
        case "flock":
            return new StatisGather.Flock(type, field, alias);
        case "count":
            return new StatisGather.Count(type, field, alias);
        case "ratio":
            return new StatisGather.Ratio(type, field, alias);
        case "max"  :
            return new StatisGather.Max  (type, field, alias);
        case "min"  :
            return new StatisGather.Min  (type, field, alias);
        case "sum"  :
            return new StatisGather.Sum  (type, field, alias);
        }

        throw new UnsupportedOperationException ("Unsupported field " + alias);
    }

    private static final Function asInt    = (Function<Object, Number>) Synt::asInt;
    private static final Function asLong   = (Function<Object, Number>) Synt::asLong;
    private static final Function asFloat  = (Function<Object, Number>) Synt::asFloat;
    private static final Function asDouble = (Function<Object, Number>) Synt::asDouble;
    private static final Function asString = (Function<Object, String>) Synt::asString;

    private static class Fields {
        public final  Diman[] dimans;
        public final  Index[] indics;
        public Fields(Diman[] dimans,
                      Index[] indics) {
            this.dimans = dimans;
            this.indics = indics;
        }
    }

    /**
     * 计数排序
     * 按数量从多到少排列
     */
    private static final Comparator<Object[]> Counts = new Comparator<Object[]>() {
        @Override
        public int compare(Object[] o1, Object[] o2) {
            long   cnt1 = (long)   o1[2];
            long   cnt2 = (long)   o2[2];
            if (cnt1 != cnt2) {
                return  cnt1>cnt2 ? -1:1;
            }

            return 0;
        }
    };

    /**
     * 计算排序
     * 先按数量从多到少排
     * 再按求和从大到小排
     */
    private static final Comparator<Object[]> Mounts = new Comparator<Object[]>() {
        @Override
        public int compare(Object[] o1, Object[] o2) {
            long   cnt1 = (long)   o1[2];
            long   cnt2 = (long)   o2[2];
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
    };

    /**
     * 统计排序
     * 按给定字段依次对比
     * 负号前缀可表示逆序
     */
    public static class Orders implements Comparator<Map> {
        private final String [] fields;
        private final boolean[] desces;

        public Orders(String [] fields) {
            this.fields = fields;
            this.desces = new boolean[fields.length];

            // 整理出逆序表
            for(int i = 0; i < fields.length; i ++ ) {
                String fn = fields[i];
                if (fn.startsWith("-")) {
                    fields[i] = fn.substring(1);
                    desces[i] = true ;
                } else {
                    desces[i] = false;
                }
            }
        }

        @Override
        public int compare(Map m1, Map m2) {
            for(int i = 0; i < fields.length; i ++ ) {
                String fn = fields[i ];
                Object v1 = m1.get(fn);
                Object v2 = m2.get(fn);

                int x  = compares (v1, v2);
                if (x != 0) {
                    return desces [i] ? 0 - x : x;
                }
            }

            return  0;
        }

        private static int compares(Object v1, Object v2) {
            // 空值总是最小
            if (v1 == null) {
                if (v2 == null) {
                    return  0;
                } else {
                    return -1;
                }
            } else {
                if (v2 == null) {
                    return  1;
                }
            }

            // 能比较则比较
            if (v1 instanceof Comparable
            &&  v2 instanceof Comparable) {
                int x  = ((Comparable)v1).compareTo((Comparable)v2);
                if (x != 0) {
                    return  x;
                }
            }

            // 数组逐一比较
            if (v1 instanceof  Object[]
            &&  v2 instanceof  Object[] ) {
                Object[] a1 = (Object[] ) v1;
                Object[] a2 = (Object[] ) v2;
                    int l  = Integer.min(a1.length, a2.length);
                for(int i  = 0; i < l; i ++ ) {
                    int x  =  compares  (a1 [ i ] , a2 [ i ] );
                    if (x != 0) {
                        return  x;
                    }
                }
            }

            return 0;
        }
    }

    // jdk 1.7 加上这个后排序不会报错
    static {
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
    }

}
