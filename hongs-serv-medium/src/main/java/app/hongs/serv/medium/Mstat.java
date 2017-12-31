package app.hongs.serv.medium;

import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.db.Table;
import app.hongs.db.util.FetchCase;
import app.hongs.util.Synt;
import java.util.Collection;
import java.util.Map;

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

    @Override
    public Map search(Map rd, FetchCase caze)
      throws HongsException
    {
        /**
         * 一个关联只有一组状态
         * 故如发现查的单一关联
         * 则可视为查询状态详情
         */
        Object lnk = Synt.defoult(getLink(  ), rd.get(LINK   ));
        Object lid = Synt.defoult(getLinkId(), rd.get(LINK_ID));
        if (lnk != null
        &&  lid != null
        &&!(lid instanceof Map)
        &&!(lid instanceof Collection)
        &&!(lid instanceof Object[ ])) {
            return getInfo (rd, caze);
        }

        return super.search(rd, caze);
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
        if (lnk == null || lid == null || lid.length() == 0) {
            return -1;
        }

        int    ret;
        long   now;
        String sql;
        now =  System.currentTimeMillis() / 1000;

        sql = "UPDATE `"+table.tableName+"` SET `"+col+"` = `"+col+"` + "+num+",`"+MTIME+"` = "+now+" WHERE `" + LINK + "` = ? AND `" + LINK_ID + "` = ? ";
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
        if (lnk == null || lid == null || lid.length() == 0) {
            return -1;
        }

        int    ret;
        long   now;
        String sql;
        now =  System.currentTimeMillis() / 1000;

        sql = "UPDATE `"+table.tableName+"` SET `"+col+"` = `"+col+"` + "+num+",`"+MTIME+"` = "+now+" WHERE `" + LINK + "` = ? AND `" + LINK_ID + "` = ? ";
        ret =  db.updates(sql, lnk, lid);

        if (ret > 0) return ret;

        sql = "INSERT INTO `"+table.tableName+"` (`"+LINK+"`,`"+LINK_ID+"`,`id`,`"+MTIME+"`,`"+CTIME+"`,`"+col+"`) VALUES (?,?,?,"+now+","+now+","+num+")";
        ret =  db.updates(sql, lnk, lid, Core.newIdentity());

        return ret;
    }

}
