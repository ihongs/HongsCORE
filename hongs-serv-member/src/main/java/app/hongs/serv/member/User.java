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
        // 加密密码
        if (data.containsKey("password")) {
            data.put("password", AuthKit.getCrypt((String) data.get("password")));
        }

        // 权限限制, 仅能赋予当前登录用户所有的权限
        if (data.containsKey( "roles"  )) {
            data.put( "rtime", System.currentTimeMillis( ) / 1000 );
            List list = Synt.declare(data.get("roles"), List.class);
            AuthKit.clnRoles(list, null);
            if ( list.isEmpty() ) {
                throw new HongsException.Notice("分组设置错误, 请重试");
            }
        }

        // 部门限制, 仅能指定当前登录用户下属的部门
        if (data.containsKey( "depts"  )) {
            data.put( "rtime", System.currentTimeMillis( ) / 1000 );
            List list = Synt.declare(data.get("depts"), List.class);
            AuthKit.clnDepts(list, null);
            if ( list.isEmpty() ) {
                throw new HongsException.Notice("部门设置错误, 请重试");
            }
        }

        return super.add(data);
    }

    @Override
    public int put(String id, Map data, FetchCase caze) throws HongsException {
        // 加密密码
        if (data.containsKey("password")) {
            data.put("password", AuthKit.getCrypt((String) data.get("password")));
        }

        // 权限限制, 仅能赋予当前登录用户所有的权限
        if (data.containsKey( "roles"  )) {
            data.put( "rtime", System.currentTimeMillis( ) / 1000 );
            List list = Synt.declare(data.get("roles"), List.class);
            AuthKit.clnRoles(list,  id );
            if ( list.isEmpty() ) {
                throw new HongsException.Notice("分组设置错误, 请重试");
            }
        }

        // 部门限制, 仅能指定当前登录用户下属的部门
        if (data.containsKey( "depts"  )) {
            data.put( "rtime", System.currentTimeMillis( ) / 1000 );
            List list = Synt.declare(data.get("depts"), List.class);
            AuthKit.clnDepts(list,  id );
            if ( list.isEmpty() ) {
                throw new HongsException.Notice("部门设置错误, 请重试");
            }
        }

        return super.put(id, data, caze);
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
        caze = new FetchCase( );
        caze.select(".dept_id")
            .where (".user_id = ?", userId);
        rows = asoc.fetchMore(caze);
        for (Map row : rows) {
            depts.add((String) row.get("dept_id") );
        }

        asoc = this.db.getTable("a_member_dept_role");
        caze = new FetchCase();
        caze.select(".role"  )
            .where (".dept_id = ?", depts );
        rows = asoc.fetchMore(caze);
        for (Map row : rows) {
            roles.add((String) row.get("role"));
        }

        asoc = this.db.getTable("a_member_user_role");
        caze = new FetchCase();
        caze.select(".role"  )
            .where (".user_id = ?", userId);
        rows = asoc.fetchMore(caze);
        for (Map row : rows) {
            roles.add((String) row.get("role"));
        }

        return roles;
    }

    @Override
    protected void filter(FetchCase caze, Map req)
    throws HongsException {
        super.filter(caze, req);

        /**
         * 如果有指定dept_id
         * 则关联a_member_user_dept来约束范围
         */
        Object deptId = req.get("dept_id");
        if (null != deptId && !"".equals(deptId)) {
            caze.join ("a_member_user_dept", "depts")
                .on   ("`user_id` = `user`.`id`")
                .where("`dept_id` IN (?)",deptId);
        }
    }

    @Override
    protected boolean permit(FetchCase caze, Map wh, String id)
    throws HongsException {
        if (!super.permit(caze, wh, id)) {
            return false;
        }

        // 超级管理员可操作任何用户
        ActionHelper helper = Core.getInstance(ActionHelper.class);
        String uid = (String) helper.getSessibute ( Cnst.UID_SES );
        if (Cnst.ADM_UID.equals( uid )) {
            return true;
        }

        // 超级管理组可操作任何用户
        Set set = AuthKit.getDepts(uid);
        if (set.contains(Cnst.ADM_GID)) {
            return true;
        }

        // 仅能操作下级部门的用户
        Set cur = AuthKit.getDepts( id);
        Set cld ; Dept dpt = new Dept();
        for(Object gid : set) {
            cld = new HashSet(dpt.getChildIds((String) gid, true));
            if (cld.retainAll(cur)) {
                return true;
            }
        }

        return false;
    }

}
