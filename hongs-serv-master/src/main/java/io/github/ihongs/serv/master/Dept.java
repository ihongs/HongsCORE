package io.github.ihongs.serv.master;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Mtree;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.db.Table;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.util.Synt;
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
        this(DB.getInstance("master").getTable("dept"));
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
        permit( id , null);

        return super.del(id, caze);
    }

    public Set<String> getRoles(String deptId)
    throws HongsException {
        if (deptId == null) throw new HongsException(0x10000, "Dept Id required!");

        Table       asoc;
        FetchCase   caze;
        List<Map>   rows;
        Set<String> roles = new HashSet();

        asoc = this.db.getTable("a_master_dept_role");
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
        /**
         * 默认情况下不包含上级部门
         * 此时顶级需取用户所在部门
         */
        if (!caze.getOption(  "INCLUDE_PARENTS" , false   )
        && "getList".equals(caze.getOption("MODEL_START"))) {
            Object  id = req.get( "id");
            Object pid = req.get("pid");
            if (id == null && "0".equals( pid )) {
                ActionHelper helper = Core.getInstance(ActionHelper.class);
                String uid = (String) helper.getSessibute ( Cnst.UID_SES );
                if (!Cnst.ADM_UID.equals( uid )) {
                Set set = AuthKit.getUserDepts(uid);
                if (!set.contains(Cnst.ADM_GID)) {
                    req.put( "id", set);
                    req.remove( "pid" );
                }}
            }
        }

        /**
         * 如果有指定 user_id
         * 则关联 a_master_user_dept 来约束范围
         * 当其为横杠时表示取那些没有关联的部门
         */
        Object userId = req.get("user_id");
        if (null != userId && ! "".equals(userId)) {
            if ( "-".equals ( userId)) {
                caze.gotJoin("users")
                    .from   ("a_master_user_dept")
                    .by     (FetchCase.INNER)
                    .on     ("`users`.`dept_id` = `dept`.`id`")
                    .filter ("`users`.`user_id` IS NULL" /**/ );
            } else {
                caze.gotJoin("users")
                    .from   ("a_master_user_dept")
                    .by     (FetchCase.INNER)
                    .on     ("`users`.`dept_id` = `dept`.`id`")
                    .filter ("`users`.`user_id` IN (?)",userId);
            }
        }

        super.filter(caze, req);
    }

    protected void permit(String id, Map data) throws HongsException {
      String pid  = null;

        if (data != null) {
            // 权限限制, 仅能赋予当前登录用户所有的权限
            if (data.containsKey( "roles" )) {
                data.put("rtime", System.currentTimeMillis() / 1000);
                List list = Synt.asList(data.get( "roles" ));
                AuthKit.cleanDeptRoles (list, id);
                if ( list.isEmpty() ) {
                    throw new HongsException
                        .Notice("ex.master.user.dept.error")
                        .setLocalizedContext("master");
                }
                data.put("roles", list);
            }

            // 部门限制, 默认顶级, 是否可操作在下方判断
            pid = Synt.declare(data.get("pid"), "");
            if ("".equals(pid)) pid = Cnst.ADM_GID ;
        } else {
            // 删除限制, 如果部门下有用户则中止当前操作
            User user = new User();
            List list = user.table.fetchCase()
                .join  (user.db.getTable("user_dept").tableName , "depts" ,
                        "`depts`.`user_id` = `user`.`id`", FetchCase.INNER)
                .filter("`depts`.`dept_id` = ?", id )
                .limit (1)
                .getAll( );
            if (!list.isEmpty() ) {
                throw new HongsException
                    .Notice("ex.master.dept.have.users")
                    .setLocalizedContext("master");
            }
        }

        if (id == null && pid == null) {
            throw new NullPointerException("id and pid cannot be all null");
        }
        if (id != null || pid != null) {
            // 超级管理员可操作任何部门
            ActionHelper helper = Core.getInstance(ActionHelper.class);
            String uid = (String) helper.getSessibute ( Cnst.UID_SES );
            if (Cnst.ADM_UID.equals(uid )) {
                return;
            }

            // 超级管理组可操作任何部门
            // 但禁止操作顶级部门
            Set cur = AuthKit.getUserDepts(uid);
            if (cur.contains(Cnst.ADM_GID)
            && !Cnst.ADM_GID.equals( id )) {
                return;
            }

            // 仅可以操作下级部门
            for (Object gid : cur) {
                Set cld = new HashSet(this.getChildIds((String) gid, true));
                if (  null != pid
                && (gid.  equals(pid)
                ||  cld.contains(pid))) {
                    return;
                }
                if (  null !=  id
                &&  cld.contains( id) ) {
                    return;
                }
            }

            throw new HongsException
                .Notice("ex.master.dept.unit.error")
                .setLocalizedContext("master");
        }
    }

}
