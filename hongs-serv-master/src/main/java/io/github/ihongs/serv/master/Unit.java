package io.github.ihongs.serv.master;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Grade;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.util.Synt;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部门基础信息树模型
 * @author Hongs
 */
public class Unit
extends Grade {

    public Unit()
    throws HongsException {
        this(DB.getInstance("master").getTable("unit"));
    }

    public Unit(Table table)
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
            Object pid =  req.get(    "pid"   );
            if (Cnst.ADM_UID.equals( mid )) {
                caze.setOption("SCOPE" , 2);
            } else {
            Set set = AuthKit.getManaUnits(mid);
            if (set.contains(Cnst.TOP_GID)) {
                caze.setOption("SCOPE" , 1);
            } else
            if (pid. equals (Cnst.TOP_GID)) {
                set = AuthKit.getLeadUnits(mid);
                req.remove("pid");
                req.put("id",set);
            } else
            if (pid. equals ( "") || ! (pid instanceof String) ) {
                req.put("id",set);
            } else
            if (set.contains(pid) == false) {
                throw new HongsException(400, "@master:master.unit.area.error");
            }
            }
        }

        /**
         * 如果有指定 user_id
         * 则关联 a_master_unit_user 来约束范围
         * 当其为横杠时表示取那些没有关联的部门
         */
        Object uid = req.get("user_id");
        if (null != uid && ! "".equals(uid)) {
            if ( "-".equals (uid)) {
                caze.gotJoin("users2")
                    .from   ("a_master_unit_user")
                    .by     (FetchCase.INNER)
                    .on     ("`users2`.`unit_id` = `unit`.`id`")
                    .filter ("`users2`.`user_id` IS NULL" /**/ );
            } else {
                caze.gotJoin("users2")
                    .from   ("a_master_unit_user")
                    .by     (FetchCase.INNER)
                    .on     ("`users2`.`unit_id` = `unit`.`id`")
                    .filter ("`users2`.`user_id` IN (?)" , uid );
            }
        }

        super.filter(caze, req);
    }

    protected void permit(String id, Map data) throws HongsException {
      String pid  = null;

        if (data != null) {
            // 上级部门
             pid  =  (String) data.get ("pid");
            if (pid == null || pid.equals("")) {
                data.remove("pid");
                pid  = null;
            }

            // 权限限制, 仅能赋予当前登录用户所有的权限
            if (data.containsKey("roles")) {
                data.put("rtime", System.currentTimeMillis() / 1000);
                List list = Synt.asList(data.get( "roles" ));
                AuthKit.cleanUnitRoles (list, id);
            //  if ( list.isEmpty() ) {
            //      throw new HongsException(400, "@master:master.user.unit.error");
            //  }
                data.put("roles", list);
            }
        } else {
            List  list  ;
            Table tablx = db.getTable("unit_user");

            // 删除限制, 如果部门下有部门则中止当前操作
            list = table.fetchCase()
                .filter("pid = ? AND state > ?", id, 0 )
                .limit (1)
                .getAll( );
            if (!list.isEmpty() ) {
                throw new HongsException(400, "@master:master.unit.have.units");
            }

            // 删除限制, 如果部门下有用户则中止当前操作
            list = tablx.fetchCase()
                .filter("unit_id = ?", id)
                .limit (1)
                .getAll( );
            if (!list.isEmpty() ) {
                throw new HongsException(400, "@master:master.unit.have.users");
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

            // 仅可以操作管理范围的部门
            Set mur = AuthKit.getManaUnits(uid);
            if (mur.contains( id )
            &&  mur.contains(pid)) {
                return;
            }

            throw new HongsException(400, "@master:master.unit.area.error");
        }
    }

}
