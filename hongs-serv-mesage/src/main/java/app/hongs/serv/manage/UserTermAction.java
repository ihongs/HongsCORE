package app.hongs.serv.manage;

import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.anno.Action;
import app.hongs.db.DBAction;

/**
 *
 * @author Hongs
 */
@Action("manage/mesage/user/term")
public class UserTermAction extends DBAction {

    @Override
    public void initiate(ActionHelper helper, ActionRunner runner) {
        this.mod = "mesage";
        this.ent = "user_term";
    }

}
