package app.hongs.serv.handle;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.anno.Action;
import app.hongs.dh.lucene.LuceneAction;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.serv.module.Data;

/**
 * 数据存储动作
 * @author Hongs
 */
@Action("handle/auto")
public class DataAction extends LuceneAction /*implements IActing*/ {

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
        ActionRunner runner = (ActionRunner) helper.getAttribute(Cnst.RUNNER_ATTR);
        String mod = runner.getAction();
        String ent ;
        int    pos ;
        pos  = mod.lastIndexOf('/' );
        mod  = mod.substring(0, pos); // 去掉动作
        pos  = mod.lastIndexOf('/' );
        ent  = mod.substring(1+ pos); // 实体名称
        mod  = mod.substring(0, pos); // 模型名称
        return Data.getInstance(mod, ent);
    }

}
