package app.hongs.db.link;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC连接器
 * @author Hongs
 */
public class Driver {

    public  static Connection connect(String drvr, String durl, Properties info)
            throws SQLException, ClassNotFoundException {
        Class.forName(drvr);

        if (info != null && ! info.isEmpty()) {
            if (info.containsKey("username")) {
                info.put ( "user" , info.get("username") );
            }
            return DriverManager.getConnection(durl, info);
        } else {
            return DriverManager.getConnection(durl);
        }
    }
}
