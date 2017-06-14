package app.hongs.serv.manage;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Preset;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Spread;
import app.hongs.action.anno.Verify;
import app.hongs.dh.lucene.LuceneAction;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.dh.search.SearchHelper;
import app.hongs.serv.matrix.Data;
import app.hongs.util.Synt;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 数据存储动作
 * @author Hongs
 */
@Action("manage/auto")
public class DataAction extends LuceneAction {

    @Override
    public void acting(ActionHelper helper, ActionRunner runner)
    throws HongsException {
        super.acting(helper, runner);

        // 绑定特制的表单
        Method m = runner.getMethod( );
        if (m.isAnnotationPresent(Select.class)
        ||  m.isAnnotationPresent(Spread.class)
        ||  m.isAnnotationPresent(Verify.class)) {
            helper.setAttribute("form:"+mod+"/"+ent+"."+ent, getEntity(helper).getFields());
        }
        
        // 放入当前用户ID
        helper.getRequestData().put("cuid", helper.getSessibute(Cnst.UID_SES));
    }

    /**
     * 获取模型对象
     * 注意:
     *  对象 Action 注解的命名必须为 "模型路径/实体名称"
     *  方法 Action 注解的命名只能是 "动作名称", 不得含子级实体名称
     * @param helper
     * @return
     * @throws HongsException
     */
    @Override
    public LuceneRecord getEntity(ActionHelper helper)
    throws HongsException {
        return Data.getInstance(mod, ent);
    }

    /**
     * 后台总是能指定 id, 有则更新, 无则添加
     * @param helper
     * @throws HongsException 
     */
    @Action("save")
    @Preset(conf="", envm="", used={":defence", ":create"})
    @Verify(conf="", form="")
    @CommitSuccess
    public void save(ActionHelper helper) throws HongsException {
        String  id = (String) helper.getParameter("id");
        if (id == null || "".equals(id)) {
            id  = Core.newIdentity (  );
        }

        Data    sr = (Data) getEntity(helper);
        Map     rd = helper.getRequestData( );
                rd = getReqMap(helper, sr, "update", rd);
                sr.set(id, rd);
        String  ss = getRspMsg(helper, sr, "update", 1 );

        helper.reply(ss, Synt.asMap("id",id));
    }

    @Action("counts/search")
    public void counts(ActionHelper helper) throws HongsException {
        LuceneRecord sr = (LuceneRecord) getEntity(helper);
        SearchHelper sh = new SearchHelper(sr);
        Map rd = helper.getRequestData();
            rd = getReqMap (helper, sr, "counts", rd);
        Map sd = sh.counts (rd);
            sd = getRspMap (helper, sr, "counts", sd);

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
