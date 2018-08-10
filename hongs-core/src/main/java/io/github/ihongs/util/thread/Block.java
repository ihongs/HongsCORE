package io.github.ihongs.util.thread;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 全局资源锁
 *
 * 每隔一段时间自动清理锁, 以及释放可以关闭的对象;
 * 注意: 以下锁均不支持 tryLock/lockInterruptibly.
 *
 * @author Hongs
 */
public final class Block {

    private static final ReadWriteLock ST_LOCKR = new ReentrantReadWriteLock();
    private static final ReadWriteLock RW_LOCKR = new ReentrantReadWriteLock();
    private static final Map<String, Locker> ST_LOCKS = new HashMap();
    private static final Map<String, Larder> RW_LOCKS = new HashMap();

    /**
     * 读写全局计数关闭对象时
     * 务必用此锁限定存取过程
     */
    public  static final Larder CLOSER = new Larder();

    /**
     * 自启一个定时任务,
     * 每隔一段时间清理,
     * 如设为  0 不清理,
     * 默认为 10 分钟
     */
    static {
        long time = Long.parseLong(
                System.getProperty(Block.class.getName()
                        + ".cleans.period" , "600000" ));
        if ( time > 0 ) // 明确设为 0 则不清理
        new Timer(Block.class.getName()+".cleans", true)
        .schedule(new TimerTask() {
            @Override
            public void run() {
                int n;

                n = closes();
                if (Core.DEBUG > 0) {
                    CoreLogger.trace("Closed " + n +" object(s)");
                }

                if (!ST_LOCKS.isEmpty() || !RW_LOCKS.isEmpty()) {
                    n = cleans();
                    if (Core.DEBUG > 0) {
                        CoreLogger.trace("Cleared "+n+" lock(s)");
                    }
                } else {
                    if (Core.DEBUG > 0) {
                        CoreLogger.trace( "No locks be cleared" );
                    }
                }
            }
        }, time, time);
    }

    /**
     * 关闭
     * @return 关闭数量
     */
    public static int closes() {
        Lock loxk = CLOSER.writeLock();
        int  ct = 0;

        loxk.lock();
        try {
            Iterator<Map.Entry<String,Object>> it = Core.GLOBAL_CORE.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> et = it.next();
                Object inst = et.getValue();
                if (inst instanceof Closeable) {
                  Closeable clos = (Closeable) inst;
                if (clos.closeable()) {
                    clos.close();
                    it.remove( );
                    ct ++;
                }
                }
            }
        } finally {
            loxk.unlock();
        }

        return ct;
    }

    /**
     * 清理
     * @return 清理数量
     */
    public static int cleans() {
        Lock loxk;
        int  ct = 0;

        loxk = ST_LOCKR.writeLock();
        loxk.lock();
        try {
            Iterator<Map.Entry<String,Locker>> it = ST_LOCKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Locker> et = it.next();
                Locker lock = et.getValue();
                if (lock.cite <= 0) {
                    it.remove( );
                    ct ++;
                }
            }
        } finally {
            loxk.unlock();
        }

        loxk = RW_LOCKR.writeLock();
        loxk.lock();
        try {
            Iterator<Map.Entry<String,Larder>> it = RW_LOCKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Larder> et = it.next();
                Larder lock = et.getValue();
                if (lock.cite <= 0) {
                    it.remove( );
                    ct ++;
                }
            }
        } finally {
            loxk.unlock();
        }

        return ct;
    }

    /**
     * 统计
     * @return 引用计数
     */
    public static Map counts() {
        Map rs = new HashMap();
        Map st = new HashMap();
        Map rw = new HashMap();

        rs.put("Locker", st);
        Iterator<Map.Entry<String, Locker>> it = ST_LOCKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String , Locker> et = it.next();
            Locker lock = et.getValue();
            String key = et.getKey();
            st.put(key , lock.cite );
        }

        rs.put("Larder", rw);
        Iterator<Map.Entry<String, Larder>> jt = RW_LOCKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String , Larder> et = jt.next();
            Larder lock = et.getValue();
            String key = et.getKey();
            rw.put(key , lock.cite );
        }

        return rs;
    }

    /**
     * 获取基础锁
     * @param key
     * @return
     */
    public static Locker getLocker(String key) {
        Lock loxk;
        Locker lock = null;

        loxk = ST_LOCKR. readLock( );
        loxk.lock();
        try {
            lock = ST_LOCKS.get(key);
        } finally {
            loxk.unlock();
        }
        if (null != lock) {
            return  lock ;
        }

        loxk = ST_LOCKR.writeLock( );
        loxk.lock();
        try {
            lock = new Locker(/***/);
            ST_LOCKS.put(key , lock);
        } finally {
            loxk.unlock();
        }
        return lock;
    }

    /**
     * 获取读写锁
     * @param key
     * @return
     */
    public static Larder getLarder(String key) {
        Lock loxk;
        Larder lock = null;

        loxk = RW_LOCKR. readLock( );
        loxk.lock();
        try {
            lock = RW_LOCKS.get(key);
        } finally {
            loxk.unlock();
        }
        if (null != lock) {
            return  lock ;
        }

        loxk = RW_LOCKR.writeLock( );
        loxk.lock();
        try {
            lock = new Larder(/***/);
            RW_LOCKS.put(key , lock);
        } finally {
            loxk.unlock();
        }
        return lock;
    }

    /**
     * 获取读锁
     * @param key
     * @return
     */
    public static Reader getReader(String key) {
        return new Reader(getLarder(key));
    }

    /**
     * 获取写锁
     * @param key
     * @return
     */
    public static Writer getWriter(String key) {
        return new Writer(getLarder(key));
    }

    /**
     * 读写锁
     * 对 ReadWriteLock 的封装
     */
    public static final class Larder implements ReadWriteLock {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private int cite = 0;

        private Larder() {} // 避免外部 new

        public void lockr() {
            synchronized (this) {
                cite ++;
            }
            lock.readLock().lock();
        }

        public void unlockr() {
            synchronized (this) {
                cite --;
            }
            lock.readLock().unlock();
        }

        public void lockw() {
            synchronized (this) {
                cite ++;
            }
            lock.writeLock().lock();
        }

        public void unlockw() {
            synchronized (this) {
                cite --;
            }
            lock.writeLock().unlock();
        }

        @Override
        public Lock  readLock() {
            return new Reader(this);
        }

        @Override
        public Lock writeLock() {
            return new Writer(this);
        }
    }

    /**
     * 基础锁
     * 对 ReentrantLock 的封装
     */
    public static final class Locker implements Lock {
        private final ReentrantLock lock = new ReentrantLock();
        private int cite = 0;

        private Locker(){} // 避免外部 new

        @Override
        public void lock() {
            synchronized(this) {
                cite ++;
            }
            lock.lock();
        }

        @Override
        public void unlock() {
            synchronized(this) {
                cite --;
            }
            lock.unlock();
        }

        @Override
        public boolean tryLock() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void lockInterruptibly() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

    /**
     * 读锁
     * 对 ReadWriteLock.readLock 的封装
     */
    public static final class Reader implements Lock {
        private final  Larder lock;

        private Reader(Larder lock) {
            this.lock = lock;
        }

        @Override
        public void lock() {
            lock.lockr();
        }

        @Override
        public void unlock() {
            lock.unlockr();
        }

        @Override
        public boolean tryLock() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void lockInterruptibly() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

    /**
     * 写锁
     * 对 ReadWriteLock.writeLock 的封装
     */
    public static final class Writer implements Lock {
        private final  Larder lock;

        private Writer(Larder lock) {
            this.lock = lock;
        }

        @Override
        public void lock() {
            lock.lockw();
        }

        @Override
        public void unlock() {
            lock.unlockw();
        }

        @Override
        public boolean tryLock() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void lockInterruptibly() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

    /**
     * 可询问关闭的容器, 通常关闭后被删除
     */
    public static interface Closeable extends AutoCloseable {

        public boolean closeable();

        @Override
        public void close();

    }

}
