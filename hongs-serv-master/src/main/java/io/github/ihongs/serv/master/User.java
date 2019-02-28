package io.github.ihongs.serv.master;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.util.Synt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户基础信息模型
 * @author Hongs
 */
public class User
extends Model {

    public User()
    throws HongsException {
        this(DB.getInstance("master").getTable("user"));
    }

    public User(Table table)
    throws HongsException {
        super(table);
    }

    @Override
    public String add(Map data) throws HongsException {
        permit(null, data);

        return super.add(data);
    }

    @Override
    public int put(String id, Map data) throws HongsException {
        permit( id , data);

        return super.put(id, data);
    }

    @Override
    public int del(String id, FetchCase caze) throws HongsException {
        permit( id , null);

        return super.del(id, caze);
    }

    @Override
    public Map getList(Map rd, FetchCase caze) throws HongsException {
        rd = super.getList(rd, caze);

        // Add all depts for every user
        byte incs = caze.getOption("INCLUDE_DEPARTS", (byte) 0);
        if ( incs > 0 ) {
            List<Map> list = ( List ) rd.get( "list" );
            if (list != null) {
                Map<String, Map> maps = new HashMap( );
                for ( Map info : list ) {
                    String uid = info.get("id").toString( );
                    info.put( "depts" , new HashSet());
                    maps.put(uid, info);
                }
                caze = db.getTable("user_dept").fetchCase()
                        .filter("user_id IN (?)",maps.keySet());
                if (incs == 2) {
                    caze.join(db.getTable("dept").tableName,
                        "dept", "user_dept.dept_id = dept.id" )
                        .select("user_id , dept_id , dept.* " );
                }
                List<Map> rows = caze.getAll( );
                for ( Map dept : rows ) {
                    String uid = dept.remove("user_id").toString();
                    ((Set) maps.get(uid).get("depts") ).add(dept );
                }
            }
        }

        return rd;
    }

    /**
     * @deprecated
     */
    public Set<String> getRoles(String userId)
    throws HongsException {
        if (userId == null) throw new HongsException(0x10000, "User Id required!");

        Table       asoc;
        FetchCase   caze;
        List<Map>   rows;
        Set<String> roles = new HashSet();
        Set<String> depts = new HashSet();

        asoc = this.db.getTable("a_master_user_dept");
        caze = this.fetchCase();
        caze.select(asoc.name+".dept_id")
            .filter(asoc.name+".user_id = ?", userId);
        rows = asoc.fetchMore(caze);
        for (Map row : rows) {
            depts.add((String) row.get("dept_id"));
        }

        asoc = this.db.getTable("a_master_dept_role");
        caze = this.fetchCase();
        caze.select(asoc.name+".role")
            .filter(asoc.name+".dept_id = ?", depts );
        rows = asoc.fetchMore(caze);
        for (Map row : rows) {
            roles.add((String) row.get("role"));
        }

        asoc = this.db.getTable("a_master_user_role");
        caze = this.fetchCase();
        caze.select(asoc.name+".role")
            .filter(asoc.name+".user_id = ?", userId);
        rows = asoc.fetchMore(caze);
        for (Map row : rows) {
            roles.add((String) row.get("role"));
        }

        return roles;
    }

    @Override
    protected void filter(FetchCase caze, Map req)
    throws HongsException {
        /**
         * 默认情况下不包含上级部门
         * 此时顶级仅需列出当前用户
         */
        if (!caze.getOption(  "INCLUDE_PARENTS" , false   )
        && "getList".equals(caze.getOption("MODEL_START"))) {
            Object  id = req.get(/***/"id");
            Object pid = req.get("dept_id");
            if (id == null && "0".equals( pid )) {
                ActionHelper helper = Core.getInstance(ActionHelper.class);
                String uid = (String) helper.getSessibute ( Cnst.UID_SES );
                if (!Cnst.ADM_UID.equals( uid )) {
                Set set = AuthKit.getUserDepts(uid);
                if (!set.contains(Cnst.ADM_GID)) {
                    req.put   ("id", uid);
                    req.remove("dept_id");
                }}
            }
        }

        /**
         * 如果有指定 dept_id
         * 则关联 a_master_user_dept 来约束范围
         * 当其为横杠时表示取那些没有关联的用户
         */
        Object deptId = req.get("dept_id");
        if (null != deptId && ! "".equals(deptId)) {
            if ( "-".equals ( deptId)) {
                caze.gotJoin("depts")
                    .from   ("a_master_user_dept")
                    .by     (FetchCase.INNER)
                    .on     ("`depts`.`user_id` = `user`.`id`")
                    .filter ("`depts`.`dept_id` IS NULL" /**/ );
            } else {
                caze.gotJoin("depts")
                    .from   ("a_master_user_dept")
                    .by     (FetchCase.INNER)
                    .on     ("`depts`.`user_id` = `user`.`id`")
                    .filter ("`depts`.`dept_id` IN (?)",deptId);
            }
        }

        super.filter(caze, req);
    }

    protected void permit(String id, Map data) throws HongsException {
        if (data != null) {
            // 权限限制, 仅能赋予当前登录用户所有的权限
            if (data.containsKey( "roles"  )) {
                data.put("rtime", System.currentTimeMillis() / 1000);
                List list = Synt.asList(data.get( "roles" ));
                AuthKit.cleanUserRoles (list, id);
//              if ( list.isEmpty() ) {
//                  throw new HongsException
//                      .Notice("ex.master.user.role.error")
//                      .setLocalizedContext("master");
//              }
                data.put("roles", list);
            }

            // 部门限制, 仅能指定当前登录用户下属的部门
            if (data.containsKey( "depts"  )) {
                data.put("rtime", System.currentTimeMillis() / 1000);
                List list = Synt.asList(data.get( "depts" ));
                AuthKit.cleanUserDepts (list, id);
                if ( list.isEmpty() ) {
                    throw new HongsException
                        .Notice("ex.master.user.dept.error")
                        .setLocalizedContext("master");
                }
                data.put("depts", list);
            }

            // 加密密码
            data.remove ("passcode");
            if (data.containsKey("password")) {
                String password = Synt.declare(data.get("password"), "");
                String passcode = Core.newIdentity();
                passcode = AuthKit.getCrypt(password + passcode);
                password = AuthKit.getCrypt(password + passcode);
                data.put("password", password);
                data.put("passcode", passcode);
            }
        }

        if (id != null) {
            // 超级管理员可操作任何用户
            ActionHelper helper = Core.getInstance(ActionHelper.class);
            String uid = (String) helper.getSessibute ( Cnst.UID_SES );
            if (Cnst.ADM_UID.equals(uid )) {
                return;
            }

            // 可以操作自己
            if (uid.equals(id)) {
                return;
            }

            // 超级管理组可操作任何用户
            // 但不包含超级管理员
            Set cur = AuthKit.getUserDepts(uid);
            if (cur.contains(Cnst.ADM_GID)
            && !Cnst.ADM_UID.equals( id )) {
                return;
            }

            // 仅可以操作下级用户
            Set tar = AuthKit.getLessDepts( id);
            Dept dept = new Dept();
            for (Object gid : cur) {
                Set cld = new HashSet(dept.getChildIds((String) gid, true));
                cld.retainAll(tar);
                if(!cld.isEmpty()) {
                    return;
                }
            }

            throw new HongsException
                .Notice("ex.master.user.unit.error")
                .setLocalizedContext("master");
        }
    }

}
