package io.github.ihongs.util.daemon;

import io.github.ihongs.Core;
import java.util.function.Consumer;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * 延迟等待辅助
 *
 * <p>
 * 为已有异步任务增加取消和等待.
 * 未使用线程的 sleep,interrupt;
 * 复杂任务请用 Chore.run(task).
 * </p>
 *
 * <p>
 * 特别注意:
 * 并不建议在多个线程内等待执行.
 * 如果需要在多个线程内等待 get,
 * 务必设构造参数 multi 为 true.
 * </p>
 *
 * <h4>Usage:</h4>
 * <code>
 * final Defer defer = new Defer();
 * // 可以存入 GLOBAL_CORE 以便中止
 *
 * // 主程等待(异常会抛出 ExecutionException, 取消会抛出 CancellationException):
 * rs = defer.get();
 *
 * // 取消任务:
 * defer.cancel(true);
 *
 * // 任务执行中:
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

    // 0 排队 1 运行 2 (排队)等待 3 (运行)等待 4 中断 5 完成
    private final AtomicInteger stat;

    private final boolean solo;

    private Throwable fail;

    private T data;

    /**
     * 立即开始, 单处等待
     */
    public Defer () {
        this( true, false);
    }

    /**
     * @param begin 是否立即开始
     */
    public Defer (boolean begin) {
        this(begin, false);
    }

    /**
     * @param begin 是否立即开始
     * @param multi 是否多处等待
     */
    public Defer (boolean begin, boolean multi) {
        stat = new AtomicInteger(begin ? 1 : 0);
        solo = ! multi;
        fail = null;
        data = null;
    }

    /**
     * @throws CancellationException {@inheritDoc}
     */
    @Override
    public T get() throws InterruptedException, ExecutionException {
        int st = stat.getAndUpdate(t -> t < 2 ? t + 2 : t);
        if (st <  4) { // 未结束则等待
            synchronized (this) {
                wait ( );
            }
            st = stat.get( );
            if (st <  4) { // 未结束则取消
                cancel(true);
            }
            if (st == 4) { // 取消则抛异常
                throw new CancellationException();
            }
        }

        if (fail != null) {
            throw new ExecutionException(fail);
        }

        return data;
    }

    /**
     * @throws CancellationException {@inheritDoc}
     */
    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        int st = stat.getAndUpdate(t -> t < 2 ? t + 2 : t);
        if (st <  4) { // 未结束则等待
            synchronized (this) {
                wait(unit.convert(timeout, TimeUnit.MILLISECONDS));
            }
            st = stat.get( );
            if (st <  4) { // 未结束则超时
                throw new TimeoutException (/**/);
            }
            if (st == 4) { // 取消则抛异常
                throw new CancellationException();
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

    /**
     * 取消任务
     * @param  mayInterruptIfRunning 为 false 仅能取消未执行的任务
     * @return mayInterruptIfRunning 为 false 则在执行中返回 false
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (mayInterruptIfRunning ) {
            int st = stat.getAndUpdate(t -> t != 4 && t != 5 ? 4 : t); // 取消未结束的
            if (st == 2 || st == 3) { // 等待中
                synchronized (this) {
                    wakeup();
                }
            }
        } else {
            int st = stat.getAndUpdate(t -> t == 0 || t == 2 ? 4 : t); // 取消未开始的
            if (st == 1 || st == 3) { // 运行中
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isDone() {
        return stat.get() == 5;
    }

    @Override
    public boolean isCancelled() {
        return stat.get() == 4;
    }

    /**
     * 任务结束(取消或完成)
     * @return
     */
    public boolean interrupted() {
        return stat.get() >= 4;
    }

    /**
     * 任务开始
     */
    public void init() {
        this.stat.getAndUpdate(t -> t == 0 || t == 2 ? t + 1 : t); // 排队变运行
    }

    /**
     * 任务完成
     */
    public void done() {
        int st = stat.getAndSet(5);
        if (st == 2 || st == 3) {
            // 唤起等待中的主程
            synchronized (this) {
                wakeup();
            }
        }
    }

    /**
     * 任务完成(带结果)
     * @param data
     */
    public void done(T data) {
        this.data = data;
        int st = stat.getAndSet(5);
        if (st == 2 || st == 3) {
            // 唤起等待中的主程
            synchronized (this) {
                wakeup();
            }
        }
    }

    /**
     * 任务失败(带异常)
     * @param ex
     */
    public void fail(Throwable ex) {
        this.fail =  ex ;
        int st = stat.getAndSet(4);
        if (st == 2 || st == 3) {
            // 唤起等待中的主程
            synchronized (this) {
                wakeup();
            }
        }
    }

    private void wakeup() {
        if (solo) {
            notify();
        } else {
            notifyAll();
        }
    }

    /**
     * 快捷异步任务
     * @param task
     * @return
     */
    public static Defer run(Consumer<Defer> task) {
        Defer defer = new Defer( );
        Chore.getInstance().exe(() -> {
            try {
                task.accept(defer);
            } catch (Throwable e ) {
                defer.fail(e);
            }
        });
        return  defer;
    }

    /**
     * 执行异步任务(附带 Core 环境)
     * @param task
     * @return
     */
    public static Defer run(BiConsumer<Defer, Core> task) {
        String name = Core.ACTION_NAME.get()+".run";
        String lang = Core.ACTION_LANG.get();
        String zone = Core.ACTION_ZONE.get();

        Defer defer = new Defer( );
        Chore.getInstance().exe(() -> {
            try {

            Core core = Core.getInstance();
            Core.ACTION_NAME.set(name);
            Core.ACTION_LANG.set(lang);
            Core.ACTION_ZONE.set(zone);
            Core.ACTION_TIME.set(System.currentTimeMillis());

            try {
                task.accept(defer, core);
            } catch (Throwable e ) {
                defer.fail(e);
            } finally {
                core.reset( );
            }

            } catch (Throwable e ) {
                e.printStackTrace( );
            }
        });
        return  defer;
    }

}
