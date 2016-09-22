package app.hongs.serv.handle;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Spread;
import app.hongs.action.anno.Verify;
import app.hongs.dh.lucene.LuceneAction;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.serv.module.Data;
import java.lang.reflect.Method;

/**
 * 数据存储动作
 * @author Hongs
 */
@Action("handle/auto")
public class DataAction extends LuceneAction {

    @Override
    public void initiate(ActionHelper helper, ActionRunner runner)
    throws HongsException {
        super.initiate(helper, runner);

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

}
