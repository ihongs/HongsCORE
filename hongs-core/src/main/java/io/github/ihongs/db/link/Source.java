package io.github.ihongs.db.link;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.Tool;
import org.apache.commons.dbcp2.BasicDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 私有连接池
 * @author Hongs
 */
public class Source extends Link {

    private final String     jdbc;
    private final String     path;
    private final Properties info;

    public  Source(String jdbc, String name, Properties info)
            throws HongsException {
        super(name);

        this.jdbc = jdbc;
        this.path = name;
        this.info = info;
    }

    public  Source(String jdbc, String name)
            throws HongsException {
        this( jdbc, name, new Properties() );
    }

    @Override
    public  Connection open()
            throws HongsException {
        try {
            if (connection == null || connection.isClosed()) {
                connection  = open( jdbc , path , info );

                if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                    CoreLogger.trace("DB: Connect to '"+name+"' by source mode: "+jdbc+" "+path);
                }
            }

            return connection;
        } catch (SQLException ex) {
            throw new HongsException(0x1024, ex);
        }
    }

    private static final Map<String, BasicDataSource> sourcePool = new HashMap();
    private static final ReadWriteLock sourceLock = new ReentrantReadWriteLock();

    public  static Connection open(String jdbc, String name, Properties info)
            throws SQLException {
        String  namc = jdbc+" "+name;
        BasicDataSource         pool;

        sourceLock.readLock().lock();
        try {
            pool = sourcePool.get(namc);
            if (pool != null) {
                return pool.getConnection();
            }
        } finally {
            sourceLock.readLock( ).unlock();
        }

        // SQLite 数据路径处理
        if (name.startsWith("jdbc:sqlite:")) {
            String jurl = name.substring(12);
            Map injt = new HashMap();
            injt.put("CORE_PATH", Core.CORE_PATH);
            injt.put("CONF_PATH", Core.CONF_PATH);
            injt.put("DATA_PATH", Core.DATA_PATH);
            jurl = Tool.inject(jurl , injt);
            if(!new File(jurl).isAbsolute()) {
                jurl = Core.DATA_PATH +"/sqlite/"+ jurl;
            }
            if(!new File(jurl).getParentFile().exists()) {
                new File(jurl).getParentFile().mkdirs();
            }
            name = "jdbc:sqlite:"+ jurl;
        }

        sourceLock.writeLock( ).lock( );
        try {
            pool = new BasicDataSource( );
            sourcePool.put ( namc , pool);
            pool.setDriverClassName(jdbc);
            pool.setUrl/* Source */(name);

            if (info.containsKey("username")) {
                pool.setUsername(info.getProperty("username"));
            }
            if (info.containsKey("password")) {
                pool.setPassword(info.getProperty("password"));
            }

            // 连接属性
            if (info.containsKey("connectionProperties")) {
                pool.setConnectionProperties(info.getProperty("connectionProperties").trim().replace("&", ";"));
            }
            if (info.containsKey("connectionInitSqls")) {
                pool.setConnectionInitSqls(Arrays.asList(info.getProperty("connectionInitSqls").trim().split("\\s*;\\s*")));
            }

            // 基础设置
            if (info.containsKey("initSize")) {
                pool.setInitialSize(Integer.parseInt(info.getProperty("initSize")));
            }
            if (info.containsKey("maxTotal")) {
                pool.setMaxTotal(Integer.parseInt(info.getProperty("maxTotal")));
            }
            if (info.containsKey("minIdle")) {
                pool.setMinIdle(Integer.parseInt(info.getProperty("minIdle")));
            }
            if (info.containsKey("maxIdle")) {
                pool.setMaxIdle(Integer.parseInt(info.getProperty("maxIdle")));
            }
            if (info.containsKey("maxWait")) {
                pool.setMaxWaitMillis(Long.parseLong(info.getProperty("maxWait")));
            }

            // 回收检测
            if (info.containsKey("logAbandoned")) {
                pool.setLogAbandoned(Boolean.parseBoolean(info.getProperty("logAbandoned")));
            }
            if (info.containsKey("testOnCreate")) {
                pool.setTestOnBorrow(Boolean.parseBoolean(info.getProperty("testOnCreate")));
            }
            if (info.containsKey("testOnBorrow")) {
                pool.setTestOnBorrow(Boolean.parseBoolean(info.getProperty("testOnBorrow")));
            }
            if (info.containsKey("testOnReturn")) {
                pool.setTestOnReturn(Boolean.parseBoolean(info.getProperty("testOnReturn")));
            }
            if (info.containsKey("testWhileIdle")) {
                pool.setTestWhileIdle(Boolean.parseBoolean(info.getProperty("testWhileIdle")));
            }
            if (info.containsKey("validationQuery")) {
                pool.setValidationQuery(info.getProperty("validationQuery"));
            }
            if (info.containsKey("validationQueryTimeout")) {
                pool.setValidationQueryTimeout(Integer.parseInt(info.getProperty("validationQueryTimeout")));
            }
            if (info.containsKey("removeAbandonedTimeout")) {
                pool.setRemoveAbandonedTimeout(Integer.parseInt(info.getProperty("removeAbandonedTimeout")));
            }
            if (info.containsKey("removeAbandonedOnBorrow")) {
                pool.setRemoveAbandonedOnBorrow(Boolean.parseBoolean(info.getProperty("removeAbandonedOnBorrow")));
            }
            if (info.containsKey("removeAbandonedOnMaintenance")) {
                pool.setRemoveAbandonedOnMaintenance(Boolean.parseBoolean(info.getProperty("removeAbandonedOnMaintenance")));
            }

            // 其他设置
            if (info.containsKey(   "poolPreparedStatements" )) {
                pool.setPoolPreparedStatements(Boolean.parseBoolean(info.getProperty(  "poolPreparedStatements")));
            }
            if (info.containsKey("maxOpenPreparedStatements" )) {
                pool.setMaxOpenPreparedStatements(Integer.parseInt(info.getProperty("maxOpenPreparedStatements")));
            }
            if (info.containsKey("numTestsPerEvictionRun")) {
                pool.setNumTestsPerEvictionRun(Integer.parseInt(info.getProperty("numTestsPerEvictionRun")));
            }
            if (info.containsKey("maxConnLifetimeMillis" )) {
                pool.setMaxConnLifetimeMillis ( Long.parseLong (info.getProperty("maxConnLifetimeMillis" )));
            }
            if (info.containsKey("minEvictableIdleTimeMillis")) {
                pool.setMinEvictableIdleTimeMillis(Long.parseLong(info.getProperty("minEvictableIdleTimeMillis")));
            }
            if (info.containsKey("softMinEvictableIdleTimeMillis")) {
                pool.setSoftMinEvictableIdleTimeMillis(Long.parseLong(info.getProperty("softMinEvictableIdleTimeMillis")));
            }
            if (info.containsKey("timeBetweenEvictionRunsMillis" )) {
                pool.setTimeBetweenEvictionRunsMillis (Long.parseLong(info.getProperty("timeBetweenEvictionRunsMillis" )));
            }

            return     pool.getConnection();
        } finally {
            sourceLock.writeLock().unlock();
        }
    }

}
