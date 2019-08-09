package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Synt;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 数据操作命令
 * @author hong
 */
@Cmdlet("matrix.data")
public class DataCmdlet {

    @Cmdlet("revert")
    public static void revert(String[] args) throws HongsException, InterruptedException {
        Map opts = CmdletHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "time:i",
            "!A",
            "?Usage: revert --conf CONF_NAME --form FORM_NAME [--time TIMESTAMP] ID0 ID1 ..."
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        Set< String >  ds = Synt.asSet ( opts.get(""));
        long ct = Synt.declare(opts.get("time"),  0L );
        long dt = Core.ACTION_TIME .get() /1000;
        Data dr = Data.getInstance( conf,form );

        dr.setUserId(Synt.defoult(user, Cnst.ADM_UID));
//      user = dr.getUserId( );
        form = dr.getFormId( );

        Map sd = new HashMap();
        sd.put( "memo", memo );

        Table  tb = dr.getTable();
        String tn = tb.tableName ;
        Loop   lp   ; // 查询迭代
        int  c  = 0 ; // 操作总数
        int  i  = 0 ; // 变更计数
        if (ct == 0) {
            String fa = "`a`.*"  ;
            String fc = "COUNT(*) AS _cnt_" ;
            String qa = "SELECT "+fa+" FROM `"+tn+"` AS `a` WHERE `a`.`form_id` = ? AND `a`.`etime`  = ?";
            String qc = "SELECT "+fc+" FROM `"+tn+"` AS `a` WHERE `a`.`form_id` = ? AND `a`.`etime`  = ?";
            if (! ds.isEmpty() ) {
                c  = ds.size();
                qa = qa + " AND a.id IN (?)";
                lp = tb.db.query(qa, 0, 0, form,  0, ds);
            } else {
                lp = tb.db.query(qa, 0, 0, form,  0    );
                c  = Synt .declare (
                     tb.db.fetchOne(   qc, form,  0    )
                          .get("_cnt_"), 0 );
            }
        } else {
            String fx = "`x`.*"  ;
            String fa = "`a`.id, MAX(a.ctime) AS ctime" ;
            String fc = "COUNT(DISTINCT a.id) AS _cnt_" ;
            String qa = "SELECT "+fa+" FROM `"+tn+"` AS `a` WHERE `a`.`form_id` = ? AND `a`.`ctime` <= ?";
            String qc = "SELECT "+fc+" FROM `"+tn+"` AS `a` WHERE `a`.`form_id` = ? AND `a`.`ctime` <= ?";
            String qx = " WHERE x.id = b.id AND x.ctime = b.ctime AND x.`form_id` = ? AND x.`ctime` <= ?";
            if (! ds.isEmpty() ) {
                c  = ds.size();
                qa = qa + " AND a.id IN (?)";
                qx = qx + " AND x.id IN (?)";
                qa = qa + " GROUP BY `a`.id";
                qx = "SELECT "+fx+" FROM `"+tn+"` AS `x`, ("+qa+") AS `b` "+qx;
                lp = tb.db.query(qx, 0, 0, form, ct, ds, form, ct, ds);
            } else {
                qa = qa + " GROUP BY `a`.id";
                qx = "SELECT "+fx+" FROM `"+tn+"` AS `x`, ("+qa+") AS `b` "+qx;
                lp = tb.db.query(qx, 0, 0, form, ct    , form, ct    );
                c  = Synt .declare (
                     tb.db.fetchOne(   qc, form, ct    )
                          .get("_cnt_"), 0 );
            }
        }

        long tm = System.currentTimeMillis();
        CmdletHelper.progres(tm, c, i);
        dr.begin(  );

        for(Map od : lp ) {
            String id = ( String ) od.get( Cnst.ID_KEY );
            if (Synt.declare(od.get("etime"), 0L) != 0L) {
            if (Synt.declare(od.get("state"), 1 ) >= 1 ) {
                sd.put ("rtime", od.get("ctime"));
                dr.redo( dt, id, sd );
            }  else  {
                dr.drop( dt, id, sd );
            }} else  {
            if (Synt.declare(od.get("state"), 1 ) >= 1 ) {
                od = Synt.toMap( od.get( "data"));
                od.putAll(sd);
                dr.set(id,od);
            }  else  {
                dr.delDoc(id);
            }}
                ds.remove(id);
            CmdletHelper.progres(tm, c, ++ i);
//          if (i % 500 == 0) {
//              dr.commit(  );
//          }
        }

        // 不存在的直接删掉
        for(String id:ds) {
            dr.delDoc(id);
            CmdletHelper.progres(tm, c, ++ i);
 //         if (i % 500 == 0) {
 //             dr.commit(  );
 //         }
        }

        dr.commit( );
        CmdletHelper.progred( );

        CmdletHelper.println("Revert "+i+" item(s) in "+dr.getDbName());
    }

    @Cmdlet("import")
    public static void impart(String[] args) throws HongsException, InterruptedException {
        Map opts = CmdletHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "!A",
            "?Usage: import --conf CONF_NAME --form FORM_NAME DATA DATA ..."
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        long dt = Core.ACTION_TIME .get() /1000;
        Data dr = Data.getInstance( conf,form );

        String[] dats = (String[]) opts.get("");

        dr.setUserId(Synt.defoult(user, Cnst.ADM_UID));
//      user = dr.getUserId( );
//      form = dr.getFormId( );

        dr.begin();

        int i  = 0;
        for(String text : dats) {
            Map sd = data(text);
            String id = (String) sd.get(Cnst.ID_KEY);
            if (id == null) { //
                id = Core.newIdentity();
                sd.put(Cnst.ID_KEY, id);
            }   sd.put( "memo" , memo );
            i += dr.save ( dt, id, sd );
//          if (i % 500 == 0) {
//               dr.commit( );
//          }
        }

        dr.commit( );

        CmdletHelper.println("Import "+i+" item(s) to "+dr.getDbName());
    }

    @Cmdlet("update")
    public static void update(String[] args) throws HongsException, InterruptedException {
        Map opts = CmdletHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "!A",
            "?Usage: update --conf CONF_NAME --form FORM_NAME FIND DATA"
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        long dt = Core.ACTION_TIME .get() /1000;
        Data dr = Data.getInstance( conf,form );

        String[] dats = (String[]) opts.get("");
        if (dats.length < 2) {
            CmdletHelper.println ( "Need FIND DATA." );
            return;
        }

        dr.setUserId(Synt.defoult(user, Cnst.ADM_UID));
//      user = dr.getUserId( );
//      form = dr.getFormId( );

        Map rd = data(dats[0]);
        Map sd = data(dats[1]);
        sd.put( "memo", memo );
        rd.put(Cnst.RB_KEY , Synt.setOf(Cnst.ID_KEY));

        dr.begin();

        int i  = 0;
        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY);
            i += dr.save(dt, id, sd);
//          if (i % 500 == 0) {
//               dr.commit( );
//          }
        }

        dr.commit( );

        CmdletHelper.println("Update "+i+" item(s) in "+dr.getDbName());
    }

    @Cmdlet("delete")
    public static void delete(String[] args) throws HongsException, InterruptedException {
        Map opts = CmdletHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "!A",
            "?Usage: delete --conf CONF_NAME --form FORM_NAME FIND_TERM"
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        long dt = Core.ACTION_TIME .get() /1000;
        Data dr = Data.getInstance( conf,form );

        String[] dats = (String[]) opts.get("");
        if (dats.length < 1) {
            CmdletHelper.println ( "Need FIND_TERM." );
            return;
        }

        dr.setUserId(Synt.defoult(user, Cnst.ADM_UID));
//      user = dr.getUserId( );
//      form = dr.getFormId( );

        Map rd = data(dats[0]);
        Map sd = new HashMap();
        sd.put( "memo", memo );
        rd.put(Cnst.RB_KEY , Synt.setOf(Cnst.ID_KEY));

        dr.begin();

        int i  = 0;
        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY);
            i += dr.drop(dt, id, sd);
//          if (i % 500 == 0) {
//               dr.commit( );
//          }
        }

        dr.commit( );

        CmdletHelper.println("Delete "+i+" item(s) in "+dr.getDbName());
    }

    @Cmdlet("search")
    public static void search(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "!A",
            "?Usage: search --conf CONF_NAME --form FORM_NAME FIND_TERM"
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        Data dr = Data.getInstance( conf,form );

        String[] dats = (String[]) opts.get("");
        if (dats.length < 1) {
            CmdletHelper.println("Need FIND_TERM.");
            return;
        }

        Map rd = data(dats[0]);

        for(Map od : dr.search(rd, 0, 0)) {
            CmdletHelper.preview(od);
        }
    }

    private static Map data(String text) {
        text = text.trim();
        if (text.startsWith("<") && text.endsWith(">")) {
            throw  new UnsupportedOperationException("Unsupported html: "+ text);
        } else
        if (text.startsWith("[") && text.endsWith("]")) {
            throw  new UnsupportedOperationException("Unsupported list: "+ text);
        } else
        if (text.startsWith("{") && text.endsWith("}")) {
            return ( Map ) Dawn.toObject  (text);
        } else {
            return ActionHelper.parseQuery(text);
        }
    }

}
