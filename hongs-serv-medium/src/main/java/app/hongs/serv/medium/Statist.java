package app.hongs.serv.medium;

import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.db.DB;
import app.hongs.db.Table;
import java.util.Map;

/**
 *
 * @author Hongs
 */
public class Statist {

    public static int update(String lnk, String lid, String col, int num)
    throws HongsException {
        Table  tab =      DB.getInstance("medium").getTable("statist");
        Map    uns = FormSet.getInstance("medium").getEnum ("statist_link");
        long   now =  System.currentTimeMillis(  ) / 1000;
        String sql ;
        int    ret ;

        if (! uns.containsKey(lnk) || lid == null) {
            return -1 ;
        }

        sql = "UPDATE `"+tab.tableName+"` SET `"+col+"` = `"+col+"` + "+num+", `mtime` = "+now+" WHERE link = ? AND link_id = ? ";
        ret =  tab.db.updates(sql, lnk, lid);
        return ret;
    }

    public static int create(String lnk, String lid, String col, int num)
    throws HongsException {
        Table  tab =      DB.getInstance("medium").getTable("statist");
        Map    uns = FormSet.getInstance("medium").getEnum ("statist_link");
        long   now =  System.currentTimeMillis(  ) / 1000;
        String sql ;
        int    ret ;

        if (! uns.containsKey(lnk) || lid == null) {
            return -1 ;
        }

        sql = "UPDATE `"+tab.tableName+"` SET `"+col+"` = `"+col+"` + "+num+", `mtime` = "+now+" WHERE link = ? AND link_id = ? ";
        ret =  tab.db.updates(sql, lnk, lid);
        if (ret > 0) {
            return ret;
        }

        sql = "INSERT INTO `"+tab.tableName+"` (`link`,`link_id`,`"+col+"`,`ctime`,`mtime`) VALUES (?,?,"+num+","+now+","+now+")";
        ret =  tab.db.updates(sql, lnk, lid);
        return ret;
    }

}
