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
            Object pid =  req.get(  "unit_id" );
            if (Cnst.ADM_UID.equals( mid )) {
                caze.setOption("SCOPE" , 1);
            } else {
            Set set = AuthKit.getManaUnits(mid);
            if (set.contains(Cnst.TOP_GID)) {
                caze.setOption("SCOPE" , 2);
            } else
            if (pid. equals (Cnst.TOP_GID)) {
                caze.filter ("`user`.`id` = ''"  ); // 空查询
            } else
            if (pid. equals ("" ) || ! (pid instanceof String) ) {
                caze.by     (FetchCase.DISTINCT  ); // 去重复
                caze.gotJoin("units2")
                    .from   ("a_master_unit_user")
                    .by     (FetchCase.INNER)
                    .on     ("`units2`.`user_id` = `user`.`id`")
                    .filter ("`units2`.`unit_id` IN (?)" , set );
            } else
            if (set.contains(pid) == false) {
                throw new HongsException(400, "@master:master.user.area.error");
            }
            }
        }

        /**
         * 如果有指定 unit_id
         * 则关联 a_master_unit_user 来约束范围
         * 当其为横杠时表示取那些没有关联的用户
         */
        Object pid = req.get("unit_id");
        if (null != pid && ! "".equals(pid)) {
            if ( "-".equals (pid ) ) {
                caze.gotJoin("units2")
                    .from   ("a_master_unit_user")
                    .by     (FetchCase.INNER)
                    .on     ("`units2`.`user_id` = `user`.`id`")
                    .filter ("`units2`.`unit_id` IS NULL" /**/ );
            } else {
                caze.gotJoin("units2")
                    .from   ("a_master_unit_user")
                    .by     (FetchCase.INNER)
                    .on     ("`units2`.`user_id` = `user`.`id`")
                    .filter ("`units2`.`unit_id` IN (?)" , pid );
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
            if (data.containsKey("password") ) {
                String pw = Synt.declare(data.get("password") , "" );
                String pc ;
                if (pw.isEmpty()) {
                    data.put("password", null);
                    data.put("passcode", null);
                } else {
                    pc = Core.newIdentity(   );
                    pc = AuthKit.getCrypt(pw + pc);
                    pw = AuthKit.getCrypt(pw + pc);
                    data.put("password" , pw );
                    data.put("passcode" , pc );
                }
                data.put("ptime", System.currentTimeMillis() / 1000);
            } else {
                data.remove( "passcode" );
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
            //  if ( list.isEmpty() ) {
            //      throw new HongsException(400, "@master:master.user.role.error");
            //  }
                data.put("roles", list);
            }

            // 部门限制, 仅能指定当前登录用户下属的部门
            if (data.containsKey("units")) {
                data.put("rtime", System.currentTimeMillis() / 1000);
                List list = Synt.asList(data.get( "units" ));
                AuthKit.cleanUserUnits (list, id);
                if ( list.isEmpty() ) {
                    throw new HongsException(400, "@master:master.user.unit.error");
                }
                data.put("units", list);
            }
        }

        if (id != null) {
            // 超级管理员可操作任何用户
            ActionHelper helper = Core.getInstance(ActionHelper.class);
            String uid = (String) helper.getSessibute ( Cnst.UID_SES );
            if (Cnst.ADM_UID.equals(uid) || id.equals ( uid )) {
                return;
            }

            // 仅可以操作管理范围的用户
            Set cur = AuthKit.getUserUnits( id);
            Set mur = AuthKit.getManaUnits(uid);
            for(Object pid : cur ) {
            if (mur.contains(pid)) {
                return;
            }}

            throw new HongsException(400, "@master:master.user.area.error");
        }
    }

}
