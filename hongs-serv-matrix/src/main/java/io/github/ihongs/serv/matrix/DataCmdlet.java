package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.anno.Cmdlet;
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
    public static void synchr(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "time:i",
            "!A",
            "?Usage: update --conf CONF_NAME --form FORM_NAME ID_0 ID_1 ..."
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        long ct = Synt.declare(opts.get("time"), 0L);
        long dt = Core.ACTION_TIME .get(      );
        Data dr = Data.getInstance( conf,form );
        if (user == null) {
            user  = Cnst.ADM_UID;
        }

        Map sd = new HashMap();
        sd.put("form_id",form);
        sd.put("user_id",user);
        sd.put("memo"   ,memo);

        int  c = 0; // 操作总数
        int  i = 0; // 变更计数

        Set<String> ds = Synt.asSet (opts.get(""));
        FetchCase   fc = dr.getTable().fetchCase();
            fc.filter("form_id = ?", form);
        if (ct != 0) {
            fc.filter("ctime <= ?" ,  ct );
            fc.assort("ctime DESC");
            fc.gather("id");
        } else {
            fc.filter("etime = 0" );
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

        CmdletHelper.progres(dt, c,i);

        for(Map od : fc.select( )) {
            String id = ( String ) od.get( Cnst.ID_KEY );
            if (Synt.declare(od.get("state"), 1 ) <= 0 ) {
                dr.del (/**/ id /**/);
            } else
            if (Synt.declare(od.get("etime"), 0L) == 0L) {
                od = Synt.toMap (od.get( "data"));
                dr.set (/**/ id, od );
            } else {
                sd.put ("rtime", od.get("ctime"));
                dr.redo( dt, id, sd );
            }
            ds.remove(id);
            CmdletHelper.progres(dt, c, ++ i);
        }
        for(String id:ds) {
            dr.del   (id);
            CmdletHelper.progres(dt, c, ++ i);
        }
        if (c > i) {
            CmdletHelper.progred();
        }

        CmdletHelper.println("Revert "+i+" item(s) in "+dr.getDbName());
    }

    @Cmdlet("import")
    public static void impart(String[] args) throws HongsException {
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
        long dt = Core.ACTION_TIME .get(      );
        Data dr = Data.getInstance( conf,form );
        if (user == null) {
            user  = Cnst.ADM_UID;
        }

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
            data.put("memo", memo);
            i += dr.save(dt,id, data);
        }

        CmdletHelper.println("Import "+i+" item(s) to "+dr.getDbName());
    }

    @Cmdlet("update")
    public static void update(String[] args) throws HongsException {
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
        long dt = Core.ACTION_TIME .get(      );
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

        int i  = 0;
        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY) ;
            i += dr.save(dt, id, sd);
        }

        CmdletHelper.println("Update "+i+" item(s) in "+dr.getDbName());
    }

    @Cmdlet("delete")
    public static void delete(String[] args) throws HongsException {
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
        long dt = Core.ACTION_TIME .get(      );
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

        int i  = 0;
        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY) ;
            i += dr.drop(dt, id, sd);
        }

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
