package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.dh.IReflux;

/**
 * 事务提交包装
 *
 * 包裹一段需要事务的子过程
 * 亦可用于 Java 8 的函数式
 *
 * @author Kevin
 */
public final class CommitHolder {

    public static interface Wrap {
        public void invoke() throws Throwable;
    }

    public static void invoke(Wrap wrap)
    throws HongsException {
        invoke(wrap, Core.getInstance());
    }

    public static void invoke(Wrap wrap, Core core)
    throws HongsException {
        // 全局中标识为事务模式
        // 外部已指定则不再处理
        if (core.containsKey(Cnst.REFLUX_MODE)) {
            try {
                // 执行
                wrap.invoke();
            } catch (Throwable ex) {
                if (ex instanceof HongsException) {
                    throw (HongsException) ex;
                } else
                if (ex instanceof HongsExemption) {
                    throw (HongsExemption) ex;
                }

                // 其他异常需包裹
                throw new  HongsException (0x1140, ex);
            }
            return;
        }

        try {
            // 开启
            core.put(Cnst.REFLUX_MODE, true);
            for(Object o : core.values()) {
                if (o instanceof IReflux) {
                    ((IReflux) o).begin();
                }
            }

            try {
                // 执行
                wrap.invoke();

                // 提交
                for(Object o : core.values().toArray()) {
                    if (o instanceof IReflux) {
                        ((IReflux) o).commit();
                    }
                }
            } catch (Throwable ex) {
                // 回滚
                for(Object o : core.values().toArray()) {
                    if (o instanceof IReflux) {
                        ((IReflux) o).revert();
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
            core.remove(Cnst.REFLUX_MODE);
        }
    }

}
