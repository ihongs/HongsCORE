package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CruxCause;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
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

    static public void run(Runnable run)
    throws CruxException, CruxExemption {
        run(run, Core.getInstance());
    }

    static public void run(Runnable run, Core core)
    throws CruxException, CruxExemption {
        // 全局中标识为事务模式
        // 外部已指定则不再处理
        if (core.isset(Cnst.REFLUX_MODE)) {
            try {
                // 执行
                run.run();
            } catch (Throwable ex) {
                if (ex instanceof CruxCause) {
                    throw ((CruxCause) ex).toException();
                } else {
                    throw new CruxException( ex , 1109 );
                }
            }
            return;
        }

        try {
            // 开启
            core.set( Cnst.REFLUX_MODE , true );
            Flux crux = new Flux(core);
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
                        ((IReflux) o).cancel();
                    }
                }

                if (ex instanceof CruxCause) {
                    throw ((CruxCause) ex).toException();
                } else {
                    throw new CruxException( ex , 1108 );
                }
            }
        } finally {
            // 重置
            core.unset(Cnst.REFLUX_MODE);
            Core.THREAD_CORE.set( core );
        }
    }

    static public void commit() {
        commit(Core.getInstance());
    }

    static public void commit(Core core) {
        Boolean r = (Boolean) core.get(Cnst.REFLUX_MODE);
        Flux crux = new Flux (core);
        if (r != null && r == true) {
            // 事务内提交后继续启用事务
            for(Object o : crux.values().toArray()) {
                if (o instanceof IReflux) {
                    IReflux x = (IReflux) o;
                    x.commit();
                    x.begin ();
                }
            }
        } else {
            for(Object o : crux.values().toArray()) {
                if (o instanceof IReflux) {
                    IReflux x = (IReflux) o;
                    x.commit();
                }
            }
        }
    }

    static public void cancel() {
        cancel(Core.getInstance());
    }

    static public void cancel(Core core) {
        Boolean r = (Boolean) core.get(Cnst.REFLUX_MODE);
        Flux crux = new Flux (core);
        if (r != null && r == true) {
            // 事务内回滚后继续启用事务
            for(Object o : crux.values().toArray()) {
                if (o instanceof IReflux) {
                    IReflux x = (IReflux) o;
                    x.cancel();
                    x.begin ();
                }
            }
        } else {
            for(Object o : crux.values().toArray()) {
                if (o instanceof IReflux) {
                    IReflux x = (IReflux) o;
                    x.cancel();
                }
            }
        }
    }

    private static final class Flux extends Core {

        private Flux (Core core) {
            super(core);
        }

        @Override
        public Object put(String key, Object obj) {
            // 代理登记, 预开启事务
            if (obj instanceof IReflux) {
                ((IReflux) obj).begin();
            }
            return super.put(key , obj);
        }

        public Collection<Object> values() {
            return sup().values();
        }

    }

}
