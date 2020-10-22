package io.github.ihongs.serv.auth;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsCause;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.VerifyHelper;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.serv.master.Dept;
import io.github.ihongs.serv.master.UserAction;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.verify.Wrong;
import io.github.ihongs.util.verify.Wrongs;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Enumeration;
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
     * @throws HongsException
     */
    public static void redirect(ActionHelper helper, Map rst)
    throws HongsException {
        String k, v, r;
        CoreConfig cc = CoreConfig.getInstance("oauth2");

        do {
            k = cc.getProperty("oauth2.bak.prm", "r");
            r = v = helper.getParameter(k);
            if (v != null && !v.isEmpty()) {
                break;
            }

            k = cc.getProperty("oauth2.bak.cok");
            if (k != null && !k.isEmpty()) {
                v = (String) helper.getCookibute( k );
                if (v != null && !v.isEmpty()) {
                    helper.setCookibute(k, null); // 清除 Cookies
                    break;
                }
            }

            k = cc.getProperty("oauth2.bak.ses");
            if (k != null && !k.isEmpty()) {
                v = (String) helper.getSessibute( k );
                if (v != null && !v.isEmpty()) {
                    helper.setSessibute(k, null); // 清除 Session
                    break;
                }
            }

            v = cc.getProperty("oauth2.bak.url", Core.BASE_HREF + "/");
        } while (false);

        if ("_mine_info_".equals(r)) {
            Object id = helper.getSessibute(Cnst.UID_SES);
            Map    rd = helper.getRequestData();
                   rd.put (Cnst.ID_KEY , id);
            new UserAction().getInfo(helper);
        } else
        if ("_sign_info_".equals(r)) {
            helper.reply(Synt.mapOf("info", rst));
        } else
        if ("-".equals(r)) {
            helper.reply( "" );
        } else {
            helper.redirect(v);
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
     * @throws HongsException
     */
    public static void redirect(ActionHelper helper, HongsCause err)
    throws HongsException {
        String k, v, r;
        CoreConfig cc = CoreConfig.getInstance("oauth2");

        do {
            k = cc.getProperty("oauth2.bak.prm", "r");
            r = v = helper.getParameter(k);
            if (v != null && !v.isEmpty()) {
                break;
            }

            k = cc.getProperty("oauth2.bak.cok");
            if (k != null && !k.isEmpty()) {
                v = (String) helper.getCookibute( k );
                if (v != null && !v.isEmpty()) {
                    helper.setCookibute(k, null); // 清除 Cookies
                    break;
                }
            }

            k = cc.getProperty("oauth2.bak.ses");
            if (k != null && !k.isEmpty()) {
                v = (String) helper.getSessibute( k );
                if (v != null && !v.isEmpty()) {
                    helper.setSessibute(k, null); // 清除 Session
                    break;
                }
            }

            v = cc.getProperty("oauth2.bak.url" , Core.BASE_HREF+"/");
        } while (false);

        if ("_mine_info_".equals(r)
        ||  "_sign_info_".equals(r)
        ||  "-".equals(r)) {
            // 输出 JSON
            String errno = "Ex" + Integer.toHexString(err.getErrno());
            helper.reply( Synt.mapOf (
                "ok" , false,
                "ern", errno,
                "err", err.getMessage(),
                "msg", err.getLocalizedMessage()
            ));
        } else {
            // 输出 HTML
            String m = err.getLocalizedMessage();
            helper.redirect (v, m, 401);
        }
    }

    /**
     * 自运营登录
     * @param ah
     * @param uuid
     * @param uname 名称
     * @param uhead 头像
     * @return
     * @throws HongsException
     */
    public static Map userSign(ActionHelper ah, String uuid, String uname, String uhead)
    throws HongsException {
        long      time = System.currentTimeMillis() / 1000 ;
        HttpSession sd = ah.getRequest( ).getSession(false);

        // 重建会话
        if (sd != null) {
            Enumeration<String> ns = sd.getAttributeNames();
            Map<String, Object> ss = new HashMap (/*Copy*/);
            while ( ns.hasMoreElements() ) {
                String nn = ns.nextElement (    );
                ss.put(nn , sd.getAttribute(nn) );
            }
            sd.invalidate();
            sd = ah.getRequest().getSession(true);
            for(Map.Entry<String,Object> et: ss.entrySet()) {
                sd.setAttribute(et.getKey(), et.getValue());
            }
        } else {
            sd = ah.getRequest().getSession(true);
        }
        String ssid = sd.getId();

        // 设置会话
        sd.setAttribute(Cnst.UID_SES, uuid);
        sd.setAttribute(Cnst.UST_SES, time);
        sd.setAttribute(NAME, uname);
        sd.setAttribute(HEAD, uhead);

        // 返回数据
        Map rd = new HashMap();
        rd.put(Cnst.SID_KEY, ssid);
        rd.put(Cnst.UID_SES, uuid);
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
     * @throws HongsException
     */
    public static Map openSign(ActionHelper ah, String unit, String code, String uname, String uhead)
    throws HongsException {
        DB    db = DB.getInstance("master");
        Table tb = db.getTable("user_sign");
        Table ub = db.getTable("user");
        Map   ud = tb.fetchCase()
                     .from(tb.tableName, "s")
                     .join(ub.tableName, "u", "`u`.`id` = `s`.`user_id`")
                     .filter("`s`.`unit` = ? AND `s`.`code` = ?", unit, code)
                     .select("`u`.`id`, `u`.`name`, `u`.`head`, `u`.`state`")
                     .getOne(   );

        int     stat  = Synt.declare(ud.get("state"), 0);
        String  uuid  ;

        if (!ud.isEmpty()) {
            // 锁定或系统账号
            if (stat <= 0) {
                throw new Wrongs(Synt.mapOf("state" ,
                      new Wrong ( "core.sign.state.invalid")
                )).setLocalizedContext("master");
                //.setLocalizedOptions(  stat  );
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
            ud  =  new HashMap( );
            ud.put("user_id",  uuid   );
            ud.put("unit"   ,  unit   );
            ud.put("code"   ,  code   );
            db.getTable("user_sign").insert(ud);

            // 加入公共部门
            ud  =  new HashMap( );
            ud.put("user_id",  uuid   );
            ud.put("dept_id", "CENTRE");
            db.getTable("dept_user").insert(ud);

            // 赋予公共权限. 仅用部门即可(2019/02/28)
//          ud  =  new HashMap( );
//          ud.put("user_id",  uuid   );
//          ud.put("role"   , "centre");
//          db.getTable("user_role").insert(ud);
        }

        ud = userSign( ah , uuid, uname, uhead );
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
     * 获取特征加密字符串
     * @param p
     * @return
     * @throws HongsException
     */
    public static String getCrypt(String p) throws HongsException {
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
            throw  new HongsException(ex);
        }
    }

    /**
     * 获取分组拥有的权限
     * @param gid
     * @return
     * @throws HongsException
     */
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

    /**
     * 获取用户拥有的权限
     * @param uid
     * @return
     * @throws HongsException
     */
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
     * 获取用户所在的分组
     * @param uid
     * @return
     * @throws HongsException
     */
    public static Set getUserDepts(String uid) throws HongsException {
        Table rel = DB.getInstance("master").getTable("dept_user");
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

    /**
     * 获取所在的下级分组
     * @param uid
     * @return
     * @throws HongsException
     */
    public static Set getMoreDepts(String uid) throws HongsException {
        Table rel = DB.getInstance("master").getTable("dept_user");
        List<Map> lst = rel.fetchCase()
            .filter("user_id = ?", uid)
            .select("dept_id" )
            .getAll();
        Set set = new HashSet();
        Dept dp = new Dept();
        for(Map row : lst) {
            String id = (String) row.get("dept_id");
            set.addAll( dp.getChildIds(id , true) );
        }
        return set;
    }

    /**
     * 获取所在的下级分组
     * @param deptIds
     * @return
     * @throws HongsException
     */
    public static Set getMoreDepts(Set<String> deptIds) throws HongsException {
        Set set = new HashSet();
        Dept dp = new Dept();
        for(String id:deptIds ) {
            set.addAll( dp.getChildIds(id , true) );
        }
        return set;
    }

    /**
     * 获取所在的顶层分组
     * @param uid
     * @return
     * @throws HongsException
     */
    public static Set getLessDepts(String uid) throws HongsException {
        Table rel = DB.getInstance("master").getTable("dept_user");
        List<Map> lst = rel.fetchCase()
            .filter("user_id = ?", uid)
            .select("dept_id" )
            .getAll();
        Set set = new TreeSet();
        Dept dp = new Dept();
        for(Map row : lst) {
            String id = (String) row.get("dept_id");
            set.add(getDeptPath(id, dp));
        }
        return getPeakPids(set);
    }

    /**
     * 获取所在的顶层分组
     * @param deptIds
     * @return
     * @throws HongsException
     */
    public static Set getLessDepts(Set<String> deptIds) throws HongsException {
        Set set = new TreeSet();
        Dept dp = new Dept();
        for(String id:deptIds ) {
            set.add(getDeptPath(id, dp));
        }
        return getPeakPids(set);
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
