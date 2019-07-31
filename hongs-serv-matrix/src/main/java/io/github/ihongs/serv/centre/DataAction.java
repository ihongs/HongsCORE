package io.github.ihongs.serv.centre;

import io.github.ihongs.Cnst;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.dh.search.SearchAction;
import io.github.ihongs.serv.matrix.Data;
import io.github.ihongs.util.Synt;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 数据存储动作
 * @author Hongs
 */
@Action("centre/auto")
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
        Data data = Data.getInstance(runner.getModule(), runner.getEntity());
        data.setUserId((String) helper.getSessibute(Cnst.UID_SES));
        return data;
    }

    @Override
    protected Map getReqMap (ActionHelper helper, IEntity ett, String opr, Map req)
    throws HongsException {
        req = super.getReqMap(helper, ett, opr, req);

        // 默认的终端标识
        String meno = Synt.asString(req.get("meno"));
        if ( meno != null && ! meno.isEmpty()) {
            req.put("meno", "centre."+ meno );
        } else {
            req.put("meno", "centre");
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

}
