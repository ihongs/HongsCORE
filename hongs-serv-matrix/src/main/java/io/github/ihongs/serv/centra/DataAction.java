package io.github.ihongs.serv.centra;

import io.github.ihongs.Cnst;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.dh.search.SearchAction;
import io.github.ihongs.dh.search.TitlesHelper;
import io.github.ihongs.serv.matrix.Data;
import io.github.ihongs.util.Synt;
import java.lang.reflect.Method;
import java.util.Map;

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
    @Select(conf="", form="")
    public void reveal(ActionHelper helper) throws HongsException {
        Data   ett = (Data) getEntity(helper);
        Map    req = helper.getRequestData( );
        Map    rsp = ett.reveal(req);
        helper.reply(rsp);
    }

    @Action("remind")
    @Select(conf="", form="")
    public void remind(ActionHelper helper) throws HongsException {
        Data   ett = (Data) getEntity(helper);
        Map    req = helper.getRequestData( );
        Map    rsp = ett.remind(req);
        helper.reply(rsp);
    }

}
