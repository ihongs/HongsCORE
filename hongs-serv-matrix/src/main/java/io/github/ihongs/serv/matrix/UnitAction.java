package io.github.ihongs.serv.matrix;

import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.DB;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("centra/matrix/unit")
public class UnitAction {
    
    protected final Unit model;

    public UnitAction() throws HongsException {
        model = (Unit) DB.getInstance("matrix").getModel("unit");
    }

    @Action("list")
    public void getList(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        Map sd = model.getList(rd);
        helper.reply(sd);
    }

    @Action("info")
    public void getInfo(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        Map sd = model.getInfo(rd);
        helper.reply(sd);
    }

    @Action("save")
    @Verify(conf="matrix", form="unit")
    @CommitSuccess
    public void doSave(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        String id = model.set(rd);
        CoreLocale  lang = CoreLocale.getInstance().clone( );
                    lang.load("matrix");
        String ms = lang.translate("core.save.unit.success");
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
        String ms = ln.translate("core.delete.unit.success", null, Integer.toString(rn));
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
