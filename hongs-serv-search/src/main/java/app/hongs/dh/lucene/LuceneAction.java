package app.hongs.dh.lucene;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.anno.Action;
import app.hongs.dh.IActing;
import app.hongs.dh.IAction;
import app.hongs.dh.IEntity;
import app.hongs.dh.JointGate;

/**
 * Lucene 模型动作
 * @author Hongs
 */
@Action()
public class LuceneAction extends JointGate implements IAction, IActing {

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
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());
        return LuceneRecord.getInstance (runner.getModule(), runner.getEntity());
    }

}
