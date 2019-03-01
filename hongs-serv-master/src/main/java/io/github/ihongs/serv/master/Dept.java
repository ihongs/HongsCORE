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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部门基础信息树模型
 * @author Hongs
 */
public class Dept
extends Grade {

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
            if (!Cnst.ADM_UID.equals( mid )) {
            Set set = AuthKit.getUserDepts(mid);
            if (!set.contains(Cnst.ADM_GID)) {
                Object  id = req.get( "id");
                Object pid = req.get("pid");
                if ("0".equals(  pid  )) {
                    set = AuthKit.getLessDepts(set);
                    req.put( "id", set);
                    req.remove( "pid" );
                } else {
                    set = AuthKit.getMoreDepts(set);
                    req.put( "id", set);
                }
            } else caze.setOption("SCOPE" , 2 );
            } else caze.setOption("SCOPE" , 1 );
        }

        /**
         * 如果有指定 user_id
         * 则关联 a_master_user_dept 来约束范围
         * 当其为横杠时表示取那些没有关联的部门
         */
        Object uid = req.get("user_id");
        if (null != uid && ! "".equals(uid)) {
            if ( "-".equals (uid)) {
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
                    .filter ("`users`.`user_id` IN (?)" , uid );
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
                AuthKit.cleanDeptRoles (list, id);
//              if ( list.isEmpty() ) {
//                  throw new HongsException
//                      .Notice("ex.master.user.dept.error")
//                      .setLocalizedContext("master");
//              }
                data.put("roles", list);
            }
        } else {
            List  list  ;
            Table tablx = db.getTable("user_dept");

            // 删除限制, 如果部门下有部门则中止当前操作
            list = table.fetchCase()
                .filter("pid = ? AND state > ?", id, 0 )
                .limit (1)
                .getAll( );
            if (!list.isEmpty() ) {
                throw new HongsException
                    .Notice("ex.master.dept.have.depts")
                    .setLocalizedContext("master");
            }

            // 删除限制, 如果部门下有用户则中止当前操作
            list = tablx.fetchCase()
                .filter("dept_id = ?", id)
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
