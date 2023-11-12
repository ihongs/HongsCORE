package io.github.ihongs.dh.search;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.lucene.LuceneRecord;
import io.github.ihongs.dh.search.StatisHandle.TYPE;
import io.github.ihongs.dh.search.StatisHandle.Field;
import io.github.ihongs.dh.search.StatisHandle.Range;
import io.github.ihongs.dh.search.StatisGather.Dimen;
import io.github.ihongs.dh.search.StatisGather.Index;
import io.github.ihongs.dh.search.StatisGrader.Coach;
import io.github.ihongs.dh.search.StatisGrader.Count;
import io.github.ihongs.dh.search.StatisGrader.Tally;
import io.github.ihongs.dh.search.StatisGrader.Total;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
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
     * rd.fn.or 可指定要置顶的取值集合
     * rd.fn.ar 可指定要统计的取值集合
     * rd.fn.nr 可指定要忽略的取值集合(只是不作统计, 并非查询约束)
     * </pre>
     *
     * @param rd
     * @return
     * @throws CruxException
     */
    public Map acount(Map rd) throws CruxException {
        IndexSearcher finder = that.getFinder();

        Set<String> ab = Synt.toTerms(rd.get(Cnst.AB_KEY));
        Set<String> rb = Synt.toTerms(rd.get(Cnst.RB_KEY));
        Set<String> ob = Synt.toTerms(rd.get(Cnst.OB_KEY));
        boolean ld  =  ab != null && ab.contains("linked");
        boolean rt  =  ab != null && ab.contains("resort");
        boolean ro  =  ab != null && ab.contains("rezero");
        int     rl  =  rb != null  ? rb.size() : 0 ;

        Map<String, Map    > counts  = new HashMap(rl);
        Map<String, Set    > countz  = new HashMap(rl); // 特选
        Map<String, Set    > countx  = new HashMap(rl); // 排除
        Map<String, Integer> styles  = new HashMap(rl); // 模式

        Map<String, Map    > counts2 = new HashMap(rl);
        Map<String, Set    > countx2 = new HashMap(rl);

        Map<String, Map    > counts3 = new HashMap(rl);
        Map<String, Set    > countx3 = new HashMap(rl);

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
                throw new CruxException(400, "Field "+f+" is not exists");
            }

            // 默认统计
            counts.put(k, new HashMap());

            Map     vd = null;
            Set     vs = null;
            Object  vo = rd.get(k);
            if (vo instanceof Map) {
                Map vm = (Map) vo ;

                // 模式
                int cm =  -1 ;
                Function<Object, Object> vf;
                Function<Object, Coach > cf;
                String  CURR_IN_REL = Cnst.IN_REL;
                switch(Synt.declare(vm.get(Cnst.RB_KEY), "")) {
                    case "total":
                        CURR_IN_REL = Cnst.RG_REL;
                        vf = (v) -> new Range(v);
                        cf = (v) -> new Total((Range)v);
                        cm = StatisGrader.TOTAL ;
                        styles.put(k, cm);
                        break;
                    case "tally":
                        CURR_IN_REL = Cnst.RG_REL;
                        vf = (v) -> new Range(v);
                        cf = (v) -> new Tally((Range)v);
                        cm = StatisGrader.TALLY ;
                        styles.put(k, cm);
                        break;
                    case "range":
                        CURR_IN_REL = Cnst.RG_REL;
                        vf = (v) -> new Range(v);
                        cf = (v) -> new Count(v);
                        cm = StatisGrader.RANGE ;
                        styles.put(k, cm);
                        break;
                    case "asset":
                        vf = (v) -> f . apply(v);
                        cf = (v) -> new Count(v);
                        cm = StatisGrader.ASSET ;
                        styles.put(k, cm);
                        break;
                    case "count":
                        vf = (v) -> f . apply(v);
                        cf = (v) -> new Count(v);
                        cm = StatisGrader.COUNT ;
                        styles.put(k, cm);
                        break;
                    default:
                        vf = (v) -> f . apply(v);
                        cf = (v) -> new Count(v);
                }

                try {

                // 内定
                vs = Synt.asSet(vm.get(Cnst.AR_KEY));
                if (vs != null && !vs.isEmpty()) {
                    Map vz = new HashMap(vs.size());
                    for(Object v : vs) {
                        Object s = vf.apply(v);
                        Coach  c = cf.apply(s);
                        vz.put(s , c );
                    }
                    counts.put(k , vz);

                    // 未指定则设为仅限特定
                    if (cm == -1 /**/) {
                        styles.put(k, StatisGrader.ASSET);
                    }
                } else {
                    if (cm == -1 /**/) {
                        styles.put(k, StatisGrader.COUNT);
                    }
                }

                // 排除
                vs = Synt.asSet(vm.get(Cnst.NR_KEY));
                if (vs != null && !vs.isEmpty()) {
                    Set vx = new HashSet(vs.size());
                    for(Object v : vs) {
                        Object s = f . apply(v);
                        vx.add(s /**/);
                    }
                    countx.put(k , vx);
                }

                // 备选
                vs = Synt.asSet(vm.get(Cnst.OR_KEY));
                if (vs != null && !vs.isEmpty()) {
                    Map vz = new HashMap(vs.size());
                    Set vx = new HashSet(vs.size());
                    for(Object v : vs) {
                        Object s = vf.apply(v);
                        Coach  c = cf.apply(s);
                        vz.put(s , c );
                        vx.add(s /**/);
                        c .top(1 /**/); // 设为置顶, 排序优先
                    }
                    if (counts.containsKey(k)) {
                        counts.get(k).putAll(vz);
                    } else {
                        counts.put(k , vz);
                    }
                    if (countz.containsKey( k )) {
                        countz.get(k).addAll(vx);
                    } else {
                        countz.put(k , vx);
                    }
                }

                // 已选
                vs = Synt.asSet(vm.get(CURR_IN_REL));
                if (vs != null && !vs.isEmpty()) {
                    Map vz = new HashMap(vs.size());
                    Set vx = new HashSet(vs.size());
                    for(Object v : vs) {
                        Object s = vf.apply(v);
                        Coach  c = cf.apply(s);
                        vz.put(s , c );
                        vx.add(s /**/);
                        c .top(2 /**/); // 设为置顶, 排序优先
                    }
                    if (counts.containsKey(k)) {
                        counts.get(k).putAll(vz);
                    } else {
                        counts.put(k , vz);
                    }
                    if (countz.containsKey( k )) {
                        countz.get(k).addAll(vx);
                    } else {
                        countz.put(k , vx);
                    }
                }

                // 已选条件, 拆出备用
                if (ld && vs != null && !vs.isEmpty()) {
                    vd  = new HashMap (rd);
                    vm  = new HashMap (vm);
                    vm.remove(CURR_IN_REL);
                    vd.put(k , vm);
                }

                } catch (ClassCastException ex) {
                    throw new CruxException(ex, 400); // 数据转换失败
                }
            }

            /**
             * 仿 LinkedIn 筛选统计
             * 将已选的部分单独统计
             * 其他未选部分一起统计
             */
            if (vd == null) {
                Map vz  = counts.get(k);
                if (vz != null) counts2.put (k , vz);
                Set<Object> vx= countx .get (k);
                if (vx != null) countx2.put (k , vx);
            } else {
                counts3.clear();
                countx3.clear();
                Map vz  = counts.get(k);
                if (vz != null) counts3.put (k , vz);
                Set<Object> vx= countx .get (k);
                if (vx != null) countx3.put (k , vx);
                acount(vd, finder, counts3, countx3, styles);
            }
        }

        int n = acount(rd, finder, counts2, countx2, styles);

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

        for(Map.Entry<String, Map> et : counts.entrySet()) {
                String        k  = et.getKey  ();
            Map<Object,Coach> m  = et.getValue();
            Set<Object      > w  = countz.get(k);
            Collection<Coach> x  = m .values ( );
            Coach[]           a  = new Coach [x.size()];
                              a  = x .toArray(a);

            int al = a.length;
            int wl = w != null ? w.size() : 0;
            int nl = Dict.getValue(rd, rn, k, Cnst.RN_KEY);

            /**
             * 排序, 保障特选的排在前面
             * rezero则将特选为零往后排
             */
            Comparator<Coach> cp;
            if (ro) {
                cp = OrderH2;
                if (ob != null && ! ob.isEmpty()) {
                    if (ob.contains(k+"!")
                    ||  ob.contains("-"+k)) {
                        cp = OrderD2;
                    } else
                    if (ob.contains(  k  )) {
                        cp = OrderA2;
                    } else
                    if (od == 2) {
                        cp = OrderD2;
                    } else
                    if (od == 1) {
                        cp = OrderA2;
                    }
                }
            } else {
                cp = OrderH2;
                if (ob != null && ! ob.isEmpty()) {
                    if (ob.contains(k+"!")
                    ||  ob.contains("-"+k)) {
                        cp = OrderD1;
                    } else
                    if (ob.contains(  k  )) {
                        cp = OrderA1;
                    } else
                    if (od == 2) {
                        cp = OrderD1;
                    } else
                    if (od == 1) {
                        cp = OrderA1;
                    }
                }
            }
            Arrays.sort(a , cp);

            /**
             * 清理, 去掉末尾计数为零的
             * rezero则将特选为零也去掉
             */
            if (ro) {
                int i = al -1;
                for(; i >  -1; i -- ) {
                    Coach  c = a[ i ];
                    if (c.cnt() != 0) {
                        break;
                    }
                    if (c.top() != 0) {
                        wl --;
                    }
                }
                al = i + 1;
            } else {
                int i = al -1;
                for(; i >  -1; i -- ) {
                    Coach  c = a[ i ];
                    if (c.cnt() != 0) {
                        break;
                    }
                    if (c.top() != 0) {
                        break;
                    }
                }
                al = i + 1;
            }

            /**
             * 截取, 保障特选非零的不丢
             */
            if (nl > 0) {
                al = Math.min(al, Math.max(wl, nl));
            }

            /**
             * 重排, 不理置顶按计数重排
             */
            if (rt && al > wl && wl > 0) {
                if (cp == OrderD2 || cp == OrderD1) {
                    Arrays.sort (a, 0, al, OrderD0);
                } else
                if (cp == OrderA2 || cp == OrderA1) {
                    Arrays.sort (a, 0, al, OrderA0);
                }
            }

            cnts.put(k, new MyList(a, 0, al));
        }

        return cnts;
    }

    private int acount(
            Map rd, IndexSearcher finder,
            Map<String , Map    > counts,
            Map<String , Set    > countx,
            Map<String , Integer> styles
    ) throws CruxException {
        Field[] fields = getGraderFields(counts.keySet(), rd);

        try {
            Query q = that.padQry(rd);

            if (4 == (4 & Core.DEBUG)) {
                CoreLogger.debug("StatisHelper.acount: " + q.toString());
            }

            if (counts.isEmpty()) {
                return finder.count(q);
            }

            StatisGrader.Fetch c = new StatisGrader.Fetch(fields, counts, countx, styles);

            finder.search(q, c);
            return c . count( );
        } catch (IOException e) {
            throw new CruxException(e);
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
     *  !total  计数,求和,最小,最大
     *  !tally  同上,不求和
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
     * @throws CruxException
     */
    public List<Map> assort (Map rd) throws CruxException {
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
            throw new CruxException(e);
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
     *  !total  计数,求和,最小,最大
     *  !tally  同上,不求和
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
     * @throws CruxException
     */
    public Map assort (Map rd, int rn, int pn) throws CruxException {
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
            page.put("total", 0 );
            page.put("state", 0 );
        } else
        if (rn <= 0) { // rn 为 0 不要分页只要列表
            data.remove( "page" );
        } else
        if (pn <= 0) { // pn 为 0 不要列表只要分页
            data.remove( "list" );

            int p = (int) Math.ceil(((double) rc) / ((double) rn));
            page.put("count", rc);
            page.put("total", p );
            page.put("state", 1 );
        } else
        {
            int p = (int) Math.ceil(((double) rc) / ((double) rn));
            page.put("count", rc);
            page.put("total", p );

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
    public void search(Map rd, final Consumer<Field[]> fx) throws CruxException {
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
            throw new CruxException(e);
        }
    }

    private Function getGraderFormat(String x) throws CruxException {
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
        } catch ( CruxException e) {
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

        List<Dimen> dimens = new ArrayList();
        List<Index> indics = new ArrayList();
        Map <String, Map   > items = that.getFields();
        Map <String, String> types ;

        try {
            types = FormSet.getInstance().getEnum("__types__");
        } catch ( CruxException e) {
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
                dimens.add( (Dimen) o );
            }
        }

        return new Fields (
            dimens.toArray(new Dimen[dimens.size()]),
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
            String [  ] rs = rz . toArray(new String [rz.size()]);
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
        case "tally":
            return new StatisGather.Tally(type, field, alias);
        case "total":
            return new StatisGather.Total(type, field, alias);
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
        public final  Dimen[] dimans;
        public final  Index[] indics;
        public Fields(Dimen[] dimans,
                      Index[] indics) {
            this.dimans = dimans;
            this.indics = indics;
        }
    }

    /**
     * 数组片段
     * @param <T>
     */
    private static class MyList<T> implements List<T> {

        private final T[] a;
        private final int b;
        private final int l;

        public MyList(T[] a, int b, int l) {
            this.a = a;
            this.b = b;
            this.l = l;
        }

        @Override
        public T get(int i) {
            return a[i];
        }

        @Override
        public T set(int i, T e) {
            T v  = a[i];
            a[i] = e;
            return v;
        }

        @Override
        public int size() {
            return l -  b;
        }

        @Override
        public boolean isEmpty() {
            return l == b;
        }

        @Override
        public List<T> subList(int f, int t) {
            return new MyList ( a, f, t );
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T> () {
                int i = b;

                @Override
                public boolean hasNext() {
                    return i < l;
                }

                @Override
                public T next() {
                    return a [i ++];
                }

            };
        }

        @Override
        public Object[] toArray() {
            return a;
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return a;
        }

        //** 有限只读, 下列方法均不支持 */

        @Override
        public boolean add(T e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean addAll(int index, Collection<? extends T> c) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void add(int i, T e) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public T remove(int i) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int indexOf(Object o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public int lastIndexOf(Object o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ListIterator<T> listIterator() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ListIterator<T> listIterator(int j) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

    /**
     * 仅按计数正序重排
     */
    private static final Comparator<Coach> OrderA0 = new Comparator<Coach>() {
        @Override
        public int compare(Coach o1, Coach o2) {
            int cnt1  = o1.cnt();
            int cnt2  = o2.cnt();
            if (cnt1 != cnt2) {
                return cnt1 < cnt2 ? -1 : 1 ;
            }

            return 0;
        }
    };

    /**
     * 置顶条目总排前面
     * 按计数从小到大排
     */
    private static final Comparator<Coach> OrderA1 = new Comparator<Coach>() {
        @Override
        public int compare(Coach o1, Coach o2) {
            // 置顶总是逆序
            int top1  = o1.top();
            int top2  = o2.top();
            if (top1 != top2) {
                return top1 > top2 ? -1 : 1 ;
            }

            int cnt1  = o1.cnt();
            int cnt2  = o2.cnt();
            if (cnt1 != cnt2) {
                return cnt1 < cnt2 ? -1 : 1 ;
            }

            return 0;
        }
    };

    /**
     * 计数为零总排后面
     * 置顶条目总排前面
     * 按计数从小到大排
     */
    private static final Comparator<Coach> OrderA2 = new Comparator<Coach>() {
        @Override
        public int compare(Coach o1, Coach o2) {
            // 零总是排最后
            int cnt1  = o1.cnt();
            int cnt2  = o2.cnt();
            if (cnt1 != cnt2) {
                if (0== cnt2) return -1 ;
                if (0== cnt1) return  1 ;
            }

            // 置顶总是逆序
            int top1  = o1.top();
            int top2  = o2.top();
            if (top1 != top2) {
                return top1 > top2 ? -1 : 1 ;
            }

            if (cnt1 != cnt2) {
                return cnt1 < cnt2 ? -1 : 1 ;
            }

            return 0;
        }
    };

    /**
     * 仅按计数逆序重排
     */
    private static final Comparator<Coach> OrderD0 = new Comparator<Coach>() {
        @Override
        public int compare(Coach o1, Coach o2) {
            int cnt1  = o1.cnt();
            int cnt2  = o2.cnt();
            if (cnt1 != cnt2) {
                return cnt1 > cnt2 ? -1 : 1 ;
            }

            return 0;
        }
    };

    /**
     * 置顶条目总排前面
     * 按计数从大到小排
     */
    private static final Comparator<Coach> OrderD1 = new Comparator<Coach>() {
        @Override
        public int compare(Coach o1, Coach o2) {
            // 置顶总是逆序
            int top1  = o1.top();
            int top2  = o2.top();
            if (top1 != top2) {
                return top1 > top2 ? -1 : 1 ;
            }

            int cnt1  = o1.cnt();
            int cnt2  = o2.cnt();
            if (cnt1 != cnt2) {
                return cnt1 > cnt2 ? -1 : 1 ;
            }

            return 0;
        }
    };

    /**
     * 计数为零总排后面
     * 置顶条目总排前面
     * 按计数从大到小排
     */
    private static final Comparator<Coach> OrderD2 = new Comparator<Coach>() {
        @Override
        public int compare(Coach o1, Coach o2) {
            // 零总是排最后
            int cnt1  = o1.cnt();
            int cnt2  = o2.cnt();
            if (cnt1 != cnt2) {
                if (0== cnt2) return -1 ;
                if (0== cnt1) return  1 ;
            }

            // 置顶总是逆序
            int top1  = o1.top();
            int top2  = o2.top();
            if (top1 != top2) {
                return top1 > top2 ? -1 : 1 ;
            }

            if (cnt1 != cnt2) {
                return cnt1 > cnt2 ? -1 : 1 ;
            }

            return 0;
        }
    };

    /**
     * 置顶条目总排前面
     */
    private static final Comparator<Coach> OrderH1 = new Comparator<Coach>() {
        @Override
        public int compare(Coach o1, Coach o2) {
            // 置顶总是逆序
            int top1  = o1.top();
            int top2  = o2.top();
            if (top1 != top2) {
                return top1 > top2 ? -1 : 1 ;
            }

            return 0;
        }
    };

    /**
     * 计数为零总排后面
     * 置顶条目总排前面
     */
    private static final Comparator<Coach> OrderH2 = new Comparator<Coach>() {
        @Override
        public int compare(Coach o1, Coach o2) {
            // 零总是排最后
            int cnt1  = o1.cnt();
            int cnt2  = o2.cnt();
            if (cnt1 != cnt2) {
                if (0== cnt2) return -1 ;
                if (0== cnt1) return  1 ;
            }

            // 置顶总是逆序
            int top1  = o1.top();
            int top2  = o2.top();
            if (top1 != top2) {
                return top1 > top2 ? -1 : 1 ;
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
