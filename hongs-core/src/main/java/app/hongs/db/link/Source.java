package app.hongs.db.link;

import app.hongs.Core;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.util.Tool;
import org.apache.commons.dbcp.BasicDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
    public  Connection connect()
            throws HongsException {
        try {
            if (connection == null || connection.isClosed()) {
                connection  = connect( jdbc , path , info );
                
                if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                    CoreLogger.trace("DB: Connect to '"+name+"' by source mode: "+jdbc+" "+path);
                }
            }

            initial(); // 预置

            return connection;
        } catch (SQLException ex) {
            throw new HongsException(0x1024, ex);
        }
    }

    private static final Map<String, BasicDataSource> sourcePool = new HashMap();
    private static final ReadWriteLock sourceLock = new ReentrantReadWriteLock();

    public  static Connection connect(String jdbc, String name, Properties info)
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

            // 基础设置
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

            // 回收设置
            if (info.containsKey("logAbandoned")) {
                pool.setLogAbandoned(Boolean.parseBoolean(info.getProperty("logAbandoned")));
            }
            if (info.containsKey("removeAbandoned")) {
                pool.setRemoveAbandoned(Boolean.parseBoolean(info.getProperty("removeAbandoned")));
            }
            if (info.containsKey("removeAbandonedTimeout")) {
                pool.setRemoveAbandonedTimeout(Integer.parseInt(info.getProperty("removeAbandonedTimeout")));
            }

            // 检测设置
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

            // 其他设置
            if (info.containsKey("numTestsPerEvictionRun")) {
                pool.setNumTestsPerEvictionRun(Integer.parseInt(info.getProperty("numTestsPerEvictionRun")));
            }
            if (info.containsKey("poolPreparedStatements")) {
                pool.setPoolPreparedStatements(Boolean.parseBoolean(info.getProperty("poolPreparedStatements")));
            }
            if (info.containsKey("maxOpenPreparedStatements")) {
                pool.setMaxOpenPreparedStatements(Integer.parseInt(info.getProperty("maxOpenPreparedStatements")));
            }
            if (info.containsKey("minEvictableIdleTimeMillis")) {
                pool.setMinEvictableIdleTimeMillis(Long.parseLong(info.getProperty("minEvictableIdleTimeMillis")));
            }
            if (info.containsKey("timeBetweenEvictionRunsMillis")) {
                pool.setTimeBetweenEvictionRunsMillis(Long.parseLong(info.getProperty("timeBetweenEvictionRunsMillis")));
            }

            return     pool.getConnection();
        } finally {
            sourceLock.writeLock().unlock();
        }
    }

}
