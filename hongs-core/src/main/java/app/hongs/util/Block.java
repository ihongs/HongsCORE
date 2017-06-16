package app.hongs.util;

import app.hongs.Core;
import app.hongs.CoreLogger;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 全局资源锁
 * @author Hongs
 */
public final class Block {

    private static final ReadWriteLock ST_LOCKR = new ReentrantReadWriteLock();
    private static final ReadWriteLock RW_LOCKR = new ReentrantReadWriteLock();
    private static final Map<String , Locker> ST_LOCKS = new HashMap();
    private static final Map<String , Larder> RW_LOCKS = new HashMap();

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
                if (!ST_LOCKS.isEmpty() || !RW_LOCKS.isEmpty()) {
                    int n = cleans();
                    if (Core.DEBUG > 0) {
                        CoreLogger.trace("Cleared "+n+" lock(s)");
                    }
                } else {
                    if (Core.DEBUG > 0) {
                        CoreLogger.trace( "No locks be cleared" );
                    }
                }
            }
        } , time , time);
    }

    /**
     * 加锁
     * @param key 资源标识符
     * @param fun 操作
     */
    public static void locker(String key, Runnable fun) {
        Locker lock = getLocker(key);

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
        Larder lock = getLarder(key);

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
        Larder lock = getLarder(key);

        lock.lockw( );
        try {
            fun.run();
        } finally {
            lock.unlockw();
        }
    }

    /**
     * 清理
     * @return 清理数量
     */
    public static int cleans() {
        Lock loxk;
        int ct = 0;

        loxk = ST_LOCKR.writeLock();
        loxk.lock();
        try {
            Iterator<Map.Entry<String, Locker>> it = ST_LOCKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String , Locker> et = it.next();
                Locker lock = et.getValue();
                if (lock.cite <= 0) {
                    it.remove();
                    ct ++;
                }
            }
        } finally {
            loxk.unlock();
        }

        loxk = RW_LOCKR.writeLock();
        loxk.lock();
        try {
            Iterator<Map.Entry<String, Larder>> it = RW_LOCKS.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String , Larder> et = it.next();
                Larder lock = et.getValue();
                if (lock.cite <= 0) {
                    it.remove();
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

        rs.put("ReentrantBlock", st);
        Iterator<Map.Entry<String, Locker>> it = ST_LOCKS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String , Locker> et = it.next();
            Locker lock = et.getValue();
            String key = et.getKey();
            st.put(key , lock.cite );
        }

        rs.put("ReadWriteBlock", rw);
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
     * 基础锁
     * 对 ReentrantLock 的封装
     */
    public final static class Locker {
        private final ReentrantLock lock = new ReentrantLock();
        private int cite = 0;

        private Locker(){} // 避免外部 new

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
     * 对 ReadWriteLock 的封装
     */
    public final static class Larder {
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
    }

}
