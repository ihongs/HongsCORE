package app.hongs.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 资源锁
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
        StLock lock = null;
        java.util.concurrent.locks.Lock loxk;

        // 获取对应的锁
        do {
            loxk = ST_LOCKR. readLock( );
            loxk.lock();
            try {
                lock = ST_LOCKS.get(key);
            } finally {
                loxk.unlock();
            }
            if (lock != null) {
                break;
            }

            loxk = ST_LOCKR.writeLock( );
            loxk.lock();
            try {
                lock = new StLock(/***/);
                ST_LOCKS.put(key , lock);
            } finally {
                loxk.unlock();
            }
        } while (false );

        loxk = ST_LOCKR.writeLock( );
        loxk.lock();
        try {
            lock.xcount ++;
        } finally {
            loxk.unlock( );
        }

        // 锁住当前操作
        lock.lock.lock();
        try {
            fun.run();
        } finally {
            lock.lock.unlock();

            loxk = ST_LOCKR.writeLock( );
            loxk.lock();
            try {
                if  (  lock.xcount <= 1) {
                    ST_LOCKS.remove(key);
                }
                lock.xcount --;
            } finally {
                loxk.unlock( );
            }
        }
    }

    /**
     * 读锁
     * @param key 资源标识符
     * @param fun 操作
     */
    public static void reader(String key, Runnable fun) {
        RwLock lock = null;
        java.util.concurrent.locks.Lock loxk;

        // 获取对应的锁
        do {
            loxk = RW_LOCKR. readLock( );
            loxk.lock();
            try {
                lock = RW_LOCKS.get(key);
            } finally {
                loxk.unlock();
            }
            if (lock != null) {
                break;
            }

            loxk = RW_LOCKR.writeLock( );
            loxk.lock();
            try {
                lock = new RwLock(/***/);
                RW_LOCKS.put(key , lock);
            } finally {
                loxk.unlock();
            }
        } while (false );

        loxk = ST_LOCKR.writeLock( );
        loxk.lock();
        try {
            lock.rcount ++;
        } finally {
            loxk.unlock( );
        }

        // 锁住当前操作
        lock.lock. readLock().lock();
        try {
            fun.run();
        } finally {
            lock.lock. readLock().unlock();

            loxk = ST_LOCKR.writeLock( );
            loxk.lock();
            try {
                if  (  lock.rcount <= 1
                   &&  lock.wcount <= 0) {
                    ST_LOCKS.remove(key);
                }
                lock.rcount --;
            } finally {
                loxk.unlock( );
            }
        }
    }

    /**
     * 写锁
     * @param key 资源标识符
     * @param fun 操作
     */
    public static void writer(String key, Runnable fun) {
        RwLock lock = null;
        java.util.concurrent.locks.Lock loxk;

        // 获取对应的锁
        do {
            loxk = RW_LOCKR. readLock( );
            loxk.lock();
            try {
                lock = RW_LOCKS.get(key);
            } finally {
                loxk.unlock();
            }
            if (lock != null) {
                break;
            }

            loxk = RW_LOCKR.writeLock( );
            loxk.lock();
            try {
                lock = new RwLock(/***/);
                RW_LOCKS.put(key , lock);
            } finally {
                loxk.unlock();
            }
        } while (false );

        loxk = ST_LOCKR.writeLock( );
        loxk.lock();
        try {
            lock.wcount ++;
        } finally {
            loxk.unlock( );
        }

        // 锁住当前操作
        lock.lock.writeLock().lock();
        try {
            fun.run();
        } finally {
            lock.lock.writeLock().unlock();

            loxk = ST_LOCKR.writeLock( );
            loxk.lock();
            try {
                if  (  lock.wcount <= 1
                   &&  lock.rcount <= 0) {
                    ST_LOCKS.remove(key);
                }
                lock.wcount --;
            } finally {
                loxk.unlock( );
            }
        }
    }

    private static class StLock {
        public final ReentrantLock lock;
        public int xcount = 0;
        public StLock() {
            lock = new ReentrantLock( );
        }
    }

    private static class RwLock {
        public final ReadWriteLock lock;
        public int rcount = 0;
        public int wcount = 0;
        public RwLock() {
            lock = new ReentrantReadWriteLock();
        }
    }

}
