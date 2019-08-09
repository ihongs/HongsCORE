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
import io.github.ihongs.serv.matrix.Data;
import io.github.ihongs.util.Synt;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * 数据存储动作
 * @author Hongs
 */
@Action("centra/auto")
public class DataAction extends SearchAction {

    public DataAction() {
        sub.add("revert");
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
    public IEntity getEntity(ActionHelper helper)
    throws HongsException {
        ActionRunner runner = (ActionRunner) helper.getAttribute(ActionRunner.class.getName());
        Data   entity = Data.getInstance(runner.getModule(), runner.getEntity());
        String userId = (String) helper.getSessibute(Cnst.UID_SES);
//      if  (  userId == null  ) userId = Cnst.ADM_GID; // 禁止匿名
        entity.setUserId(userId);
        return entity;
    }

    @Override
    protected Map getReqMap (ActionHelper helper, IEntity ett, String opr, Map req)
    throws HongsException {
        req = super.getReqMap(helper, ett, opr, req);

        // 默认的终端标识
        String meno = Synt.asString(req.get("meno"));
        if ( meno != null && ! meno.isEmpty()) {
            req.put("meno", "centra."+ meno );
        } else {
            req.put("meno", "centra");
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
        if (met.isAnnotationPresent(Select.class)
        ||  met.isAnnotationPresent(Verify.class)) {
            Data dat = (Data) getEntity(helper);
            Map  fcs =  dat.getFields();
            helper.setAttribute("form:"+ mod +"."+ ent, fcs);
        }
    }

    //** 恢复 **/

    @Action("revert/update")
    @CommitSuccess
    public void revert(ActionHelper helper) throws HongsException {
        Data  sr = (Data) getEntity(helper);
        Map   rd = helper.getRequestData( );
            sr.revert(rd);
        helper.reply ("");
    }

    @Action("revert/search")
    public void review(ActionHelper helper) throws HongsException {
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());
        String ent = runner.getEntity();
        String mod = runner.getModule();

        Data    sr = (Data) getEntity(helper);
        Map     rd = helper.getRequestData( );
        rd.put ( "form_id" , sr.getFormId() );
        rd.put ( "user_id" , rd.get("user") ); // user_id 总是当前用户, 筛选需绕过去
        Map     sd = sr.getModel().search(rd);

        // 详情数据转换
        if (sd.containsKey("info")) {
            Map df = (Map) sd.remove("info" );
            Map dt = (Map) io.github.ihongs.util.Dawn.toObject(
                  (String) df.remove("data"));
            sd.put("logs", df);
            sd.put("info", dt);

            // 补充枚举和关联
            Set ab = Synt.toTerms(rd.get( Cnst.AB_KEY ));
            if (ab != null) {
                byte md = 0;
                if (ab.contains("_text")) md += SelectHelper.TEXT;
                if (ab.contains("_time")) md += SelectHelper.TIME;
                if (ab.contains("_link")) md += SelectHelper.LINK;
                if (ab.contains("_fork")) md += SelectHelper.FORK;
                if (ab.contains(".form")) md += SelectHelper.FORM;
                if (ab.contains(".enum")) md += SelectHelper.ENUM;
                if (md != 0) {
                    new SelectHelper().addItemsByForm(mod, ent).select(sd, md);
                }
            }
        }

        helper.reply(sd);
    }

}
