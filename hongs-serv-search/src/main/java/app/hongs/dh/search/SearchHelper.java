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
    private Map<String, Map<String, String>> forks = null;

    public SearchHelper(LuceneRecord that) {
        this.that = that;
    }

    public LuceneRecord getRecord() {
        return that;
    }

    public void setEnums(Map dict) {
        enums = dict;
    }

    public void setForks(Map dict) {
        forks = dict;
    }

    /**
     * 通过表单配置设置枚举数据(及关联关系)
     * @param conf
     * @param form
     * @param md 1 绑定枚举, 2 绑定关联, 3 全绑定
     * @throws HongsException
     */
    public void setLinks(String conf, String form, byte md) throws HongsException {
        if (1 != (1 & md) && 2 != (2 & md)) {
            return;
        }
        if (enums == null) {
            enums = new HashMap();
        }
        if (forks == null) {
            forks = new HashMap();
        }
        Map<String, Map> fs = FormSet.getInstance(conf ).getForm(form );
        for(Map.Entry<String, Map> et : fs.entrySet()) {
            Map    fc = et.getValue();
            String fn = et.getKey(  );
            if (1 == (1 & md) && "enum".equals(fc.get("__type__"))) {
                String xn = (String) fc.get("enum");
                String xc = (String) fc.get("conf");
                if (xn == null || "".equals(xn)) xn = fn  ;
                if (xc == null || "".equals(xc)) xc = conf;
                Map xe = FormSet.getInstance(xc).getEnumTranslated(xn);
                enums.put(fn, xe);
            } else
            if (2 == (2 & md) && "fork".equals(fc.get("__type__"))) {
                forks.put(fn, fc);
            }
        }
    }

    /**
     * 追加名称列
     * 此方法通过 addEnums,addForks 来执行具体的关联操作
     * 请预先使用 setEnums,setForks 或 setLinks 设置关联
     * @param info 为通过 counts 得到的 info
     * @throws HongsException
     */
    public void addNames(Map info) throws HongsException {
        Iterator<Map.Entry> it = info.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = it.next( );
            Object lo = et.getValue();
            if (!(lo instanceof List)) {
                continue;
            }

            List<List> ls = (List) lo;
            String fn = Synt.asserts(et.getKey() , "" );

            if (enums != null && enums.containsKey(fn)) {
                addEnums(ls, enums.get(fn), fn);
            } else
            if (forks != null && forks.containsKey(fn)) {
                addForks(ls, forks.get(fn), fn);
            } else {
                for ( List lx  :  ls ) {
                    if (lx.size() < 3) {
                        lx.add (lx.get(1));
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
    protected void addEnums(List<List> ls, Map es, String fn) {
        for ( List lx : ls) {
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
            lx.add( lv );
        }
    }

    /**
     * 通过调用关联动作来补全名称
     * @param ls
     * @param fs
     * @param fn
     * @throws HongsException
     */
    protected void addForks(List<List> ls, Map fs, String fn) throws HongsException {
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
        for ( List lx : ls) {
            String lv = (String) lx.get( 1 );
            List<List>  lw = lm.get(lv);
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
            List<List>  lw = lm.get (lv);
            if (lw != null) {
                for ( List lx : lw) {
                    lx.add(lt);
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
        Set<String> cntz = Synt.asTerms(rd.get("cnt")   );
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
