package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.CommitRunner;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.PrivTable;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Inst;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

/**
 * 数据操作命令
 * @author hong
 */
@Combat("matrix.data")
public class DataCombat {

    @Combat("revert-all")
    public static void revertAll(String[] args)
    throws CruxException {
        Map opts = CombatHelper.getOpts(args, new String[] {
            "user:s",
            "memo:s",
            "time:i",
            "bufs:i",
            "truncate:b",
            "cascades:b",
            "includes:b",
            "incloses:b",
            "progress:b"
        });

        List<String> argz = new ArrayList(16);
        argz.add("--conf"); argz.add("");
        argz.add("--form"); argz.add("");
        if (opts.containsKey("user")) {
            argz.add("--user"); argz.add(Synt.asString(opts.get("user")));
        }
        if (opts.containsKey("memo")) {
            argz.add("--memo"); argz.add(Synt.asString(opts.get("memo")));
        }
        if (opts.containsKey("time")) {
            argz.add("--time"); argz.add(Synt.asString(opts.get("time")));
        }
        if (opts.containsKey("bufs")) {
            argz.add("--bufs"); argz.add(Synt.asString(opts.get("bufs")));
        }
        if (Synt.declare(opts.containsKey("truncate"), false)) {
            argz.add("--truncate");
        }
        if (Synt.declare(opts.containsKey("cascades"), false)) {
            argz.add("--cascades");
        }
        if (Synt.declare(opts.containsKey("includes"), false)) {
            argz.add("--includes");
        }
        if (Synt.declare(opts.containsKey("incloses"), false)) {
            argz.add("--incloses");
        }
        if (Synt.declare(opts.containsKey("progress"), false)) {
            argz.add("--progress");
        }
        args  = argz.toArray(new String[]{});

        Set<String> ls = getRevertNames();
        for(String  ln : ls) {
            int p = ln.indexOf(":");
            args[1] = ln.substring(0,p);
            args[3] = ln.substring(1+p);
            CombatHelper.println("revert "+Syno.concat(" ",(Object[])args));
            try {
                revert( args );
            }
            catch (CruxException e) {
                String s;
                ByteArrayOutputStream b;
                b = new ByteArrayOutputStream();
                e.printStackTrace(new PrintStream (b));
                s = b.toString().replaceAll("\r\n|\r|\n", "$0\t");
                CombatHelper.println(s);
            }
            if (CombatHelper.aborted()) {
                CombatHelper.println( "Task aborted" );
                return;
            }
        }
    }

    @Combat("revert")
    public static void revert(String[] args)
    throws CruxException {
        Map opts = CombatHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "time:i",
            "bufs:i",
            "truncate:b",
            "cascades:b",
            "includes:b",
            "incloses:b",
            "rollback:b",
            "progress:b",
            "!A",
            "?Usage: revert --conf CONF_NAME --form FORM_NAME [--time TIMESTAMP] ID0 ID1 ..."
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        Data dr = Data.getInstance(conf, form );
        Casc da = new Casc(dr, opts);

        revert(da, opts);
    }
    public static void revert(Casc da, Map opts)
    throws CruxException {
        Data dr = da.getInstance();
        String form = dr.getFormId();
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        long ct = Synt.declare(opts.get("time"),  0L );
        int  bn = Synt.declare(opts.get("bufs"), 1000);
        Set<String> ds = Synt.asSet ( opts.get( "" ) );

        dr.setUserId(Synt.defoult(user, Cnst.ADM_UID));

        Map sd = new HashMap();
        sd.put( "memo", memo );
        sd.put( "meno", "revert");

        Table  tb = dr.getTable();
        String tn = DB.Q(tb.tableName);
        PreparedStatement ps;
        ResultSet rs;
        Loop      lp;
        int  c  = 0 ; // 操作总数
        int  i  = 0 ; // 变更计数
        int  j  = 0 ; // 事务计数
        if (ct != 0) {
            String fx = "x.*" ;
            String fa = "a.id , MAX(a.ctime) AS ctime" ;
            String fc = "COUNT(DISTINCT a.id) AS _cnt_";
            String qa = "SELECT "+fa+" FROM "+tn+" AS a WHERE a.form_id = ? AND a.ctime <= ?";
            String qc = "SELECT "+fc+" FROM "+tn+" AS a WHERE a.form_id = ? AND a.ctime <= ?";
            String qx = " WHERE x.id = b.id AND x.ctime = b.ctime AND x.form_id = ? AND x.ctime <= ?";
            if (! ds.isEmpty() ) {
                c  = ds.size();
                qa = qa + " AND a.id IN (?)";
                qx = qx + " AND x.id IN (?)";
                qa = qa + " GROUP BY a.id"  ;
                qx = "SELECT "+fx+" FROM "+tn+" AS x, ("+qa+") AS b "+qx;
                ps = tb.db.prepare (qx, form, ct, ds, form, ct, ds);
            } else {
                c  = Synt .declare (
                     tb.db.fetchOne(qc, form, ct    )
                          .get("_cnt_"), 0 );
                qa = qa + " GROUP BY a.id"  ;
                qx = "SELECT "+fx+" FROM "+tn+" AS x, ("+qa+") AS b "+qx;
                ps = tb.db.prepare (qx, form, ct    , form, ct    );
            }
        } else {
            String fa = "a.*" ;
            String fc = "COUNT(*) AS _cnt_" ;
            String qa = "SELECT "+fa+" FROM "+tn+" AS a WHERE a.form_id = ? AND a.etime  = ?";
            String qc = "SELECT "+fc+" FROM "+tn+" AS a WHERE a.form_id = ? AND a.etime  = ?";
            if (! ds.isEmpty() ) {
                c  = ds.size();
                qa = qa + " AND a.id IN (?)";
                ps = tb.db.prepare (qa, form, ct, ds);
            } else {
                c  = Synt .declare (
                     tb.db.fetchOne(qc, form, ct    )
                          .get("_cnt_"), 0 );
                ps = tb.db.prepare (qa, form, ct    );
            }
        }
        CombatHelper.println("Search "+c+" item(s) for "+form);

        if (CombatHelper.aborted()) {
            CombatHelper.println( "Task aborted" );
            return;
        }

        // 规避 OOM, 连接参数需加 useCursorFetch=true
        try {
            tb.db.open().setAutoCommit(false);
            ps.setFetchSize(10000);
            rs = ps.executeQuery();
            lp = new Loop(rs , ps);
        } catch ( SQLException e ) {
            throw new CruxException(e);
        }

        /**
         * 清空全部数据
         * 以便更新结构
         */
        if (Synt.declare(opts.get("truncate"), false)) {
            da.truncate();
            CombatHelper.println( "Truncate "+ form );
        }

        if (CombatHelper.aborted()) {
            CombatHelper.println( "Task aborted" );
            return;
        }

        long tx = System.currentTimeMillis();
        boolean pr = Synt.declare(opts.get("progress"), false);

        da.begin ( );
        CombatHelper.println("Revert for "+form+" to "+dr.getDbName());
        if ( pr) {
            CombatHelper.progres(i, c);
        }
        if ( ct  !=  0  ) {
        for(Map od : lp ) {
            String id = ( String ) od.get( Cnst.ID_KEY ) ;
            sd.put( "name" , od.get( "name"));
            sd.put( "data" , od.get( "data"));
            sd.put("rtime" , od.get("ctime"));
            if (Synt.declare(od.get("etime"), 0L) != 0L) {
            long tc = System.currentTimeMillis( ) / 1000 ;
            if (Synt.declare(od.get("state"), 1 ) >= 1 ) {
                da.rev(id,sd,tc);
            }  else {
                da.del(id,sd,tc);
            }} else {
            if (Synt.declare(od.get("state"), 1 ) >= 1 ) {
                da.rev(id,sd);
            }  else {
                da.del(id,sd);
            }}
                ds.remove(id);
                i ++;
                j ++;
            if (j == bn) {
                j  =  0;
                da.commit(  );
                da.begin (  );
                if ( pr) {
                    CombatHelper.progres(i, c, System.currentTimeMillis() - tx);
                } else {
                    CombatHelper.println(i+"/"+c);
                }
            }
            if (CombatHelper.aborted()) {
                CombatHelper.println("Task aborted");
                return;
            }
        }} else {
        for(Map od : lp ) {
            String id = ( String ) od.get( Cnst.ID_KEY ) ;
            sd.put("rtime" , od.get("ctime"));
            sd.put( "data" , od.get( "data"));
            if (Synt.declare(od.get("state"), 1 ) >= 1 ) {
                da.rev(id,sd);
            }  else {
                da.del(id,sd);
            }
                ds.remove(id);
                i ++;
                j ++;
            if (j == bn) {
                j  =  0;
                da.commit(  );
                da.begin (  );
                if ( pr) {
                    CombatHelper.progres(i, c, System.currentTimeMillis() - tx);
                } else {
                    CombatHelper.println(i+"/"+c);
                }
            }
            if (CombatHelper.aborted()) {
                CombatHelper.println("Task aborted");
                return;
            }
        }}
        da.commit( );
        if ( pr) {
            CombatHelper.progres(i, c, System.currentTimeMillis() - tx);
        } else {
            CombatHelper.println(i+"/"+c);
        }
        CombatHelper.println("Revert "+i+" item(s) for "+form+" to "+dr.getDbName());

        if (CombatHelper.aborted()) {
            CombatHelper.println( "Task aborted" );
            return;
        }

        /**
         * 删掉多余数据
         */
        if (ds.isEmpty()) {
            return;
        }
        da.begin ( );
        for(String id:ds) {
            dr.delDoc(id);
        }
        da.commit( );
        i=ds.size( );
        CombatHelper.println("Remove "+i+" item(s) for "+form+" in "+dr.getDbName());
    }

    @Combat("repair-all")
    public static void repairAll(String[] args)
    throws CruxException {
        Map opts = CombatHelper.getOpts(args, new String[] {
            "skip:i",
            "bufs:i",
            "ends:i",
            "truncate:b",
            "cascades:b",
            "includes:b",
            "incloses:b",
            "progress:b"
        });

        List<String> argz = new ArrayList(16);
        argz.add("--conf"); argz.add("");
        argz.add("--form"); argz.add("");
        if (opts.containsKey("skip")) {
            argz.add("--skip"); argz.add(Synt.asString(opts.get("skip")));
        }
        if (opts.containsKey("bufs")) {
            argz.add("--bufs"); argz.add(Synt.asString(opts.get("bufs")));
        }
        if (opts.containsKey("ends")) {
            argz.add("--ends"); argz.add(Synt.asString(opts.get("ends")));
        }
        if (Synt.declare(opts.containsKey("truncate"), false)) {
            argz.add("--truncate");
        }
        if (Synt.declare(opts.containsKey("cascades"), false)) {
            argz.add("--cascades");
        }
        if (Synt.declare(opts.containsKey("includes"), false)) {
            argz.add("--includes");
        }
        if (Synt.declare(opts.containsKey("incloses"), false)) {
            argz.add("--incloses");
        }
        if (Synt.declare(opts.containsKey("progress"), false)) {
            argz.add("--progress");
        }
        args  = argz.toArray(new String[]{});

        Set<String> ls = getRevertNames();
        for(String  ln : ls) {
            int p = ln.indexOf(":");
            args[1] = ln.substring(0,p);
            args[3] = ln.substring(1+p);
            CombatHelper.println("repair "+Syno.concat(" ",(Object[])args));
            try {
                revert( args );
            }
            catch (CruxException e) {
                String s;
                ByteArrayOutputStream b;
                b = new ByteArrayOutputStream();
                e.printStackTrace(new PrintStream (b));
                s = b.toString().replaceAll("\r\n|\r|\n", "$0\t");
                CombatHelper.println(s);
            }
            if (CombatHelper.aborted()) {
                CombatHelper.println( "Task aborted" );
                return;
            }
        }
    }

    @Combat("repair")
    public static void repair(String[] args) throws CruxException, IOException, SQLException {
        Map opts = CombatHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "skip:i",
            "bufs:i",
            "ends:i",
            "truncate:b",
            "cascades:b",
            "includes:b",
            "incloses:b",
            "progress:b",
            "!A",
            "?Usage: repair --conf CONF_NAME --form FORM_NAME [--skip RNUM_SKIP] [--ends RNUM_ENDS]"
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        Data dr = Data.getInstance(conf, form );
        Casc da = new Casc(dr, opts);

        repair(da, opts);
    }
    public static void repair(Casc da, Map opts) throws CruxException, IOException, SQLException {
        Data   dr = da.getInstance();
        Table  tb = dr.getTable ( );
        String fn = dr.getFormId( );
        String tn = tb.tableName;

        int skip  = Synt.declare(opts.get("skip"), 0);
        int bufs  = Synt.declare(opts.get("bufs"), 0);
        int ends  = Synt.declare(opts.get("ends"), 0);

        if (bufs == 0) {
            bufs  = 1000;
        }
        if (ends == 0) {
            Map one = tb.db.fetchOne("SELECT MAX(rnum) AS rnum FROM "+tn);
            ends  = Synt.declare( one.get("rnum"), ends );
        }

        boolean p = Synt.declare(opts.get("progress"), false);

        /**
         * 清空全部数据
         * 以便更新结构
         */
        if (Synt.declare(opts.get("truncate"), false)) {
            da.truncate ( );
            CombatHelper.println( "Truncate "  + fn );
            if (CombatHelper.aborted()) {
                CombatHelper.println("Task aborted.");
                return;
            }
        }

        // 查询准备
        PreparedStatement ps;
        ResultSet         rs;
        Map               ro;
        ro = new HashMap(02);
        ps = tb.db.prepare("SELECT * FROM "+DB.quoteField(tn)+" WHERE form_id = "+DB.quoteValue(fn)+" AND etime = 0 AND rnum > ? AND rnum <= ?");

        // 计数计时
        int  i = 0;
        int  j = 0;
        int  k = skip;
        int  l = ends - skip;
        long t = System.currentTimeMillis( );

        for ( ; skip < ends ; skip += bufs ) {
            int lens = skip + bufs ;
            if (lens > ends) {
                lens = ends ;
            }

            ps.setInt(1, skip);
            ps.setInt(2, lens);
            rs = ps.executeQuery( );

            da.begin ( );
            while (rs.next()) {
                String id = rs.getString( "id" );
                String ds = rs.getString("data");
                int    st = rs.getInt( "state" );
                ro.put( "id" , id);
                ro.put("data", ds);
                if (st > 0) {
                    da.rev(id, ro);
                    i ++;
                } else {
                    da.del(id, ro);
                    j ++;
                }
            }
            rs.close ( );
            da.commit( );

            if (p) {
                float per = (float) (lens - k) / l;
                CombatHelper.progres(per, lens+" +"+i+",-"+j+" "+ Inst.phrase(System.currentTimeMillis() - t));
            } else {
                CombatHelper.println(/**/ lens+" +"+i+",-"+j+" "+ Inst.phrase(System.currentTimeMillis() - t));
            }

            if (CombatHelper.aborted()) {
                CombatHelper.println("Task aborted!");
                break;
            }
        }
        CombatHelper.printed();
    }

    @Combat("import")
    public static void impart(String[] args)
    throws CruxException {
        Map opts = CombatHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "!A",
            "?Usage: import --conf CONF_NAME --form FORM_NAME DATA DATA ..."
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        Data dr = Data.getInstance(conf, form );

        impart(dr, opts);
    }
    public static void impart(Data dr, Map opts)
    throws CruxException {
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        String[] dats = (String[]) opts.get("");

        dr.setUserId(Synt.defoult(user, Cnst.ADM_UID));

        dr.begin();

        int  i = 0;
        long t = System.currentTimeMillis( ) / 1000 ;
        for(String text : dats) {
            Map sd = data(text);
            String id = (String) sd.get(Cnst.ID_KEY);
            if (id == null) {
                id = Core.newIdentity();
                sd.put(Cnst.ID_KEY, id);
            }   sd.put( "memo" , memo );
            try {
                i += dr.put(id, sd, t );
            } catch (Exception ex) {
                throw new Exid(ex , id);
            }
        }

        dr.commit( );

        CombatHelper.println("Import "+i+" item(s) to "+dr.getFormId());
    }

    @Combat("update")
    public static void update(String[] args)
    throws CruxException {
        Map opts = CombatHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "!A",
            "?Usage: update --conf CONF_NAME --form FORM_NAME FIND DATA"
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        Data dr = Data.getInstance(conf, form );

        update(dr, opts);
    }
    public static void update(Data dr, Map opts)
    throws CruxException {
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        String[] dats = (String[]) opts.get("");

        if (dats.length < 2) {
            CombatHelper.println ( "Need FIND DATA." );
            return;
        }

        dr.setUserId(Synt.defoult(user, Cnst.ADM_UID));

        Map rd = data(dats[0]);
        Map sd = data(dats[1]);
        sd.put( "memo", memo );
        rd.put(Cnst.RB_KEY , Synt.setOf(Cnst.ID_KEY));

        dr.begin();

        int  i = 0;
        long t = System.currentTimeMillis( ) / 1000 ;
        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY);
            try {
                i += dr.put(id, sd, t);
            } catch (Exception ex) {
                throw new Exid(ex, id);
            }
        }

        dr.commit( );

        CombatHelper.println("Update "+i+" item(s) in "+dr.getFormId());
    }

    @Combat("delete")
    public static void delete(String[] args)
    throws CruxException {
        Map opts = CombatHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "!A",
            "?Usage: delete --conf CONF_NAME --form FORM_NAME FIND_TERM"
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        Data dr = Data.getInstance(conf, form );

        delete(dr, opts);
    }
    public static void delete(Data dr, Map opts)
    throws CruxException {
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        String[] dats = (String[]) opts.get("");

        if (dats.length < 1) {
            CombatHelper.println ( "Need FIND_TERM." );
            return;
        }

        dr.setUserId(Synt.defoult(user, Cnst.ADM_UID));

        Map rd = data(dats[0]);
        Map sd = new HashMap();
        sd.put( "memo", memo );
        rd.put(Cnst.RB_KEY , Synt.setOf(Cnst.ID_KEY));

        dr.begin();

        int  i = 0;
        long t = System.currentTimeMillis( ) / 1000 ;
        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY);
            try {
                i += dr.del(id, sd, t);
            } catch (Exception ex) {
                throw new Exid(ex, id);
            }
        }

        dr.commit( );

        CombatHelper.println("Delete "+i+" item(s) in "+dr.getFormId());
    }

    @Combat("search")
    public static void search(String[] args)
    throws CruxException {
        Map opts = CombatHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "!A",
            "?Usage: search --conf CONF_NAME --form FORM_NAME FIND_TERM"
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        Data dr = Data.getInstance(conf, form );

        search(dr, opts);
    }
    public static void search(Data dr, Map opts)
    throws CruxException {
        String[] dats = (String[]) opts.get("");

        if (dats.length < 1) {
            CombatHelper.println ( "Need FIND_TERM." );
            return;
        }

        Map rd = data(dats[0]);

        for(Map od : dr.search(rd, 0, 0)) {
            CombatHelper.println(od);
        }
    }

    /**
     * 归并命令
     * @param args
     * @throws CruxException
     */
    @Combat("uproot")
    public static void uproot(String[] args)
    throws CruxException {
        Map opts = CombatHelper.getOpts(
            args ,
            "uid=s" ,
            "uids=s",
            "conf:s",
            "form:s",
            "?Usage: attach --uid UID --uids UID1,UID2... [--conf CONF_NAME --form FORM_NAME]"
        );

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        String uid  = (String) opts.get("uid" );
        String uidz = (String) opts.get("uids");

        Set<String> uids = Synt.toSet( uidz );

        // 获取路径
        Set<String> ents ;
        if (conf != null && ! conf.isEmpty( )
        &&  form != null && ! form.isEmpty()) {
            ents  = Synt.setOf(conf+":"+form);
        }  else  {
            ents  = getUprootNames();
        }

        // 批量更新
        CommitRunner.run(() -> {
            try {
                for(String n : ents) {
                    int    p = n.lastIndexOf(".");
                    String c = n.substring(0 , p);
                    String f = n.substring(1 + p);
                    Data   d = Data.getInstance(c, f);
                    uproot(d , uid , uids);
                }
            } catch (CruxException ex) {
                throw ex.toExemption();
            }
        });
    }
    public static void uproot(Data ent, String uid, Set<String> uids)
    throws CruxException {
        Map cols = ent .getFields();
        Set colz = new HashSet();
        Map relz = new HashMap();

        // 组织条件, 类似: fn1 IN (uids) OR fn2 IN (uids)
        for(Object ot : cols.entrySet()) {
            Map.Entry et = (Map.Entry) ot;
            Map    fc = (Map   ) et.getValue();
            String fn = (String) et.getKey  ();
            String tp = (String) fc.get("__type__");
            String cf = (String) fc.get(  "conf"  );
            String mf = (String) fc.get(  "form"  );
            String at = (String) fc.get("data-at" );
            // 关联到用户的规则:
            // 类型为 fork 或者 pick
            // 表单为 master 的 user, 或关联接口为 master/user/list
            if (("fork".equals(tp) ||   "pick".equals(tp))
            && (("user".equals(mf) && "master".equals(cf))
            || "centra/master/user/list".equals(at)
            || "centre/master/user/list".equals(at)
            ))  {
                relz.put(
                    fn, Synt.mapOf(
                    fn, Synt.mapOf(
                        Cnst.IN_REL, uids
                    ))
                );
            }
        }

        // 没有关联到用户则不必处理
        if (relz.isEmpty()) return;

        colz.addAll(relz.keySet());
        colz.add   (Cnst.ID_KEY  );

        // 查询数据, 逐条将 uids 置换为 uid
        Data.Loop loop = ent.search(Synt.mapOf(
            Cnst.RB_KEY , colz,
            Cnst.OR_KEY , relz
        ) , 0 , 0);
        for(Map row : loop) {
            String id = (String) row.get(Cnst.ID_KEY);

            // 寻找那些包含 uids 的换为 uid
            for(Object fn : relz.keySet()) {
                Object fv = row .get(fn);
                if (fv == null) continue;
                if (fv instanceof Collection) {
                    List   val = Synt.asList  (fv);
                    if (val.removeAll(uids) ) {
                        val.add /**/ (uid );
                        row.put( fn , val );
                    }
                } else {
                    String str = Synt.asString(fv);
                    if (uids.contains(str ) ) {
                        row.put( fn , uid );
                    }
                }
            }

            row.put("meno", "system");
            row.put("memo", "uproot");
            ent.put( id, row );
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
            return ( Map ) Dist.toObject  (text);
        } else {
            return ActionHelper.parseQuery(text);
        }
    }

    /**
     * 获取全部可恢复模型名
     * @return
     */
    public static Set<String> getRevertNames() {
        Set<String> ents = new LinkedHashSet();

        // 提取所有表单记录
        try {
            // furl_id 为 - 表示这是一个内置关联项, 这样的无需处理
            Loop lo = DB
                .getInstance("matrix")
                .getTable   ( "form" )
                .fetchCase  ( )
                .filter("state > 0 AND furl_id != '-'")
                .select("id")
                .select();
            for(Map ro : lo ) {
                String id = ro.get("id").toString();
                ents.add("centra/data/" +id+":"+id);
            }
        } catch (CruxException ex) {
            throw ex.toExemption( );
        }

        // 增加额外定制的表
        Set<String> incl = Synt.toSet(
            CoreConfig.getInstance("matrix")
                      .getProperty("core.matrix.revert.include")
        );
        if (incl != null && ! incl.isEmpty()) {
            ents.   addAll(incl);
        }

        // 排除特殊处理的表
        Set<String> excl = Synt.toSet(
            CoreConfig.getInstance("matrix")
                      .getProperty("core.matrix.revert.exclude")
        );
        if (excl != null && ! excl.isEmpty()) {
            ents.removeAll(excl);
        }

        return ents;
    }

    /**
     * 获取全部需归并模型名
     * @return
     */
    public static Set<String> getUprootNames() {
        Set<String> ents = new LinkedHashSet();

        // 提取所有表单记录
        try {
            // furl_id 为 - 表示这是一个内置关联项, 这样的无需处理
            Loop lo = DB
                .getInstance("matrix")
                .getTable   ( "form" )
                .fetchCase  ( )
                .filter("state > 0 AND furl_id != '-'")
                .select("id")
                .select();
            for(Map ro : lo ) {
                String id = ro.get("id").toString();
                ents.add("centra/data/" +id+":"+id);
            }
        } catch (CruxException ex) {
            throw ex.toExemption( );
        }

        // 增加额外定制的表
        Set<String> incl = Synt.toSet(
            CoreConfig.getInstance("matrix")
                      .getProperty("core.matrix.uproot.include")
        );
        if (incl != null && ! incl.isEmpty()) {
            ents.   addAll(incl);
        }

        // 排除特殊处理的表
        Set<String> excl = Synt.toSet(
            CoreConfig.getInstance("matrix")
                      .getProperty("core.matrix.uproot.exclude")
        );
        if (excl != null && ! excl.isEmpty()) {
            ents.removeAll(excl);
        }

        return ents;
    }

    /**
     * 级联操作代理类,
     * 供恢复数据时用.
     */
    public static class Casc {

        protected final Data    that    ;
        protected final Table   table   ;
        protected final boolean cascades;
        protected final boolean includes;
        protected final boolean incloses;
        protected final boolean rollback;
        protected final Consumer<Map> dc;

        public Casc (Data data, boolean cascades, boolean includes, boolean incloses, boolean rollback)
        throws CruxException {
            this.that  =  data;
            this.cascades = cascades;
            this.includes = includes;
            this.incloses = incloses;
            this.rollback = rollback;

            // 解密, 用于下方数据恢复
            table  = data.getTable();
            if ( null != table && table instanceof PrivTable ) {
                dc = ((PrivTable) table).decrypt();
            } else {
                dc = ( m ) -> { };
            }
        }

        public Casc (Data data, Map opts)
        throws CruxException {
            this(
                data,
                Synt.declare (opts.get("cascades"), false),
                Synt.declare (opts.get("includes"), false),
                Synt.declare (opts.get("incloses"), false),
                Synt.declare (opts.get("rollback"), false)
            );
        }

        public Data getInstance() {
            return that;
        }

        public void rev(String id, Map sd, long ctime) throws CruxException {
            try {
                // 解密并解析
                dec(sd);
                Map dd = that.getData((String) sd.get("data"));
                long rtime = Synt.declare(sd.get("rtime"), 0L);

                // 填充并写入
                pad( id, dd );
                set( id, dd );

            if (rollback) {
                FenceCase sc = that.fenceCase ( );
                Object[] param = new Object[] {id, rtime};
                String   where = "id = ? AND ctime = ?";
                String   wher2 = "id = ? AND ctime > ?";

                Map ud = new HashMap();
                ud.put("etime",   0  );

                sc.filter(wher2, param);
                sc.delete(  );

                sc.filter(where, param);
                sc.update(ud);
            } else {
                FenceCase sc = that.fenceCase ( );
                Object[] param = new String[] {id, "0"};
                String   where = "id = ? AND etime = ?";

                Map ud = new HashMap();
                ud.put("etime", ctime);

                Map nd = new HashMap();
                nd.put("ctime", ctime);
                nd.put("rtime", rtime);
                nd.put("etime",   0  );
                nd.put("state",   3  );
                nd.put(  "id" ,  id  );

                // 数据快照和日志标题
                nd.put("data", sd.get("data"));
                nd.put("name", sd.get("name"));

                // 操作备注和终端代码
                if (sd.containsKey("memo")) {
                    nd.put("memo", that.getTval(sd, "memo"));
                }
                if (sd.containsKey("meno")) {
                    nd.put("meno", that.getTval(sd, "meno"));
                }

                sc.filter(where, param);
                sc.update(ud);

                sc.insert(nd);
            } // End rollback
            } catch (Exception ex) {
                throw new Exid(ex, id);
            }
        }

        public void rev(String id, Map sd) throws CruxException {
            try {
                // 解密并解析
                dec(sd);
                Map dd = that.getData((String) sd.get("data"));

                // 填充并写入
                pad( id, dd );
                set( id, dd );
            } catch (Exception ex) {
                throw new Exid(ex, id);
            }
        }

        public void del(String id, Map sd, long ctime) throws CruxException {
            try {
                that.delDoc(id);

                long rtime = Synt.declare(sd.get("rtime"), 0L);

            if (rollback) {
                FenceCase sc = that.fenceCase ( );
                Object[] param = new Object[] {id, rtime};
                String   where = "id = ? AND ctime = ?";
                String   wher2 = "id = ? AND ctime > ?";

                Map ud = new HashMap();
                ud.put("etime",   0  );

                sc.filter(wher2, param);
                sc.delete(  );

                sc.filter(where, param);
                sc.update(ud);
            } else {
                FenceCase sc = that.fenceCase ( );
                Object[] param = new String[] {id, "0"};
                String   where = "id = ? AND etime = ?";

                Map ud = new HashMap();
                ud.put("etime", ctime);

                Map nd = new HashMap();
                nd.put("ctime", ctime);
                nd.put("rtime", rtime);
                nd.put("etime",   0  );
                nd.put("state",   0  );
                nd.put(  "id" ,  id  );

                // 数据快照和日志标题
                nd.put("data", sd.get("data"));
                nd.put("name", sd.get("name"));

                // 操作备注和终端代码
                if (sd.containsKey("memo")) {
                    nd.put("memo", that.getTval(sd, "memo"));
                }
                if (sd.containsKey("meno")) {
                    nd.put("meno", that.getTval(sd, "meno"));
                }

                sc.filter(where, param);
                sc.update(ud);

                sc.insert(nd);
            } // End rollback
            } catch (Exception ex) {
                throw new Exid(ex, id);
            }
        }

        public void del(String id, Map sd) throws CruxException {
            try {
                that.delDoc(id);
            } catch (Exception ex) {
                throw new Exid(ex, id);
            }
        }

        public void pad(String id, Map dd) throws CruxException {
            if (includes) that.includes(dd);
            if (incloses) that.incloses(dd);
        }

        public void set(String id, Map dd) throws CruxException {
            that.setDoc(id, that.padDoc(dd));
        }

        public void dec(Map sd) {
            dc.accept(sd);
        }

        public void begin( ) {
            that.begin( );
            if (table != null) {
                table.db.begin( );
            }
        }

        public void commit() {
            if (cascades) {
                that.commit( );
            } else {
                that.submit( );
            }
            if (table != null) {
                table.db.commit();
            }
        }

        public void truncate() throws CruxException {
            IndexWriter iw = that.getWriter();
            /**/ String pd = that.getPartId();
            try {
                if (pd != null && ! pd.isEmpty()) {
                    iw.deleteDocuments(new Term("@"+Data.PART_ID_KEY, pd));
                } else {
                    iw.deleteAll(/**/);
                }   iw.commit();
                iw.deleteUnusedFiles();
                iw.maybeMerge();
            } catch ( IOException ex ) {
                throw new CruxException(ex);
            }
        }

    }

    /**
     * 异常携带ID
     * 以便于定位
     */
    public static class Exid extends CruxException {

        private final String id ;

        public Exid(Throwable cause, String id) {
            super(cause);
            this.id = id;
        }

        @Override
        public String toString() {
            return getMessage ();
        }

        @Override
        public String getMessage() {
            return "ID: "+id+". "+getCause().getMessage();
        }

        @Override
        public String getLocalizedMessage() {
            return "ID: "+id+". "+getCause().getLocalizedMessage();
        }

    }

}
