package app.hongs.serv.member;

import app.hongs.Cnst;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.NaviMap;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Verify;
import app.hongs.db.DB;
import app.hongs.db.util.FetchCase;
import app.hongs.serv.auth.RoleMap;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户动作接口
 * @author Hongs
 */
@Action("centra/member/user")
public class UserAction {

    private final User model;

    public UserAction()
    throws HongsException {
        model = (User) DB.getInstance("member").getModel("user");
    }

    @Action("list")
    public void getList(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        FetchCase fc = model.fetchCase();
        fc.setOption("INCLUDE_REMOVED", Synt.declare(rd.get("include-removed"), false));
        rd = model.getList(rd, fc);

        // Remove the password field, don't show password in page
        List<Map> list = (List) rd.get("list");
        for (Map  info :  list) {
            info.remove("password");
        }

        helper.reply(rd);
    }

    @Action("info")
    public void getInfo(ActionHelper helper)
    throws HongsException {
        Map    rd = helper.getRequestData(  );
        String id = helper.getParameter("id");
        String nc = helper.getParameter("navi-conf");
        String ud = (String) helper.getSessibute(Cnst.UID_SES);

        if (id != null && id.length() != 0) {
            rd = model.getInfo(rd);
        } else {
            rd =  new  HashMap(  );
        }

        // With all roles
        if (nc != null && nc.length() != 0) {
            List rs = !Cnst.ADM_UID.equals(ud) ?
                new RoleMap(NaviMap.getInstance(nc)).getRoleTranslated():
                new RoleMap(NaviMap.getInstance(nc)).getRoleTranslates();
            Dict.put(rd, rs, "enum", "roles..role");
        }

        // Remove the password field, don't show password in page
        Map info  = (Map) rd.get("info");
        if (info != null) {
            info.remove("password");
        }

        helper.reply(rd);
    }

    @Action("save")
    @Verify(conf="member", form="user")
    @CommitSuccess
    public void doSave(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();

        // Ignore empty password in update
        if ("".equals(rd.get("password"))) {
            rd.remove("password");
        }

        String id = model.set(rd);

        Map sd = new HashMap();
        sd.put( "id" , id);
        sd.put("name", rd.get("name"));
        sd.put("head", rd.get("head"));

        CoreLocale  ln = CoreLocale.getInstance().clone( );
                    ln.load("member" );
        String ms = ln.translate("core.save.user.success");
        helper.reply(ms, sd);
    }

    @Action("delete")
    @CommitSuccess
    public void doDelete(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        FetchCase fc = model.fetchCase();
        fc.setOption("INCLUDE_REMOVED", Synt.declare(rd.get("include-removed"), false));

        // 不能删除自己和超级管理员
        Set rs = Synt.asSet(rd.get(model.table.primaryKey));
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
                    ln.load("member");
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
