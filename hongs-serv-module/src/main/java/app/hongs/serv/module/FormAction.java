package app.hongs.serv.module;

import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.db.DB;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.db.Table;
import app.hongs.db.link.Loop;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("manage/module/form")
public class FormAction {
    
    private final Form model;

    public FormAction() throws HongsException {
        model = (Form) DB.getInstance("module").getModel("form");
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
        String ms = lang.translate("core.save.form.success");
        helper.reply(ms, info);
    }

    @Action("delete")
    @CommitSuccess
    public void doDelete(ActionHelper helper)
    throws HongsException {
        Map  data = helper.getRequestData();
        int  rows = model.delete(data );
        CoreLocale  lang = CoreLocale.getInstance().clone( );
                    lang.load("module");
        String ms = lang.translate("core.delete.form.success", Integer.toString(rows));
        helper.reply(ms, rows);
    }

    @Action("unique")
    public void isUnique(ActionHelper helper)
    throws HongsException {
        boolean v = model.unique(helper.getRequestData());
        helper.reply(null, v);
    }
    
    @Action("fork/list")
    public void getForkList() throws HongsException {
        Table ft = model.table;
        Table ut = model.db.getTable("unit");
    }
    
    public void getForkList(List list, Table ft, Table ut) throws HongsException {
        Loop rows = ut
                .select(".`id`,.`name`")
                .filter("")
                .oll();
    }

}
