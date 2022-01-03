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
     * 分类计数
     *
     * <pre>
     * rd.rn 为统计结果长度
     * rd.rb 为要统计的字段
     * rd.ob 为要排序的字段
     * rd.ab 含 linked 时 rd.fn.in 对应字段 fn 分块统计
     * rd.fn.rn 可单独指定字段统计长度
     * rd.fn.ar 可指定要统计的取值集合
     * rd.fn.nr 可指定要忽略的取值集合(只是不作统计, 并非查询约束)
     * </pre>
     *
     * @param rd
     * @return
     * @throws HongsException
     */
    public Map acount(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();

        Set<String> ab = Synt.toTerms(rd.get(Cnst.AB_KEY));
        Set<String> rb = Synt.toTerms(rd.get(Cnst.RB_KEY));
        Set<String> ob = Synt.toTerms(rd.get(Cnst.OB_KEY));
        boolean ln  =  ab != null && ab.contains("linked");
        int     rl  =  rb != null  ? rb.size() : 0 ;

        Map<String, Map<Object, Long>> counts  = new HashMap(rl);
        Map<String, Set<Object      >> countx  = new HashMap(rl); // 排除

        Map<String, Map<Object, Long>> counts2 = new HashMap(rl);
        Map<String, Set<Object      >> countx2 = new HashMap(rl);

        Map<String, Map<Object, Long>> counts3 = new HashMap(rl);
        Map<String, Set<Object      >> countx3 = new HashMap(rl);

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

        if (rb != null && !rb.isEmpty())
        for(String k : rb) {
            Function f = getGraderFormat(k);
            if (null == f) {
                throw new HongsException(400, "Field "+f+" is not exists");
            }

            // 默认统计
            counts.put(k, new HashMap());

            Map     vd = null;
            Set     vs = null;
            Object  vo = rd.get(k);
            if (vo instanceof Map) {
                Map vm = (Map) vo ;

                try {

                // 特选
                vs = Synt.asSet(vm.get(Cnst.AR_KEY));
                if (vs != null && !vs.isEmpty()) {
                    Map vz = new HashMap(vs.size());
                    for(Object v : vs) {
                        Object s = f.apply(v);
                        vz.put(s , 0L);
                    }
                    counts.put(k , vz);
                }

                // 排除
                vs = Synt.asSet(vm.get(Cnst.NR_KEY));
                if (vs != null && !vs.isEmpty()) {
                    Set vz = new HashSet(vs.size());
                    for(Object v : vs) {
                        Object s = f.apply(v);
                        vz.add(s /**/);
                    }
                    countx.put(k , vz);
                }

                } catch ( ClassCastException ex) {
                    throw new HongsException(400, ex); // 数据转换失败
                }

                // 分块条件
                if (ln != true) vs = null ; else {
                vs = Synt.asSet(vm.get(Cnst.IN_REL));
                if (vs != null && !vs.isEmpty()) {
                    vd  = new HashMap (rd);
                    vm  = new HashMap (vm);
                    vm.remove(Cnst.IN_REL);
                    vd.put(k , vm);
                }}
            }

            if ( vs == null || vs.isEmpty() ) {
                Map<Object, Long> vz = counts.get(k);
                if (vz != null) counts2.put (k , vz);
                Set<Object      > vx = countx.get(k);
                if (vx != null) countx2.put (k , vx);
            } else {
                counts3.clear();
                countx3.clear();
                Map<Object, Long> vz = counts.get(k);
                if (vz != null) counts3.put (k , vz);
                Set<Object      > vx = countx.get(k);
                if (vx != null) countx3.put (k , vx);
                acount(vd, finder, counts3, countx3);
            }
        }

        int n = acount(rd, finder, counts2, countx2);

        Map cnts = new HashMap();
        cnts.put("__count__", n);

        //** 排序并截取统计数据 **/

        int rn  = Synt.declare(rd.get(Cnst.RN_KEY), 0); // Top N

        int od  = 0;
        if (ob != null) {
        if (ob.contains("!")
        ||  ob.contains("-")) { // 默认逆序
            od  = 2;
        } else
        if (ob.contains("*")) { // 默认正序
            od  = 1;
        }}

        for(Map.Entry<String, Map<Object, Long>> et : counts.entrySet()) {
                 String        k = et.getKey  ();
            Map <Object, Long> m = et.getValue();
            List<Object  []  > a = new ArrayList(m.size());

            for(Map.Entry<Object, Long> e : et.getValue().entrySet()) {
                Object v = e.getKey  ();
                long   c = e.getValue();
                if (c != 0) {
                    a.add( new Object[] {v, null, c} );
                }
            }

            // 排序
            if (ob != null && ! ob.isEmpty()) {
                if (ob.contains(k+"!")
                ||  ob.contains("-"+k)) {
                    Collections.sort(a, CountD);
                } else
                if (ob.contains(  k  )) {
                    Collections.sort(a, CountA);
                } else
                if (od == 2) {
                    Collections.sort(a, CountD);
                } else
                if (od == 1) {
                    Collections.sort(a, CountA);
                }
            }

            // 截选 Top N
            n = Dict.getValue(rd, rn, k, Cnst.RN_KEY);
            if (n > 0 && n < a.size()) {
                a = a.subList( 0, n );
            }

            cnts.put( k, a );
        }

        return cnts;
    }

    private int acount( Map rd, IndexSearcher finder,
            Map<String, Map<Object , Long > > counts,
            Map<String, Set<Object        > > countx) throws HongsException {
        Field[] fields = getGraderFields(counts.keySet(), rd);

        try {
            Query q = that.padQry(rd);

            if (4 == (4 & Core.DEBUG)) {
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
     *
     * <pre>
     * rd.rn 为统计结果长度
     * rd.rb 为要统计的字段
     * rd.ob 为要排序的字段
     * rd.ab 含 linked 时 rd.fn.rg 对应字段 fn 分块统计
     * rd.fn.rn 可单独指定字段统计长度
     * rd.fn.ar 可指定要统计的区间集合
     * rd.fn.nr 可指定要忽略的区间集合(只是不作统计, 并非查询约束)
     * </pre>
     *
     * @param rd
     * @return
     * @throws HongsException
     */
    public Map amount(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();

        Set<String> ab = Synt.toTerms(rd.get(Cnst.AB_KEY));
        Set<String> rb = Synt.toTerms(rd.get(Cnst.RB_KEY));
        Set<String> ob = Synt.toTerms(rd.get(Cnst.OB_KEY));
        boolean ln  =  ab != null && ab.contains("linked");
        int     rl  =  rb != null  ? rb.size() : 0 ;

        Map<String, Map<Range, Ratio>> counts  = new HashMap(rl);
        Map<String, Set<Range       >> countx  = new HashMap(rl); // 排除

        Map<String, Map<Range, Ratio>> counts2 = new HashMap(rl);
        Map<String, Set<Range       >> countx2 = new HashMap(rl);

        Map<String, Map<Range, Ratio>> counts3 = new HashMap(rl);
        Map<String, Set<Range       >> countx3 = new HashMap(rl);

        /**
         * 根据请求数据进行综合判断,
         * 如果字段已经作为过滤条件,
         * 则此字段的统计需单独进行,
         * 且需抛开此字段的过滤数据.
         *
         * 例如某数据有一个时间字段且每条记录只能有一个时间,
         * 如果没有以下处理则选某段后其他未选区间数量将为零.
         *
         * 与 LinkedIn 左侧筛选类似.
         */

        if (rb != null && !rb.isEmpty())
        for(String k : rb) {
            Function f = getGraderFormat(k);
            if (null == f || f == asString) {
                throw new HongsException(400, "Field "+f+" is not exists or is not number");
            }

            Map     vd = null;
            Set     vs = null;
            Object  vo = rd.get(k);
            if (vo instanceof Map) {
                Map vm = (Map) vo ;

                try {

                // 特选
                vs = Synt.asSet(vm.get(Cnst.AR_KEY));
                if (vs != null && !vs.isEmpty()) {
                    Map vz = new HashMap(vs.size());
                    for(Object v : vs) {
                        Range  s = new Range(v);
                        Ratio  r = new Ratio( );
                        vz.put(s , r);
                    }
                    counts.put(k, vz);
                }

                // 排除
                vs = Synt.asSet(vm.get(Cnst.NR_KEY));
                if (vs != null && !vs.isEmpty()) {
                    Set vz = new HashSet(vs.size());
                    for(Object v : vs) {
                        Range  s = new Range(v);
                    //  Ratio  r = new Ratio( );
                        vz.add(s);
                    }
                    countx.put(k, vz);
                }

                } catch ( ClassCastException ex) {
                    throw new HongsException(400, ex); // 区间格式不对
                }

                // 分块条件
                if (ln != true) vs = null ; else {
                vs = Synt.asSet(vm.get(Cnst.RG_REL));
                if (vs != null && !vs.isEmpty()) {
                    vd  = new HashMap (rd);
                    vm  = new HashMap (vm);
                    vm.remove(Cnst.RG_REL);
                    vd.put(k , vm);
                }}
            }

            if ( vs == null || vs.isEmpty() ) {
                Map<Range, Ratio> vz = counts.get(k);
                if (vz != null) counts2.put (k , vz);
                Set<Range       > vx = countx.get(k);
                if (vz != null) countx2.put (k , vx);
            } else {
                counts3.clear();
                countx3.clear();
                Map<Range, Ratio> vz = counts.get(k);
                if (vz != null) counts3.put (k , vz);
                Set<Range       > vx = countx.get(k);
                if (vz != null) countx3.put (k , vx);
                amount(vd, finder, counts3, countx3);
            }
        }

        int n = amount(rd, finder, counts2, countx2);

        Map cnts = new HashMap();
        cnts.put("__count__", n);

        //** 排序并截取统计数据 **/

        int rn  = Synt.declare(rd.get(Cnst.RN_KEY), 0); // Top N

        int od  = 0;
        if (ob != null) {
        if (ob.contains("!")
        ||  ob.contains("-")) { // 默认逆序
            od  = 2;
        } else
        if (ob.contains("*")) { // 默认正序
            od  = 1;
        }}

        for(Map.Entry<String, Map<Range, Ratio>> et : counts.entrySet()) {
                 String        k = et.getKey  ();
            Map <Range, Ratio> m = et.getValue();
            List<Object  []  > a = new ArrayList(m.size());

            for(Map.Entry<Range, Ratio> e : et.getValue().entrySet()) {
                Range  v = e.getKey  ();
                Ratio  c = e.getValue();
                if (c.cnt != 0) {
                    a.add( new Object[] {
                        v, null,
                        c.cnt, c.sum,
                        c.min, c.max
                    });
                }
            }

            // 排序
            if (ob != null && ! ob.isEmpty()) {
                if (ob.contains(k+"!")
                ||  ob.contains("-"+k)) {
                    Collections.sort(a, MountD);
                } else
                if (ob.contains(  k  )) {
                    Collections.sort(a, MountA);
                } else
                if (od == 2) {
                    Collections.sort(a, MountD);
                } else
                if (od == 1) {
                    Collections.sort(a, MountA);
                }
            }

            // 截选 Top N
            n = Dict.getValue(rd, rn, k, Cnst.RN_KEY);
            if (n > 0 && n < a.size()) {
                a = a.subList( 0, n );
            }

            cnts.put( k, a );
        }

        return cnts;
    }

    private int amount( Map rd, IndexSearcher finder,
            Map<String, Map<Range , Ratio > > counts,
            Map<String, Set<Range         > > countx) throws HongsException {
        Field[] fields = getGraderFields(counts.keySet(), rd);

        try {
            Query q = that.padQry(rd);

            if (4 == (4 & Core.DEBUG)) {
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
     *
     * <pre>
     * rd.ob 为要排序的字段
     * rd.rb 中根据字段类型区分维度指标, 维度将作为聚合条件, 指标将用于数值统计
     * 字段格式为: filed!func, 未加后缀的字段将作为维度
     * 指标后缀有:
     *  !ratio  计数,求和,最小,最大
     *  !count  计数
     *  !sum    求和
     *  !min    最小
     *  !max    最大
     *  !crowd  去重计数
     *  !first  取首个值
     *  !flock  取全部值
     * 特殊维度有:
     *  !range,!scope 区间, 通过 rd.fn.ar 指定 fn 的区间集合
     * </pre>
     *
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

            if (4 == (4 & Core.DEBUG)) {
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
     *
     * <pre>
     * rd.ob 为要排序的字段
     * rd.rb 中根据字段类型区分维度指标, 维度将作为聚合条件, 指标将用于数值统计
     * 字段格式为: filed!func, 未加后缀的字段将作为维度
     * 指标后缀有:
     *  !ratio  计数,求和,最小,最大
     *  !count  计数
     *  !sum    求和
     *  !min    最小
     *  !max    最大
     *  !crowd  去重计数
     *  !first  取首个值
     *  !flock  取全部值
     * 特殊维度有:
     *  !scope,!range 区间, 通过 rd.fn.ar 指定 fn 的区间集合
     * </pre>
     *
     * @param rd
     * @param rn 条数
     * @param pn 页码
     * @return
     * @throws HongsException
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

            if (4 == (4 & Core.DEBUG)) {
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
            int j = n.indexOf  ('!');
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
            Set<String> rz = Dict.getValue (rd, Set.class, alias, Cnst.AR_KEY);
            if (null == rz ) rz = new LinkedHashSet();
            String [  ] rs = rz . toArray(new String[rz.size()] );
            return new StatisGather.Scope(type, field, alias, rs);
        }
        case "first":
            return new StatisGather.First(type, field, alias);
        case "flock":
            return new StatisGather.Flock(type, field, alias);
        case "crowd":
            return new StatisGather.Crowd(type, field, alias);
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
     * 按数量从少到多排列
     */
    private static final Comparator<Object[]> CountA = new Comparator<Object[]>() {
        @Override
        public int compare(Object[] o1, Object[] o2) {
            long   cnt1 = (long)   o1[2];
            long   cnt2 = (long)   o2[2];
            if (cnt1 != cnt2) {
                return  cnt1<cnt2 ? -1:1;
            }

            return 0;
        }
    };

    /**
     * 计数排序
     * 按数量从多到少排列
     */
    private static final Comparator<Object[]> CountD = new Comparator<Object[]>() {
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
     * 先按数量从少到多排
     * 再按求和从小到大排
     */
    private static final Comparator<Object[]> MountA = new Comparator<Object[]>() {
        @Override
        public int compare(Object[] o1, Object[] o2) {
            long   cnt1 = (long)   o1[2];
            long   cnt2 = (long)   o2[2];
            if (cnt1 != cnt2) {
                return  cnt1<cnt2 ? -1:1;
            }

            double sum1 = (double) o1[3];
            double sum2 = (double) o2[3];
            if (sum1 != sum2) {
                return  sum1<sum2 ? -1:1;
            }

            return 0;
        }
    };

    /**
     * 计算排序
     * 先按数量从多到少排
     * 再按求和从大到小排
     */
    private static final Comparator<Object[]> MountD = new Comparator<Object[]>() {
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
                if (fn.  endsWith("!")) { // 新版逆序后缀
                    fields[i] = fn.substring(0, fn.length() - 1);
                    desces[i] = true ;
                } else
                if (fn.startsWith("-")) { // 旧版逆序前缀
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
