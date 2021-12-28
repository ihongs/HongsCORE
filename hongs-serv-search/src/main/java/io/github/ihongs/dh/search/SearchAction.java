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
import java.util.Arrays;
import java.util.HashSet;
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
        StatisHelper sh = new StatisHelper(sr);

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
        StatisHelper sh = new StatisHelper(sr);

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
        StatisHelper sh = new StatisHelper(sr);

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
        Map fs = sr.getFields( );
        Map ts = FormSet.getInstance( ).getEnum("__types__");
        String cn = Dict.getValue(fs, "default", "@","conf");
//      String nn = Dict.getValue(fs, "unknown", "@","form");

        // 字段限定
        Set ns ;
        Set ks = NUM_KINDS;
        Set hs = CAL_HANDS;
        if (nb < ACT_NAMES.length) {
            ns = sr.getCaseNames(ACT_NAMES[nb] + "able");
        if (ns == null || ns.isEmpty()) {
            ns = sr.getCaseNames("statable");
        }} else {
            ns = sr.getCaseNames("statable");
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
                int p  = fn.indexOf  ('!');
                if (p != -1) {
                    fx = fn.substring(1+p);
                    fn = fn.substring(0,p);

                    // 统计行数
                    if (fx.equals("count")
                    &&  fn.equals( "*" ) ) {
                        continue;
                    }
                }
            }

            Map fc = (Map)fs.get(fn);
            if (! fs.containsKey(fn)) {
                throw new HongsException(400, "Field '"+fn+"' is not existent");
            }
            if (! ns.contains   (fn)) {
                throw new HongsException(400, "Field '"+fn+"' is not statable");
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
                Set on  = Dict.getValue ( rd, Set.class, fn, Cnst.AR_KEY);
                if (on == null) {
                    String xc = Synt.defxult((String) fc.get("conf"), (String) cn);
                    String xn = Synt.defxult((String) fc.get("enum"), (String) fn);
                    try {
                        on = FormSet.getInstance(xc).getEnum(xn).keySet();
                        Dict.put( rd, on, fn, Cnst.AR_KEY );
                    } catch ( HongsException ex) {
                    if (ex.getErrno() != 913) {
                        throw ex;
                    }}
                }
            }
        }

        if (ob != null ) for (Object fu : ob) {
            String fn = fu != null ? fu.toString() : "";

            /**
             * 非聚合统计中
             * ! 表默认逆序
             * * 表默认正序
             */
            if (nb != 3
            && (fn.equals("-")
            ||  fn.equals("!")
            ||  fn.equals("*") ) ) {
                continue;
            }

            /**
             * 排序字段:
             * 对返回的列表排序时,
             * 无效字段会影响效率,
             * 故对照查询字段检查.
             */
            if (fn.startsWith("-") ) {
                fn = fn.substring(1);
            } else
            if (fn.  endsWith("!") ) {
                fn = fn.substring(0, fn.length() - 1);
            }
            if (rb == null || ! rb.contains(fn)) {
                throw new HongsException(400, "Field '"+fu+"' can not be sorted");
            }
        }
    }

    private static final String [  ] ACT_NAMES = new String [] {"", "acount", "amount", "assort"};                       // 统计名称
    private static final Set<String> CAL_HANDS = new HashSet(Arrays.asList("sum", "min" , "max"  , "ratio" , "range" )); // 计算方法
    private static final Set<String> NUM_KINDS = new HashSet(Arrays.asList("int", "long", "float", "double", "number")); // 数字类型

}
