package io.github.ihongs.action.anno;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.dh.ITrnsct;
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
        Core   core;
        Object mode;

        // 全局中标识为事务模式
        core = Core.getInstance( );
        mode = core.got(Cnst.TRNSCT_MODE);
        core.put(Cnst.TRNSCT_MODE , true);

        try {
            // 开启
            for(Object o : core.values()) {
                if (o instanceof ITrnsct) {
                    ((ITrnsct) o).begin();
                }
            }

            try {
                chains.doAction();

                // 提交
                for(Object o : core.values().toArray()) {
                    if (o instanceof ITrnsct) {
                        ((ITrnsct) o).commit();
                    }
                }
            } catch (Throwable ex) {
                // 回滚
                for(Object o : core.values().toArray()) {
                    if (o instanceof ITrnsct) {
                        ((ITrnsct) o).revert();
                    }
                }

                if (ex instanceof HongsException) {
                    throw (HongsException) ex;
                } else
                if (ex instanceof HongsExemption) {
                    throw (HongsExemption) ex;
                }

                // 其他异常需包裹
                throw new  HongsException (0x1140, ex);
            }
        } finally {
            if (mode  != null) {
                core.put(Cnst.TRNSCT_MODE , mode);
            }
        }
    }

    /**
     * 包裹一段需要事务的子过程
     * 亦可用于 Java 8 的函数式
     * @param run
     * @throws HongsException
     */
    public static void invoke(Runnable run)
    throws HongsException {
        Core   core;
        Object mode;

        // 全局中标识为事务模式
        core = Core.getInstance( );
        mode = core.got(Cnst.TRNSCT_MODE);
        core.put(Cnst.TRNSCT_MODE , true);

        try {
            // 开启
            for(Object o : core.values()) {
                if (o instanceof ITrnsct) {
                    ((ITrnsct) o).begin();
                }
            }

            try {
                // 执行
                run.run();

                // 提交
                for(Object o : core.values().toArray()) {
                    if (o instanceof ITrnsct) {
                        ((ITrnsct) o).commit();
                    }
                }
            } catch (Throwable ex) {
                // 回滚
                for(Object o : core.values().toArray()) {
                    if (o instanceof ITrnsct) {
                        ((ITrnsct) o).revert();
                    }
                }

                if (ex instanceof HongsException) {
                    throw (HongsException) ex;
                } else
                if (ex instanceof HongsExemption) {
                    throw (HongsExemption) ex;
                }

                // 其他异常需包裹
                throw new  HongsException (0x1140, ex);
            }
        } finally {
            if (mode  != null) {
                core.put(Cnst.TRNSCT_MODE , mode);
            }
        }
    }
}
