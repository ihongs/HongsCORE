package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.dh.ITrnsct;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Synt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
                sd.put("rtime" , od.get("ctime"));
                dr.rev(id,sd);
            }  else  {
                dr.del(id,sd);
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

        int i = 0 ;
        for(String text : dats) {
            Map sd = data(text);
            String id = (String) sd.get(Cnst.ID_KEY);
            if (id == null) { //
                id = Core.newIdentity();
                sd.put(Cnst.ID_KEY, id);
            }   sd.put( "memo" , memo );
            i+= dr.put( id, sd );
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

        int i = 0 ;
        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY);
            i+= dr.put( id, sd );
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

        int i = 0 ;
        for(Map od : dr.search(rd, 0, 0)) {
            String id = (String) od.get(Cnst.ID_KEY);
            i+= dr.del( id, sd );
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

    /**
     * 归并命令
     * @param args
     * @throws HongsException
     */
    @Cmdlet("uproot")
    public void uproot(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(
            args ,
            "uid=s" ,
            "uids=s",
            "conf=s",
            "form=s",
            "?Usage: attach --uid UID --uids UID1,UID2... [--conf CONF_NAME --form FORM_NAME]"
        );

        String uid  = (String) opts.get("uid" );
        String uidz = (String) opts.get("uids");
        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");

        Set<String> uids = Synt.toSet(uidz);
        List<Data>  ents = new ArrayList ();

        if ((conf == null || conf.isEmpty() )
        && ( form == null || form.isEmpty())) {
            // unit_id 为 - 表示这是一个内置关联项, 这样的无需处理
            Loop loop;
            loop = DB.getInstance("matrix")
                     .getTable   ( "data" )
                     .fetchCase  ( )
                     .filter("`state` > 0")
                     .filter("`unit_id` != '-'")
                     .select("`id`")
                     .select(  );
            conf = "centra/data";
        for(Map row : loop) {
            form = Synt.asString(row.get( "id" ));
            ents.add(Data.getInstance(conf,form));
        } } else {
            ents.add(Data.getInstance(conf,form));
        }

        // 在全局中标识为事务模式
        Core   core;
        Object mode;
        core = Core.getInstance( );
        mode = core.got(Cnst.TRNSCT_MODE);
        core.put(Cnst.TRNSCT_MODE , true);

        try {
            // 开启
            for(Object o : core.values()) {
                if (o instanceof ITrnsct) {
                    ((ITrnsct) o).begin();
                }
            }

            try {
                for(  Data ent : ents) {
                    uproot(ent , uid , uids);
                }

                // 提交
                for(Object o : core.values().toArray()) {
                    if (o instanceof ITrnsct) {
                        ((ITrnsct) o).commit();
                    }
                }
            } catch (Throwable ex) {
                // 回滚
                for(Object o : core.values().toArray()) {
                    if (o instanceof ITrnsct) {
                        ((ITrnsct) o).revert();
                    }
                }

                throw ex;
            }
        } finally {
            if (mode  != null) {
                core.put(Cnst.TRNSCT_MODE, mode);
            }
        }
    }

    /**
     * 归并账号
     * @param uid
     * @param uids
     * @throws HongsException
     */
    public static void uproot(String uid, Set<String> uids) throws HongsException {
        Set<String> ents = new LinkedHashSet();

        // 提取所有表单记录
        // unit_id 为 - 表示这是一个内置关联项, 这样的无需处理
        Loop lo = DB
                .getInstance("matrix")
                .getTable   ( "form" )
                .fetchCase  ( )
                .filter("`state` > 0")
                .filter("`unit_id` != '-'")
                .select("`id`")
                .select();
        for(Map ro : lo ) {
            String id = ro.get("id").toString( );
            ents.add( "centra/data/"+id+"."+id );
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

        // 逐一进行账号归并
        for(String n : ents) {
            int    p = n.lastIndexOf(".");
            String c = n.substring (0, p);
            String f = n.substring (1+ p);
            uproot(Data.getInstance(c, f), uid, uids);
        }
    }

    /**
     * 归并账号
     * @param ent
     * @param uid
     * @param uids
     * @throws HongsException
     */
    public static void uproot(Data ent, String uid, Set<String> uids) throws HongsException {
        Map cols = ent .getFields();
        Set colz = new HashSet();
        Map relz = new HashMap();
        long now = System.currentTimeMillis() / 1000;

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
            return ( Map ) Dawn.toObject  (text);
        } else {
            return ActionHelper.parseQuery(text);
        }
    }

}
