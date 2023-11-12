package io.github.ihongs.action.anno;

import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import java.lang.annotation.Annotation;

/**
 * 动作注解执行器
 *
 * 包裹器必须指定执行器方可执行
 *
 * @author Hongs
 */
public interface FilterInvoker {
    public void invoke(ActionHelper helper, ActionRunner runner, Annotation anno) throws CruxException;
}
