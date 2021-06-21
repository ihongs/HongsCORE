package io.github.ihongs.util.reflex;

import io.github.ihongs.CoreLogger;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
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
public final class Latch {

    private static final ReadWriteLock ST_LOCKR = new ReentrantReadWriteLock();
    private static final ReadWriteLock RW_LOCKR = new ReentrantReadWriteLock();
    private static final Map<String, Locker> ST_LOCKS = new HashMap();
    private static final Map<String, Leader> RW_LOCKS = new HashMap();

    /**
     * 清理, 仅供定时任务
     */
    public static void clean() {
        if (! ST_LOCKS.isEmpty( )
        ||  ! RW_LOCKS.isEmpty()) {
            int  n  =  cleans ( );
            CoreLogger.trace("Cleared {} lock(s)", n);
        } else {
            CoreLogger.trace( "No locks be cleared" );
        }
    }

    /**
     * 清理
     * @return 清理数量
     */
    public static int cleans() {
        long tt = System.currentTimeMillis() - 60000;
        int  ct = 0;
        Lock loxk  ;

        loxk = ST_LOCKR.writeLock();
        loxk.lock();
        try {
            Iterator<Map.Entry<String, Locker>> it = ST_LOCKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String , Locker> et = it.next();
                Locker lock = et.getValue();
                if (lock.cite <= 0 && lock.time <= tt) {
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
            Iterator<Map.Entry<String, Leader>> it = RW_LOCKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String , Leader> et = it.next();
                Leader lock = et.getValue();
                if (lock.cite <= 0 && lock.time <= tt) {
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

        rs.put( "Locker", st );
        rs.put( "Larder", rw );

        Lock loxk  ;

        loxk = ST_LOCKR.readLock();
        loxk.lock();
        try {
            Iterator<Map.Entry<String, Locker>> it = ST_LOCKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String , Locker> et = it.next();
                Locker lock = et.getValue();
                String key = et.getKey();
                st.put(key , lock.cite );
            }
        } finally {
            loxk.unlock();
        }

        loxk = RW_LOCKR.readLock();
        loxk.lock();
        try {
            Iterator<Map.Entry<String, Leader>> it = RW_LOCKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String , Leader> et = it.next();
                Leader lock = et.getValue();
                String key = et.getKey();
                rw.put(key , lock.cite );
            }
        } finally {
            loxk.unlock();
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
    public static Leader getLeader(String key) {
        Lock loxk;
        Leader lock = null;

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
            lock = new Leader(/***/);
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
        return new Reader(getLeader(key));
    }

    /**
     * 获取写锁
     * @param key
     * @return
     */
    public static Writer getWriter(String key) {
        return new Writer(getLeader(key));
    }

    /**
     * 读写锁
     * 对 ReadWriteLock 的封装
     */
    public static final class Leader implements ReadWriteLock {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private long  time = 0;
        private  int  cite = 0;

        private Leader() {} // 避免外部 new

        public void lockr() {
            synchronized (this) {
                cite ++;
            }
            lock.readLock().lock();
        }

        public void unlockr() {
            synchronized (this) {
                cite --;
                time = System.currentTimeMillis();
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
                time = System.currentTimeMillis();
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
        private long  time = 0;
        private  int  cite = 0;

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
                time = System.currentTimeMillis();
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
        private final  Leader lock;

        private Reader(Leader lock) {
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
        private final  Leader lock;

        private Writer(Leader lock) {
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

}
