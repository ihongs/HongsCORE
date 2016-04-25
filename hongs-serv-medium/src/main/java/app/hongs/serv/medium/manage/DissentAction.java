package app.hongs.serv.medium.manage;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.db.DB;
import app.hongs.db.DBAction;
import app.hongs.db.Model;
import app.hongs.serv.medium.ALinkModel;

/**
 * 文章举报
 * @author Hongs
 */
@Action("manage/medium/article/dissent")
public class DissentAction extends DBAction {
    
    @Override
    public Model getEntity(ActionHelper helper) throws HongsException {
        ALinkModel model = (ALinkModel) DB.getInstance("medium").getModel("dissent");
        model.setLink("article");
        return model;
    }
    
}
