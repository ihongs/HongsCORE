package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.db.util.FetchCase;
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
        if ( user == null ) user = Cnst.ADM_UID;
        long ct = Synt.declare(opts.get("time"), -1L);
        long dt = Core.ACTION_TIME .get(      );
        long di = (dt / 1000 );
        Data dr = Data.getInstance( conf,form );
        Loop lp ;

        Map sd = new HashMap();
        sd.put("form_id",form);
        sd.put("user_id",user);
        sd.put("memo"   ,memo);

        int  c  = 0; // 操作总数
        int  i  = 0; // 变更计数

        /**
         * 对于时间参数
         * 0 和 -1 都是写最新数据到索引库
         * 0 会留历史记录 -1 不留历史记录
         * 从数据库恢复到索引库用 -1 即可
         * nameable,wordable 等改变需用 0
         */

        Set<String> ds = Synt.asSet (opts.get(""));
        FetchCase   fc = dr.getTable().fetchCase();
            fc.filter("form_id = ?", form);
        if (ct <= 0) {
            fc.filter("etime  = ?" ,  0  );
        } else {
            fc.filter("ctime <= ?" ,  ct );
            fc.assort("ctime DESC");
        }
        if (! ds.isEmpty()) {
            fc.filter("id IN ( ? )",  ds );
            c = ds.size ( );
        } else {
            c = Synt.declare(
                fc.clone ()
                  .select("COUNT(*) AS c")
                  .getOne()
                  .get("c")
                , 0);
        }
        if (ct <= 0) {
            lp  = fc.select(   );
        } else {
            lp  = dr.getTable ()
                    .db.query (
                  "SELECT _.* FROM ("
                + fc.getSQL(   )
                + ") _ GROUP BY _.id"
                + " ORDER BY _.ctime"
                , 0 , 0 ,
                  fc.getParams()
            );
        }

        dr.begin(  );
        CmdletHelper.progres(dt, c, i);

        if (ct >= 0) {
            for(Map od : lp ) {
                String id = ( String ) od.get( Cnst.ID_KEY );
                if (Synt.declare(od.get("state"), 1 ) >= 1 ) {
                if (Synt.declare(od.get("etime"), 0L) == 0L) {
                    od = Synt.toMap( od.get( "data"));
                    od.putAll ( /**/ sd );
                    dr.save( di, id, od );
                }  else  {
                    sd.put ("rtime", od.get("ctime"));
                    dr.redo( di, id, sd );
                }} else  {
                    dr.drop( di, id, sd );
                }
                    ds.remove(id);
                CmdletHelper.progres(dt, c, ++ i);
//              if (i % 500 == 0) {
//                  dr.commit(  );
//              }
            }
        } else {
            for(Map od : lp ) {
                String id = ( String ) od.get( Cnst.ID_KEY );
                if (Synt.declare(od.get("state"), 1 ) >= 1 ) {
                    od = Synt.toMap( od.get( "data"));
                    od.putAll(sd);
                    dr.set(id,od);
                }  else  {
                    dr.delDoc(id);
                }
                    ds.remove(id);
                CmdletHelper.progres(dt, c, ++ i);
//              if (i % 500 == 0) {
//                  dr.commit(  );
//              }
            }
        }

        // 不存在的直接删掉
        for(String id:ds) {
            dr.delDoc(id);
            CmdletHelper.progres(dt, c, ++ i);
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
        if (user == null) {
            user  = Cnst.ADM_UID;
        }

        dr.begin();

        int i  = 0;
        String[] dats = (String[]) opts.get("");
        for(String text : dats) {
            String id ;
            Map  data = data(text);
            id = (String) data.get(Cnst.ID_KEY);
            if (id == null) { //
                id  = Core.newIdentity( );
                data.put(Cnst.ID_KEY, id);
            }
            data.put("form_id", form);
            data.put("user_id", user);
            data.put("memo"   , memo);
            i += dr.save(dt,id, data);
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
        if (user == null) {
            user  = Cnst.ADM_UID;
        }

        String[] dats = (String[]) opts.get("");
        if (dats.length < 2) {
            CmdletHelper.println("Need FIND DATA.");
            return;
        }

        Map rd = data(dats[0]);
        Map sd = data(dats[1]);
        sd.put("form_id",form);
        sd.put("user_id",user);
        sd.put("memo"   ,memo);
        rd.put(Cnst.RB_KEY , Synt.setOf(Cnst.ID_KEY));

        dr.begin();

        int i  = 0;
        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY) ;
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
        if (user == null) {
            user  = Cnst.ADM_UID;
        }

        String[] dats = (String[]) opts.get("");
        if (dats.length < 1) {
            CmdletHelper.println("Need FIND_TERM.");
            return;
        }

        Map rd = data(dats[0]);
        Map sd = new HashMap();
        sd.put("form_id",form);
        sd.put("user_id",user);
        sd.put("memo"   ,memo);
        rd.put(Cnst.RB_KEY , Synt.setOf(Cnst.ID_KEY));

        dr.begin();

        int i  = 0;
        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY) ;
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
            return (Map) io.github.ihongs.util.Data.toObject(text);
        } else {
            return ActionHelper.parseQuery(text);
        }
    }

}
