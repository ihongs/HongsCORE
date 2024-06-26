package io.github.ihongs.util.daemon;

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
 * 如果需要用完立即清除锁, 请用 delock 替代 unlock
 *
 * 异常代码:
 * Ex860=程序中断
 * Ex861=保存超时
 * Ex862=连接超时
 *
 * @author Hongs
 */
public final class Gate {

    private static final ReadWriteLock ST_LOCKR = new ReentrantReadWriteLock();
    private static final ReadWriteLock RW_LOCKR = new ReentrantReadWriteLock();
    private static final Map<String, Locker> ST_LOCKS = new HashMap();
    private static final Map<String, Leader> RW_LOCKS = new HashMap();

    private Gate() {}

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
        long tt = System.currentTimeMillis() - Chore.getInstance().getTimedSec() * 1000; // 超一个周期未使用即释放
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
        Map  st, rw, rs;
        Lock loxk  ;

        loxk = ST_LOCKR.readLock();
        loxk.lock();
        st = new HashMap(ST_LOCKS.size());
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
        rw = new HashMap(RW_LOCKS.size());
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

        rs = new HashMap(2);
        rs.put("Locker",st);
        rs.put("Larder",rw);

        return rs;
    }

    /**
     * 移除基础锁
     * @param key
     * @return
     */
    public static Locker delLocker(String key) {
        Lock loxk = ST_LOCKR.writeLock();
        loxk.lock();
        try {
            return  ST_LOCKS.remove(key);
        } finally {
            loxk.unlock();
        }
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
            if (null != lock) {
                return  lock ;
            }
        } finally {
            loxk.unlock();
        }

        loxk = ST_LOCKR.writeLock( );
        loxk.lock();
        try {
            lock = ST_LOCKS.get(key);
            if (null != lock) {
                return  lock ;
            }

            lock = new Locker( key );
            ST_LOCKS.put(key , lock);
        } finally {
            loxk.unlock();
        }
        return lock;
    }

    /**
     * 移除基础锁
     * @param key
     * @return
     */
    public static Leader delLeader(String key) {
        Lock loxk = RW_LOCKR.writeLock();
        loxk.lock();
        try {
            return  RW_LOCKS.remove(key);
        } finally {
            loxk.unlock();
        }
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
            if (null != lock) {
                return  lock ;
            }
        } finally {
            loxk.unlock();
        }

        loxk = RW_LOCKR.writeLock( );
        loxk.lock();
        try {
            lock = RW_LOCKS.get(key);
            if (null != lock) {
                return  lock ;
            }

            lock = new Leader( key );
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
        return getLeader(key). readLock();
    }

    /**
     * 获取写锁
     * @param key
     * @return
     */
    public static Writer getWriter(String key) {
        return getLeader(key).writeLock();
    }

    /**
     * 基础锁
     * 对 ReentrantLock 的封装
     */
    public static final class Locker implements Delock, Lock {
        private final ReentrantLock lock = new ReentrantLock();
        private final String   key  ;
        private volatile long  time = 0;
        private volatile int   cite = 0;

        private Locker(String key) {
            this.key = key;
        }

        /**
         * 解锁时无引用则立即移除锁
         */
        @Override
        public void delock() {
        //  synchronized(this) { // 已在锁内, 无需再锁, 下同
                time = System.currentTimeMillis();
                cite --;
                if (cite == 0) {
                    delLocker(key);
                }
        //  }
            lock.unlock();
        }

        @Override
        public void unlock() {
        //  synchronized(this) { // 已在锁内, 无需再锁, 下同
                time = System.currentTimeMillis();
                cite --;
        //  }
            lock.unlock();
        }

        @Override
        public void lock() {
            lock.lock();
        //  synchronized(this) {
                cite ++;
        //  }
        }

        @Override
        public boolean tryLock() {
            if (! lock.tryLock() ) {
                return false;
            }
        //  synchronized(this) {
                cite ++;
        //  }
            return true;
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit)
        throws InterruptedException {
            if (! lock.tryLock(time, unit)) {
                return false;
            }
        //  synchronized(this) {
                cite ++;
        //  }
            return true;
        }

        @Override
        public void lockInterruptibly()
        throws InterruptedException {
            lock.lockInterruptibly();
        //  synchronized(this) {
                cite ++;
        //  }
        }

        /**
         * @deprecated 不支持
         * @throws UnsupportedOperationException
         */
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

    /**
     * 读写锁
     * 对 ReadWriteLock 的封装
     */
    public static final class Leader implements ReadWriteLock {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final String   key  ;
        private volatile long  time = 0;
        private volatile int   cite = 0;
        private Reader reader = null;
        private Writer writer = null;

        private Leader(String key) {
            this.key = key;
        }

        /**
         * 解锁时无引用则立即移除锁
         */
        public void delockr() {
            synchronized (this) {
                time = System.currentTimeMillis();
                cite --;
                if (cite == 0 ) {
                    delLeader(key);
                }
            }
            lock. readLock().unlock();
        }

        /**
         * 解锁时无引用则立即移除锁
         */
        public void delockw() {
        //  synchronized (this) { // 写锁是独占的, 此时不存在其他读写
                time = System.currentTimeMillis();
                cite --;
                if (cite == 0 ) {
                    delLeader(key);
                }
        //  }
            lock.writeLock().unlock();
        }

        public void unlockr() {
            synchronized (this) {
                time = System.currentTimeMillis();
                cite --;
            }
            lock. readLock().unlock();
        }

        public void unlockw() {
        //  synchronized (this) { // 写锁是独占的, 此时不存在其他读写
                time = System.currentTimeMillis();
                cite --;
        //  }
            lock.writeLock().unlock();
        }

        public void lockr() {
            lock. readLock().lock();
            synchronized (this) {
                cite ++;
            }
        }

        public void lockw() {
            lock.writeLock().lock();
        //  synchronized (this) {
                cite ++;
        //  }
        }

        public boolean tryLockr() {
            if (!lock. readLock().tryLock()) {
                return false;
            }
            synchronized (this) {
                cite ++;
            }
            return true;
        }

        public boolean tryLockw() {
            if (!lock.writeLock().tryLock()) {
                return false;
            }
        //  synchronized (this) {
                cite ++;
        //  }
            return true;
        }

        public boolean tryLockr(long time, TimeUnit unit)
        throws InterruptedException {
            if (!lock. readLock().tryLock(time , unit)) {
                return false;
            }
            synchronized (this) {
                cite ++;
            }
            return true;
        }

        public boolean tryLockw(long time, TimeUnit unit)
        throws InterruptedException {
            if (!lock.writeLock().tryLock(time, unit)) {
                return false;
            }
        //  synchronized (this) {
                cite ++;
        //  }
            return true;
        }

        public void lockrInterruptibly()
        throws InterruptedException {
            lock. readLock().lockInterruptibly();
            synchronized (this) {
                cite ++;
            }
        }

        public void lockwInterruptibly()
        throws InterruptedException {
            lock.writeLock().lockInterruptibly();
        //  synchronized (this) {
                cite ++;
        //  }
        }

        @Override
        public Reader  readLock() {
            if (reader == null) {
                reader  = new Reader(this);
            }
            return reader;
        }

        @Override
        public Writer writeLock() {
            if (writer == null) {
                writer  = new Writer(this);
            }
            return writer;
        }
    }

    /**
     * 读锁
     * 对 ReadWriteLock.readLock 的封装
     */
    public static final class Reader implements Delock, Lock {
        private final  Leader lock;

        private Reader(Leader lock) {
            this.lock = lock;
        }

        /**
         * 解锁时无引用则立即移除锁
         */
        @Override
        public void delock() {
            lock.delockr();
        }

        @Override
        public void unlock() {
            lock.unlockr();
        }

        @Override
        public void lock() {
            lock.lockr();
        }

        @Override
        public boolean tryLock() {
            return lock.tryLockr();
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit)
        throws InterruptedException {
            return lock.tryLockr(time, unit);
        }

        @Override
        public void lockInterruptibly()
        throws InterruptedException {
            lock.lockrInterruptibly();
        }

        /**
         * @deprecated 不支持
         * @throws UnsupportedOperationException
         */
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

    /**
     * 写锁
     * 对 ReadWriteLock.writeLock 的封装
     */
    public static final class Writer implements Delock, Lock {
        private final  Leader lock;

        private Writer(Leader lock) {
            this.lock = lock;
        }

        /**
         * 解锁时无引用则立即移除锁
         */
        @Override
        public void delock() {
            lock.delockw();
        }

        @Override
        public void unlock() {
            lock.unlockw();
        }

        @Override
        public void lock() {
            lock.lockw();
        }

        @Override
        public boolean tryLock() {
            return lock.tryLockw();
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit)
        throws InterruptedException {
            return lock.tryLockw(time, unit);
        }

        @Override
        public void lockInterruptibly()
        throws InterruptedException {
            lock.lockwInterruptibly();
        }

        /**
         * @deprecated 不支持
         * @throws UnsupportedOperationException
         */
        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

    public static interface Delock {
        /**
         * 解锁时无引用则立即移除锁
         */
        public void delock();
    }

}
