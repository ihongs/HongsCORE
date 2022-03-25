package io.github.ihongs.util.daemon;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CoreSerial;
import io.github.ihongs.HongsException;

import java.io.File;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 批量任务
 * 此类用于批量执行一些操作, 应用场景同异步任务
 * 但需要间隔一段时间, 或累积到一定数量才会执行
 * 注意: 数量按工作线程计算, 如: 工作线程数2, 缓冲区容量5, 首次执行需要累积到10个才会处理.
 * @author Hongs
 * @param <T> 任务的数据类型
 */
public abstract class Batch<T> extends CoreSerial implements AutoCloseable {

    private transient File back = null;
    public  transient ExecutorService     servs;
    public            BlockingQueue  <T>  tasks;
    public            List<Collection<T>> cache;

    /**
     * @param name      任务集名称, 退出时保存现有任务待下次启动时执行, 为 null 则不保存
     * @param maxTasks  最多容纳的任务数量
     * @param maxServs  最多可用的线程数量
     * @param timeout   间隔此毫秒时间后开始执行
     * @param sizeout   缓冲区长度达此数量后执行
     * @param diverse   是否去重(缓冲类型): true 为 Set, false 为 List
     */
    protected Batch(String name, int maxTasks, int maxServs, int timeout, int sizeout, boolean diverse) {
        servs = Executors.newCachedThreadPool(  );
        tasks = new LinkedBlockingQueue(maxTasks);
        cache = new ArrayList ( maxServs );

        for(int i = 0; i < maxServs; i ++) {
            cache.add(diverse ? new LinkedHashSet() : new ArrayList());
        }

        if (name != null) {
            back  = new File(Core.DATA_PATH
                  + File.separator + "serial"
                  + File.separator + name + ".batch.ser");
            if (back.exists()) {
                try {
                    load(back);
                }
                catch (HongsException ex ) {
                    throw ex.toExemption();
                }
            }
        } else {
            name = this.getClass().getSimpleName();
        }

        for(int i = 0; i < maxServs; i ++) {
            servs.execute(new Btask(this, "CORE-Batch-"+name+"-"+i, cache.get(i), timeout, sizeout));
        }

        //tasks.offer(null); // 放一个空对象促使其执行终止时未执行完的任务
    }

    @Override
    protected void imports() {
        // Nothing to do.
    }

    @Override
    public void close() {
        if (!servs.isShutdown( )) {
            servs.shutdownNow( );
        }

        if (back == null) {
            if (!tasks.isEmpty()) {
                CoreLogger.error("There has {} task(s) not run.", tasks.size());
            }
            return;
        }

        /**
         * 将缓冲区滞留的任务写回 tasks
         * 即使重启后 servs 数量改变也没有关系
         */
        Collection<T> c = new ArrayList();
        for (Collection<T> taskz : cache) {
            c.addAll(taskz);
            taskz.clear ( );
        }
        c.addAll(tasks);
        tasks.clear ( );
        tasks.addAll(c);

        File   file;
        file = back;
        back = null;

        if (!tasks.isEmpty()) {
            try {
                save( file );
                CoreLogger.trace("There has {} task(s) not run, save to '{}'.", tasks.size(), back.getPath());
            } catch (HongsException ex) {
                CoreLogger.error(   ex);
            }
        } else
        if (file.exists()) {
            file.delete();
        }
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

    /**
     * 获取缓冲区任务数量
     * @return
     */
    public int getCacheSize() {
        int m  = 0;
        for(Collection<T> x : cache) {
            m += x.size();
        }
        return m;
    }

    /**
     * 检查缓冲区是否为空
     * @return
     */
    public boolean isEmptyCache() {
        for(Collection<T> x : cache) {
            if (!x.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static class Btask implements Runnable {

        private final Batch batch;
        private final String name;
        private final Collection cache;
        private final int timeout;
        private final int sizeout;

        public Btask(Batch batch, String name, Collection cache, int timeout, int sizeout) {
            this.batch = batch;
            this.name  = name ;
            this.cache = cache;
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
        int m = a.tasks.size()+a.getCacheSize();
        System.out.println("end!!!"+(m>0?m:""));
    }

}
