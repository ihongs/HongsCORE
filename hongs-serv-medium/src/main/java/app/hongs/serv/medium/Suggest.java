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
public class Suggest {
    
    public static int update(String lnk, String uid, int num)
    throws HongsException {
        Table  tab =      DB.getInstance("medium").getTable("suggest");
        Map    uns = FormSet.getInstance("medium").getEnum ("suggest_unit");
        long   now =  System.currentTimeMillis(  ) / 1000;
        String sql ;
        int    ret ;

        if (! uns.containsKey(lnk) || uid == null) {
            return -1 ;
        }
        
        sql = "UPDATE `"+tab.tableName+"` SET `count` = `count` + "+num+", `mtime` = "+now+" WHERE unit = ? AND user_id = ?";
        ret =  tab.db.updates(sql, lnk, uid);
        return ret;
    }

    public static int create(String lnk, String uid, int num)
    throws HongsException {
        Table  tab =      DB.getInstance("medium").getTable("suggest");
        Map    uns = FormSet.getInstance("medium").getEnum ("suggest_unit");
        long   now =  System.currentTimeMillis(  ) / 1000;
        String sql ;
        int    ret ;

        if (! uns.containsKey(lnk) || uid == null) {
            return -1 ;
        }
        
        sql = "UPDATE `"+tab.tableName+"` SET `count` = `count` + "+num+", `mtime` = "+now+" WHERE unit = ? AND user_id = ?";
        ret =  tab.db.updates(sql, lnk, uid);
        if (ret > 0) {
            return ret;
        }
        
        sql = "INSERT INTO `"+tab.tableName+"` (`unit`,`user_id`,`count`,`mtime`) VALUES (?,?,"+num+","+now+")";
        ret =  tab.db.updates(sql, lnk, uid);
        return ret;
    }

}
