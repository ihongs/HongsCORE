package io.github.ihongs.dh.search;

import io.github.ihongs.Cnst;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Preset;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.dh.IActing;
import io.github.ihongs.dh.IAction;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.dh.ModelGate;
import io.github.ihongs.util.Synt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 搜索动作
 * @author Hongs
 */
@Action()
public class SearchAction extends ModelGate implements IAction, IActing {

    protected Set<String> sub = Synt.setOf("statis");

    @Override
    public IEntity getEntity(ActionHelper helper)
    throws HongsException {
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());
        return SearchEntity.getInstance (runner.getModule(), runner.getEntity());
    }

    @Override
    public void acting(ActionHelper helper, ActionRunner runner) throws HongsException {
        String ent = runner.getEntity();
        String mod = runner.getModule();

        // 特别扩展的资源
        if (sub.contains(ent)) {
            int   pos;
            pos = mod.lastIndexOf( "/");
            ent = mod.substring(1+ pos);
            mod = mod.substring(0, pos);
            runner.setEntity(ent);
            runner.setModule(mod);
        }

        super.acting(helper, runner);
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

    @Action("statis/search")
    @Preset(conf="", form="")
    public void counts(ActionHelper helper) throws HongsException {
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());
//      String ent = runner.getEntity();
        String mod = runner.getModule();

        SearchEntity sr = (SearchEntity) getEntity(helper);
        SearchHelper sh = new SearchHelper(sr);
        Map rd = helper.getRequestData();
            rd = getReqMap(helper, sr, "acount", rd);
        Map sd = Synt.mapOf( "info", sh.acount(rd) );
            sd = getRspMap(helper, sr, "acount", sd);

        // 增加标题
        titled(mod, sr.getFields(), rd, sd);

        helper.reply(sd);
    }

    @Action("statis/amount")
    @Preset(conf="", form="")
    public void statis(ActionHelper helper) throws HongsException {
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());
//      String ent = runner.getEntity();
        String mod = runner.getModule();

        SearchEntity sr = (SearchEntity) getEntity(helper);
        SearchHelper sh = new SearchHelper(sr);
        Map rd = helper.getRequestData();
            rd = getReqMap(helper, sr, "amount", rd);
        Map sd = Synt.mapOf( "info", sh.amount(rd) );
            sd = getRspMap(helper, sr, "amount", sd);

        // 增加标题
        titled(mod, sr.getFields(), rd, sd);

        helper.reply(sd);
    }

    /**
     * 追加枚举和关联名称
     * @param mod
     * @param fs 字段配置
     * @param rd 请求数据
     * @param sd 响应数据
     * @throws HongsException
     */
    protected void titled(String mod, Map fs, Map rd, Map sd) throws HongsException {
        Set ab = Synt.toTerms(rd.get(Cnst.AB_KEY));
        Map xd = (Map) sd.get("info");
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
            new SearchTitler(  )
             .addItemsByForm(fs)
             .addTitle( xd , md);
        }
    }

}
