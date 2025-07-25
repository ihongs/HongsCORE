package io.github.ihongs.serv.auth;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CruxCause;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.VerifyHelper;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.serv.master.Unit;
import io.github.ihongs.serv.master.UserAction;
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
 * 登录及权限工具
 * @author Hongs
 */
public class AuthKit {

    private  AuthKit () {}

    private static final byte[] PAZZ = {'A','B','C','D','E','F','1','2','3','4','5','6','7','8','9','0'};
    private static final String NAME = "uname" ;
    private static final String HEAD = "uhead" ;

    /**
     * 登录成功后跳转
     * 依此检查 Parameters,Cookies,Session 中是否有指定返回路径
     * 都没有指定时则跳转到默认地址
     * 默认地址缺失则跳转到网站首页
     * 也可用特殊值要求返回特定数据
     *  _mine_info 用户信息
     *  _sign_info 会话信息
     *  - 无返回信息
     * @param helper
     * @param rst
     * @throws CruxException
     */
    public static void redirect(ActionHelper helper, Map rst)
    throws CruxException {
        CoreConfig cc = CoreConfig.getInstance("auth");
        String v = cc.getProperty("auth.redirect.url", "" );
        String r = cc.getProperty("auth.redirect.key", "r");
               r = helper.getParameter(r);

        if (r != null && ! r.isEmpty( )) {
            if ("_mine_info_".equals(r)) {
                Object id = helper.getSessibute(Cnst.UID_SES);
                Map    rd = helper.getRequestData( );
                       rd.put (Cnst.ID_KEY , id);
                new UserAction().getInfo(helper);
            } else
            if ("_sign_info_".equals(r)) {
                helper.reply(Synt.mapOf(
                    "info", rst
                ));
            } else
            if ("-".equals(r)) {
                helper.reply("");
            } else {
                helper.ensue(r );
            }
        } else {
            r= (String) helper.getSessibute("redirect");
            if (r != null) {
                helper.setAttribute( "redirect", null );
                helper.ensue(r );
            } else {
                helper.ensue(v );
            }
        }
    }

    /**
     * 登录失败后跳转
     * 依此检查 Parameters,Cookies,Session 中是否有指定返回路径
     * 都没有指定时则跳转到默认地址
     * 默认地址缺失则跳转到网站首页
     * 如指定特殊值则会返回错误信息
     * @param helper
     * @param err
     * @throws CruxException
     */
    public static void redirect(ActionHelper helper, CruxCause err)
    throws CruxException {
        CoreConfig cc = CoreConfig.getInstance("auth");
        String v = cc.getProperty("auth.redirect.url", "" );
        String r = cc.getProperty("auth.redirect.key", "r");
               r = helper.getParameter(r);

        if (r != null && ! r.isEmpty( )) {
            if ("_mine_info_".equals(r)
            ||  "_sign_info_".equals(r)
            ||  "-".equals(r)) {
                // 输出 JSON
                helper.reply( Synt.mapOf (
                    "ok" , false,
                    "ern", err.getStage  (),
                    "err", err.getMessage(),
                    "msg", err.getLocalizedMessage()
                ));
            } else {
                // 输出 HTML
                String m = err.getLocalizedMessage();
                       r = ActionDriver.fixUrl ( r );
                helper.setAttribute( "redirect", r );
                helper.error(401, m);
            }
        } else {
            r= (String) helper.getSessibute("redirect");
            if (r != null) {
                helper.setAttribute( "redirect", null );
                // 输出 HTML
                String m = err.getLocalizedMessage();
                       r = ActionDriver.fixUrl ( r );
                helper.setAttribute( "redirect", r );
                helper.error(401, m);
            } else {
                // 输出 HTML
                String m = err.getLocalizedMessage();
                       v = ActionDriver.fixUrl ( v );
                helper.setAttribute( "redirect", v );
                helper.error(401, m);
            }
        }
    }

    /**
     * 自运营登录
     * @param ah
     * @param unit
     * @param uuid
     * @param uname 名称
     * @param uhead 头像
     * @return
     * @throws CruxException
     */
    public static Map userSign(ActionHelper ah, String unit, String uuid, String uname, String uhead)
    throws CruxException {
        long      time = System.currentTimeMillis() / 1000 ;
        HttpSession sd = ah.getRequest( ).getSession(false);

        // 重建会话
        if (sd != null) {
            Set<String> ns = Synt.toTerms(CoreConfig.getInstance("auth").get("auth.save.session"));
        if (ns != null) {
            Map<String, Object> ss = new HashMap(ns.size());
            for(String  nn : ns) {
                ss.put( nn , sd.getAttribute(nn));
            }
            sd.invalidate();
            sd = ah.getRequest().getSession(true);
            for(Map.Entry<String,Object> et: ss.entrySet()) {
                sd.setAttribute(et.getKey(), et.getValue());
            }
        } else {
            sd = ah.getRequest().getSession(true);
        }
        } else {
            sd = ah.getRequest().getSession(true);
        }
        String ssid = sd.getId();

        // 设置会话
        sd.setAttribute(Cnst.UID_SES, uuid);
        sd.setAttribute(Cnst.USK_SES, unit);
        sd.setAttribute(Cnst.UST_SES, time);
        sd.setAttribute(NAME, uname);
        sd.setAttribute(HEAD, uhead);

        // 返回数据
        Map rd = new HashMap();
        rd.put(Cnst.SID_KEY, ssid);
        rd.put(Cnst.UID_SES, uuid);
        rd.put(Cnst.USK_SES, unit);
        rd.put(Cnst.UST_SES, time);
        rd.put(NAME, uname);
        rd.put(HEAD, uhead);

        return rd;
    }

    /**
     * 第三方登录
     * @param ah
     * @param unit
     * @param code
     * @param uname 名称
     * @param uhead 头像
     * @return
     * @throws CruxException
     */
    public static Map openSign(ActionHelper ah, String unit, String code, String uname, String uhead)
    throws CruxException {
        DB    db = DB.getInstance("master");
        Table tb = db.getTable("user_sign");
        Table ub = db.getTable("user");
        Map   ud = tb.fetchCase()
                     .from(tb.tableName, "s")
                     .join(ub.tableName, "u", "u.id = s.user_id", FetchCase.LEFT)
                     .filter("s.unit = ? AND s.code = ?" , unit , code)
                     .select("u.id, u.name, u.head, u.state")
                     .getOne(   );

        int     stat  = Synt.declare(ud.get("state"), 0);
        String  uuid  ;

        if (!ud.isEmpty()) {
            // 锁定或系统账号
            if (stat <= 0) {
                throw new Wrongs(Synt.mapOf("state" ,
                      new Wrong ("@master:core.sign.state.invalid")
                ));
            }

            uuid  = (String) ud.get( "id" );
            uname = (String) ud.get("name");
            uhead = (String) ud.get("head");
        } else {
            ud  =  new HashMap( );
            ud.put("name", uname);
            ud.put("head", uhead);

            // 校验及下载头像
            ud  =  new VerifyHelper(  )
              .addRulesByForm("master", "user")
              .verify( ud, true, true );

            uuid  = db.getModel("user").add(ud);
            uname = (String) ud.get("name");
            uhead = (String) ud.get("head");

            // 第三方登录项
            ud = new HashMap(3);
            ud.put("user_id", uuid );
            ud.put("unit"   , unit );
            ud.put("code"   , code );
            db.getTable("user_sign").insert(ud);

            CoreConfig cc = CoreConfig.getInstance("master");
            /**/String rr ;

            // 加入公共部门
            rr  = cc.getProperty("core.public.regs.unit","");
            if (! rr.isEmpty()) {
                for (Object role : Synt.toSet(rr)) {
                    ud = new HashMap(3);
                    ud.put("user_id", uuid );
                    ud.put("unit_id", role );
                    ud.put("type"   ,  00  );
                    db.getTable("unit_user").insert(ud);
                }
            }

            // 赋予公共权限
            rr  = cc.getProperty("core.public.regs.role","");
            if (! rr.isEmpty()) {
                for (Object role : Synt.toSet(rr)) {
                    ud = new HashMap(2);
                    ud.put("user_id", uuid );
                    ud.put("role"   , role );
                    db.getTable("user_role").insert(ud);
                }
            }
        }

        ud = userSign( ah , unit, uuid, uname, uhead );
        ud.put("unit", /**/ unit);
        ud.put("regs", 0 == stat);
        return ud;
    }

    /**
     * 快速输出登录的错误
     * @param k 字段
     * @param w 错误
     * @return
     */
    public static Map getWrong(String k, String w) {
        String n = w.startsWith("core.") ? w.substring( 5 ) : w ;
        String s = CoreLocale.getInstance("master").translate(w);
        Map e  = new HashMap();
        if (k != null && ! "".equals(k)) {
        Map m  = new HashMap();
            m.put(   k   , s );
            e.put( "errs", m );
            e.put( "msg" , s );
        } else {
            e.put( "msg" , s );
        }
        e.put("ok", false);
        return e;
    }

    /**
     * 获取特征加密字符串
     * @param p
     * @return
     * @throws CruxException
     */
    public static String getCrypt(String p) throws CruxException {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            byte[] a = m.digest(p.getBytes());
            byte[] b = new byte[a.length * 2];
            for (int i = 0, j = 0; i < a.length; i ++) {
                byte c = a[i];
                b[j++] = PAZZ[c >>> 4 & 15];
                b[j++] = PAZZ[c       & 15];
            }
            return new String(b);
        }   catch (NoSuchAlgorithmException ex) {
            throw  new CruxException(ex);
        }
    }

    //** 分组权限辅助方法 **/

    /**
     * 设置第三方登录标识
     * @param unit
     * @param code
     * @param uid
     * @throws CruxException
     */
    public static void setUserSign(String unit, String code, String uid)
    throws CruxException {
        Table  tab = DB.getInstance("master").getTable("user_sign");
        Map    row = tab.fetchCase ()
            .filter("unit = ?", unit)
            .filter("code = ?", code)
            .select("user_id" )
            .getOne();
        if (row == null || row.isEmpty()) {
            tab.insert(Synt.mapOf(
                "user_id", uid ,
                "unit"   , unit,
                "code"   , code
            ));
        } else
        if (! uid.equals(row.get("user_id"))) {
            throw new CruxException("@master:core.sign.oauth.diverse", unit, code, uid, row.get("user_id"));
        }
    }

    /**
     * 获取分组拥有的权限
     * @param gid
     * @return
     * @throws CruxException
     */
    public static Set getUnitRoles(String gid) throws CruxException {
        Table rel = DB.getInstance("master").getTable("unit_role");
        List<Map> lst = rel.fetchCase()
            .filter("unit_id = ?", gid)
            .select("role"    )
            .getAll();
        Set set = new HashSet();
        for(Map row : lst) {
            set.add(row.get("role"));
        }
        return set;
    }

    /**
     * 获取用户拥有的权限
     * @param uid
     * @return
     * @throws CruxException
     */
    public static Set getUserRoles(String uid) throws CruxException {
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
     * 获取用户所在的分组
     * @param uid
     * @return
     * @throws CruxException
     */
    public static Set getUserUnits(String uid) throws CruxException {
        Table rel = DB.getInstance("master").getTable("unit_user");
        List<Map> lst = rel.fetchCase()
            .filter("user_id = ?", uid)
            .select("unit_id" )
            .getAll();
        Set set = new HashSet();
        for(Map row : lst) {
            set.add(row.get("unit_id"));
        }
        return set;
    }

    /**
     * 获取管理的全部分组
     * @param uid
     * @return
     * @throws CruxException
     */
    public static Set getManaUnits(String uid) throws CruxException {
        Table rel = DB.getInstance("master").getTable("unit_user");
        List<Map> lst = rel.fetchCase()
            .filter("type = ?"   , 1  )
            .filter("user_id = ?", uid)
            .select("unit_id" )
            .getAll();
        Set set = new HashSet();
        Unit dp = new Unit();
        for(Map row : lst) {
            String id = (String) row.get("unit_id");
            set.addAll( dp.getChildIds(id , true) );
            set.add   ( id );
        }
        return set;
    }

    /**
     * 获取管理的顶层分组
     * @param uid
     * @return
     * @throws CruxException
     */
    public static Set getLeadUnits(String uid) throws CruxException {
        Table rel = DB.getInstance("master").getTable("unit_user");
        List<Map> lst = rel.fetchCase()
            .filter("type = ?"   , 1  )
            .filter("user_id = ?", uid)
            .select("unit_id" )
            .getAll();
        Set set = new TreeSet();
        Unit dp = new Unit();
        for(Map row : lst) {
            String id = (String) row.get("unit_id");
            set.add(getDeepPath(id, dp));
        }
        return getPeakPids(set);
    }

    /**
     * 检查并清理不合规的权限设置数据
     * 操作人员无法增减自己没有的权限
     * @param list 权限设置数据
     * @param gid  部门ID
     * @throws CruxException
     */
    public static void cleanUnitRoles(List<Map> list, String gid) throws CruxException {
        String cid = (String) Core.getInstance(ActionHelper.class).getSessibute("uid");
        if (Cnst.ADM_UID.equals(cid)) {
            cleanListItems(list,"role");
            return; // 超级管理员可以更改任何组的权限, 即使自己没有
        }

            Set urs = getUserRoles(cid);
        if (gid != null) {
            Set xrs = getUnitRoles(gid);
            xrs.addAll(urs);
            urs = xrs;
        }

        cleanListItems(list,"role",urs);
    }

    /**
     * 检查并清理不合规的权限设置数据
     * 操作人员无法增减自己没有的权限
     * @param list 权限设置数据
     * @param uid  用户ID
     * @throws CruxException
     */
    public static void cleanUserRoles(List<Map> list, String uid) throws CruxException {
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

    /**
     * 检查并清理不合规的部门设置数据
     * 操作人员无法增减自己不在的部门
     * @param list 权限设置数据
     * @param uid  用户ID
     * @throws CruxException
     */
    public static void cleanUserUnits(List<Map> list, String uid) throws CruxException {
        String cid = (String) Core.getInstance(ActionHelper.class).getSessibute("uid");
        if (Cnst.ADM_UID.equals(cid)) {
            cleanListItems(list,"unit_id");
            return; // 超级管理员可以更改任何人的部门, 即使自己没有
        }

            Set uds = getManaUnits(cid);
        if (uid != null) {
            Set xds = getUserUnits(uid);
            xds.addAll(uds);
            uds = xds;
        }

        cleanListItems(list,"unit_id",uds);
    }

    private static String getDeepPath(String id, Unit dp) throws CruxException {
        List <String> ds = dp.getParentIds(id);
        StringBuilder sb = new StringBuilder();
        Collections.reverse(ds);
        for ( String  xd : ds ) {
               sb.append(xd).append("/");
        }
        return sb.append(id).toString( );
    }

    private static Set getPeakPids(Set<String> set) {
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
