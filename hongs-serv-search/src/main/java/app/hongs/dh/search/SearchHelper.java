package app.hongs.dh.search;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.util.Data;
import app.hongs.util.Synt;

import java.io.IOException;
import java.util.Collections;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

    static {
        // jdk 1.7 加上这个后排序不会报错
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
    }

    public SearchHelper(LuceneRecord that) {
        this.that = that;
    }

    public final LuceneRecord getRecord( ) {
        return that;
    }

    protected Set<String> getForkTypes() {
        return that.getCaseTypes("fork");
    }

    protected Set<String> getEnumTypes() {
        Set<String> ets = new HashSet(that.getSaveTypes("enum"));
        ets.addAll(that.getSaveTypes( "date" ));
        ets.addAll(that.getSaveTypes("number"));
        return ets;
    }

    /**
     * 通过表单配置设置枚举数据(及关联关系)
     * @param info
     * @param conf
     * @param form
     * @param md 1 绑定枚举, 2 绑定关联, 3 全绑定
     * @throws HongsException
     */
    public void addLabel(Map info, byte md, String conf, String form) throws HongsException {
        if (1 != (1 & md) && 2 != (2 & md)) {
            return;
        }

        Map<String, Map> fields = FormSet.getInstance(conf).getForm(form);
        Map<String, Map<String, String>> enums = new HashMap();
        Map<String, Map<String, String>> forks = new HashMap();
        Set<String> ets = getEnumTypes();
        Set<String> fts = getForkTypes();

        for(Map.Entry<String, Map> et : fields.entrySet()) {
            Map    fc = et.getValue();
            String fn = et.getKey(  );
            String ft = (String) fc.get("__type__");
            if (1 == (1 & md) && ets.contains( ft )) {
                String xn = (String) fc.get("enum");
                String xc = (String) fc.get("conf");
                if (xn == null || "".equals(xn)) xn = fn  ;
                if (xc == null || "".equals(xc)) xc = conf;
                Map xe = FormSet.getInstance(xc).getEnumTranslated(xn);
                enums.put(fn, xe);
            } else
            if (2 == (2 & md) && fts.contains( ft )) {
                forks.put(fn, fc);
            }
        }

        addLabel(info, enums, forks);
    }

    /**
     * 追加名称列
     * 此方法通过 addEnums,addForks 来执行具体的关联操作
     * 请预先使用 setEnums,setForks 或 setLinks 设置关联
     * @param info 为通过 counts 得到的 info
     * @param enums
     * @param forks
     * @throws HongsException
     */
    public void addLabel(Map info,
            Map<String, Map<String, String>> enums,
            Map<String, Map<String, String>> forks)
            throws HongsException {
        Iterator<Map.Entry> it = info.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = it.next( );
            Object lo = et.getValue();
            if (!(lo instanceof List)) {
                continue;
            }

            List<Map> ls = (List) lo;
            String fn = Synt.asserts(et.getKey() , "" );

            if (enums != null && enums.containsKey(fn)) {
                addEnums(ls, enums.get(fn), fn);
            } else
            if (forks != null && forks.containsKey(fn)) {
                addForks(ls, forks.get(fn), fn);
            } else {
                // 没有对应的枚举表则用值来补全
                for(Map lx :ls) {
                    if (lx.containsKey("label")) {
                        lx.put("label",lx.get("value"));
                    }
                }
            }
        }
    }

    /**
     * 通过查找枚举信息来补全名称
     * @param ls
     * @param es
     * @param fn
     */
    protected void addEnums(List<Map> ls, Map es, String fn) {
        for ( Map  lx : ls) {
            String lv = (String) lx.get(1);
            if (lv != null) {
                lv  = (String) es.get(lv ); // 得到标签
            }
            if (lv == null) {
                lv  = (String) es.get("*"); // 其他类型
            }
            if (lv == null) {
                continue;
            }
            lx.put("label", lv);
        }
    }

    /**
     * 通过调用关联动作来补全名称
     * @param ls
     * @param fs
     * @param fn
     * @throws HongsException
     */
    protected void addForks(List<Map> ls, Map fs, String fn) throws HongsException {
        String at = (String) fs.get("data-at");
        String vk = (String) fs.get("data-vk");
        String tk = (String) fs.get("data-tk");
        if (at == null || at.length() == 0
        ||  vk == null || vk.length() == 0
        ||  tk == null || tk.length() == 0 ) {
            CoreLogger.error("data-at, data-vk or data-tk can not be empty in field "+fn);
            return;
        }

        // 映射关系
        Map<String, List> lm = new HashMap();
        for ( Map lx : ls) {
            String lv = (String) lx.get( 1 );
            List<Map>  lw = lm.get(lv);
            if (lw == null) {
                lw =  new ArrayList(  );
                lm.put(lv , lw);
            }
            lw.add(lx);
        }

        // 查询结构
        Map rd = new HashMap( );
        Set rb = new HashSet( );
        int ps = at.indexOf("?");
        if (ps > -1) {
            String aq;
            aq = at.substring(ps + 1).trim();
            at = at.substring(0 , ps).trim();
            if (!"".equals(aq)) {
                if (aq.startsWith("{") && aq.endsWith("}")) {
                    rd = (  Map  ) Data.toObject(aq);
                } else {
                    rd = ActionHelper.parseQuery(aq);
                }
            }
        }
        rb.add(vk);
        rb.add(tk);
        rd.put(Cnst.RN_KEY, 0 );
        rd.put(Cnst.RB_KEY, rb);
        rd.put(Cnst.ID_KEY, lm.keySet());

        // 获取结果
        ActionHelper ah = ActionHelper.newInstance();
        ah.setAttribute("IN_FORK", true);
        ah.setRequestData(rd);
        new ActionRunner (at, ah).doInvoke();
        Map sd  = ah.getResponseData(  );
        List<Map> lz = (List) sd.get("list");
        if (lz == null) {
            return;
        }

        // 整合数据
        for ( Map  ro : lz) {
            String lv = Synt.declare(ro.get(vk), "");
            String lt = Synt.declare(ro.get(tk), "");

            List<Map> lw = lm.get(lv);
            if  (  null != lw)
            for (Map  lx : lw) {
                lx.put ( "label", lt);
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

        int         topz = Synt.declare(rd.get(Cnst.RN_KEY), 0);
        Set<String> cntz = Synt.asTerms(rd.get(Cnst.RB_KEY)   );
        Map<String, Map<String, Integer>> counts = new HashMap( );
        Map<String, Map<String, Integer>> countz = new HashMap( );
        Map<String, Set<String         >> countx = new HashMap();

        //** 整理待统计的数据 **/

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

        /**
         * 根据请求数据进行综合判断,
         * 如果字段已经作为过滤条件,
         * 则此字段的统计需单独进行,
         * 且需抛开此字段的过滤数据.
         *
         * 例如某数据有一个地区字段且每条记录只能有一个地区,
         * 如果没有以下处理则选某地后其他未选地区数量将为零.
         *
         * 最终结果类似 LinkedIn 的筛选
         */

        for(String k : cxts) {
            Set    vs = null;
            Object vo = rd.get(k );
            if (vo instanceof Map) {
                Map vm = (Map) vo ;
                if (vm.containsKey(Cnst.EQ_REL)) {
                    vs = Synt.declare(vm.get(Cnst.EQ_REL), Set.class);
                } else
                if (vm.containsKey(Cnst.IN_REL)) {
                    vs = Synt.declare(vm.get(Cnst.IN_REL), Set.class);
                }
            } else {
                if (!"".equals(vo)) {
                    vs = Synt.declare(rd.get(k), Set.class);
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
                }

                for(Object v : vs) {
                    String s = v.toString();
                    if (vx == null || !vx.contains(s)) {
                        vz.put( s, 0 );
                    }
                }

                Map xd = new HashMap();
                xd.putAll(rd);
                xd.remove( k);
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
            List<Map> a = new ArrayList();
            for (Map.Entry<String, Integer> e : et.getValue()) {
                 Map  b = new HashMap(  );
                a.add(b);
                b.put("value", e.getKey(  ));
                b.put("count", e.getValue());
            }
            cnts.put(et.getKey(), a);
        }

        return resp;
    }

    private int counts(Map rd,
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
                            String k = et.getKey();
                            String[] vals = doc.getValues(k);
                            Map<String, Integer> cntc = et.getValue();

                            for (String val: vals) {
                                if (cntc.containsKey(val)) {
                                    cntc.put(val , cntc.get(val) + 1);
                                }
                            }
                        }

                        for (Map.Entry<String, Map<String, Integer>> et : counts.entrySet()) {
                            String k = et.getKey();
                            String[] vals = doc.getValues(k);
                            Map<String, Integer> cntc = et.getValue();
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

    public Map statis(Map rd) throws HongsException {
        IndexSearcher finder = that.getFinder();
        IndexReader   reader = that.getReader();

        Map  resp = new HashMap();
        Map  cnts = new HashMap();
        resp.put( "info" , cnts );

        Set<String> cntz = Synt.asTerms(rd.get(Cnst.RB_KEY));
        Map<String, Map<Minmax, Cntsum>> counts = new HashMap();
        Map<String, Set<Minmax        >> countx = new HashMap();

        //** 整理待统计的数据 **/

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

                        Minmax mm = new Minmax(a[1]);
                        countx.get( a[0] ).add( mm );
                    }
                } else {
                    if (!fields.containsKey(a[0])) {
                        throw new HongsException.Common("Field "+a[0]+" not exists");
                    }
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

                        Minmax mm = null;
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

        for(String k : cxts) {
            Minmax mm = null;
            Object vo = rd.get(k );
            if (vo instanceof Map) {
                Map vm = (Map) vo ;
                if (vm.containsKey(Cnst.RG_REL)) {
                    mm = new Minmax(Synt.declare(vm.get(Cnst.EQ_REL), ""));
                } else
                if (vm.containsKey(Cnst.EQ_REL)) {
                    mm = new Minmax(Synt.declare(vm.get(Cnst.EQ_REL), 0D));
                }
            } else {
                if (!"".equals(vo) && !( vo instanceof Collection )) {
                    mm = new Minmax(Synt.declare(vo, 0D));
                }
            }

            if (mm == null) {
                if (counts.containsKey(k)) {
                    counts2.put(k, counts.get(k));
                }
                if (countx.containsKey(k)) {
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

                if (vx == null || !vx.contains(mm)) {
                    vz.put(mm, new Cntsum());
                }

                Map xd = new HashMap();
                xd.putAll(rd);
                xd.remove( k);
                statis(xd, counts3, countx3, reader, finder);
            }
        }

        int z = statis(rd, counts2, countx2, reader, finder);
        cnts.put("__total__", z);

        //** 排序统计数据 **/

        Map<String, List<Map.Entry<Minmax, Cntsum>>> cntlst = new HashMap();

        for (Map.Entry<String, Map<Minmax, Cntsum>> et : counts.entrySet()) {
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
            List<Map>  a = new ArrayList();
            for (Map.Entry<Minmax, Cntsum> e : et.getValue()) {
                 Map   b = new HashMap();
                Cntsum c = e.getValue( );
                Minmax m = e.getKey(   );
                String k = m != null ? m.toString() : null;
                a.add( b );
                b.put("value", k/**/);
                b.put("label", c.cnt);
                b.put( "sum" , c.sum);
                if (c.cnt == 0) {
                    b.put("min" , 0 );
                    b.put("max" , 0 );
                } else {
                    b.put("min" , c.min);
                    b.put("max" , c.max);
                }
            }
            cnts.put(et.getKey(), a );
        }

        return resp;
    }

    private int statis(Map rd,
            Map<String, Map<Minmax , Cntsum>> counts,
            Map<String, Set<Minmax         >> countx,
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

                if (!counts.isEmpty()) {
                    for(ScoreDoc dox : docs) {
                        Document doc = reader.document(dox.doc);

                        for (Map.Entry<String, Map<Minmax, Cntsum>> et : counts.entrySet()) {
                            String k = et.getKey();
                            Map<Minmax, Cntsum > cntc = et.getValue();
                            Set<Minmax> cntx = countx.get(k);
                            String[] vals = doc.getValues(k);

                            F1: for (String val: vals) {
                                double v = Synt.asserts(val, 0D);
                                F2: for (Map.Entry<Minmax, Cntsum> mc : cntc.entrySet()) {
                                    Minmax m = mc.getKey(  );

                                    /*
                                     * 键为空表示此为总计行
                                     * 总计需跳过忽略的取值
                                     */
                                    if (m != null) {
                                        if (!m.covers(v)) {
                                            continue  F2;
                                        }
                                    } else
                                    for (Minmax w : cntx) {
                                        if ( w.covers(v)) {
                                            continue  F1;
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

    private static class Sorted implements Comparator<Map.Entry<String, Integer>> {
        @Override
        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
            int i1 = o1.getValue(), i2 = o2.getValue();
            return i1 == i2 ? 0 : ( i2 > i1 ? 1 : -1 );
        }
    }

    private static class Sortes implements Comparator<Map.Entry<Minmax, Cntsum >> {
        @Override
        public int compare(Map.Entry<Minmax, Cntsum > o1, Map.Entry<Minmax, Cntsum > o2) {
            // 区间为空的表示总计, 确保它总是在第一个
            Minmax k1 = o1.getKey(  ) , k2 = o2.getKey(  );
            if (k1 == null) return  1;
            if (k2 == null) return -1;

            Cntsum x1 = o1.getValue() , x2 = o2.getValue();
            return x1.cnt != x2.cnt ? ( x2.cnt > x2.cnt ? 1 : -1)
                : (x1.sum != x2.sum ? ( x2.sum > x1.sum ? 1 : -1) : 0);
        }
    }

    private static class Cntsum {
        public int    cnt = 0;
        public double sum = 0;
        public double min = Double.MIN_VALUE;
        public double max = Double.MAX_VALUE;

        public void add(double v) {
            cnt += 1;
            sum += v;
            if (min > v) min = v;
            if (max < v) max = v;
        }
    }

    private static class Minmax {
        public double min = Double.MIN_VALUE;
        public double max = Double.MAX_VALUE;
        public boolean le = false;
        public boolean ge = false;

        public Minmax(double n) {
            min = max = n;
        }

        public Minmax(String s) {
            Object[] a = Synt.asRange(s);
            if (a[0] != null) min = Synt.declare(a[0], 0D);
            if (a[1] != null) max = Synt.declare(a[1], 0D);
            le = (boolean) a[2];
            ge = (boolean) a[3];
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(le ? "(" : "[");
            sb.append(min != Double.MIN_VALUE ? min : "");
            sb.append(",");
            sb.append(max != Double.MAX_VALUE ? max : "");
            sb.append(le ? ")" : "]");
            return sb.toString();
        }

        @Override
        public int hashCode() {
            return toString().hashCode( );
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Minmax)) {
                return false;
            }
            Minmax m = (Minmax) o;
            return m.min == min && m.max == max;
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
    }

}
