package foo.hongs.action.anno;

import foo.hongs.action.ActionRunner;
import foo.hongs.HongsException;
import foo.hongs.action.ActionHelper;
import java.lang.annotation.Annotation;

/**
 * 动作注解执行器
 *
 * 包裹器必须指定执行器方可执行
 *
 * @author Hongs
 */
public interface FilterInvoker {
    public void invoke(ActionHelper helper, ActionRunner runner, Annotation anno) throws HongsException;
}
