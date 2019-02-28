package io.github.ihongs.serv.master;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.NaviMap;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.normal.serv.Record;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.serv.auth.RoleMap;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import java.util.Calendar;
import java.util.HashMap;
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
    public void getList(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        FetchCase fc = model.fetchCase();
        fc.setOption("INCLUDE_REMOVED", Synt.declare(rd.get("include-removed"), false));
        fc.setOption("INCLUDE_PARENTS", Synt.declare(rd.get("include-parents"), false));
        fc.setOption("INCLUDE_DEPARTS", Synt.declare(rd.get("with-depts") , (byte) 0 ));
        rd = model.getList(rd, fc);

        // Remove the password field, don't show password in page
        List<Map> list = (List) rd.get("list");
        for (Map  info :  list) {
            info.remove("password");
            info.remove("passcode");
        }

        helper.reply(rd);
    }

    @Action("info")
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
            List rs = new RoleMap (NaviMap.getInstance(nc))
                .getRoleTranslated(
                    ! Cnst.ADM_UID.equals (ud)
                    ? AuthKit.getUserRoles(ud)
                    : null
                );
            Dict.put(rd, rs, "enum", "roles..role");
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
        boolean cp;

        // Ignore empty password in update
        if (null  ==  rd.get("password")
        ||  "".equals(rd.get("password"))) {
            rd.remove("password");
            rd.remove("passcode");
            cp = false;
        } else
        if (null  ==  rd.get(   "id"   )
        &&  "".equals(rd.get(   "id"   ))) {
            cp = false;
        } else
        {
            cp = true ;
        }

        String id = model.set(rd);

        Map sd = new HashMap();
        sd.put( "id" , id);
        sd.put("name", rd.get("name"));
        sd.put("head", rd.get("head"));

        CoreLocale  ln = CoreLocale.getInstance().clone( );
                    ln.load("master");
        String ms = ln.translate("core.save.user.success");
        helper.reply(ms, sd);

        /**
         * 2019/02/26
         * 有修改密码则将重试次数归零,
         * 若密码重试次数标记有用到IP,
         * 需告知登录的校验标记改用ID.
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
        }
    }

    @Action("delete")
    @CommitSuccess
    public void doDelete(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        FetchCase fc = model.fetchCase();
        fc.setOption("INCLUDE_REMOVED", Synt.declare(rd.get("include-removed"), false));

        // 不能删除自己和超级管理员
        Set rs = Synt.asSet( rd.get( model.table.primaryKey ) );
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

        int rn = model.delete(rd, fc);
        CoreLocale  ln = CoreLocale.getInstance().clone( );
                    ln.load("master");
        String ms = ln.translate("core.delete.user.success", Integer.toString(rn));
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
