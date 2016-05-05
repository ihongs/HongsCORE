package app.hongs.db.pool;

import app.hongs.HongsException;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * C3P0 连接池
 * @author Hongs
 */
public class C3p0 {

    private static final Map<String, ComboPooledDataSource> sourcePool = new HashMap();
    private static final ReadWriteLock sourceLock = new ReentrantReadWriteLock();

    public  static Connection connect(String drvr, String jdbc, Properties info)
            throws HongsException {
        String namc = drvr +" "+ jdbc;
        ComboPooledDataSource    pool;
        sourceLock.readLock().lock( );
        try
        {
          pool = sourcePool.get(namc);
        }
        finally
        {
          sourceLock.readLock().unlock();
        }

        if (pool == null)
        {
          sourceLock.writeLock( ).lock();
          try
          {
            pool = new ComboPooledDataSource();
            sourcePool.put( namc, pool );
            pool.setDriverClass ( drvr );
            pool.setJdbcUrl     ( jdbc );
//          pool.setProperties  ( info ); // 无效, 只能用下面的方式来设置

            if (info.containsKey("username")) {
              pool.setUser    (info.getProperty("username"));
            }
            if (info.containsKey("password")) {
              pool.setPassword(info.getProperty("password"));
            }

            // 基本配置
            if (info.containsKey("initialPoolSize")) {
              pool.setInitialPoolSize(Integer.parseInt(info.getProperty("initialPoolSize")));
            }
            if (info.containsKey("minPoolSize")) {
              pool.setMinPoolSize(Integer.parseInt(info.getProperty("minPoolSize")));
            }
            if (info.containsKey("maxPoolSize")) {
              pool.setMaxPoolSize(Integer.parseInt(info.getProperty("maxPoolSize")));
            }
            if (info.containsKey("maxIdleTime")) {
              pool.setMaxIdleTime(Integer.parseInt(info.getProperty("maxIdleTime")));
            }
            if (info.containsKey("maxStatements")) {
              pool.setMaxStatements(Integer.parseInt(info.getProperty("maxStatements")));
            }
            if (info.containsKey("maxStatementsPerConnection")) {
              pool.setMaxStatementsPerConnection(Integer.parseInt(info.getProperty("maxStatementsPerConnection")));
            }

            // 连接配置
            if (info.containsKey("maxConnectionAge")) {
              pool.setMaxConnectionAge(Integer.parseInt(info.getProperty("maxConnectionAge")));
            }
            if (info.containsKey("numHelperThreads")) {
              pool.setNumHelperThreads(Integer.parseInt(info.getProperty("numHelperThreads")));
            }
            if (info.containsKey("checkoutTimeout" )) {
              pool.setCheckoutTimeout (Integer.parseInt(info.getProperty("checkoutTimeout" )));
            }
            if (info.containsKey("acquireIncrement")) {
              pool.setAcquireIncrement(Integer.parseInt(info.getProperty("acquireIncrement")));
            }
            if (info.containsKey("acquireRetryDelay")) {
              pool.setAcquireRetryDelay(Integer.parseInt(info.getProperty("acquireRetryDelay")));
            }
            if (info.containsKey("acquireRetryAttempts")) {
              pool.setAcquireRetryAttempts(Integer.parseInt(info.getProperty("acquireRetryAttempts")));
            }

            // 连接检测
            if (info.containsKey("automaticTestTable")) {
              pool.setAutomaticTestTable(info.getProperty("automaticTestTable"));
            }
            if (info.containsKey("preferredTestQuery")) {
              pool.setPreferredTestQuery(info.getProperty("preferredTestQuery"));
            }
            if (info.containsKey("testConnectionOnCheckin" )) {
              pool.setTestConnectionOnCheckin (Boolean.parseBoolean(info.getProperty("testConnectionOnCheckin" )));
            }
            if (info.containsKey("testConnectionOnCheckout")) {
              pool.setTestConnectionOnCheckout(Boolean.parseBoolean(info.getProperty("testConnectionOnCheckout")));
            }
            if (info.containsKey("idleConnectionTestPeriod")) {
              pool.setIdleConnectionTestPeriod(Integer.parseInt/**/(info.getProperty("idleConnectionTestPeriod")));
            }
            if (info.containsKey("breakAfterAcquireFailure")) {
              pool.setBreakAfterAcquireFailure(Boolean.parseBoolean(info.getProperty("breakAfterAcquireFailure")));
            }
          }
          catch (PropertyVetoException ex)
          {
            throw new app.hongs.HongsException(0x1024, ex);
          }
          finally
          {
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
