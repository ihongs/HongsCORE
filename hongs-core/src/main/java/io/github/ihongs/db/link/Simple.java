package io.github.ihongs.db.link;

import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.db.DBConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 简单连接器
 * @author Hongs
 */
public class Simple extends Link {

    private final String     jdbc;
    private final String     path;
    private final Properties info;

    public  Simple(String jdbc, String name, Properties info)
            throws HongsException {
        super(name);

        this.jdbc = jdbc;
        this.path = name;
        this.info = info;
    }

    public  Simple(String jdbc, String name)
            throws HongsException {
        this( jdbc, name, new Properties() );
    }

    @Override
    public  Connection open()
            throws HongsException {
        try {
            if (connection == null || connection.isClosed()) {
                connection  = open( jdbc , path , info );

                CoreLogger.trace("DB: Connect to '{}' by simple mode: {} {}", name, jdbc, path);
            }

            return connection;
        } catch (SQLException ex) {
            throw new HongsException(ex, 1024);
        } catch (ClassNotFoundException ex ) {
            throw new HongsException(ex, 1024);
        }
    }

    public  static Connection open(String jdbc, String name, Properties info)
            throws SQLException, ClassNotFoundException {
        Class.forName(jdbc);

        // 补全文件型库路径;
        name = DBConfig.fixSourceName( name );

        if (info != null && ! info.isEmpty()) {
            if (info.containsKey("username")) {
                info.put ( "user" , info.get("username") );
            }
            if (info.containsKey("connectionProperties") ) {
                name += "?" + info.getProperty("connectionProperties").replace(";", "&");
            }
            return DriverManager.getConnection(name, info);
        } else {
            return DriverManager.getConnection(name);
        }
    }

}
