package io.github.ihongs.util.reflex;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CoreSerial;
import io.github.ihongs.HongsException;

import java.io.File;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 异步任务
 * 将任务放入后台, 当前线程可以继续去做其它事情
 * 适合那些不需要立即返回的操作, 如邮件发送程序
 * @author Hongs
 * @param <T> 任务的数据类型
 */
public abstract class Async<T> extends CoreSerial implements AutoCloseable {

    private transient File back = null;
    public  transient ExecutorService  servs;
    public            BlockingQueue<T> tasks;

    /**
     * @param name      任务集名称, 退出时保存现有任务待下次启动时执行, 为 null 则不保存
     * @param maxTasks  最多容纳的任务数量
     * @param maxServs  最多可用的线程数量
     * @throws io.github.ihongs.HongsException
     */
    protected Async(String name, int maxTasks, int maxServs) throws HongsException {
        servs = Executors.newCachedThreadPool(  );
        tasks = new LinkedBlockingQueue(maxTasks);

        if (name != null) {
            back  = new File(Core.DATA_PATH
                  + File.separator + "serial"
                  + File.separator + name + ".async.ser");
            if (back.exists()) {
                load ( back );
            }
        }

        for(int i = 0; i < maxServs; i ++) {
            servs.execute(new Atask(this, "async:"+name+"["+i+"]"));
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
                CoreLogger.error("There has "+ tasks.size() +" task(s) not run.");
            }
            return;
        }

        File   file;
        file = back;
        back = null;

        if (!tasks.isEmpty()) {
            try {
                save( file );
                CoreLogger.trace("There has "+ tasks.size() +" task(s) not run, save to '"+file.getPath()+"'.");
            } catch (HongsException ex) {
                CoreLogger.error(   ex);
            }
        } else
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
           this.close(   );
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
                    if (0 != Core.DEBUG && 8 != (8 & Core.DEBUG)) {
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
     * @throws io.github.ihongs.HongsException
     */
    public static void main(String[] args) throws IOException, HongsException {
        io.github.ihongs.cmdlet.CmdletRunner.init(args);

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
        int m = a.tasks.size();
        System.out.println("end!!!"+(m>0?m:""));
    }

}
