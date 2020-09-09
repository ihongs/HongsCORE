package io.github.ihongs.serv.matrix;

import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.db.DB;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("centra/matrix/form")
public class FormAction {

    protected final Form model;

    public FormAction() throws HongsException {
        model = (Form) DB.getInstance("matrix").getModel("form");
    }

    @Action("list")
    @Select(conf="matrix", form="form")
    public void getList(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        Map sd = model.getList(rd);
        helper.reply(sd);
    }

    @Action("info")
    @Select(conf="matrix", form="form")
    public void getInfo(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        Map sd = model.getInfo(rd);
        helper.reply(sd);
    }

    @Action("save")
    public void doSave(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        String id = model.set(rd);
        CoreLocale  ln = CoreLocale.getInstance().clone( );
                    ln.load("matrix");
        String ms = ln.translate("core.save.form.success");
        helper.reply(ms, id);
    }

    @Action("delete")
    @CommitSuccess
    public void doDelete(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        int rn = model.delete(rd);
        CoreLocale  ln = CoreLocale.getInstance().clone( );
                    ln.load("matrix");
        String ms = ln.translate("core.delete.form.success", null, Integer.toString(rn));
        helper.reply(ms, rn);
    }

    @Action("unique")
    public void isUnique(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        boolean un = model.unique(rd);
        helper.reply(null, un ? 1:0 );
    }

}
