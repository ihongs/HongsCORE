package app.hongs.db.link;

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
public class Origin {

    public  static Connection connect(String jndi, String namc, Properties info)
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
