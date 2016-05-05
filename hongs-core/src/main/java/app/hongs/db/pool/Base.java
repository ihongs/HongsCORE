package app.hongs.db.pool;

import app.hongs.HongsException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * 内部数据源
 * @author Hongs
 */
public class Base {

    public  static Connection connect(String comp, String namc, Properties info)
            throws HongsException, NamingException {
        Context ct;
        DataSource ds;
        InitialContext ic;
        ic = new InitialContext ( );
        ct = (Context) ic.lookup(comp);
        ds = (DataSource) ct.lookup(namc);

        try {
            if (info.isEmpty()) {
                return ds.getConnection();
            } else {
                return ds.getConnection(
                       info.getProperty("username") ,
                       info.getProperty("password"));
            }
        } catch (SQLException ex) {
            throw new app.hongs.HongsException(0x1022, ex);
        }
    }

}
