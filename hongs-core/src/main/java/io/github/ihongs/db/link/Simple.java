package io.github.ihongs.db.link;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.Tool;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
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

                if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                    CoreLogger.trace("DB: Connect to '"+name+"' by simple mode: "+jdbc+" "+path);
                }
            }

            return connection;
        } catch (SQLException ex) {
            throw new HongsException(0x1024, ex);
        } catch (ClassNotFoundException ex ) {
            throw new HongsException(0x1024, ex);
        }
    }

    public  static Connection open(String jdbc, String name, Properties info)
            throws SQLException, ClassNotFoundException {
        Class.forName(jdbc);

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
