package io.github.ihongs.serv.master;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.util.Synt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 用户维护命令
 * @author hong
 */
@Combat("master.user")
public class UserCombat {

    /**
     * 归并命令
     * @param args
     * @throws HongsException
     */
    @Combat("uproot")
    public static void uproot(String[] args) throws HongsException {
        Map opts = CombatHelper.getOpts(
            args ,
            "uid=s" ,
            "uids=s",
            "!A",
            "!U",
            "?Usage: attach --uid UID --uids UID1,UID2..."
        );

        String uid  = (String) opts.get("uid" );
        String uidz = (String) opts.get("uids");
        Set<String> uids = Synt.toSet  ( uidz );

        DB  db = DB.getInstance("master");
        try {
            db.begin( );
            uproot(uid , uids);
            db.commit();
        } catch (HongsException ex) {
            db.revert();
            throw ex;
        }
    }

    /**
     * 归并账号
     * @param uid  目标账号
     * @param uids 被并账号
     * @throws HongsException
     */
    public static void uproot(String uid, Set<String> uids) throws HongsException {
        DB    db;
        Table tb;
        Loop  lo;

        db = DB.getInstance("master");

        //** 关联登录 **/

        tb = db.getTable("user_sign");
        tb.update(Synt.mapOf(
            "user_id", uid
        ), "`user_id` IN (?)", uids );

        //** 用户权限 **/

        tb = db.getTable("user_role");

        lo = tb.fetchCase()
               .filter("`user_id` = ?", uid)
               .select("`role`")
               .select();
        Set rids = new HashSet();
        for(Map ro : lo) {
            rids.add(ro.get("role"));
        }

        lo = tb.fetchCase()
               .filter("`user_id` IN (?) AND `role` NOT IN (?)", uids, rids)
               .select("`role`")
               .select();
            rids.clear();
        for(Map ro : lo) {
            rids.add(ro.get("role"));
        }

        for(Object rid : rids) {
            tb.insert(Synt.mapOf(
                "role"   , rid,
                "user_id", uid
            ));
        }

        //** 用户分组 **/

        tb = db.getTable("dept_user");

        lo = tb.fetchCase()
               .filter("`user_id` = ?", uid)
               .select("`dept_id`")
               .select();
        Set dids = new HashSet();
        for(Map ro : lo) {
            dids.add(ro.get("dept_id"));
        }

        lo = tb.fetchCase()
               .filter("`user_id` IN (?) AND `dept_id` NOT IN (?)", uids, dids)
               .select("`dept_id`")
               .select();
            dids.clear();
        for(Map ro : lo) {
            dids.add(ro.get("dept_id"));
        }

        for(Object did : dids) {
            tb.insert(Synt.mapOf(
                "dept_id", did,
                "user_id", uid
            ));
        }

        //** 用户资料 **/

        tb = db.getTable("user");

        lo = tb.fetchCase()
               .filter("`id` = ?", uid)
               .select("`phone`,`phone_checked`,`email`,`email_checked`,`username`")
               .select();
        Map info = new HashMap();
        boolean phoneChecked = false;
        boolean emailChecked = false;
        boolean loginChecked = false;
        for(Map ro : lo) {
            info.putAll(ro);

            Object phone = info.get("phone");
            if (Synt.declare(ro.get("phone_checked"), false)
            &&  phone != null && ! phone.equals("")) {
                phoneChecked = true ;
            }

            Object email = info.get("email");
            if (Synt.declare(ro.get("email_checked"), false)
            &&  email != null && ! email.equals("")) {
                emailChecked = true ;
            }

            Object login = info.get("username");
            if (login != null && ! login.equals("")) {
                loginChecked = true ;
            }
        }

        lo = tb.fetchCase()
               .filter("`id` IN (?)", uids )
               .assort("`ctime` DESC, `mtime` DESC")
               .select("`phone`,`phone_checked`,`email`,`email_checked`,`username`,`password`,`passcode`")
               .select();
        for(Map ro : lo) {
            if (! phoneChecked) {
            Object phone  =  ro.get("phone");
            if (Synt.declare(ro.get("phone_checked"), false)
            &&  phone != null && ! phone.equals("")) {
                phoneChecked = true ;
                info.put("phone_checked", 1);
                info.put("phone"    , phone);
            }}

            if (! emailChecked) {
            Object email  =  ro.get("email");
            if (Synt.declare(ro.get("email_checked"), false)
            &&  email != null && ! email.equals("")) {
                emailChecked = true ;
                info.put("email_checked", 1);
                info.put("email"    , email);
            }}

            if (! loginChecked) {
            Object login  =  ro.get("username");
            if (login != null && ! login.equals("")) {
                loginChecked = true ;
                info.put("username" , login);
                info.put("password" , info.get("password") );
                info.put("passcode" , info.get("passcode") );
            }}
        }

        // 更新资料和权限时间
        long now = System.currentTimeMillis() / 1000;
        info.put("rtime", now);
        info.put("mtime", now);
        tb.update(info, "`id`  =  ? " , uid );

        // 其他用户标记为删除
        info.clear();
        info.put("state",  0 );
        info.put("rtime", now);
        info.put("mtime", now);
        tb.update(info, "`id` IN (?)" , uids);

        //** 其他关联 **/

        /**
         * 仅能更新普通的关联到用户
         * 对那些有额外唯一约束的表
         * 请自行处理
         */

        db = DB.getInstance();

        String  u = CoreConfig.getInstance("master").getProperty("core.master.uproot");
        if (null != u && ! u.isEmpty())
        for(String  n : u.split( "," )) {
            int p = n . indexOf( ":" );
            if (p < 0) {
                throw new HongsException("Config item 'core.master.uproot' must be '[DB.]TABLE:FIELD'");
            }
            String  t = n.substring(0 , p).trim();
            String  f = n.substring(1 + p).trim();

            tb = db.getTable(t);
            tb.db.execute("UPDATE `"+tb.tableName+"` SET `"+f+"` = ? WHERE `"+f+"` IN (?)", uid , uids);
        }
    }

}
