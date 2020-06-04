package io.github.ihongs.action.anno;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.CommitHolder;
import java.lang.annotation.Annotation;

/**
 * 操作成功才提交数据更改
 *
 * 由Action.doAction自动调用
 *
 * @author Hongs
 */
public class CommitInvoker implements FilterInvoker {

    @Override
    public void invoke(ActionHelper helper, ActionRunner chains, Annotation anno)
    throws HongsException {
        // 为兼容 Java8 之前的未使用函数式
        CommitHolder.invoke(new CommitHolder.Wrap() {
            public void invoke ( ) throws Throwable {
                chains.doAction( );
            }
        });
    }

}
