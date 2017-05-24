package app.hongs.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 全局资源锁
 * @author Hongs
 */
public class Lock {

    private static final ReadWriteLock ST_LOCKR = new ReentrantReadWriteLock();
    private static final ReadWriteLock RW_LOCKR = new ReentrantReadWriteLock();
    private static final Map<String, StLock> ST_LOCKS = new HashMap();
    private static final Map<String, RwLock> RW_LOCKS = new HashMap();

    /**
     * 加锁
     * @param key 资源标识符
     * @param fun 操作
     */
    public static void locker(String key, Runnable fun) {
        StLock lock = getStLock(key);
        inlock(lock, ST_LOCKR);

        lock.lock.lock();
        try {
            fun.run(   );
        } finally {
            lock.lock.unlock();

            unlock(lock, ST_LOCKR, ST_LOCKS, key);
        }
    }

    /**
     * 读锁
     * @param key 资源标识符
     * @param fun 操作
     */
    public static void reader(String key, Runnable fun) {
        RwLock lock = getRwLock(key);
        inlock(lock, RW_LOCKR);

        lock.lock. readLock().lock();
        try {
            fun.run();
        } finally {
            lock.lock. readLock().unlock();

            unlock(lock, RW_LOCKR, RW_LOCKS, key);
        }
    }

    /**
     * 写锁
     * @param key 资源标识符
     * @param fun 操作
     */
    public static void writer(String key, Runnable fun) {
        RwLock lock = getRwLock(key);
        inlock(lock, RW_LOCKR);

        lock.lock.writeLock().lock();
        try {
            fun.run();
        } finally {
            lock.lock.writeLock().unlock();

            unlock(lock, RW_LOCKR, RW_LOCKS, key);
        }
    }

    private static void inlock(MyLock lock, ReadWriteLock lockr) {
        java.util.concurrent.locks.Lock loxk;

        loxk = lockr.writeLock( );
        loxk.lock();
        try {
            lock.cite ++ ;
        } finally {
            loxk.unlock();
        }
    }

    private static void unlock(MyLock lock, ReadWriteLock lockr, Map locks, String key) {
        java.util.concurrent.locks.Lock loxk;

        loxk = lockr.writeLock( );
        loxk.lock();
        try {
            lock.cite -- ;
            if (lock.cite  <=  0) {
                locks.remove(key);
            }
        } finally {
            loxk.unlock();
        }
    }

    private static StLock getStLock(String key) {
        java.util.concurrent.locks.Lock loxk;
        StLock lock = null;

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
            lock = new StLock(/***/);
            ST_LOCKS.put(key , lock);
        } finally {
            loxk.unlock();
        }
        return lock;
    }

    private static RwLock getRwLock(String key) {
        java.util.concurrent.locks.Lock loxk;
        RwLock lock = null;

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
            lock = new RwLock(/***/);
            RW_LOCKS.put(key , lock);
        } finally {
            loxk.unlock();
        }
        return lock;
    }

    private static class MyLock {
        public int cite = 0;
    }

    private static class StLock extends MyLock {
        public final ReentrantLock lock = new ReentrantLock();
    }

    private static class RwLock extends MyLock {
        public final ReadWriteLock lock = new ReentrantReadWriteLock();
    }

}
