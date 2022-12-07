package io.github.ihongs.serv.centra;

import io.github.ihongs.Cnst;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.SelectHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.dh.search.SearchAction;
import io.github.ihongs.dh.search.TitlesHelper;
import io.github.ihongs.serv.matrix.Data;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Synt;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * 数据存储动作
 * @author Hongs
 */
@Action("centra/data")
public class DataAction extends SearchAction {

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
    public IEntity getEntity(ActionHelper helper)
    throws HongsException {
        ActionRunner runner = (ActionRunner) helper.getAttribute(ActionRunner.class.getName());
        Data   entity = Data.getInstance(runner.getModule(), runner.getEntity());
        String userId = (String) helper.getSessibute(Cnst.UID_SES);
//      if  (  userId == null  ) userId = Cnst.GUS_UID; // 禁止匿名
        entity.setUserId(userId);
        return entity;
    }

    @Override
    protected Map getReqMap(ActionHelper helper, IEntity ett, String opr, Map req)
    throws HongsException {
        req = super.getReqMap(helper, ett, opr, req);

        // 默认的终端标识
        if ("create".equals(opr)
        ||  "update".equals(opr)
        ||  "delete".equals(opr) ) {
        String meno = Synt.asString(req.get("meno"));
        if (meno == null || meno.isEmpty()) {
            req.put ( "meno" , "centra" );
        } else
        if ( ! meno.equals    ("centra" )
        &&   ! meno.startsWith("centra.") ) {
            req.put ( "meno" , "centra."  +  meno  );
        }
        }

        return req;
    }

    @Override
    public void acting(ActionHelper helper, ActionRunner runner)
    throws HongsException {
        super.acting(helper, runner);

        String ent = runner.getEntity();
        String mod = runner.getModule();
        Method met = runner.getMethod();

        // 绑定特制的表单
        String fcn = "form:" + mod +":"+ ent ;
        Object fco = helper.getAttribute(fcn);
        if (fco == null
        && (met.isAnnotationPresent(Select.class)
        ||  met.isAnnotationPresent(Verify.class)
        ||  met.isAnnotationPresent(TitlesHelper.Titles.class)
        )) {
            Data dat = (Data) getEntity(helper);
                 fco =  dat . getFields();
            helper.setAttribute(fcn, fco);
        }
    }

    //** 恢复 **/

    @Action("revert")
    @CommitSuccess
    public void revert(ActionHelper helper) throws HongsException {
        Data   ett = (Data) getEntity(helper);
        Map    req = helper.getRequestData( );

        // 默认的终端标识
        String meno = Synt.asString(req.get("meno"));
        if (meno == null || meno.isEmpty()) {
            req.put ( "meno" , "centra" );
        } else
        if ( ! meno.equals    ("centra" )
        &&   ! meno.startsWith("centra.") ) {
            req.put ( "meno" , "centra."  +  meno  );
        }

        helper.reply("", ett.revert(req));
    }

    @Action("reveal")
    public void reveal(ActionHelper helper) throws HongsException {
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());
        String ent = runner.getEntity();
        String mod = runner.getModule();

        Data   ett = (Data) getEntity(helper);
        Map    req = helper.getRequestData( );
               req.put("form_id", ett.getFormId());
               req.put("user_id", req.get("user"));
        Map    rsp = ett.getModel( ).search( req );

        // 详情数据转换
        if (rsp.containsKey("info")) {
            Map inf = (Map) rsp.get("info");
        if (inf.containsKey("data")) {
            Map dat = (Map) Dawn.toObject(
                   (String) inf.get("data")
            );
            rsp.put("info", dat);
            rsp.put("snap", inf);
            inf.remove( "data" );

            // 补充枚举和关联
            Set ab = Synt.toTerms(req.get(Cnst.AB_KEY));
            if (null != ab) {
                byte md = 0;
                if (ab.contains(".enfo")) md += SelectHelper.ENFO;
                if (ab.contains(".info")) md += SelectHelper.INFO;
                if (ab.contains(".fall")) md += SelectHelper.FALL;
                if (ab.contains("_text")) md += SelectHelper.TEXT;
                if (ab.contains("_time")) md += SelectHelper.TIME;
                if (ab.contains("_link")) md += SelectHelper.LINK;
                if (ab.contains("_fork")) md += SelectHelper.FORK;
                if (md != 0) {
                    new SelectHelper().addItemsByForm(mod, ent).select(rsp, md);
                }

                // 新的和旧的
                if (ab.contains("older")
                ||  ab.contains("newer")) {
                    Object  id = inf.get(/**/ "id");
                    Object fid = inf.get("form_id");
                    long ctime = Synt.declare(inf.get("ctime"), 0L);
                    if (ab.contains("older")) {
                        Map row = ett.getModel().table.fetchCase()
                           .filter("`id` = ? AND `form_id` = ? AND `ctime` < ?", id, fid, ctime)
                           .assort("`ctime` DESC")
                           .select("`ctime`")
                           .getOne();
                        inf.put("older" , ! row.isEmpty() ? row.get("ctime") : null);
                    }
                    if (ab.contains("newer")) {
                        Map row = ett.getModel().table.fetchCase()
                           .filter("`id` = ? AND `form_id` = ? AND `ctime` > ?", id, fid, ctime)
                           .assort("`ctime`  ASC")
                           .select("`ctime`")
                           .getOne();
                        inf.put("newer" , ! row.isEmpty() ? row.get("ctime") : null);
                    }
                }
            }
        }}

        helper.reply(rsp);
    }

}
