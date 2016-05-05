package app.hongs.db.link;

import app.hongs.Core;
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
 * DBCP连接池
 * @author Hongs
 */
public class Source {

    private static final Map<String, BasicDataSource> sourcePool = new HashMap();
    private static final ReadWriteLock sourceLock = new ReentrantReadWriteLock();

    public  static Connection connect(String drvr, String durl, Properties info)
            throws SQLException {
        String  namc = drvr+" "+durl;
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
        if (durl.startsWith("jdbc:sqlite:")) {
            String jurl = durl.substring(12);
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
            durl = "jdbc:sqlite:"+ jurl;
        }

        sourceLock.writeLock( ).lock( );
        try {
            pool = new BasicDataSource( );
            sourcePool.put ( namc , pool);
            pool.setDriverClassName(drvr);
            pool.setUrl/* Source */(durl);

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

            return     pool.getConnection();
        } finally {
            sourceLock.writeLock().unlock();
        }
    }

}
