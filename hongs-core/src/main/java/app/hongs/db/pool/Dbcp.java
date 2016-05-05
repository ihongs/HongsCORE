package app.hongs.db.pool;

import app.hongs.HongsException;
import org.apache.commons.dbcp.BasicDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * DBCP 连接池
 * @author Hongs
 */
public class Dbcp {

    private static final Map<String, BasicDataSource> sourcePool = new HashMap();
    private static final ReadWriteLock sourceLock = new ReentrantReadWriteLock();

    public  static void  remove(String drvr, String jdbc) {
        sourcePool.remove(drvr + " " + jdbc);
    }

    public  static Connection connect(String drvr, String jdbc, Properties info)
            throws HongsException {
        String  namc = drvr+" "+jdbc;
        BasicDataSource         pool;

        sourceLock.readLock().lock();
        try {
            pool = sourcePool.get(namc);
        } finally {
            sourceLock.readLock().unlock();
        }

        if (pool == null) {
            sourceLock.writeLock( ).lock();
            try {
                pool = new BasicDataSource( );
                sourcePool.put ( namc , pool);
                pool.setDriverClassName(drvr);
                pool.setUrl            (jdbc);

                if (info.containsKey("username")) {
                    pool.setUsername(info.getProperty("username"));
                }
                if (info.containsKey("password")) {
                    pool.setPassword(info.getProperty("password"));
                }

                if (info.containsKey("initialSize")) {
                    pool.setInitialSize(Integer.parseInt(info.getProperty("initialSize")));
                }
                if (info.containsKey("maxActive")) {
                    pool.setMaxActive(Integer.parseInt(info.getProperty("maxActive")));
                }
                if (info.containsKey("minIdle")) {
                    pool.setMinIdle(Integer.parseInt(info.getProperty("minIdle")));
                }
                if (info.containsKey("maxIdle")) {
                    pool.setMaxIdle(Integer.parseInt(info.getProperty("maxIdle")));
                }
                if (info.containsKey("maxWait")) {
                    pool.setMaxWait(Long.parseLong(info.getProperty("maxWait")));
                }

                // 其他设置
                if (info.containsKey("maxOpenPreparedStatements" )) {
                    pool.setMaxOpenPreparedStatements(Integer.parseInt(info.getProperty("maxOpenPreparedStatements")));
                }
                if (info.containsKey("minEvictableIdleTimeMillis")) {
                    pool.setMinEvictableIdleTimeMillis(Long.parseLong(info.getProperty("minEvictableIdleTimeMillis")));
                }
            } finally {
                sourceLock.writeLock().unlock();
            }
        }

        try {
            return pool.getConnection();
        } catch (SQLException ex) {
            throw new app.hongs.HongsException(0x1024, ex);
        }
    }

}
