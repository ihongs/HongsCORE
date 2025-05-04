package io.github.ihongs.util.daemon;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.util.Inst;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 守护线程池
 *
 * 系统配置项(default.properties):
 *  core.daemon.pool.size=线程池的容量, 默认为处理器核数 * 2
 *  core.daemon.run.timed=常规定时间隔, 默认为十分钟 (00:10)
 *  core.daemon.run.daily=日常维护时间, 默认每日零点 (00:00)
 *
 * @author hongs
 */
@Core.Singleton
@Core.Soliloquy
public final class Chore implements AutoCloseable {

    private final ThreadGroup TGS = new ThreadGroup ( "CORE-Chore" );
    private final ScheduledExecutorService SES;
    private final int DDP = 86400; // 24 小时
    private final int DDT ;
    private final int DTT ;

    private Chore () {
        CoreConfig  cc = CoreConfig.getInstance("default");
        String dt = cc.getProperty("core.daemon.run.daily", "00:00");
        String tt = cc.getProperty("core.daemon.run.timed", "00:10");
        int    ps = Runtime.getRuntime().availableProcessors() * 2  ;
               ps = cc.getProperty("core.daemon.pool.size", ps > 2 ? ps : 2);
        DateTimeFormatter df = DateTimeFormatter . ofPattern("H:m" );

        try {
            DDT = (int) LocalTime.parse(dt, df).atDate(LocalDate.EPOCH).atZone(ZoneId.of("UTC")).toInstant().getEpochSecond();
        }
        catch (DateTimeParseException e) {
            throw new Error("Wrong format for core.daemon.run.daily '"+dt+"'. It needs to be 'H:mm'");
        }
        if (DDT < 0 || DDT > 86400) {
            throw new Error("Wrong config for core.daemon.run.timed '"+dt+"', must be 0:00 to 23:59");
        }

        try {
            DTT = (int) LocalTime.parse(tt, df).atDate(LocalDate.EPOCH).atZone(ZoneId.of("UTC")).toInstant().getEpochSecond();
        }
        catch (DateTimeParseException e) {
            throw new Error("Wrong format for core.daemon.run.timed '"+tt+"'. It needs to be 'H:mm'");
        }
        if (DTT < 60 || DTT > 3600) {
            throw new Error("Wrong config for core.daemon.run.timed '"+tt+"', must be 0:01 to 1:00" );
        }

        SES = Executors.newScheduledThreadPool(ps, new ThreadFactory() {
            final AtomicInteger a = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread( TGS , r);
                t.setName(TGS.getName() + "-" + a.incrementAndGet());
                t.setDaemon(false);
                return t ;
            }
        });
    }

    public static Chore getInstance() {
        return Core.GLOBAL_CORE.got(Chore.class.getName(), () -> new Chore());
    }

    /**
     * 获取任务容器
     * @return
     */
    public ScheduledExecutorService getExecutor() {
        return SES;
    }

    /**
     * 获取线程分组
     * @return
     */
    public ThreadGroup getGroup() {
        return TGS;
    }

    /**
     * 获取每天执行时间, 单位秒
     * @return
     */
    public int getDailySec() {
        return DDT;
    }

    /**
     * 获取间隔执行时间, 单位秒
     * @return
     */
    public int getTimedSec() {
        return DTT;
    }

    /**
     * 关闭容器
     * 请勿执行, 特供 Core.close 联调
     */
    @Override
    public void close() {
        try {
            if (! SES.isShutdown()) {
                  SES.shutdown  ( );
            if (! SES.awaitTermination(1, TimeUnit.SECONDS)) {
                  SES.shutdownNow();
            }}
        } catch ( InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 异步任务
     * @param task
     */
    public void exe(Runnable task) {
        if (4 == (4 & Core.DEBUG)) {
            String name = task.getClass().getName();
            CoreLogger.trace ( "Will run " + name );
        }

        SES.execute(task);
    }

    /**
     * 异步任务
     * @param task
     * @return
     */
    public Future run(Runnable task) {
        if (4 == (4 & Core.DEBUG)) {
            String name = task.getClass().getName();
            CoreLogger.trace ( "Will run " + name );
        }

        return SES.submit(task);
    }

    /**
     * 延时任务
     * @param task
     * @param delay 延迟秒数
     * @return
     */
    public ScheduledFuture run(Runnable task, int delay) {
        if (4 == (4 & Core.DEBUG)) {
            String time = Inst.format(delay * 1000 + System.currentTimeMillis(), "MM-dd HH:mm:ss", Locale.getDefault(), ZoneId.systemDefault());
            String name = task.getClass().getName();
            CoreLogger.trace("Will run " + name + " at " + time);
        }

        return SES.schedule(task , delay, TimeUnit.SECONDS);
    }

    /**
     * 定时任务
     * @param task
     * @param delay 延迟秒数
     * @param perio 间隔秒数, 从每次开始时间算
     * @return
     */
    public ScheduledFuture run(Runnable task, int delay, int perio) {
        if (4 == (4 & Core.DEBUG)) {
            String time = Inst.format(delay * 1000 + System.currentTimeMillis(), "MM-dd HH:mm:ss", Locale.getDefault(), ZoneId.systemDefault());
            String timr = Inst.phrase(perio * 1000);
            String name = task.getClass().getName();
            CoreLogger.trace("Will run " + name + " at " + time + ", interval: " + timr);
        }

        return SES.scheduleAtFixedRate(task , delay, perio, TimeUnit.SECONDS);
    }

    /**
     * 定时任务
     * @param task
     * @param delay 延迟秒数
     * @param perio 间隔秒数, 从上次结束时间算
     * @return
     */
    public ScheduledFuture ran(Runnable task, int delay, int perio) {
        if (4 == (4 & Core.DEBUG)) {
            String time = Inst.format(delay * 1000 + System.currentTimeMillis(), "MM-dd HH:mm:ss", Locale.getDefault(), ZoneId.systemDefault());
            String timr = Inst.phrase(perio * 1000);
            String name = task.getClass().getName();
            CoreLogger.trace("Will run " + name + " at " + time + ", postpone: " + timr);
        }

        return SES.scheduleWithFixedDelay(task , delay, perio, TimeUnit.SECONDS);
    }

    /**
     * 每日任务
     * @param task
     * @param hour 几点(0-23)
     * @param min  几分
     * @param sec  几秒
     * @param zone 时区
     * @return
     */
    public ScheduledFuture runDaily(Runnable task, int hour, int min, int sec, ZoneId zone) {
        // 计算延时, 指定时区
        LocalDateTime cal0 = LocalDateTime.now(zone);
        LocalDateTime cal1 = LocalDateTime.of (cal0.getYear(), cal0.getMonth(), cal0.getDayOfMonth(), hour, min, sec);
        if (cal1.isBefore( cal0 ) ) {
            cal1 = cal1.plusDays(1);
        }
        int ddt  = (int) Duration.between(cal0,cal1).getSeconds() + 1;

        return run (task, ddt, DDP);
    }

    /**
     * 日常维护任务
     * 使用系统默认的时区
     * @param task
     * @param hour 几点(0-23)
     * @param min  几分
     * @param sec  几秒
     * @return
     */
    public ScheduledFuture runDaily(Runnable task, int hour, int min, int sec) {
        return runDaily(task, hour, min, sec, ZoneId.systemDefault());
    }

    /**
     * 日常维护任务
     * 默认每天零点时运行
     * 或在 default.properties 设置 core.daemon.run.daily=HH:mm
     * @param task
     * @return
     */
    public ScheduledFuture runDaily(Runnable task) {
        return runDaily(task, DDT / 3600, DDT % 3600 / 60, DDT % 60 );
    }

    /**
     * 基础定时任务
     * 默认每隔十分钟运行
     * 或在 default.properties 设置 core.daemon.run.timed=HH:mm
     * @param task
     * @return
     */
    public ScheduledFuture runTimed(Runnable task) {
        return ran (task, DTT, DTT);
    }

    /**
     * 异步任务, 可等待, 可中止
     * @param task
     * @return
     */
    public Defer defer(Consumer<Defer> task) {
        Defer def = new Defer(false);
        this.exe(() -> {
            def.begin();
            task.accept(def);
            def.done ();
        });
        return def;
    }

    /**
     * 延迟等待工具
     * @param <T>
     */
    public static class Defer<T> implements Future<T> {

        // 0 待运行 1 运行中 2 主程等待 3 中断 4 完成
        final AtomicInteger stat;

        private  Throwable  fail;

        private  T data ;

        public Defer() {
            this(true);
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
            }
            if (fail != null) {
                throw new ExecutionException(fail);
            }
            return data;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (! mayInterruptIfRunning) {
                int st = stat.get();
                if ( st < 1 ) {
                    stat.set(3);
                    return true;
                }
            } else {
                int st = stat.get();
                if ( st < 2 ) {
                    stat.set(3);
                    return true;
                } else
                if ( st < 3 ) {
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
         * 任务取消或完成
         * @return
         */
        public boolean interrupted() {
            return stat.get() >= 3;
        }

        /**
         * 任务开始
         */
        public void begin() {
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

    /**
     * 任务执行包装
     * 规避异常导致后续任务不再执行
     * 将截获并记录未处理的内部异常
     */
    public static class Actor implements Runnable {

        private final Runnable R;
        private final String N;
        private final String L;
        private final String Z;

        /**
         * @param task 任务
         * @param name 对应 Core.ACTION_NAME
         */
        public Actor(Runnable task, String name) {
            R= task;
            N= name;
            L= Core.ACTION_LANG.get();
            Z= Core.ACTION_ZONE.get();
        }

        /**
         * @param task 任务
         */
        public Actor(Runnable task) {
            this(task, Core.ACTION_NAME.get() + ".run");
        }

        @Override
        public void run() {
            try {

            Core c = Core.getInstance();
            Core.ACTION_NAME.set(N);
            Core.ACTION_LANG.set(L);
            Core.ACTION_ZONE.set(Z);
            Core.ACTION_TIME.set(System.currentTimeMillis());

            try {
                R.run ( );
            } catch ( Throwable e ) {
                CoreLogger.error(e);
            } finally {
                c.reset();
            }

            } catch ( Throwable e ) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 最小任务间隔
     * 规避定时任务执行太久致后续连续触发
     * 注意: 此封装的实例不得多点并行执行
     */
    public static class Least implements Runnable {

        private final Runnable R;
        private final    long  L;
        private volatile long  T;

        /**
         * @param task
         * @param least 最小间隔, 低于此秒数则略过
         */
        public Least(Runnable task, int least) {
            L= least * 1000L; // 转为毫秒
            R= task;
            T= 0L;
        }

        @Override
        public void run() {
            long t = System.currentTimeMillis();
            long l = t - T;
            this.T = t ;
            if ( L < l )
                 R.run();
        }

    }

}
