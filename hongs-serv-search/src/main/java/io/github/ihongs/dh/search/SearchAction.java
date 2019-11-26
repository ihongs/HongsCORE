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
import io.github.ihongs.util.Synt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
        Map rd = helper.getRequestData();
        Object  wd = rd.get(Cnst.WD_KEY);
        if(null != wd && "".equals(wd) ) {
           List ob  = Synt.asList(rd.get(Cnst.OB_KEY));
            if( ob == null) {
                ob  =  new  ArrayList( );
                rd.put(Cnst.OB_KEY , ob);
                ob.add( 0 , "-");
            } else
            if(!ob.contains("-")) {
                ob.add( 0 , "-");
            }
        }

        super.search(helper);
    }

    @Action("acount")
    @Preset(conf="", form="")
    public void acount(ActionHelper helper) throws HongsException {
        SearchEntity sr = (SearchEntity) getEntity(helper);
        StatisHelper sh = new StatisHelper(sr);

        Map rd = helper.getRequestData();
        rd = getReqMap(helper, sr, "acount", rd);

        acheck(sr, rd, false ); // 检查参数
        Map xd = sh.acount(rd);
        atitle(sr, rd, xd/**/); // 增加标题

        Map sd = Synt.mapOf("info" , xd);
        sd = getRspMap(helper, sr, "acount", sd);

        helper.reply(sd);
    }

    @Action("amount")
    @Preset(conf="", form="")
    public void amount(ActionHelper helper) throws HongsException {
        SearchEntity sr = (SearchEntity) getEntity(helper);
        StatisHelper sh = new StatisHelper(sr);

        Map rd = helper.getRequestData();
        rd = getReqMap(helper, sr, "amount", rd);

        acheck(sr, rd, true  ); // 检查参数
        Map xd = sh.amount(rd);
        atitle(sr, rd, xd/**/); // 增加标题

        Map sd = Synt.mapOf("info" , xd);
        sd = getRspMap(helper, sr, "amount", sd);

        helper.reply(sd);
    }

    /**
     * 检查参数是否可统计
     * @param sr 字段配置
     * @param rd 请求数据
     * @param nb 是否数值
     * @throws HongsException
     */
    protected void acheck(SearchEntity sr, Map rd, boolean nb) throws HongsException {
        Pattern pt = Pattern.compile("(^-|:.*$)" );
        Set rb = Synt.toTerms(rd.get(Cnst.RB_KEY));
        Map fs = sr.getFields( );
        Set st = sr.getCaseNames("statable");
        Set ss = ! nb ? null
               : (Set) FormSet.getInstance()
                   .getEnum("__saves__")
                   .get    (  "number" );

        if (rb!= null) for (Object fn : rb ) {
            if (fn!= null) {
                // 清理重点和排除值, 如: -fn:0,-fn:1
                fn = pt.matcher (fn.toString()).replaceAll("");
            }
            if (! fs.containsKey(fn)) {
                throw new HongsException(400, "Field '"+fn+"' is not exists"  );
            }
            if (! st.contains   (fn)) {
                throw new HongsException(400, "Field '"+fn+"' is not statable");
            }
            Map fc = (Map)fs.get(fn);
            if (  ss  != null
            &&  ! ss.contains   (fc.get("__type__"))
            &&  ! ss.contains   (fc.get(  "type"  ))  ) {
                throw new HongsException(400, "Field '"+fn+"' is not numeric" );
            }
        }
    }

    /**
     * 追加枚举和关联名称
     * @param sr 字段配置
     * @param rd 请求数据
     * @param xd 统计数据
     * @throws HongsException
     */
    protected void atitle(SearchEntity sr, Map rd, Map xd) throws HongsException {
        Set ab = Synt.toTerms(rd.get(Cnst.AB_KEY));
        if (ab == null || xd == null) {
            return ;
        }

        byte md = 0;
        if (ab.contains("_text")) {
            md += 1;
        }
        if (ab.contains("_fork")) {
            md += 2;
        }

        if (md != 0) {
            Map fs = sr.getFields();
            new TitlesHelper(  )
             .addItemsByForm(fs)
             .addTitle( xd , md);
        }
    }

}
