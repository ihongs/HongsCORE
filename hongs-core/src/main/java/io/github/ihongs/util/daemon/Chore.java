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
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 守护线程池
 *
 * 系统配置项(default.properties):
 *  core.daemon.pool.size=线程池的容量, 默认为处理器核数 - 1
 *  core.daemon.run.timed=常规定时间隔, 默认为十分钟 (00:10)
 *  core.daemon.run.daily=日常维护时间, 默认每日零点 (00:00)
 *
 * @author hongs
 */
public final class Chore implements AutoCloseable, Core.Singleton, Core.Soliloquy {

    private final ScheduledExecutorService SES;
    private final int DDP = 86400; // 24 小时
    private final int DDT ;
    private final int DTT ;

    private Chore () {
        CoreConfig  cc = CoreConfig.getInstance("default");
        String tt = cc.getProperty("core.daemon.run.timed", "00:10");
        String dt = cc.getProperty("core.daemon.run.daily", "00:00");
        int    ps = Runtime.getRuntime().availableProcessors() - 1  ;
               ps = cc.getProperty("core.daemon.pool.size", ps > 1 ? ps : 1);
        DateTimeFormatter df = DateTimeFormatter . ofPattern("H:m" );

        try {
            DDT = (int) LocalTime.parse(dt, df).atDate(LocalDate.EPOCH).atZone(Core.getZoneId()).toInstant().toEpochMilli() / 1000;
        }
        catch (DateTimeParseException e) {
            throw new Error("Wrong format for core.daemon.run.daily '"+dt+"'. It needs to be 'H:mm'");
        }

        try {
            DTT = (int) LocalTime.parse(tt, df).atDate(LocalDate.EPOCH).atZone(ZoneId.of("UTC")).toInstant().toEpochMilli() / 1000;
        }
        catch (DateTimeParseException e) {
            throw new Error("Wrong format for core.daemon.run.timed '"+tt+"'. It needs to be 'H:mm'");
        }
        if (DTT < 60 || DTT > 28800) {
            throw new Error("Wrong config for core.daemon.run.timed '"+tt+"', must be 0:01 to 8:00" );
        }

        SES = Executors.newScheduledThreadPool(ps, new ThreadFactory() {
            final AtomicInteger a = new AtomicInteger( 0 );
            @Override
            public Thread newThread(Runnable r) {
                Thread t ;
                t = new Thread(r);
                t.setDaemon(true);
                t.setName("CORE-Chore-" + a.incrementAndGet());
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
     * 请勿执行
     * @deprecated 特供 Core.close 联调
     */
    @Override
    public void close() {
        SES.shutdown ();
        try {
            if (! SES.isTerminated()) {
            if (! SES.awaitTermination(1, TimeUnit.MINUTES)) {
                System.err.println("CORE-Chore is timeout!");
            }}
        } catch ( InterruptedException e) {
            throw new RuntimeException(e) ;
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
     * 同 run(task, delay, perio, false)
     * @param task
     * @param delay 延迟秒数
     * @param perio 间隔秒数
     * @return
     */
    public ScheduledFuture run(Runnable task, int delay, int perio) {
        return run(task, delay, perio, false);
    }

    /**
     * 定时任务
     * @param task
     * @param delay 延迟秒数
     * @param perio 间隔秒数
     * @param rate  true 从开始执行算间隔, false 从执行完成算间隔
     * @return
     */
    public ScheduledFuture run(Runnable task, int delay, int perio, boolean rate) {
        if (4 == (4 & Core.DEBUG)) {
            String time = Inst.format(delay * 1000 + System.currentTimeMillis(), "MM-dd HH:mm:ss", Locale.getDefault(), ZoneId.systemDefault());
            String timr = Inst.phrase(perio * 1000);
            String name = task.getClass().getName();
            CoreLogger.trace("Will run " + name + " at " + time + (rate ? ", rate: " : ", delay: ") + timr);
        }

        return rate
             ? SES.scheduleAtFixedRate   (task , delay, perio, TimeUnit.SECONDS)
             : SES.scheduleWithFixedDelay(task , delay, perio, TimeUnit.SECONDS);
    }

    /**
     * 每日任务
     * 当前时区
     * @param task
     * @param hour 几点(0-23)
     * @param min  几分
     * @param sec  几秒
     * @return
     */
    public ScheduledFuture runDaily(Runnable task, int hour, int min, int sec) {
        return runDaily(task, hour, min, sec, Core.getZoneId());
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
        int ddt = (int) Duration.between(cal1, cal0).getSeconds() + 1;

        return run(task, ddt, DDP, true );
    }

    /**
     * 日常维护任务
     * 默认每天零点时运行
     * 或在 default.properties 设置 core.daemon.run.daily=HH:mm
     * @param task
     * @return
     */
    public ScheduledFuture runDaily(Runnable task) {
        // 计算延时, 默认时区
        LocalDateTime cal0 = LocalDateTime.now();
        LocalDateTime cal1 = LocalDateTime.of (cal0.getYear(), cal0.getMonth(), cal0.getDayOfMonth(), 0, 0, 0);
        if (cal1.isBefore( cal0 ) ) {
            cal1 = cal1.plusDays(1);
        }
        int ddt = (int) Duration.between(cal1, cal0).getSeconds() + 1;

        return run(task, ddt, DDP, true );
    }

    /**
     * 基础定时任务
     * 默认每隔十分钟运行
     * 或在 default.properties 设置 core.daemon.run.timed=HH:mm
     * @param task
     * @return
     */
    public ScheduledFuture runTimed(Runnable task) {
        return run(task, DTT, DTT, false);
    }

}
