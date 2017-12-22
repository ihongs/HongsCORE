package app.hongs.serv.member;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.db.DB;
import app.hongs.db.util.FetchCase;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.serv.auth.AuthKit;
import app.hongs.util.Synt;
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
        this(DB.getInstance("member").getTable("user"));
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

    public Set<String> getRoles(String userId)
    throws HongsException {
        if (userId == null) throw new HongsException(0x10000, "User Id required!");

        Table       asoc;
        FetchCase   caze;
        List<Map>   rows;
        Set<String> roles = new HashSet();
        Set<String> depts = new HashSet();

        asoc = this.db.getTable("a_member_user_dept");
        caze = this.fetchCase();
        caze.select(asoc.name+".dept_id")
            .filter(asoc.name+".user_id = ?", userId);
        rows = asoc.fetchMore(caze);
        for (Map row : rows) {
            depts.add((String) row.get("dept_id"));
        }

        asoc = this.db.getTable("a_member_dept_role");
        caze = this.fetchCase();
        caze.select(asoc.name+".role")
            .filter(asoc.name+".dept_id = ?", depts );
        rows = asoc.fetchMore(caze);
        for (Map row : rows) {
            roles.add((String) row.get("role"));
        }

        asoc = this.db.getTable("a_member_user_role");
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
        if (!caze.getOption(  "INCLUDE_PARENTS"  ,  false  )
        && "getList".equals(caze.getOption("MODEL_METHOD"))) {
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
         * 如果有指定dept_id
         * 则关联a_member_user_dept来约束范围
         */
        Object deptId = req.get("dept_id");
        if (null != deptId && ! "".equals(deptId)) {
            caze.gotJoin("depts")
                .from   ("a_member_user_dept")
                .by     (FetchCase.INNER)
                .on     ("`depts`.`user_id` = `user`.`id`")
                .filter ("`depts`.`dept_id` = ?" , deptId );
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
                if ( list.isEmpty() ) {
                    throw new HongsException
                        .Notice("ex.member.user.role.error")
                        .setLocalizedContext("member");
                }
                data.put("roles", list);
            }

            // 部门限制, 仅能指定当前登录用户下属的部门
            if (data.containsKey( "depts"  )) {
                data.put("rtime", System.currentTimeMillis() / 1000);
                List list = Synt.asList(data.get( "depts" ));
                AuthKit.cleanUserDepts (list, id);
                if ( list.isEmpty() ) {
                    throw new HongsException
                        .Notice("ex.member.user.dept.error")
                        .setLocalizedContext("member");
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
            if (Cnst.ADM_UID.equals( uid )) {
                return;
            }

            // 超级管理组可操作任何用户(除超级用户)
            Set set = AuthKit.getUserDepts(uid);
            if (set.contains(Cnst.ADM_GID)
            && !Cnst.ADM_UID.equals(  id )) {
                return;
            }

            // 仅可以操作下级部门的用户
            if (uid.equals( id )) {
                return; // 但可操作自己
            }
            Set cur = AuthKit.getUserDepts( id);
            Set cld ; Dept dpt = new Dept();
            for(Object gid : set) {
                cld = new HashSet(dpt.getChildIds((String) gid, true));
                cld.retainAll( cur );
                if (! cld.isEmpty( )) {
                    return;
                }
            }

            throw new HongsException
                .Notice("ex.member.user.unit.error")
                .setLocalizedContext("member");
        }
    }

}
