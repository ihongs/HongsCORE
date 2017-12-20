package app.hongs.serv.member;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.db.DB;
import app.hongs.db.Mtree;
import app.hongs.db.util.FetchCase;
import app.hongs.db.Table;
import app.hongs.serv.auth.AuthKit;
import app.hongs.util.Synt;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部门基础信息树模型
 * @author Hongs
 */
public class Dept
extends Mtree {

    public Dept()
    throws HongsException {
        this(DB.getInstance("member").getTable("dept"));
    }

    public Dept(Table table)
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
        permit( id ,(String) null);

        return super.del(id, caze);
    }

    public Set<String> getRoles(String deptId)
    throws HongsException {
        if (deptId == null) throw new HongsException(0x10000, "Dept Id required!");

        Table       asoc;
        FetchCase   caze;
        List<Map>   rows;
        Set<String> roles = new HashSet();

        asoc = this.db.getTable("a_member_dept_role");
        caze = this.fetchCase();
        caze.select(asoc.name+".role")
            .filter(asoc.name+".dept_id = ?", deptId);
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
         * 如果有指定user_id
         * 则关联a_member_user_dept来约束范围
         */
        Object userId = req.get("user_id");
        if (null != userId && ! "".equals(userId)) {
            caze.gotJoin("users")
                .from   ("a_member_user_dept")
                .by     (FetchCase.INNER)
                .on     ("`users`.`dept_id` = `dept`.`id`")
                .filter ("`users`.`user_id` IN (?)",userId);
        }
    }

    protected void permit(String id, Map data) throws HongsException {
        // 权限限制, 仅能赋予当前登录用户所有的权限
        if (data.containsKey( "roles" )) {
            data.put("rtime", System.currentTimeMillis() / 1000);
            List list = Synt.asList(data.get( "roles" ));
            AuthKit.cleanDeptRoles(list,  id );
            if ( list.isEmpty() ) {
                throw new HongsException.Notice("分组设置错误, 请重试");
            }
            data.put("roles", list);
        }

        // 部门限制, 仅能指定当前登录用户下属的部门
        String pid = Synt.declare ( data.get("pid"), "");

        permit(id, pid);
    }

    protected void permit(String id, String pid)
    throws HongsException {
        if (id == null && pid == null) {
            throw new NullPointerException("id and pid can not all be null");
        }

        // 超级管理员可操作任何部门
        ActionHelper helper = Core.getInstance(ActionHelper.class);
        String uid = (String) helper.getSessibute ( Cnst.UID_SES );
        if (Cnst.ADM_UID.equals( uid )) {
            return;
        }

        // 超级管理组可操作任何部门
        Set set = AuthKit.getUserDepts(uid);
        if (set.contains(Cnst.ADM_GID)) {
            return;
        }

        // 仅能操作下级部门
        Set cld ; Dept dpt = new Dept();
        for(Object gid : set) {
            cld = new HashSet(dpt.getChildIds((String) gid, true));
            if ( null != pid) {
            if (gid.  equals (pid)) {
                return;
            }
            if (cld.contains (pid)) {
                return;
            }}
            if ( null !=  id) {
            if (cld.contains ( id)) {
                return;
            }}
        }

        throw new HongsException.Notice("您无权操作 ID 为 "+id+" 的部门");
    }

}
