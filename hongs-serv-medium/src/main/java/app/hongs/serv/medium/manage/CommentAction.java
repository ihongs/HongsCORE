package app.hongs.serv.medium.manage;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.db.DB;
import app.hongs.db.DBAction;
import app.hongs.db.Model;
import app.hongs.serv.medium.ALinkModel;

/**
 * 文章评论
 * @author Hongs
 */
@Action("manage/medium/article/comment")
public class CommentAction extends DBAction {
    
    @Override
    public Model getEntity(ActionHelper helper) throws HongsException {
        ALinkModel model = (ALinkModel) DB.getInstance("medium").getModel("comment");
        model.setLink("article");
        return model;
    }
    
}
