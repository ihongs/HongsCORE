package io.github.ihongs.util.daemon;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
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

        SimpleDateFormat sdf = new SimpleDateFormat("H:m");

        sdf.setTimeZone(TimeZone.getDefault (/* Local */));
        try {
            DDT = (int) ( sdf.parse(dt).getTime() / 1000 );
        }
        catch (ParseException e) {
            throw new Error("Wrong format for core.daemon.run.daily '"+dt+"'. It needs to be 'H:mm'");
        }

        sdf.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        try {
            DTT = (int) ( sdf.parse(tt).getTime() / 1000 );
        }
        catch (ParseException e) {
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
        return Core.GLOBAL_CORE.get(Chore.class.getName(), () -> new Chore());
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
            CoreLogger.trace("Will exe {}" , name );
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
            CoreLogger.trace("Will run {}" , name );
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
            Date   date = new Date(System.currentTimeMillis()+ delay * 1000L);
            String time = new SimpleDateFormat("MM-dd HH:mm:ss").format(date);
            String name = task.getClass().getName();
            CoreLogger.trace("Will run {} at {}", name, time);
        }

        return SES.schedule(task , delay, TimeUnit.SECONDS);
    }

    /**
     * 定时任务
     * @param task
     * @param delay 延迟秒数
     * @param perio 间隔秒数
     * @return 
     */
    public ScheduledFuture run(Runnable task, int delay, int perio) {
        if (4 == (4 & Core.DEBUG)) {
            Date   date = new Date(System.currentTimeMillis()+ delay * 1000L);
            String time = new SimpleDateFormat("MM-dd HH:mm:ss").format(date);
            String name = task.getClass().getName();
            CoreLogger.trace("Will run {} at {}", name, time);
        }

        return SES.scheduleAtFixedRate(task , delay, perio, TimeUnit.SECONDS);
    }

    /**
     * 每日任务
     * @param task
     * @param hour 几点(0-23)
     * @param min  几分
     * @param sec  几秒
     * @return 
     */
    public ScheduledFuture runDaily(Runnable task, int hour, int min, int sec) {
        // 计算延时
        Calendar cal0 = Calendar.getInstance( );
        Calendar cal1 = Calendar.getInstance( );
        cal0.setTimeZone(TimeZone.getDefault());
        cal1.setTimeZone(TimeZone.getDefault());
        cal1.set(Calendar.HOUR_OF_DAY, hour);
        cal1.set(Calendar.MINUTE     , min );
        cal1.set(Calendar.SECOND     , sec );
        cal1.set(Calendar.MILLISECOND,  0  );
        cal1.set(Calendar.MONTH, cal0.get(Calendar.MONTH) );
        cal1.set(Calendar.YEAR , cal0.get(Calendar.YEAR ) );
        cal1.set(Calendar.DATE , cal0.get(Calendar.DATE ) );
        if ( cal1.before(cal0) ) cal1.add(Calendar.DATE, 1);
        int ddt = (int) (cal1.getTimeInMillis() - cal0.getTimeInMillis()) / 1000 + 1;

        return run(task, ddt, DDP);
    }

    /**
     * 日常维护任务
     * 默认每天零点时运行
     * 或在 default.properties 设置 core.daemon.run.daily=HH:mm
     * @param task
     * @return 
     */
    public ScheduledFuture runDaily(Runnable task) {
        // 计算延时
        Calendar cal0 = Calendar.getInstance( );
        Calendar cal1 = Calendar.getInstance( );
        cal0.setTimeZone(TimeZone.getDefault());
        cal1.setTimeZone(TimeZone.getDefault());
        cal1.setTimeInMillis( DDT * 1000L ); // 起始时间
        cal1.set(Calendar.MONTH, cal0.get(Calendar.MONTH) );
        cal1.set(Calendar.YEAR , cal0.get(Calendar.YEAR ) );
        cal1.set(Calendar.DATE , cal0.get(Calendar.DATE ) );
        if ( cal1.before(cal0) ) cal1.add(Calendar.DATE, 1);
        int ddt = (int) (cal1.getTimeInMillis() - cal0.getTimeInMillis()) / 1000 + 1;

        return run(task, ddt, DDP);
    }

    /**
     * 基础定时任务
     * 默认每隔十分钟运行
     * 或在 default.properties 设置 core.daemon.run.timed=HH:mm
     * @param task
     * @return 
     */
    public ScheduledFuture runTimed(Runnable task) {
        return run(task, DTT, DTT);
    }

}
