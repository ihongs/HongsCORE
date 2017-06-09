package app.hongs.dh.search;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Spread;
import app.hongs.dh.lucene.LuceneAction;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.util.Synt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 搜索动作
 * @author Hongs
 */
@Action()
public class SearchAction extends LuceneAction {

    @Override
    public void acting(ActionHelper helper, ActionRunner runner) throws HongsException {
        super.acting(helper, runner);

        // 给统计腾出空间
        if ("counts".equals(ent) || "statis".equals(ent)) {
            int pos = mod.lastIndexOf("/");
            ent = mod.substring(1 + pos);
            mod = mod.substring(0 , pos);
        }
    }

    @Action("search")
    @Select()
    @Spread()
    @Override
    public void search(ActionHelper helper) throws HongsException {
        /**
         * 有指定查询条件则按匹配度排序
         */
        Map rd = helper.getRequestData();
        Object  wd = rd.get(Cnst.WD_KEY);
        if(null != wd && "".equals(wd) ) {
           List ob  = Synt.declare(rd.get(Cnst.OB_KEY), List.class);
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
                 sr.close  (  );

        /**
         * 追加枚举名称
         */
        Map xd = (Map) sd.get("info");
       byte md = Synt.declare(helper.getParameter("md") , (byte) 0);
        if (md != 0 && xd != null && mod != null && ent != null) {
            if (FormSet.hasConfFile( mod )) {
                sh.addLabel(xd , md, mod, ent);
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
                 sr.close  (  );

        /**
         * 追加枚举名称
         */
        Map xd = (Map) sd.get("info");
       byte md = Synt.declare(helper.getParameter("md") , (byte) 0);
        if (md != 0 && xd != null && mod != null && ent != null) {
            if (FormSet.hasConfFile( mod )) {
                sh.addLabel(xd , md, mod, ent);
            }
        }

        helper.reply(sd);
    }

}
