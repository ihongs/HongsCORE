package app.hongs.serv.handle;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Spread;
import app.hongs.action.anno.Verify;
import app.hongs.dh.IEntity;
import app.hongs.dh.search.SearchAction;
import app.hongs.serv.matrix.Data;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 数据存储动作
 * @author Hongs
 */
@Action("handle/auto")
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
        return Data.getInstance(mod, ent);
    }

    @Override
    public void acting(ActionHelper helper, ActionRunner runner)
    throws HongsException {
        super.acting(helper , runner);

        // 绑定特制的表单
        Method m = runner.getMethod();
        if (m.isAnnotationPresent(Select.class)
        ||  m.isAnnotationPresent(Spread.class)
        ||  m.isAnnotationPresent(Verify.class)) {
            Data dat = (Data) getEntity(helper);
            Map fcs = dat.getFields();
            helper.setAttribute("form:"+mod+"/"+ent+"."+ent, fcs);
        }

        // 放入当前用户ID
        helper.getRequestData().put("cuid", helper.getSessibute(Cnst.UID_SES));
    }

}
