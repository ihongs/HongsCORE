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
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    @Combat("revert")
    public static void revert(String[] args)
    throws CruxException, InterruptedException {
        revert(args, new Inst());
    }
    public static void revert(String[] args, Inst df)
    throws CruxException, InterruptedException {
        Map opts = CombatHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "time:i",
            "bufs:i",
            "drop:b",
            "includes:b",
            "cascades:b",
            "!A",
            "?Usage: revert --conf CONF_NAME --form FORM_NAME [--time TIMESTAMP] ID0 ID1 ..."
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        long ct = Synt.declare(opts.get("time"),  0L );
        int  bn = Synt.declare(opts.get("bufs"), 1000);
        Set<String> ds = Synt.asSet ( opts.get( "" ) );

        Data dr = df.getInstance (conf, form);
        dr.setUserId(Synt.defoult(user, Cnst.ADM_UID));
//      user = dr.getUserId( );
        form = dr.getFormId( );

        Map sd = new HashMap();
        sd.put( "memo", memo );
        sd.put( "meno", "revert");

        Table  tb = dr.getTable();
        String tn = tb.tableName ;
        PreparedStatement ps;
        ResultSet rs;
        Loop      lp;
        int  c  = 0 ; // 操作总数
        int  i  = 0 ; // 变更计数
        int  j  = 0 ; // 事务计数
        if (ct == 0) {
            String fa = "`a`.*"  ;
            String fc = "COUNT(*) AS _cnt_" ;
            String qa = "SELECT "+fa+" FROM `"+tn+"` AS `a` WHERE `a`.`form_id` = ? AND `a`.`etime`  = ?";
            String qc = "SELECT "+fc+" FROM `"+tn+"` AS `a` WHERE `a`.`form_id` = ? AND `a`.`etime`  = ?";
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
                ps = tb.db.prepare (qx, form, ct, ds, form, ct, ds);
            } else {
                c  = Synt .declare (
                     tb.db.fetchOne(qc, form, ct    )
                          .get("_cnt_"), 0 );
                qa = qa + " GROUP BY `a`.id";
                qx = "SELECT "+fx+" FROM `"+tn+"` AS `x`, ("+qa+") AS `b` "+qx;
                ps = tb.db.prepare (qx, form, ct    , form, ct    );
            }
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
        if (Synt.declare(opts.get("drop"), false)) {
            IndexWriter iw = dr.getWriter();
            /**/ String pd = dr.getPartId();
            try {
                if (pd != null && ! pd.isEmpty( )) {
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

        /**
         * 级联更新操作
         * 默认不作级联
         */
        Casc da = new Casc(
             dr ,
             Synt.declare (opts.get("includes") , false) ,
             Synt.declare (opts.get("cascades") , false)
        );

        boolean pr = ( Core . DEBUG  ==  0 );
        long tm = System.currentTimeMillis();
        long tc = tm / 1000 ;
        if (pr) CombatHelper.progres( i, c );

        dr.begin ( );

        for(Map od : lp ) {
            String id = ( String ) od.get( Cnst.ID_KEY ) ;
            if (Synt.declare(od.get("etime"), 0L) != 0L) {
            if (Synt.declare(od.get("state"), 1 ) >= 1 ) {
                sd.put("rtime" , od.get("ctime"));
                sd.put( "data" , od.get( "data"));
                da.rev(id,sd,tc);
            }  else {
                da.del(id,sd,tc);
            }} else {
            if (Synt.declare(od.get("state"), 1 ) >= 1 ) {
                sd.put("rtime" , od.get("ctime"));
                sd.put( "data" , od.get( "data"));
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
                dr.begin (  );
                if ( pr) {
                    CombatHelper.progres(i, c);
                }
            }
        }

        da.commit( );
        dr.begin ( );
        if ( pr) {
            CombatHelper.progres(i, c);
        }

        /**
         * 删掉多余数据
         */
        for(String id:ds) {
            dr.delDoc(id);
        }

        da.commit( );
        if ( pr) {
            CombatHelper.progres( );
        }

        CombatHelper.println("Revert "+i+" item(s) for "+form+" to "+dr.getDbName());
    }

    @Combat("import")
    public static void impart(String[] args)
    throws CruxException, InterruptedException {
        impart(args, new Inst());
    }
    public static void impart(String[] args, Inst df)
    throws CruxException, InterruptedException {
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
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        String[] dats = (String[]) opts.get("");

        Data dr = df.getInstance (conf, form);
        dr.setUserId(Synt.defoult(user, Cnst.ADM_UID));
//      user = dr.getUserId( );
//      form = dr.getFormId( );

        dr.begin();

        int i = 0 ;
        for(String text : dats) {
            Map sd = data(text);
            String id = (String) sd.get(Cnst.ID_KEY);
            if (id == null) {
                id = Core.newIdentity();
                sd.put(Cnst.ID_KEY, id);
            }   sd.put( "memo" , memo );
            i+= dr.put( id, sd );
        }

        dr.commit( );

        CombatHelper.println("Import "+i+" item(s) to "+dr.getFormId());
    }

    @Combat("update")
    public static void update(String[] args)
    throws CruxException, InterruptedException {
        update(args, new Inst());
    }
    public static void update(String[] args, Inst df)
    throws CruxException, InterruptedException {
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
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        String[] dats = (String[]) opts.get("");

        if (dats.length < 2) {
            CombatHelper.println ( "Need FIND DATA." );
            return;
        }

        Data dr = df.getInstance (conf, form);
        dr.setUserId(Synt.defoult(user, Cnst.ADM_UID));
//      user = dr.getUserId( );
//      form = dr.getFormId( );

        Map rd = data(dats[0]);
        Map sd = data(dats[1]);
        sd.put( "memo", memo );
        rd.put(Cnst.RB_KEY , Synt.setOf(Cnst.ID_KEY));

        dr.begin();

        int i = 0 ;
        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY);
            i+= dr.put( id, sd );
        }

        dr.commit( );

        CombatHelper.println("Update "+i+" item(s) in "+dr.getFormId());
    }

    @Combat("delete")
    public static void delete(String[] args)
    throws CruxException, InterruptedException {
        delete(args, new Inst());
    }
    public static void delete(String[] args, Inst df)
    throws CruxException, InterruptedException {
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
        String user = (String) opts.get("user");
        String memo = (String) opts.get("memo");
        String[] dats = (String[]) opts.get("");

        if (dats.length < 1) {
            CombatHelper.println ( "Need FIND_TERM." );
            return;
        }

        Data dr = df.getInstance (conf, form);
        dr.setUserId(Synt.defoult(user, Cnst.ADM_UID));
//      user = dr.getUserId( );
//      form = dr.getFormId( );

        Map rd = data(dats[0]);
        Map sd = new HashMap();
        sd.put( "memo", memo );
        rd.put(Cnst.RB_KEY , Synt.setOf(Cnst.ID_KEY));

        dr.begin();

        int  i = 0;
        long t = System.currentTimeMillis( ) / 1000 ;
        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY);
            i+= dr.del( id, sd, t );
        }

        dr.commit( );

        CombatHelper.println("Delete "+i+" item(s) in "+dr.getFormId());
    }

    @Combat("search")
    public static void search(String[] args)
    throws CruxException {
        search(args, new Inst());
    }
    public static void search(String[] args, Inst df)
    throws CruxException {
        Map opts = CombatHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "!A",
            "?Usage: search --conf CONF_NAME --form FORM_NAME FIND_TERM"
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        String[] dats = (String[]) opts.get("");

        if (dats.length < 1) {
            CombatHelper.println ( "Need FIND_TERM." );
            return;
        }

        Data dr = df.getInstance (conf, form);
        Map  rd = data ( dats[0] );

        for(Map od : dr.search(rd, 0, 0)) {
            CombatHelper.preview (  od  );
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
        uproot(args, new Inst());
    }
    public static void uproot(String[] args, Inst df)
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

        Set<String> uids = Synt.toSet (uidz);
        Set<String> ents ;

        // 获取路径
        if (conf == null || conf.isEmpty()
        ||  form == null || form.isEmpty()) {
            ents  = df.getAllPaths();
        if (ents == null || ents.isEmpty()) {
            return;
        }}  else  {
            ents  = Synt.setOf(conf + "." + form);
        }

        // 批量更新
        CommitRunner.run(() -> {
            try {
                for(String n : ents) {
                    int    p = n.lastIndexOf(".");
                    String c = n.substring(0 , p);
                    String f = n.substring(1 + p);
                    uproot (df.getInstance(c , f), uid, uids);
                }
            } catch (CruxException ex) {
                throw ex.toExemption();
            }
        });
    }

    /**
     * 归并账号(所有模型的数据)
     * @param uid
     * @param uids
     * @throws CruxException
     */
    public static void uproot(String uid, Set<String> uids) throws CruxException {
        Set<String> ents = new Inst( ).getAllPaths( );
        for(String n : ents) {
            int    p = n.lastIndexOf(".");
            String c = n.substring (0, p);
            String f = n.substring (1+ p);
            uproot(Data.getInstance(c, f), uid, uids);
        }
    }

    /**
     * 归并账号(指定模型的数据)
     * @param ent
     * @param uid
     * @param uids
     * @throws CruxException
     */
    public static void uproot(Data ent, String uid, Set<String> uids) throws CruxException {
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
     * 数据实体工厂类,
     * 供扩展实体时用.
     */
    public static class Inst {

        public Data getInstance(String conf, String form) throws CruxException {
            return  Data . getInstance(conf, form);
        }

        public Set<String> getAllPaths() {
            Set<String> ents = new LinkedHashSet();

            // 提取所有表单记录
            try {
                // furl_id 为 - 表示这是一个内置关联项, 这样的无需处理
                Loop lo = DB
                    .getInstance("matrix")
                    .getTable   ( "form" )
                    .fetchCase  ( )
                    .filter("`state` > 0")
                    .filter("`furl_id` != '-'")
                    .select("`id`")
                    .select();
                for(Map ro : lo ) {
                    String id = ro.get("id").toString();
                    ents.add("centra/data/" +id+"."+id);
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

    }

    /**
     * 级联操作代理类,
     * 供恢复数据时用.
     */
    public static class Casc {

        private final Data    that    ;
        private final boolean includes;
        private final boolean cascades;
        private final Consumer<Map> dc;

        public Casc (Data data, boolean includes, boolean cascades)
        throws CruxException {
            this.that  =  data;
            this.includes = includes;
            this.cascades = cascades;

            // 解密, 用于下方数据恢复
            Table tb = data.getTable();
            if (tb != null && tb instanceof PrivTable) {
                dc  = ((PrivTable) tb).decrypt();
            } else {
                dc  = ( Map m ) -> { };
            }
        }

        public void del(String id, Map sd, long ctime) throws CruxException {
            that.del(id, sd, ctime);
        }

        public void del(String id, Map sd) throws CruxException {
            that.delDoc( id );
        }

        public void rev(String id, Map sd, long ctime) throws CruxException {
            String   uid   = that.getUserId();
            String   fid   = that.getFormId();
            Object[] param = new String[] {id, fid, "0"};
            String   where = "`id`=? AND `form_id`=? AND `etime`=?";
            long     rtime = Synt.declare (sd. get( "rtime" ), 0L );

            // 解密并解析
            dc.accept(sd);
            Map od = that.getData((String) sd. get( "data" ));

            if ( includes ) that.padInf(od,od);
            that.setDoc(id, that.padDoc(od ) );

            Map ud = new HashMap();
            ud.put("etime", ctime);

            Map nd = new HashMap();
            nd.put("ctime", ctime);
            nd.put("rtime", rtime);
            nd.put("etime",   0  );
            nd.put("state",   3  );
            nd.put("form_id", fid);
            nd.put("user_id", uid);

            // 数据快照和日志标题
            nd.put("__data__", od);
            nd.put("data", that.getData(od));
            nd.put("name", that.getText(od, "name"));

            // 操作备注和终端代码
            if (od.containsKey("memo")) {
                nd.put("memo", that.getText(sd, "memo"));
            }
            if (od.containsKey("meno")) {
                nd.put("meno", that.getText(sd, "meno"));
            }

            that.getTable().update(ud, where, param);
            that.getTable().insert(nd);
        }

        public void rev(String id, Map sd) throws CruxException {
            // 解密并解析
            dc.accept(sd);
            Map od = that.getData((String) sd. get( "data" ));

            if ( includes ) that.padInf(od,od);
            that.setDoc(id, that.padDoc(od ) );
        }

        public void commit( ) {
            if ( cascades ) that.commit( );
            else            that.submit( );
        }

    }

}
