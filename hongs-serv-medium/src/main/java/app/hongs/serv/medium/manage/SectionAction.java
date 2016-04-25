package app.hongs.serv.medium.manage;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.db.DB;
import app.hongs.db.DBAction;
import app.hongs.db.Model;
import app.hongs.serv.medium.ABaseModel;

/**
 * 文章分类
 * @author Hongs
 */
@Action("manage/medium/section")
public class SectionAction extends DBAction {

    @Override
    public Model getEntity(ActionHelper helper) throws HongsException {
        ABaseModel model = (ABaseModel) DB.getInstance("medium").getModel("section");
        model.setType("default");
        return model;
    }

}
