package app.hongs.db.link;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * 内部连接池
 * @author Hongs
 */
public class Origin {

    public  static Connection connect(String comp, String namc, Properties info)
            throws SQLException, NamingException {
        if (comp == null || comp.length( ) == 0) {
            comp = "java:comp/env";
        }

        Context ct;
        DataSource ds;
        InitialContext ic;
        ic = new InitialContext ( );
        ct = (Context) ic.lookup(comp);
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
