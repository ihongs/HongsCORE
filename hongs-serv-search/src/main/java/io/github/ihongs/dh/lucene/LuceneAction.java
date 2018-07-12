package io.github.ihongs.dh.lucene;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.dh.IActing;
import io.github.ihongs.dh.IAction;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.dh.ModelGate;

/**
 * Lucene 模型动作
 * @author Hongs
 */
@Action()
public class LuceneAction extends ModelGate implements IAction, IActing {

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
