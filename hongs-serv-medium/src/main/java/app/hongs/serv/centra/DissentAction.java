package app.hongs.serv.centra;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Assign;
import app.hongs.db.DB;
import app.hongs.db.DBAction;
import app.hongs.db.Model;
import app.hongs.serv.medium.Mlink;

/**
 * 举报
 * @author Hongs
 */
@Action("centra/medium/dissent")
@Assign(conf="medium", name="dissent")
public class DissentAction extends DBAction {

    @Override
    public void create(ActionHelper helper) {
        // 禁止添加
    }
    @Override
    public void delete(ActionHelper helper) {
        // 禁止删除
    }
    @Override
    public void isExists(ActionHelper helper) {

    }
    @Override
    public void isUnique(ActionHelper helper) {

    }

    @Override
    public Model getEntity(ActionHelper helper) throws HongsException {
        Mlink mod =  (Mlink) DB.getInstance("medium").getModel("dissent");
        mod.setLink  (helper.getParameter("link"   ));
        mod.setLinkId(helper.getParameter("link_id"));
        return mod;
    }

}
