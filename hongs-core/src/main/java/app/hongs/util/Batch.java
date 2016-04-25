package app.hongs.util;

import app.hongs.Core;
import app.hongs.CoreLogger;
import app.hongs.CoreSerial;
import app.hongs.HongsException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 批量任务
 * 此类用于批量执行一些操作, 应用场景同异步任务
 * 不同点在于仅当数量或时间累积到一定量时才执行
 * 注意: 数量和时间按工作线程计算.
 * 例如: 工作线程数2, 缓冲区容量5,
 * 首次执行需要累积到10个才会处理.
 * @author Hongs
 * @param <T> 任务的数据类型
 */
public abstract class Batch<T> extends CoreSerial implements Core.Destroy {

    private File back = null;
    public  BlockingQueue<T> tasks;
    public transient ExecutorService/**/ servs;
    public transient List<Collection<T>> cache;

    /**
     * @param name      任务集名称, 退出时保存现有任务待下次启动时执行, 为 null 则不保存
     * @param maxTasks  最多容纳的任务数量
     * @param maxServs  最多可用的线程数量
     * @param sizeout   缓冲区长度达到此数量后执行
     * @param timeout   此秒后还没有新的进来则执行
     * @param deduplicate 是否去重(缓冲类型): true 为 Set, false 为 List
     * @throws HongsException
     */
    protected Batch(String name, int maxTasks, int maxServs, int sizeout, int timeout, boolean deduplicate) throws HongsException {
        servs = Executors.newCachedThreadPool(  );
        tasks = new LinkedBlockingQueue(maxTasks);
        cache = new ArrayList();

        for(int i = 0; i < maxServs; i ++) {
            cache.add(deduplicate ? new LinkedHashSet() : new ArrayList());
        }

        if (name != null) {
            back  = new File(Core.DATA_PATH
                  + File.separator + "serial"
                  + File.separator + name + ".batch.ser");
            if (back.exists()) {
                load ( back );
            }
        }

        for(int i = 0; i < maxServs; i ++) {
            servs.execute(new Btask(this, cache.get(i), sizeout, timeout));
        }

        //tasks.offer(null); // 放一个空对象促使其执行终止时未执行完的任务
    }

    @Override
    protected void imports() {
        // Nothing to do.
    }

    @Override
    public void destroy() {
        if (!servs.isShutdown( )) {
            servs.shutdownNow( );
        }

        if (back == null) {
            if (!tasks.isEmpty()) {
                CoreLogger.error("There has "+ tasks.size() +" task(s) not run in '"+back.getPath()+"'.");
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
                save ( file );
            } catch (HongsException ex) {
                CoreLogger.error  ( ex);
            }
        } else
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
           this.destroy( );
        } finally {
          super.finalize();
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
        private final int sizeout;
        private final int timeout;
        private final Collection cache;

        public Btask(Batch batch, Collection cache, int sizeout, int timeout) {
            this.batch = batch;
            this.cache = cache;
            this.sizeout = sizeout;
            this.timeout = timeout;
        }

        @Override
        public void run() {
            Object  data;
            while ( true) {
                try {
                    data = batch.tasks.poll(timeout, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (null  !=  data) {
                    cache.add(data);
                    if (cache.size( ) >= sizeout) {
                        try {
                            batch.run(cache);
                        } catch (Error | Exception e) {
                            ByteArrayOutputStream b = new ByteArrayOutputStream();
                            e.printStackTrace(new PrintStream(b));
                            String n = batch.getClass().getName();
                            String s = /**/b.toString();
                            CoreLogger.getLogger(n).error(s);
                        }
                        cache.clear();
                    }
                } else {
                    if (cache.isEmpty() == false) {
                        try {
                            batch.run(cache);
                        } catch (Error | Exception e) {
                            ByteArrayOutputStream b = new ByteArrayOutputStream();
                            e.printStackTrace(new PrintStream(b));
                            String n = batch.getClass().getName();
                            String s = /**/b.toString();
                            CoreLogger.getLogger(n).error(s);
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
     * @throws app.hongs.HongsException
     */
    public static void main(String[] args) throws IOException, HongsException {
        app.hongs.cmdlet.CmdletRunner.init(args);

        Batch a = new Batch<String>("test", Integer.MAX_VALUE, 2, 5, 10, false) {

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
                a.destroy( );
                break;
            }
            a.add( x);
        }
        int m = a.tasks.size()+a.getCacheSize();
        System.out.println("end!!!"+(m>0?m:""));
    }

}
