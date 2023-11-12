package io.github.ihongs.serv.master;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.NaviMap;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门动作接口
 * @author Hongs
 */
@Action("centra/master/unit")
public class UnitAction {

    private final Unit model;

    public UnitAction()
    throws CruxException {
        model = (Unit) DB.getInstance("master").getModel("unit");
    }

    @Action("list")
    @Select(conf="master", form="unit")
    public void getList(ActionHelper helper)
    throws CruxException {
        Map  rd = helper.getRequestData();
        byte wd =  Synt.declare(rd.get("with-units") , (byte) 0);
     boolean fd =  Synt.declare(rd.get("find-depth") ,  false  );

        // With sub units
        if ( fd ) {
            String pid =  Synt.declare(rd.get("pid") , "" );
            if (! "".equals( pid ) && ! "-".equals( pid ) ) {
                Collection ids = model.getChildIds( pid , true );
                           ids.add(pid);
                rd.put (  "pid"  , ids);
            }
        }

        rd = model.getList (rd);
        List<Map> list = (List) rd.get("list");
        if (list != null) {

        // With all units
        if (wd == 1) {
            for ( Map info : list ) {
                String id = info.get("id").toString (   );
                info.put("units", model.getParentIds(id));
            }
        } else
        if (wd == 2) {
            for ( Map info : list ) {
                String id = info.get("id").toString (   );
                info.put("units", model.getParents  (id));
            }
        }

        }

        helper.reply(rd);
    }

    @Action("info")
    @Select(conf="master", form="unit")
    public void getInfo(ActionHelper helper)
    throws CruxException {
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

        helper.reply(rd);
    }

    @Action("save")
    @CommitSuccess
    public void doSave(ActionHelper helper)
    throws CruxException {
        Map rd = helper.getRequestData();
        String id = model.set(rd);
        CoreLocale  ln = CoreLocale.getMultiple("master", "default");
        String ms = ln.translate("core.save.unit.success");
        helper.reply(ms, id);
    }

    @Action("delete")
    @CommitSuccess
    public void doDelete(ActionHelper helper)
    throws CruxException {
        Map rd = helper.getRequestData();
        int rn = model.delete(rd);
        CoreLocale  ln = CoreLocale.getMultiple("master", "default");
        String ms = ln.translate("core.delete.unit.success", null,Integer.toString(rn));
        helper.reply(ms, rn);
    }

    @Action("unique")
    public void isUnique(ActionHelper helper)
    throws CruxException {
        Map rd = helper.getRequestData();
        FetchCase fc = model.fetchCase();
        fc.setOption("INCLUDE_REMOVED", Synt.declare(rd.get("include-removed"), false));
        boolean   rv = model.unique(rd, fc);
        helper.reply( null, rv ? 1 : 0 );
    }
}
