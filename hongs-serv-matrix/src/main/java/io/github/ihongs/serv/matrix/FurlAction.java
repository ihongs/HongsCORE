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
@Action("centra/matrix/furl")
public class FurlAction {
    
    protected final Furl model;

    public FurlAction() throws HongsException {
        model = (Furl) DB.getInstance("matrix").getModel("furl");
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
    @Verify(conf="matrix", form="furl")
    @CommitSuccess
    public void doSave(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        String id = model.set(rd);
        CoreLocale  ln = CoreLocale.getMultiple("matrix", "default");
        String ms = ln.translate("core.save.furl.success");
        helper.reply(ms, id);
    }

    @Action("delete")
    @CommitSuccess
    public void doDelete(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        int rn = model.delete(rd);
        CoreLocale  ln = CoreLocale.getMultiple("matrix", "default");
        String ms = ln.translate("core.delete.furl.success", null, Integer.toString(rn));
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
