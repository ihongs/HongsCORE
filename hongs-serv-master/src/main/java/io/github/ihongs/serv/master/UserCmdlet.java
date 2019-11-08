package io.github.ihongs.serv.master;

import io.github.ihongs.HongsException;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.anno.Cmdlet;
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
@Cmdlet("master.user")
public class UserCmdlet {

    /**
     * 归并命令
     * @param args
     * @throws HongsException
     */
    @Cmdlet("uproot")
    public static void uproot(String[] args) throws HongsException {
        Map opts = CmdletHelper.getOpts(
            args ,
            "uid=s" ,
            "uids=s",
            "!A",
            "!U",
            "?Usage: attach --uid UID --uids UID1,UID2..."
        );

        String   uid  = (String) opts.get("uid" );
        String   uidz = (String) opts.get("uids");
        String[] uids = uidz.split( "," );

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
    public static void uproot(String uid, String... uids) throws HongsException {
        DB    db;
        Table tb;
        Loop  lo;

        db = DB.getInstance("master");

        // 关联登录

        tb = db.getTable("user_sign");
        tb.update(Synt.mapOf(
            "user_id", uid
        ), "`user_id` IN (?)", (Object) uids);

        // 用户权限

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

        // 用户分组

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

        // 用户资料

        tb = db.getTable("user");

        lo = tb.fetchCase()
               .filter("`id` = ?", uid)
               .select("`phone`,`phone_checked`,`email`,`email_checked`")
               .select();
        Map info = new HashMap();
        boolean phoneChecked = false;
        boolean emailChecked = false;
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
        }

        lo = tb.fetchCase()
               .filter("`id` IN (?) AND (`phone` != ? OR `phone` IS NOT NULL) AND (`email` != ? OR `email` IS NOT NULL)", uids, "", "")
               .select("`phone`,`phone_checked`,`email`,`email_checked`")
               .assort("`mtime` DESC")
               .select();
        for(Map ro : lo) {
            if (! phoneChecked) {
            Object phone = info.get("phone");
            if (Synt.declare(ro.get("phone_checked"), false)
            &&  phone != null && ! phone.equals("")) {
                phoneChecked = true ;
                info.put("phone_checked", true );
                info.put("phone"        , phone);
            }}

            if (! emailChecked) {
            Object email = info.get("email");
            if (Synt.declare(ro.get("email_checked"), false)
            &&  email != null && ! email.equals("")) {
                emailChecked = true ;
                info.put("email_checked", true );
                info.put("email"        , email);
            }}
        }

        // 更新资料和权限时间
        long now = System.currentTimeMillis() / 1000;
        info.put ("rtime", now);
        info.put ("mtime", now);
        tb.update(info
          , "`id` = ?", uid);

        // 其他用户标记为删除
        tb.update(Synt.mapOf(
            "state", 0
        ) , "`id` = ?", uid);
    }

}
