package app.hongs.serv.medium;

import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.db.Table;
import app.hongs.util.Synt;

/**
 *
 * @author Hongs
 */
public class Mstat extends Mlink {

    protected final String MTIME;
    protected final String CTIME;

    public Mstat(Table table) throws HongsException {
        super(table);
        MTIME = Synt.declare(table.getParams().get("field.mtime"), "mtime");
        CTIME = Synt.declare(table.getParams().get("field.ctime"), "ctime");
    }

    protected boolean allows(String link) throws HongsException {
        return FormSet.getInstance(db.name)
                      .getEnum( table.name + "_link" )
                      .containsKey(   link);
    }

    /**
     * 对字段数值递加
     * 仅对存在的操作
     * @param col
     * @param num
     * @return
     * @throws HongsException
     */
    public int put(String col, int num)
    throws HongsException {
        String lnk = getLink(  );
        String lid = getLinkId();
        if (lid == null || lid.equals("") || !allows(lnk)) {
            return -1;
        }

        int    ret;
        long   now;
        String sql;
        now =  System.currentTimeMillis() / 1000;

        sql = "UPDATE `"+table.tableName+"` SET `"+col+"` = `"+col+"` + "+num+", `"+MTIME+"` = "+now+" WHERE `"+LINK+"` = ? AND `"+LINK_ID+"` = ? ";
        ret =  db.updates(sql, lnk, lid);

        return ret;
    }

    /**
     * 对字段数值递加
     * 没有记录则创建
     * @param col
     * @param num
     * @return
     * @throws HongsException
     */
    public int add(String col, int num)
    throws HongsException {
        String lnk = getLink(  );
        String lid = getLinkId();
        if (lid == null || lid.equals("") || !allows(lnk)) {
            return -1;
        }

        int    ret;
        long   now;
        String sql;
        now =  System.currentTimeMillis() / 1000;

        sql = "UPDATE `"+table.tableName+"` SET `"+col+"` = `"+col+"` + "+num+", `"+MTIME+"` = "+now+" WHERE `"+LINK+"` = ? AND `"+LINK_ID+"` = ? ";
        ret =  db.updates(sql, lnk, lid);

        if (ret > 0) return ret;

        sql = "INSERT INTO `"+table.tableName+"` (`"+LINK+"`,`"+LINK_ID+"`,`"+CTIME+"`,`"+MTIME+"`,`"+col+"`) VALUES (?,?,"+num+","+now+","+now+")";
        ret =  db.updates(sql, lnk, lid);

        return ret;
    }

}
