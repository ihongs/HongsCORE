package io.github.ihongs.serv.master;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.util.Synt;
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
        if (caze == null) {
            caze = new FetchCase();
        }
        Map sd = super.getList(rd, caze);

        // 管辖范围限制标识: 0 一般用户, 1 管理员, 2 管理层
        if (Synt.declare(rd.get("bind-scope"), false )) {
            sd.put("scope", caze.getOption("SCOPE", 0));
        }

        return sd;
    }

    @Override
    protected void filter(FetchCase caze, Map req)
    throws HongsException {
        /**
         * 非超级管理员或在超级管理组
         * 限制查询为当前管辖范围以内
         */
        if (Synt.declare (req.get("bind-scope"), false)) {
            ActionHelper helper = Core.getInstance(ActionHelper.class);
            String mid = (String) helper.getSessibute ( Cnst.UID_SES );
            String pid = Synt.declare(req.get("dept_id"),"");
            if (!Cnst.ADM_UID.equals( mid )) {
            Set set = AuthKit.getUserDepts(mid);
            if (!set.contains(Cnst.ADM_GID)) {
                set = AuthKit.getMoreDepts(set);
            if (!set.contains(pid)) {
                caze.by     (FetchCase.DISTINCT  ); // 去重复
                caze.gotJoin("depts2")
                    .from   ("a_master_dept_user")
                    .by     (FetchCase.INNER)
                    .on     ("`depts2`.`user_id` = `user`.`id`")
                    .filter ("`depts2`.`dept_id` IN (?)" , set );
            }
            } else caze.setOption("SCOPE" , 2 );
            } else caze.setOption("SCOPE" , 1 );
        }

        /**
         * 如果有指定 dept_id
         * 则关联 a_master_dept_user 来约束范围
         * 当其为横杠时表示取那些没有关联的用户
         */
        Object pid = req.get("dept_id");
        if (null != pid && ! "".equals(pid)) {
            if ( "-".equals (pid ) ) {
                caze.gotJoin("depts2")
                    .from   ("a_master_dept_user")
                    .by     (FetchCase.INNER)
                    .on     ("`depts2`.`user_id` = `user`.`id`")
                    .filter ("`depts2`.`dept_id` IS NULL" /**/ );
            } else {
                caze.gotJoin("depts2")
                    .from   ("a_master_dept_user")
                    .by     (FetchCase.INNER)
                    .on     ("`depts2`.`user_id` = `user`.`id`")
                    .filter ("`depts2`.`dept_id` IN (?)" , pid );
            }
        }

        super.filter(caze, req);
    }

    protected void permit(String id, Map data) throws HongsException {
        if (data != null) {
            // 登录账号, 空串可能导致重复
            if (data.containsKey("username") ) {
                String un = Synt.declare(data.get("username") , "" );
                if (un.isEmpty()) {
                    data.put("username", null);
                }
            }

            // 加密密码, 联动密码更新时间
            data.remove ("passcode");
            if (data.containsKey("password") ) {
                String pw = Synt.declare(data.get("password") , "" );
                String pc = Core.newIdentity();
                pc = AuthKit.getCrypt(pw + pc);
                pw = AuthKit.getCrypt(pw + pc);
                data.put("password" , pw);
                data.put("passcode" , pc);
                data.put("ptime", System.currentTimeMillis() / 1000);
            }

            // 状态变更, 联动权限更新时间
            if (data.containsKey("state")) {
                data.put("rtime", System.currentTimeMillis() / 1000);
            }

            // 权限限制, 仅能赋予当前登录用户所有的权限
            if (data.containsKey("roles")) {
                data.put("rtime", System.currentTimeMillis() / 1000);
                List list = Synt.asList(data.get( "roles" ));
                AuthKit.cleanUserRoles (list, id);
//              if ( list.isEmpty() ) {
//                  throw new HongsException(400)
//                      .setLocalizedContent("master.user.role.error")
//                      .setLocalizedContext("master");
//              }
                data.put("roles", list);
            }

            // 部门限制, 仅能指定当前登录用户下属的部门
            if (data.containsKey("depts")) {
                data.put("rtime", System.currentTimeMillis() / 1000);
                List list = Synt.asList(data.get( "depts" ));
                AuthKit.cleanUserDepts (list, id);
                if ( list.isEmpty() ) {
                    throw new HongsException(400)
                        .setLocalizedContent("master.user.dept.error")
                        .setLocalizedContext("master");
                }
                data.put("depts", list);
            }
        }

        if (id != null) {
            // 超级管理员可操作任何用户
            // 但允许操作自身账号
            ActionHelper helper = Core.getInstance(ActionHelper.class);
            String uid = (String) helper.getSessibute ( Cnst.UID_SES );
            if (Cnst.ADM_UID.equals(uid) || id.equals ( uid )) {
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

            throw new HongsException(400)
                .setLocalizedContent("master.user.unit.error")
                .setLocalizedContext("master");
        }
    }

}
