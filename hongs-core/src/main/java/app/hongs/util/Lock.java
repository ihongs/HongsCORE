package app.hongs.util;

import app.hongs.Core;
import app.hongs.CoreLogger;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 全局资源锁
 * @author Hongs
 */
public final class Lock {

    private static final ReadWriteLock ST_LOCKR = new ReentrantReadWriteLock();
    private static final ReadWriteLock RW_LOCKR = new ReentrantReadWriteLock();
    private static final Map<String, StLock> ST_LOCKS = new HashMap();
    private static final Map<String, RwLock> RW_LOCKS = new HashMap();

    /**
     * 自启一个定时任务,
     * 每隔一段时间清理,
     * 默认10分钟
     */
    static {
        long time  =  Long.parseLong(
                  System.getProperty(
                  Lock.class.getName() + ".cleans.period", "600000"));
        new Timer(Lock.class.getName() + ".cleans" , true )
        .schedule( new TimerTask( ) {
            @Override
            public void run() {
                if (Core.DEBUG > 0) {
                    CoreLogger.trace("Try to clean locks\r\n"
                                    + Tool.indent(report( )));
                }
                if (ST_LOCKS.isEmpty() && RW_LOCKS.isEmpty()) {
                    return;
                }
                cleans();
            }
        } , time , time);
    }

    /**
     * 加锁
     * @param key 资源标识符
     * @param fun 操作
     */
    public static void locker(String key, Runnable fun) {
        StLock lock = getStLock(key);

        lock.lock(  );
        try {
            fun.run();
        } finally {
            lock.unlock( );
        }
    }

    /**
     * 读锁
     * @param key 资源标识符
     * @param fun 操作
     */
    public static void reader(String key, Runnable fun) {
        RwLock lock = getRwLock(key);

        lock.lockr( );
        try {
            fun.run();
        } finally {
            lock.unlockr();
        }
    }

    /**
     * 写锁
     * @param key 资源标识符
     * @param fun 操作
     */
    public static void writer(String key, Runnable fun) {
        RwLock lock = getRwLock(key);

        lock.lockw( );
        try {
            fun.run();
        } finally {
            lock.unlockw();
        }
    }

    /**
     * 清理
     */
    public static void cleans() {
        java.util.concurrent.locks.Lock loxk;

        loxk = ST_LOCKR.writeLock();
        loxk.lock();
        try {
            Iterator<Map.Entry<String, StLock>> it = ST_LOCKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String , StLock> et = it.next();
                StLock lock = et.getValue();
                if (lock.cite <= 0) {
                    it.remove();
                }
            }
        } finally {
            loxk.unlock();
        }

        loxk = RW_LOCKR.writeLock();
        loxk.lock();
        try {
            Iterator<Map.Entry<String, RwLock>> it = RW_LOCKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String , RwLock> et = it.next();
                RwLock lock = et.getValue();
                if (lock.cite <= 0) {
                    it.remove();
                }
            }
        } finally {
            loxk.unlock();
        }
    }

    /**
     * 摘要
     * @return 摘要信息
     */
    public static String report() {
        StringBuilder sb = new StringBuilder();

        sb.append("ReentrantLock:\r\n");
        Iterator<Map.Entry<String, StLock>> it = ST_LOCKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String , StLock> et = it.next();
            String key  = et.getKey(  );
            StLock lock = et.getValue();
            sb.append(key)
              .append(" => ")
              .append(lock.cite)
              .append("\r\n");
        }

        sb.append("\r\n");

        sb.append("ReadWriteLock:\r\n");
        Iterator<Map.Entry<String, RwLock>> jt = RW_LOCKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String , RwLock> et = jt.next();
            String key  = et.getKey(  );
            RwLock lock = et.getValue();
            sb.append(key)
              .append(" => ")
              .append(lock.cite)
              .append("\r\n");
        }

        return sb.toString( );
    }

    /**
     * 获取基础锁
     * @param key
     * @return
     */
    public static StLock getStLock(String key) {
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

    /**
     * 获取读写锁
     * @param key
     * @return
     */
    public static RwLock getRwLock(String key) {
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

    /**
     * 基础锁
     */
    public final static class StLock {
        private final ReentrantLock lock = new ReentrantLock();
        private int cite = 0;

        private StLock(){} // 避免外部 new

        public void lock() {
            synchronized (this) {
                cite ++;
            }
            lock.lock();
        }

        public void unlock() {
            synchronized (this) {
                cite --;
            }
            lock.unlock();
        }
    }

    /**
     * 读写锁
     */
    public final static class RwLock {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private int cite = 0;

        private RwLock() {} // 避免外部 new

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
    }

}
