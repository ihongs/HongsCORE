package app.hongs.serv.member;

import app.hongs.Cnst;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.NaviMap;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.db.DB;
import app.hongs.db.util.FetchCase;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门动作接口
 * @author Hongs
 */
@Action("bundle/member/dept")
public class DeptAction {

    private final Dept model;

    public DeptAction()
    throws HongsException {
        model = (Dept) DB.getInstance("member").getModel("dept");
    }

    @Action("list")
    public void getList(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        FetchCase fc = model.fetchCase();
        fc.setOption("INCLUDE_REMOVED", Synt.declare(rd.get("include-removed"), false));
        rd = model.getList(rd, fc);
        helper.reply(rd);
    }

    @Action("info")
    public void getInfo(ActionHelper helper)
    throws HongsException {
        Map    rd = helper.getRequestData(  );
        String id = helper.getParameter("id");
        String wr = helper.getParameter("navi-conf");
        String ud = (String) helper.getSessibute(Cnst.UID_SES);

        if (id != null && id.length() != 0) {
            rd = model.getInfo(rd);
        } else {
            rd =  new  HashMap(  );
        }

        // With all roles
        if (wr != null && wr.length() != 0) {
            List rs = !Cnst.ADM_UID.equals(ud) ?
                    NaviMap.getInstance(wr).getRoleTranslated(0, 0):
                    NaviMap.getInstance(wr).getRoleTranslated(0, 0, null);
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
                    ln.load("member" );
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
                    ln.load("member" );
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
