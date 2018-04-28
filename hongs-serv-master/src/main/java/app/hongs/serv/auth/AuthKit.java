package app.hongs.serv.auth;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.db.DB;
import app.hongs.db.Table;
import app.hongs.serv.master.Dept;
import app.hongs.util.Synt;
import app.hongs.util.verify.Wrong;
import app.hongs.util.verify.Wrongs;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpSession;

/**
 * 登录工具
 * @author Hongs
 */
public class AuthKit {

    private static final byte[] PAZZ = {'A','B','C','D','E','F','1','2','3','4','5','6','7','8','9','0'};

    /**
     * 获取特征加密字符串
     * @param pswd
     * @return
     * @throws HongsException
     */
    public static String getCrypt(String pswd) throws HongsException {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            byte[] pzwd = m.digest(pswd.getBytes());
            byte[] pxwd = new byte[pzwd.length * 2];
            for (int i = 0, j = 0; i < pzwd.length; i ++) {
                byte pzbt = pzwd[i];
                pxwd[j++] = PAZZ[pzbt >>> 4 & 15];
                pxwd[j++] = PAZZ[pzbt       & 15];
            }
            return new String(pxwd);
        } catch (NoSuchAlgorithmException ex) {
            throw new HongsException.Common(ex);
        }
    }

    /**
     * 快速输出登录的错误
     * @param k 字段
     * @param w 错误
     * @return
     * @throws HongsException
     */
    public static Map getWrong(String k, String w) throws HongsException {
        CoreLocale l = CoreLocale.getInstance("master");
        Map e  = new HashMap();
        if (k != null && ! "".equals( k )) {
        Map m  = new HashMap();
            m.put( k ,    new Wrong ( w ));
            e.put("errs", new Wrongs( m )
                .setLocalizedContext( l )
                .getErrors( ));
            e.put("msg", l.translate( w ));
        } else {
            e.put("msg", l.translate( w ));
        }
        e.put("ok", false);
        return e;
    }

    /**
     * 自运营登录
     * @param ah
     * @param place
     * @param appid
     * @param usrid
     * @param uname 名称
     * @param uhead 头像
     * @param utime 用户信息更新时间
     * @return
     * @throws HongsException
     */
    public static Map userSign(ActionHelper ah,
            String place, String appid, String usrid,
            String uname, String uhead,  long  utime)
    throws HongsException {
        HttpSession sd = ah.getRequest().getSession(false);
        long     stime = System.currentTimeMillis() / 1000;
        String   sesmk = CoreConfig.getInstance( "master").getProperty("core.keep.sess", ""); // 登录时哪些会话数据需要保留

        // 重建会话
        if (sd != null) {
            Map<String,Object> xs = new HashMap();
            Set<String> ks = Synt.toTerms(sesmk );
            ks.add(Cnst.SAE_SES);
            for(String  kn : ks) {
                Object  kv = sd.getAttribute(kn );
                if ( null != kv) xs.put( kn, kv );
            }
            sd.invalidate();
            sd = ah.getRequest().getSession(true);
            for(Map.Entry<String,Object> et: xs.entrySet()) {
                sd.setAttribute(et.getKey(), et.getValue());
            }
        } else {
            sd = ah.getRequest().getSession(true);
        }
        String   sesid = sd.getId();

        // 设置会话
        if (place != null && 0 < place.length()) {
            Set s  = Synt.asSet(sd.getAttribute(Cnst.SAE_SES));
            if (s == null) {
                s  = new HashSet();
            }   s.add  (  place  );
            s.retainAll( RoleSet.getInstance( usrid ) ); // 仅保留拥有的区域
            sd.setAttribute(Cnst.SAE_SES, s);
        }
        sd.setAttribute(Cnst.STM_SES, stime);
        sd.setAttribute(Cnst.UID_SES, usrid);
        sd.setAttribute("appid", appid);
        sd.setAttribute("uname", uname);
        sd.setAttribute("uhead", uhead);
        sd.setAttribute("utime", utime);

        // 返回数据
        Map rd = new HashMap();
        rd.put(Cnst.STM_SES, stime);
        rd.put(Cnst.UID_SES, usrid);
        rd.put("appid", appid);
        rd.put("place", place);
        rd.put("uname", uname);
        rd.put("uhead", uhead);
        rd.put("utime", utime);
        rd.put("sesid", sesid);

        // 记录登录
        DB    db = DB.getInstance("master");
        Table tb = db.getTable("user_sign");
        tb.remove("(`user_id` = ? AND `appid` = ?) OR `sesid` = ?", usrid, appid, sesid);
        Map ud = new HashMap();
        ud.put("user_id", usrid);
        ud.put("sesid", sesid);
        ud.put("appid", appid);
        ud.put("ctime", stime);
        tb.insert(ud);

        return rd;
    }

    /**
     * 第三方登录
     * @param ah
     * @param place
     * @param appid
     * @param opnid
     * @param uname 名称
     * @param uhead 头像
     * @param utime 用户信息更新时间
     * @return
     * @throws HongsException
     */
    public static Map openSign(ActionHelper ah,
            String place, String appid, String opnid,
            String uname, String uhead,  long  utime)
    throws HongsException {
        DB    db = DB.getInstance("master");
        Table tb = db.getTable("user_open");
        Map   ud = tb.fetchCase()
                     .filter("`opnid` =? AND `appid` = ?", opnid, appid)
                     .select("`user_id`")
                     .one   (   );

        // 记录关联
        String usrid;
        if (ud != null && !ud.isEmpty()) {
            usrid = ud.get("user_id").toString();
        } else {
            ud  =  new HashMap( );
            ud.put("name", uname);
            ud.put("head", uhead);
            usrid = db.getModel("user").add(ud );

            // 第三方登录项
            ud  =  new HashMap( );
            ud.put("user_id",  usrid  );
            ud.put("appid"  ,  appid  );
            ud.put("opnid"  ,  opnid  );
            db.getTable("user_open").insert(ud );

            // 赋予公共权限
            ud  =  new HashMap( );
            ud.put("user_id",  usrid  );
            ud.put("role"   , "centre");
            db.getTable("user_role").insert(ud );

            // 加入公共部门
            ud  =  new HashMap( );
            ud.put("user_id",  usrid  );
            ud.put("dept_id", "CENTRE");
            db.getTable("user_dept").insert(ud );
        }

        return userSign(ah, place, appid, usrid, uname, uhead, utime);
    }

    public static void signOut(HttpSession ss) throws HongsException {
        // 清除登录
        DB.getInstance("master")
          .getTable("user_sign")
          .remove("`sesid` = ?", ss.getId());

        // 清除会话
        ss.invalidate();
    }

    public static void signUpd(HttpSession ss) throws HongsException {
        // Nothing todo.
    }

    public static Set getUserDepts(String uid) throws HongsException {
        Table rel = DB.getInstance("master").getTable("user_dept");
        List<Map> lst = rel.fetchCase()
            .filter ("user_id = ?",uid)
            .select ("dept_id")
            .all    ();
        Set set = new HashSet();
        for(Map row : lst) {
            set.add(row.get("dept_id"));
        }
        return set;
    }

    public static Set getMoreDepts(String uid) throws HongsException {
        Table rel = DB.getInstance("master").getTable("user_dept");
        List<Map> lst = rel.fetchCase()
            .filter ("user_id = ?",uid)
            .select ("dept_id")
            .all    ();
        Set set = new HashSet();
        Dept dp = new Dept   ();
        for(Map row : lst) {
            set.addAll(dp.getChildIds((String)row.get("dept_id"), true));
        }
        return set;
    }

    /**
     * 检查并清理不合规的部门设置数据
     * 操作人员无法增减自己不在的部门
     * @param list 权限设置数据
     * @param uid  用户ID
     * @throws HongsException
     */
    public static void cleanUserDepts(List<Map> list, String uid) throws HongsException {
        String cid = (String) Core.getInstance(ActionHelper.class).getSessibute("uid");
        if (Cnst.ADM_UID.equals(cid)) {
            return; // 超级管理员可以更改任何人的部门, 即使自己没有
        }

            Set uds = getMoreDepts(cid);
        if (uid != null) {
            Set xds = getUserDepts(uid);
            xds.addAll(uds);
            uds = xds;
        }

        Iterator it = list.iterator();
        while (it.hasNext()) {
            Map    rm = (Map   ) it.next( );
            String rn = (String) rm.get ("dept_id");
            if (! uds.contains ( rn)) {
                it.remove();
            }
        }
    }

    /**
     * 检查并清理不合规的权限设置数据
     * 操作人员无法增减自己没有的权限
     * @param list 权限设置数据
     * @param uid  用户ID
     * @throws HongsException
     */
    public static void cleanUserRoles(List<Map> list, String uid) throws HongsException {
        String cid = (String) Core.getInstance(ActionHelper.class).getSessibute("uid");
        if (Cnst.ADM_UID.equals(cid)) {
            return; // 超级管理员可以更改任何人的权限, 即使自己没有
        }

            Set urs = getUserRoles(cid);
        if (uid != null) {
            Set xrs = getUserRoles(uid);
            xrs.addAll(urs);
            urs = xrs;
        }

        Iterator it = list.iterator();
        while (it.hasNext()) {
            Map    rm = (Map   ) it.next( );
            String rn = (String) rm.get ("role");
            if (! urs.contains ( rn)) {
                it.remove();
            }
        }
    }

    public static Set getUserRoles(String uid) throws HongsException {
        Table rel = DB.getInstance("master").getTable("user_role");
        List<Map> lst = rel.fetchCase()
            .filter ("user_id = ?",uid)
            .select ("role"   )
            .all    ();
        Set set = new HashSet();
        for(Map row : lst) {
            set.add(row.get("role"));
        }
        return set;
    }

    /**
     * 检查并清理不合规的权限设置数据
     * 操作人员无法增减自己没有的权限
     * @param list 权限设置数据
     * @param gid  部门ID
     * @throws HongsException
     */
    public static void cleanDeptRoles(List<Map> list, String gid) throws HongsException {
        String cid = (String) Core.getInstance(ActionHelper.class).getSessibute("uid");
        if (Cnst.ADM_UID.equals(cid)) {
            return; // 超级管理员可以更改任何组的权限, 即使自己没有
        }

            Set urs = RoleSet.getInstance();  // 额外的权限也可给组
        if (gid != null) {
            Set xrs = getDeptRoles(gid);
            xrs.addAll(urs);
            urs = xrs;
        }

        Iterator it = list.iterator();
        while (it.hasNext()) {
            Map    rm = (Map   ) it.next( );
            String rn = (String) rm.get ("role");
            if (! urs.contains ( rn)) {
                it.remove();
            }
        }
    }

    public static Set getDeptRoles(String gid) throws HongsException {
        Table rel = DB.getInstance("master").getTable("dept_role");
        List<Map> lst = rel.fetchCase()
            .filter ("dept_id = ?",gid)
            .select ("role"   )
            .all    ();
        Set set = new HashSet();
        for(Map row : lst) {
            set.add(row.get("role"));
        }
        return set;
    }

}
