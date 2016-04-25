package app.hongs.util.store;

import app.hongs.HongsException;
import app.hongs.db.DB;
import app.hongs.db.Table;
import app.hongs.util.Data;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 简单数据存储
 * @author Hongs
 */
public class Store {

    private static final ReadWriteLock lock = new ReentrantReadWriteLock(false);
    private static Table table = null;

    private static Table getTable() throws HongsException {
        try {
            lock.readLock().lock(  );
            if (null != table) {
                return  table;
            }
        }
        finally {
            lock.readLock().unlock();
        }

        try {
            lock.writeLock( ).lock();
            try {
                table = DB.getInstance("common").getTable("store");
                return  table;
            }
            catch (HongsException ex) {
                if (ex.getCode() != 0x1039) {
                    throw ex;
                }
            }

            // 表不存在则创建之
            String sql = "CREATE TABLE IF NOT EXISTS `a_common_store` ("+
                " `id` varchar(50) NOT NULL,"+
                " `data` text,\n"+
                " `ctime` int(11) DEFAULT 0,"+
                " `xtime` int(11) DEFAULT 0,"+
                " PRIMARY KEY (`id`) )";
            Map cf = new HashMap();
            DB  db = DB.getInstance("common");
            cf.put("name" , "a_common_store");
            db.execute(sql);
            table  = new Table(db , cf);
            return table;
        }
        finally {
            lock.writeLock( ).unlock( );
        }
    }

    public static Object get(String key) throws HongsException {
        String v = (String) getTable()
                .filter("id = ?", key)
                .one( /**/ )
                .get("data");
        return Data.toObject(v);
    }

    /**
     * 设置数据到某天后失效
     * @param key
     * @param val
     * @param exp 时间
     * @throws HongsException
     */
    public static void set(String key, Object val, Date exp) throws HongsException {
        del(key);
        Map  dat = new HashMap();
        Date now = new Date();
        dat.put( "id" , key );
        dat.put("data", Data.toString(val));
        dat.put("xtime", exp.getTime()/**/);
        dat.put("ctime", now.getTime()/**/);
        getTable().insert( dat );
    }

    /**
     * 设置数据过多久后失效
     * @param key
     * @param val
     * @param exp 毫秒
     * @throws HongsException
     */
    public static void set(String key, Object val, long exp) throws HongsException {
        del(key);
        Map  dat = new HashMap();
        Date now = new Date();
        dat.put( "id" , key );
        dat.put("data", Data.toString(val));
        dat.put("xtime", now.getTime()+exp);
        dat.put("ctime", now.getTime()/**/);
        getTable().insert( dat );
    }

    public static void del(String key) throws HongsException {
        getTable().delete("id = ?", key);
    }

    public static void del() throws HongsException {
        getTable().delete("xtime != 0 AND xtime <= ?", new Date().getTime());
    }

}
