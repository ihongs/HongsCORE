package io.github.ihongs.util.daemon;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CoreSerial;
import io.github.ihongs.CruxException;

import java.io.File;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 批量任务
 * 此类用于批量执行一些操作, 应用场景同异步任务
 * 但需要间隔一段时间, 或累积到一定数量才会执行
 * 注意: 数量按工作线程计算, 如: 工作线程数2, 缓冲区容量5, 首次执行需要累积到10个才会处理.
 * @author Hongs
 * @param <T> 任务的数据类型
 */
public abstract class Batch<T> extends CoreSerial implements AutoCloseable {

    private transient File  back = null;

    public  transient final ThreadGroup      group;
    public  transient final ExecutorService  servs;
    public  transient final BlockingQueue<T> tasks;
    public  transient final Collection<T>[ ] cache;

    /**
     * @param name      任务集名称, 退出时保存现有任务待下次启动时执行, 为 null 则不保存
     * @param maxTasks  最多容纳的任务数量
     * @param maxServs  最多可用的线程数量
     * @param timeout   间隔此毫秒时间后开始执行
     * @param sizeout   缓冲区长度达此数量后执行
     * @param diverse   是否去重(缓冲类型): true 为 Set, false 为 List
     * @param daemon    是否设置为守护线程
     */
    protected Batch(String name, int maxTasks, int maxServs, int timeout, int sizeout, boolean diverse, final boolean daemon) {
        final String code = name != null ? name : this.getClass().getSimpleName( );

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

        // 缓冲区
        if (diverse) {
            cache  =  new LinkedHashSet[maxServs];
            for ( int i = 0; i < maxServs; i ++ ) {
                cache[i] = new LinkedHashSet(sizeout);
            }
        } else {
            cache  =  new  ArrayList   [maxServs];
            for ( int i = 0; i < maxServs; i ++ ) {
                cache[i] = new  ArrayList   (sizeout);
            }
        }

        if (name != null) {
            back  = new File(Core.DATA_PATH
                  + File.separator + "serial"
                  + File.separator + name + ".batch.ser");
            if (back.exists()) {
                try {
                    load(back);
                } catch ( CruxException e) {
                    throw e.toExemption( );
                }
            }
        }

        for(int i = 0; i < maxServs; i ++) {
            servs.execute(new Btask(this, code, i, timeout, sizeout));
        }
    }

    /**
     * @param name      任务集名称, 退出时保存现有任务待下次启动时执行, 为 null 则不保存
     * @param maxTasks  最多容纳的任务数量
     * @param maxServs  最多可用的线程数量
     * @param timeout   间隔此毫秒时间后开始执行
     * @param sizeout   缓冲区长度达此数量后执行
     * @param diverse   是否去重(缓冲类型): true 为 Set, false 为 List
     */
    protected Batch(String name, int maxTasks, int maxServs, int timeout, int sizeout, boolean diverse) {
        this(name, maxTasks, maxServs, timeout, sizeout, diverse, false);
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
        // 将缓冲区未来得及处理的写回队列
        Collection<T> c = new ArrayList(size());
        for(Collection<T> taskz : cache) {
            c.addAll(taskz);
        }   c.addAll(tasks);
        return c;
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
                CoreLogger.trace("There has {} task(s) not run, save to '{}'.", size(), back.getPath());
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
     * 检查是否为空 (含缓冲区)
     * @return
     */
    public boolean isEmpty() {
        for(Collection<T> x : cache) {
            if (!x.isEmpty()) {
                return false;
            }
        }
        return  tasks.isEmpty();
    }

    /**
     * 获取任务数量 (含缓冲区)
     * @return
     */
    public int size() {
        int m = tasks.size();
        for(Collection<T> x : cache) {
            m = m + x.size();
        }
        return  m;
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
     * 执行一批任务
     * @param list
     */
    abstract public void run(Collection<T> list);

    private static class Btask implements Runnable {

        private final Collection cache;
        private final Batch batch;
        private final String name;
        private final int timeout;
        private final int sizeout;

        public Btask(Batch batch, String name, int index, int timeout, int sizeout) {
            this.cache = batch.cache[index];
            this.batch = batch;
            this.name  = name ;
            this.timeout = timeout;
            this.sizeout = sizeout;
        }

        @Override
        public void run() {
            // 方便日志中识别是哪个队列
            Core.ACTION_NAME.set(name);

            Object  data;
            while ( true) {
                try {
                    data = batch.tasks.poll(timeout, TimeUnit.MICROSECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (null  !=  data) {
                    cache.add(data);
                    if (cache.size( ) >= sizeout) {
                        try {
                            batch.run(cache);
                        } catch (Throwable e) {
                            String n = batch.getClass().getName();
                            if (4 == (4 & Core.DEBUG)) {
                                ByteArrayOutputStream b;
                                   b = new  ByteArrayOutputStream(  );
                                e.printStackTrace(new PrintStream(b));
                                CoreLogger.getLogger(n).error(b. toString ());
                            } else {
                                CoreLogger.getLogger(n).error(e.getMessage());
                            }
                        }
                        cache.clear();
                    }
                } else {
                    if (cache.isEmpty() == false) {
                        try {
                            batch.run(cache);
                        } catch (Throwable e) {
                            String n = batch.getClass().getName();
                            if (4 == (4 & Core.DEBUG)) {
                                ByteArrayOutputStream b;
                                   b = new  ByteArrayOutputStream(  );
                                e.printStackTrace(new PrintStream(b));
                                CoreLogger.getLogger(n).error(b. toString ());
                            } else {
                                CoreLogger.getLogger(n).error(e.getMessage());
                            }
                        }
                        cache.clear();
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

        Batch a = new Batch<String>("test", Integer.MAX_VALUE, 2, 10000, 5, false) {

            @Override
            public void run(Collection<String> x) {
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
