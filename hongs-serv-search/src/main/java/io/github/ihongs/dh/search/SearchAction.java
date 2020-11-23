package io.github.ihongs.dh.search;

import io.github.ihongs.Cnst;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Preset;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.dh.JAction;
import io.github.ihongs.dh.lucene.LuceneRecord;
import io.github.ihongs.dh.search.TitlesHelper.Titles;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 搜索动作
 * @author Hongs
 */
@Action()
public class SearchAction extends JAction {

    @Override
    public IEntity getEntity(ActionHelper helper)
    throws HongsException {
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());
        return SearchEntity.getInstance (runner.getModule(), runner.getEntity());
    }

    @Action("search")
    @Preset(conf="", form="")
    @Select(conf="", form="")
    @Override
    public void search(ActionHelper helper) throws HongsException {
        /**
         * 有指定查询条件则按匹配度排序
         */
        Map rd  = helper.getRequestData();
        Object  wd  = rd.get(Cnst.WD_KEY);
        if( wd != null && ! "".equals(wd)) {
           Set  xb  = Synt.toTerms (rd.get(Cnst.OB_KEY));
           List ob  ;
            if( xb == null) {
                ob  = new ArrayList( 1);
                rd.put(Cnst.OB_KEY, ob);
                ob.add( 0 , "-");
            } else
            if(!xb.contains("-")
            && !xb.contains("*")) {
                ob  = new ArrayList(xb);
                rd.put(Cnst.OB_KEY, ob);
                ob.add( 0 , "-");
            }
        }

        super.search(helper);
    }

    @Action("acount")
    @Preset(conf="", form="")
    @Titles(conf="", form="")
    public void acount(ActionHelper helper) throws HongsException {
        SearchEntity sr = (SearchEntity) getEntity(helper);
        StatisHandle sh = new StatisHandle(sr);

        Map rd = helper.getRequestData();
        rd = getReqMap(helper, sr, "acount", rd);

        // 检查参数
        acheck(sr, rd, 1 );

        Map xd = sh.acount ( rd );

        Map sd = Synt.mapOf("enfo", xd );
        sd = getRspMap(helper, sr, "acount", sd);

        helper.reply( sd );
    }

    @Action("amount")
    @Preset(conf="", form="")
    @Titles(conf="", form="")
    public void amount(ActionHelper helper) throws HongsException {
        SearchEntity sr = (SearchEntity) getEntity(helper);
        StatisHandle sh = new StatisHandle(sr);

        Map rd = helper.getRequestData();
        rd = getReqMap(helper, sr, "amount", rd);

        // 检查参数
        acheck(sr, rd, 2 );

        Map xd = sh.amount ( rd );

        Map sd = Synt.mapOf("enfo", xd );
        sd = getRspMap(helper, sr, "amount", sd);

        helper.reply( sd );
    }

    @Action("assort")
    @Preset(conf="", form="")
    @Select(conf="", form="")
    public void assort(ActionHelper helper) throws HongsException {
        SearchEntity sr = (SearchEntity) getEntity(helper);
        StatisHandle sh = new StatisHandle(sr);

        Map rd = helper.getRequestData();
        rd = getReqMap(helper, sr, "assort", rd);

        // 检查参数
        acheck(sr, rd, 3 );

        int rn = Synt.declare(rd.get(Cnst.RN_KEY), 0);
        int pn = Synt.declare(rd.get(Cnst.PN_KEY), 1);
        Map sd = sh.assort(rd, rn, pn);

        sd = getRspMap(helper, sr, "assort", sd);

        helper.reply( sd );
    }

    /**
     * 检查参数是否可统计
     * @param sr 字段配置
     * @param rd 请求数据
     * @param nb 1 acount, 2 amount, 3 assort
     * @throws HongsException
     */
    protected void acheck(LuceneRecord sr, Map rd, int nb) throws HongsException {
        Set rb = Synt.toTerms(rd.get(Cnst.RB_KEY));
        Set ob = Synt.toTerms(rd.get(Cnst.OB_KEY));
        Map es = Synt.asMap  (rd.get(Cnst.IN_REL));
        Map fs = sr.getFields( );
        Set st = sr.getCaseNames("sortable");
        Map ts = FormSet.getInstance( ).getEnum ("__types__");
        Set hs = Synt.setOf("sum", "min", "max", "ratio", "range");      // 计算方法
        Set ks = Synt.setOf("int", "long", "float", "double", "number"); // 数字类型
        String cn = Dict.getValue(fs, "default", "@", "conf");
//      String nn = Dict.getValue(fs, "unknown", "@", "form");

        // 枚举统计
        if (es == null ) {
            es =  new  HashMap  ( );
            rd.put(Cnst.IN_REL, es);
        }

        if (rb != null ) for (Object fu : rb) {
            String fn = null != fu ? fu.toString() : "";
            String fx = null ;

            /**
             * 分组统计:
             * 维度字段后跟函数名,
             * 判断前需要将其去除.
             */
            if (nb == 3) {
                if ("*|count".equals (fn)) {
                    continue; // 统计行数
                }
                int p  = fn. indexOf ('|');
                if (p != -1) {
                    fx = fn.substring(1+p);
                    fn = fn.substring(0,p);
                }
            }

            Map fc = (Map)fs.get(fn);
            if (! fs.containsKey(fn)) {
                throw new HongsException(400, "Field '"+fn+"' is not existent");
            }
            if (! st.contains   (fn)) {
                throw new HongsException(400, "Field '"+fn+"' is not sortable");
            }

            /**
             * 数值统计:
             * 仅数字类型可以计算,
             * 分组计算方法也一样.
             */
            if (nb == 2
            || (nb == 3 && hs.contains(fx)) ) {
                Object t = fc.get("__type__");
                Object k = fc.get(  "type"  );
                t = ts.containsKey(t) ? ts.get(t) : t;
                if (! "number".equals(t)
                &&  !  "date" .equals(t)
                &&  !("hidden".equals(t) && ks.contains(k))
                &&  !( "enum" .equals(t) && ks.contains(k))) {
                    throw new HongsException(400, "Field '"+fn+"' is not numeric");
                }

                /**
                 * 枚举补全:
                 * 如果外部未指定区间,
                 * 则从枚举配置中提取.
                 */
                if (! es.containsKey(fn)) {
                    String xc = Synt.defxult((String) fc.get("conf"), (String) cn);
                    String xn = Synt.defxult((String) fc.get("enum"), (String) fn);
                    try {
                        es.put( fn, FormSet.getInstance(xc).getEnum(xn).keySet() );
                    } catch ( HongsException ex) {
                    if (ex.getErrno() != 0x10eb) {
                        throw ex;
                    }}
                }
            }

            /**
             * 外部指定 * 代表不限
             */
            Set e  = Synt.asSet(es.get(fn));
            if (e != null && ! e.isEmpty()) {
                if (e.contains("*")) {
                    es.remove ( fn );
                }
            }
        }

        if (ob != null ) for (Object fu : ob) {
            String fn = fu != null ? fu.toString() : "";

            /**
             * 排序字段:
             * 对返回的列表排序时,
             * 无效字段会影响效率,
             * 故对照查询字段检查.
             * 逆序字段先去掉负号.
             */
            if (fn.startsWith("-") ) {
                fn = fn.substring(1);
            }
            if ( ! rb.contains(fn) ) {
                throw new HongsException(400, "Field '"+fu+"' can not be sorted");
            }
        }
    }

}
