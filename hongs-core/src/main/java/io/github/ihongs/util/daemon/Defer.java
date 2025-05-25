package io.github.ihongs.util.daemon;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 延迟等待辅助
 *
 * <p>
 * 为已有异步任务增加取消和等待.
 * 未使用线程的 sleep,interrupt;
 * 复杂任务请用 Chore.run(task).
 * </p>
 *
 * <h4>Usage:</h4>
 * <code>
 * final Defer defer = new Defer();
 * // 可以存入 GLOBAL_CORE 以便中止
 *
 * // 主程等待(有异常会抛出 ExecutionException):
 * rs = defer.get();
 *
 * // 取消任务:
 * defer.cancel(true);
 *
 * // 任务循环中:
 * if (defer.interrupted() || Thread.interrupted()) {
 *     // 中止之, 视情况 cancel 或 fail 后退出
 * }
 *
 * // 任务中结束(无结果):
 * defer.done();
 *
 * // 任务中结束(有结果):
 * defer.done(rs);
 *
 * // 任务中结束(有异常):
 * defer.fail(ex);
 * </code>
 *
 * @param <T> 返回数据类型
 */
public class Defer<T> implements Future<T>, AutoCloseable {

    // 0 待运行 1 运行中 2 主程等待 3 中断 4 完成
    final   AtomicInteger stat;

    private Throwable fail;

    private T data ;

    public Defer( ) {
        this (true);
    }

    /**
     * @param begin 是否立即开始
     */
    public Defer(boolean begin) {
        stat = new AtomicInteger(begin ? 1 : 0);
        fail = null;
        data = null;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        if (stat.get( ) < 2) {
            stat.set(2);
            synchronized(this) {
                wait( );
            }
            if (stat.get( ) < 2) {
                stat.set(3);
            }
        }
        if (fail != null) {
            throw new ExecutionException(fail);
        }
        return data;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (stat.get( ) < 2) {
            stat.set(2);
            synchronized(this) {
                wait(unit.convert(timeout, TimeUnit.MILLISECONDS));
            }
            if (stat.get( ) < 2) {
                throw new TimeoutException ( );
            }
        }
        if (fail != null) {
            throw new ExecutionException(fail);
        }
        return data;
    }

    /**
     * 关闭时将取消任务
     */
    @Override
    public void close() {
        cancel(true);
        cancel(true);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (! mayInterruptIfRunning) {
            int st = stat.get();
            if (st < 1) {
                stat.set(3);
                return true;
            }
        } else {
            int st = stat.get();
            if (st < 2) {
                stat.set(3);
                return true;
            } else
            if (st < 3) {
                stat.set(3);
                // 唤起主程
                synchronized (this) {
                  notify( );
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDone() {
        return stat.get() == 4;
    }

    @Override
    public boolean isCancelled() {
        return stat.get() == 3;
    }

    /**
     * 任务结束(取消或完成)
     * @return
     */
    public boolean interrupted() {
        return stat.get() >= 3;
    }

    /**
     * 任务开始
     */
    public void init() {
        this.stat.set(1);
    }

    /**
     * 任务完成
     */
    public void done() {
        int st = stat.getAndSet(4);
        if (st == 2) {
            // 唤起主程
            synchronized (this) {
                notify();
            }
        }
    }

    /**
     * 任务完成(带结果)
     * @param data
     */
    public void done(T data) {
        this.data = data;
        int st = stat.getAndSet(4);
        if (st == 2) {
            // 唤起主程
            synchronized (this) {
                notify();
            }
        }
    }

    /**
     * 任务失败(带异常)
     * @param ex
     */
    public void fail(Throwable ex) {
        this.fail =  ex ;
        int st = stat.getAndSet(3);
        if (st == 2) {
            // 唤起主程
            synchronized (this) {
                notify();
            }
        }
    }

}
