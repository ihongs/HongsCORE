package app.hongs.dh.search;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Preset;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Spread;
import app.hongs.dh.IEntity;
import app.hongs.dh.lucene.LuceneAction;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.util.Synt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 搜索动作
 * @author Hongs
 */
@Action()
public class SearchAction extends LuceneAction {

    protected Set<String> sub = Synt.setOf("counts", "statis");

    @Override
    public void acting(ActionHelper helper, ActionRunner runner) throws HongsException {
        // 特别扩展的资源
        ent = runner.getEntity();
        mod = runner.getModule();
        if (sub.contains(ent)) {
            int pos = mod.lastIndexOf("/");
            ent = mod.substring(1+pos);
            mod = mod.substring(0,pos);
            runner.setEntity(ent);
            runner.setModule(mod);
        }
        
        super.acting(helper, runner);
    }

    @Action("search")
    @Preset(conf="", form="")
    @Select(conf="", form="")
    @Spread(conf="", form="")
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

    @Action("counts/search")
    public void counts(ActionHelper helper) throws HongsException {
        LuceneRecord sr = (LuceneRecord) getEntity(helper);
        SearchHelper sh = new SearchHelper(sr);
        Map rd = helper.getRequestData();
            rd = getReqMap (helper, sr, "counts", rd);
        Map sd = sh.counts (rd);
            sd = getRspMap (helper, sr, "counts", sd);
//               sr.close  (  ); // 应用容器可自行关闭

        /**
         * 追加枚举名称
         */
        Map xd = (Map) sd.get("info");
       byte md = Synt.declare(helper.getParameter("md") , (byte) 0);
        if (md != 0 && xd != null && mod != null && ent != null) {
            if (FormSet.hasConfFile( mod )) {
                new SearchTitler(mod, ent).addTitle(xd , md);
            }
        }

        helper.reply(sd);
    }

    @Action("statis/search")
    public void statis(ActionHelper helper) throws HongsException {
        LuceneRecord sr = (LuceneRecord) getEntity(helper);
        SearchHelper sh = new SearchHelper(sr);
        Map rd = helper.getRequestData();
            rd = getReqMap (helper, sr, "statis", rd);
        Map sd = sh.statis (rd);
            sd = getRspMap (helper, sr, "statis", sd);
//               sr.close  (  ); // 应用容器可自行关闭

        /**
         * 追加枚举名称
         */
        Map xd = (Map) sd.get("info");
       byte md = Synt.declare(helper.getParameter("md") , (byte) 0);
        if (md != 0 && xd != null && mod != null && ent != null) {
            if (FormSet.hasConfFile( mod )) {
                new SearchTitler(mod, ent).addTitle(xd , md);
            }
        }

        helper.reply(sd);
    }

    @Override
    public IEntity getEntity(ActionHelper helper)
    throws HongsException {
        return SearchEntity.getInstance(mod, ent);
    }

}
