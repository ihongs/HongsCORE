package app.hongs.serv.module;

import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.db.DB;
import app.hongs.action.anno.CommitSuccess;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("manage/module/unit")
public class UnitAction {
    
    private final Unit model;

    public UnitAction() throws HongsException {
        model = (Unit) DB.getInstance("module").getModel("unit");
    }

    @Action("list")
    public void getList(ActionHelper helper)
    throws HongsException {
        Map data = model.getList(helper.getRequestData());
        helper.reply(data);
    }

    @Action("info")
    public void getInfo(ActionHelper helper)
    throws HongsException {
        Map data = model.getInfo(helper.getRequestData());
        helper.reply(data);
    }

    @Action("save")
    public void doSave(ActionHelper helper)
    throws HongsException {
        Map  data = helper.getRequestData();
        String id = model.set(data);
        Map  info = new HashMap();
        info.put( "id" , id);
        info.put("name", data.get("name") );
        CoreLocale  lang = CoreLocale.getInstance().clone( );
                    lang.load("module");
        String ms = lang.translate("core.save.unit.success");
        helper.reply(ms, info );
    }

    @Action("delete")
    @CommitSuccess
    public void doDelete(ActionHelper helper)
    throws HongsException {
        Map  data = helper.getRequestData();
        int  rows = model.delete(data );
        CoreLocale  lang = CoreLocale.getInstance().clone( );
                    lang.load("module");
        String ms = lang.translate("core.delete.unit.success", Integer.toString(rows));
        helper.reply(ms, rows);
    }

    @Action("unique")
    public void isUnique(ActionHelper helper)
    throws HongsException {
        boolean v = model.unique(helper.getRequestData());
        helper.reply(null, v);
    }

}
