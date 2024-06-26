package io.github.ihongs.util.daemon;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CoreSerial;
import io.github.ihongs.CruxException;

import java.io.File;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步任务
 * 将任务放入后台, 当前线程可以继续去做其它事情
 * 适合那些不需要立即返回的操作, 如邮件发送程序
 * @author Hongs
 * @param <T> 任务的数据类型
 */
public abstract class Async<T> extends CoreSerial implements AutoCloseable {

    private transient File  back = null;

    public  transient final ThreadGroup      group;
    public  transient final ExecutorService  servs;
    public  transient final BlockingQueue<T> tasks;

    /**
     * @param name      任务集名称, 退出时保存现有任务待下次启动时执行, 为 null 则不保存
     * @param maxTasks  最多容纳的任务数量
     * @param maxServs  最多可用的线程数量
     * @param daemon    是否设置为守护线程
     */
    protected Async(String name, int maxTasks, int maxServs, final boolean daemon) {
        final String code = name != null ? name : this.getClass().getName();

        group = new ThreadGroup ( "CORE-Async-" + code );
        servs = Executors.newCachedThreadPool(new ThreadFactory() {
            final AtomicInteger a = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(group, r);
                t.setName(group.getName()+"-"+a.incrementAndGet());
                t.setDaemon(daemon);
                return t ;
            }
        });

        tasks = new LinkedBlockingQueue(maxTasks);

        if (name != null) {
            back  = new File(Core.DATA_PATH
                  + File.separator + "serial"
                  + File.separator + name + ".async.ser");
            if (back.exists()) {
                try {
                    load(back);
                } catch ( CruxException e) {
                    throw e.toExemption( );
                }
            }
        }

        for(int i = 0; i < maxServs; i ++) {
            servs.execute(new Atask(this, code));
        }
    }

    /**
     * @param name      任务集名称, 退出时保存现有任务待下次启动时执行, 为 null 则不保存
     * @param maxTasks  最多容纳的任务数量
     * @param maxServs  最多可用的线程数量
     */
    protected Async(String name, int maxTasks, int maxServs) {
        this(name, maxTasks, maxServs, false);
    }

    @Override
    protected void imports() {
        // Nothing to do.
    }

    @Override
    protected void load(Object data) {
        tasks.addAll((Collection<T>) data);
    }

    @Override
    protected Object save() {
        return tasks;
    }

    @Override
    public void close() {
        if (!servs.isShutdown()) {
            servs.shutdownNow();
        }

        if (back == null) {
            if (!isEmpty()) {
                CoreLogger.error("There has {} task(s) not run.", size());
            }
            return;
        }

        try {
            File   file;
            file = back;
            back = null;

            if (!isEmpty()) {
                save(file);
                CoreLogger.trace("There has {} task(s) not run, save to '{}'.", size(), file.getPath());
            } else
            if (file.exists()) {
                file.delete();
            }
        }
        catch (CruxException ex) {
            CoreLogger.error(ex);
        }
    }

    /**
     * 检查是否为空
     * @return
     */
    public boolean isEmpty() {
        return tasks.isEmpty();
    }

    /**
     * 获取任务数量
     * @return
     */
    public int size() {
        return tasks.size();
    }

    /**
     * 添加一个任务
     * @param data
     * @throws IllegalStateException 当队列满时
     */
    public void add(T data) {
          tasks.add(  data);
    }

    /**
     * 执行一个任务
     * @param data
     */
    abstract public void run(T data);

    private static class Atask implements Runnable {

        private final Async async;
        private final String name;

        public Atask(Async async, String name) {
            this.async = async;
            this.name  = name ;
        }

        @Override
        public void run() {
            // 方便日志中识别是哪个队列
            Core.ACTION_NAME.set(name);

            Object  data;
            while ( true) {
                try {
                    data = async.tasks.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (null  ==  data) {
                    continue;
                }

                try {
                    async.run(data);
                } catch (Throwable e) {
                    String n = async.getClass().getName();
                    if (4 == (4 & Core.DEBUG)) {
                        ByteArrayOutputStream b;
                           b = new  ByteArrayOutputStream(  );
                        e.printStackTrace(new PrintStream(b));
                        CoreLogger.getLogger(n).error(b. toString ());
                    } else {
                        CoreLogger.getLogger(n).error(e.getMessage());
                    }
                }
            }
        }

    }

    /**
     * 测试方法
     * @param args
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {
        io.github.ihongs.combat.CombatRunner.init(args);

        Async a = new Async<String>("test", Integer.MAX_VALUE, 2) {

            @Override
            public void run(String x) {
                System.out.println("> "+Thread.currentThread().getId()+"\t"+x);
            }

        };

        java.util.Scanner input = new java.util.Scanner(System.in);
        System.out.println("input:");
        while (true ) {
            String x = input.nextLine().trim( );
            System.out.println("- "+Thread.currentThread().getId()+"\t"+x);
            if ("".equals(x)) {
                a.close();
                break ;
            }
            a.add( x );
        }
        int m = a.size ();
        System.out.println("end!!!"+(m>0?m:""));
    }

}
