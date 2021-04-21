package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.dh.IReflux;
import java.util.Collection;

/**
 * 事务提交包装
 *
 * 包裹一段需要事务的子过程
 * 亦可用于 Java 8 的函数式
 *
 * @author Kevin
 */
public final class CommitRunner {

    static public interface Run {
           public void run( )
           throws Throwable ;
    }

    static public void run( Run run)
    throws HongsException, HongsExemption {
        run(run, Core.getInstance());
    }

    static public void run( Run run, Core core)
    throws HongsException, HongsExemption {
        // 全局中标识为事务模式
        // 外部已指定则不再处理
        if (core.isset(Cnst.REFLUX_MODE)) {
            try {
                // 执行
                run.run();
            } catch (Throwable ex) {
                if (ex instanceof HongsException) {
                    throw (HongsException) ex ;
                } else
                if (ex instanceof HongsExemption) {
                    throw (HongsExemption) ex ;
                }

                throw new  HongsException (ex);
            }
            return;
        }

        try {
            // 开启
            core.set( Cnst.REFLUX_MODE , true );
            Hub  crux  = new Hub(core);
            Core.THREAD_CORE.set(crux);
            for(Object o : crux.values()) {
                if (o instanceof IReflux) {
                    ((IReflux) o).begin();
                }
            }

            try {
                // 执行
                run.run();

                // 提交
                for(Object o : crux.values().toArray()) {
                    if (o instanceof IReflux) {
                        ((IReflux) o).commit();
                    }
                }
            } catch (Throwable ex) {
                // 回滚
                for(Object o : crux.values().toArray()) {
                    if (o instanceof IReflux) {
                        ((IReflux) o).revert();
                    }
                }

                if (ex instanceof HongsException) {
                    throw (HongsException) ex ;
                } else
                if (ex instanceof HongsExemption) {
                    throw (HongsExemption) ex ;
                }

                // 其他异常需包裹
                throw new  HongsException (1109, ex);
            }
        } finally {
            // 重置
            core.unset(Cnst.REFLUX_MODE);
            Core.THREAD_CORE.set( core );
        }
    }

    private static final class Hub extends Core {

        private Hub (Core core) {
            super(core);
        }

        @Override
        public void set(String key, Object obj) {
            // 代理登记, 预开启事务
            if (obj instanceof IReflux) {
                ((IReflux) obj).begin();
            }
            super.set(key , obj);
        }
        
        public Collection<Object> values() {
            return sup().values();
        }

    }

}
