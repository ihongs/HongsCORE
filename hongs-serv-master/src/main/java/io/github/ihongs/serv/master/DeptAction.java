package io.github.ihongs.serv.master;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.NaviMap;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.serv.auth.RoleMap;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门动作接口
 * @author Hongs
 */
@Action("centra/master/dept")
public class DeptAction {

    private final Dept model;

    public DeptAction()
    throws HongsException {
        model = (Dept) DB.getInstance("master").getModel("dept");
    }

    @Action("list")
    public void getList(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        FetchCase fc = model.fetchCase();
        fc.setOption("INCLUDE_REMOVED", Synt.declare(rd.get("include-removed"), false));
        fc.setOption("INCLUDE_PARENTS", Synt.declare(rd.get("include-parents"), false));
        rd = model.getList(rd, fc);
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
            rd  = model.getInfo(rd);
        } else {
            rd  =  new  HashMap(  );
        }

        // With all roles
        if (nc != null && nc.length() != 0) {
            List rs = !Cnst.ADM_UID.equals(ud) ?
                new RoleMap(NaviMap.getInstance(nc)).getRoleTranslated():
                new RoleMap(NaviMap.getInstance(nc)).getRoleTranslates();
            Dict.put(rd, rs, "enum", "roles..role");
        }

        helper.reply(rd);
    }

    @Action("save")
    @CommitSuccess
    public void doSave(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        String id = model.set(rd);
        
        rd = new HashMap();
        rd.put( "id" , id);
        rd.put("name", rd.get("name"));

        CoreLocale  ln = CoreLocale.getInstance().clone( );
                    ln.load("master");
        String ms = ln.translate("core.save.dept.success");
        helper.reply(ms, rd);
    }

    @Action("delete")
    @CommitSuccess
    public void doDelete(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        int rn = model.delete(rd);
        CoreLocale  ln = CoreLocale.getInstance().clone( );
                    ln.load("master");
        String ms = ln.translate("core.delete.dept.success", Integer.toString(rn));
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
