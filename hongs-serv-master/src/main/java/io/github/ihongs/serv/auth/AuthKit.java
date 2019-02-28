package io.github.ihongs.serv.auth;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.VerifyHelper;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.serv.master.Dept;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.verify.Wrong;
import io.github.ihongs.util.verify.Wrongs;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
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
        long     stime = System.currentTimeMillis() / 1000;
        HttpSession sd = ah.getRequest().getSession(false);

        // 重建会话, 检查登录时哪些会话数据需要保留
        if (sd != null) {
            Set<String> ks = Synt. toTerms    (
                       CoreConfig.getInstance ( "master" )
                                 .getProperty ("core.keep.sess",""));
            Map<String,Object> xs = new HashMap();
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

        // 登录区域
        Set sarea  = Synt.asSet(sd.getAttribute(Cnst.SAE_SES));
        if (sarea == null) {
            sarea  = new  HashSet();
        }
        if (place != null
        && !place.isEmpty( )
        &&  RoleSet.getInstance(usrid).contains(place)) {
            sarea.add(place);
        }

        // 设置会话
        sd.setAttribute(Cnst.UID_SES, usrid);
        sd.setAttribute(Cnst.SAE_SES, sarea);
        sd.setAttribute(Cnst.STM_SES, stime);
        sd.setAttribute("uname", uname);
        sd.setAttribute("uhead", uhead);
        sd.setAttribute("utime", utime);

        // 返回数据
        Map rd = new HashMap();
        rd.put(Cnst.UID_SES, usrid);
        rd.put("sesid", sesid);
        rd.put("appid", appid);
        rd.put("uname", uname);
        rd.put("uhead", uhead);
        rd.put("utime", utime);
        rd.put("stime", stime);

        // 记录登录
        Map ud = new HashMap();
        ud.put("user_id"   , usrid);
        ud.put("sesid", sesid);
        ud.put("appid", appid);
        ud.put("ctime", stime);
        Table  tb = DB.getInstance("master").getTable("user_sign");
        tb.remove("(`user_id` = ? AND `appid` = ?) OR `sesid` = ?", usrid, appid, sesid);
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
        Table ub = db.getTable("user");
        Map   ud = tb.fetchCase()
                     .from(tb.tableName, "o")
                     .join(ub.tableName, "u", "`u`.`id` = `o`.`user_id`")
                     .filter("`opnid` = ? AND `appid` = ?", opnid, appid)
                     .select("`o`.`user_id`, `u`.`name`, `u`.`head`, `u`.`mtime`")
                     .getOne(   );

        // 记录关联
        String usrid;
        if (ud != null && !ud.isEmpty()) {
            usrid = ud.get("user_id").toString();
            uname = (String) ud.get("name");
            uhead = (String) ud.get("head");
            utime = Synt.declare(ud.get("mtime"), utime); // 信息更新时间
        } else {
            // 校验及下载头像
            VerifyHelper vh = new VerifyHelper();
            vh.addRulesByForm("master" , "user");

            ud  =  new HashMap( );
            ud.put("name", uname);
            ud.put("head", uhead);
            ud  =   vh.verify(ud , true , true );
            usrid = db.getModel("user").add(ud );
            uname = (String) ud.get("name");
            uhead = (String) ud.get("head");

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
            .filter("user_id = ?", uid)
            .select("dept_id" )
            .getAll();
        Set set = new HashSet();
        for(Map row : lst) {
            set.add(row.get("dept_id"));
        }
        return set;
    }

    public static Set getMoreDepts(String uid) throws HongsException {
        Table rel = DB.getInstance("master").getTable("user_dept");
        List<Map> lst = rel.fetchCase()
            .filter("user_id = ?", uid)
            .select("dept_id" )
            .getAll();
        Set set = new HashSet();
        Dept dp = new Dept();
        for(Map row : lst) {
            String id = (String) row.get("dept_id");
            set.addAll(dp.getChildIds( id , true ));
        }
        return set;
    }

    public static Set getLessDepts(String uid) throws HongsException {
        Table rel = DB.getInstance("master").getTable("user_dept");
        List<Map> lst = rel.fetchCase()
            .filter("user_id = ?", uid)
            .select("dept_id" )
            .getAll();
        Set set = new TreeSet();
        Dept dp = new Dept();
        for(Map row : lst) {
            String id = (String) row.get("dept_id");
            set.add   (   getDeptPath( id , dp )  );
        }
        return getPeakPids(set);
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
            cleanListItems(list,"dept_id");
            return; // 超级管理员可以更改任何人的部门, 即使自己没有
        }

            Set uds = getMoreDepts(cid);
        if (uid != null) {
            Set xds = getUserDepts(uid);
            xds.addAll(uds);
            uds = xds;
        }

        cleanListItems(list,"dept_id",uds);
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
            cleanListItems(list,"role");
            return; // 超级管理员可以更改任何人的权限, 即使自己没有
        }

            Set urs = getUserRoles(cid);
        if (uid != null) {
            Set xrs = getUserRoles(uid);
            xrs.addAll(urs);
            urs = xrs;
        }

        cleanListItems(list,"role",urs);
    }

    public static Set getUserRoles(String uid) throws HongsException {
        Table rel = DB.getInstance("master").getTable("user_role");
        List<Map> lst = rel.fetchCase()
            .filter("user_id = ?",uid)
            .select("role"    )
            .getAll();
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
            cleanListItems(list,"role");
            return; // 超级管理员可以更改任何组的权限, 即使自己没有
        }

            Set urs = getCurrRoles(cid);
        if (gid != null) {
            Set xrs = getDeptRoles(gid);
            xrs.addAll(urs);
            urs = xrs;
        }

        cleanListItems(list,"role",urs);
    }

    public static Set getDeptRoles(String gid) throws HongsException {
        Table rel = DB.getInstance("master").getTable("dept_role");
        List<Map> lst = rel.fetchCase()
            .filter("dept_id = ?", gid)
            .select("role"    )
            .getAll();
        Set set = new HashSet();
        for(Map row : lst) {
            set.add(row.get("role"));
        }
        return set;
    }

    private static Set getCurrRoles(String uid) throws HongsException {
        return RoleSet.getInstance (uid);
    }

    private static String getDeptPath(String id, Dept dp) throws HongsException {
        List <String> ds = dp.getParentIds(id);
        StringBuilder sb = new StringBuilder();
        Collections.reverse(ds);
        for ( String  xd : ds ) {
               sb.append(xd).append("/");
        }
        return sb.append(id).toString( );
    }

    private static Set  getPeakPids(Set<String> set) {
        Set     ids = new HashSet();
        String  pid =  "/" ;
        for(String id : set) {
            if (id.startsWith(pid)) {
                continue;
            }
            pid = id + "/" ;
            int  p = id.lastIndexOf("/");
            if ( p > 0 ) {
                id = id.substring  (p+1);
            }
            ids.add (id);
        }
        return  ids ;
    }

    private static void cleanListItems(List<Map> list, String fn) {
        Iterator it = list.iterator();
        while (it.hasNext()) {
            Map    rm = (Map   ) it.next(  );
            String rn = (String) rm.get (fn);
            if (rn == null
            ||  rn.length() ==0) {
                it.remove();
            }
        }
    }

    private static void cleanListItems(List<Map> list, String fn, Set rs) {
        Iterator it = list.iterator();
        while (it.hasNext()) {
            Map    rm = (Map   ) it.next(  );
            String rn = (String) rm.get (fn);
            if (rn == null
            ||  rn.length() ==0
            || !rs.contains(rn)) {
                it.remove();
            }
        }
    }

}
