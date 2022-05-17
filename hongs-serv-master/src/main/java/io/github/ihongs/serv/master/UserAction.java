package io.github.ihongs.serv.master;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.NaviMap;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.normal.serv.Record;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户动作接口
 * @author Hongs
 */
@Action("centra/master/user")
public class UserAction {

    private final User model;

    public UserAction()
    throws HongsException {
        model = (User) DB.getInstance("master").getModel("user");
    }

    @Action("list")
    @Select(conf="master", form="user")
    public void getList(ActionHelper helper)
    throws HongsException {
        Map  rd = helper.getRequestData();
        byte wd =  Synt.declare(rd.get("with-units") , (byte) 0);
     boolean fd =  Synt.declare(rd.get("find-depth") ,  false  );

        // With sub units
        if ( fd ) {
            Unit depth = (Unit) DB.getInstance("master")
                                  .getModel   ( "unit" );
            String pid =  Synt.declare(rd.get("unit_id"),  ""  );
            if (! "".equals( pid ) && ! "-".equals( pid ) ) {
                Collection ids = depth.getChildIds( pid , true );
                           ids.add(pid);
                rd.put("unit_id" , ids);
            }
        }

        rd = model.getList (rd);
        List<Map> list = (List) rd.get("list");
        if (list != null) {

        // With all units
        if (wd == 1) {
            Map<String, Map> maps = new HashMap( );
            for ( Map info : list ) {
                info.put( "units" , new HashSet());
                maps.put(info.get("id").toString(), info);
            }

            List<Map> rows = model.db.getTable("unit_user")
                .fetchCase()
                .filter("user_id IN (?)" , maps.keySet( ) )
                .select("user_id, unit_id")
                .getAll(   );
            for ( Map unit : rows ) {
                String uid = unit.remove("user_id").toString();
                ((Set) maps.get(uid).get("units") ).add(unit );
            }
        } else
        if (wd == 2) {
            Map<String, Map> maps = new HashMap( );
            for ( Map info : list ) {
                info.put( "units" , new HashSet());
                maps.put(info.get("id").toString(), info);
            }

            List<Map> rows = model.db.getTable("unit_user")
                .fetchCase()
                .join(model.db.getTable("unit").tableName ,
                    "unit", "unit_user.unit_id = unit.id" )
                .filter("user_id IN (?)" , maps.keySet( ) )
                .select("user_id, unit_id, unit.*")
                .getAll(   );
            for ( Map unit : rows ) {
                String uid = unit.remove("user_id").toString();
                ((Set) maps.get(uid).get("units") ).add(unit );
            }
        }

        // Remove the password field, don't show password in page
        for (Map  info  :  list) {
            info.remove("password");
            info.remove("passcode");
        }

        }

        helper.reply(rd);
    }

    @Action("info")
    @Select(conf="master", form="user")
    public void getInfo(ActionHelper helper)
    throws HongsException {
        Map    rd = helper.getRequestData(  );
        String id = helper.getParameter("id");
        String nc = helper.getParameter("with-roles");
        String ud = (String) helper.getSessibute(Cnst.UID_SES);

        if (id != null && id.length() != 0) {
            rd  = model.getInfo(rd);
        } else {
            rd  =  new  HashMap(  );
        }

        // With all roles
        if (nc != null && nc.length() != 0) {
            List rs = NaviMap.getInstance (nc)
                .getRoleTranslated (0,
                    ! Cnst.ADM_UID.equals (ud)
                    ? AuthKit.getUserRoles(ud)
                    : null
                );
            Dict.put(rd, rs, "enfo", "roles..role");
        }

        // Remove the password field, don't show password in page
        Map info  = (Map) rd.get("info");
        if (info != null) {
            info.remove("password");
            info.remove("passcode");
        }

        helper.reply(rd);
    }

    @Action("save")
    @Verify(conf="master", form="user")
    @CommitSuccess
    public void doSave(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();

        // Ignore empty password in update
         boolean  cp  ;
        if (null  ==  rd.get("password")
        ||  "".equals(rd.get("password"))) {
            rd.remove("password");
            rd.remove("passcode");
            cp = false;
        } else
        if (null  ==  rd.get(   "id"   )
        ||  "".equals(rd.get(   "id"   ))) {
            cp = false;
        } else
        {
            cp = true ;
        }

        /**
         * 2022/05/15
         * 区分所属的分组和管理的分组
         */
        if (rd.containsKey("units0")
        ||  rd.containsKey("units1")) {
            List<Map> unit0 = Synt.asList(rd.get("units0"));
            List<Map> unit1 = Synt.asList(rd.get("units1"));
            List<Map> units = new ArrayList((unit0 != null ? unit0.size() : 0) + (unit1 != null ? unit1.size() : 0));
            if (unit0 != null) for (Map item : unit0) {
                units.add(item); item.put("type" , 0);
            }
            if (unit1 != null) for (Map item : unit1) {
                units.add(item); item.put("type" , 1);
            }
            rd.put("units", units);
        }
        
        String id = model.set(rd);
        CoreLocale  ln = CoreLocale.getInstance().clone( );
                    ln.load("master");
        String ms = ln.translate("core.save.user.success");
        helper.reply(ms, id);

        /**
         * 2019/02/26
         * 有修改密码则将重试次数归零,
         * 若密码重试次数标记有用到IP,
         * 需告知登录的校验标记改用ID.
         *
         * 2021/06/20
         * 已加修改密码需重新登录逻辑,
         * 重写会话规避当前用户重登录.
         */
        if (cp) {
            Calendar ca;
            long     et;
            ca = Calendar.getInstance(Core.getTimezone( ));
            ca.setTimeInMillis ( Core.ACTION_TIME.get ( ));
            ca.set(Calendar.HOUR_OF_DAY, 23);
            ca.set(Calendar.MINUTE, 59);
            ca.set(Calendar.SECOND, 59);
            et = ca.getTimeInMillis()/ 1000 + 1 ;
            Record.set( "sign.retry.allow." + id, 1 , et );
            Record.del( "sign.retry.times." + id /*Drop*/);

            if ("*".equals(helper.getSessibute(Cnst.USK_SES))) {
                helper.setSessibute(Cnst.UST_SES, System.currentTimeMillis() / 1000);
            }
        }
    }

    @Action("delete")
    @CommitSuccess
    public void doDelete(ActionHelper helper)
    throws HongsException {
        // 不能删除自己和超级管理员
        Set rs = Synt.asSet(helper.getParameter(Cnst. ID_KEY ));
        if (rs != null) {
            if (rs.contains(helper.getSessibute(Cnst.UID_SES))) {
                helper.fault("不能删除当前登录用户");
                return;
            }
            if (rs.contains(Cnst.ADM_UID)) {
                helper.fault("不能删除超级管理账号");
                return;
            }
        }

        Map rd = helper.getRequestData();
        int rn = model.delete(rd);
        CoreLocale  ln = CoreLocale.getInstance().clone( );
                    ln.load("master");
        String ms = ln.translate("core.delete.user.success", null,Integer.toString(rn));
        helper.reply(ms, rn);
    }

    @Action("unique")
    public void isUnique(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        FetchCase fc = model.fetchCase();
        fc.setOption("INCLUDE_REMOVED", Synt.declare(rd.get("include-removed"), false));
        boolean   rv = model.unique(rd, fc);
        helper.reply( null, rv ? 1 : 0 );
    }

}
