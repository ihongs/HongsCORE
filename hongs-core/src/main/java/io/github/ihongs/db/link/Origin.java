package io.github.ihongs.db.link;

import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * 公共连接池
 * @author Hongs
 */
public class Origin extends Link {

    private final String     jndi;
    private final String     path;
    private final Properties info;

    public  Origin(String jndi, String name, Properties info)
            throws HongsException {
        super(name);

        this.jndi = jndi;
        this.path = name;
        this.info = info;
    }

    public  Origin(String jdbc, String name)
            throws HongsException {
        this( jdbc, name, new Properties() );
    }

    @Override
    public  Connection open()
            throws HongsException {
        try {
            if (connection == null || connection.isClosed()) {
                connection  = open( jndi , path , info );
                
                CoreLogger.trace("DB: Connect to '{}' by origin mode: {} {}", name, jndi, path);
            }

            return connection;
        } catch (SQLException ex) {
            throw new HongsException(1022, ex);
        } catch (NamingException ex ) {
            throw new HongsException(1022, ex);
        }
    }

    public  static Connection open(String jndi, String namc, Properties info)
            throws SQLException, NamingException {
        if (jndi == null || jndi.length( ) == 0) {
            jndi = "java:comp/env";
        }

        Context ct;
        DataSource ds;
        InitialContext ic;
        ic = new InitialContext ( );
        ct = (Context) ic.lookup(jndi);
        ds = (DataSource) ct.lookup(namc);

        if (info.isEmpty()) {
            return ds.getConnection();
        } else {
            return ds.getConnection(
                   info.getProperty("username") ,
                   info.getProperty("password"));
        }
    }

}
