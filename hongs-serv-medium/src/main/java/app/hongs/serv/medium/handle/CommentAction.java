package app.hongs.serv.medium.handle;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.db.DB;
import app.hongs.db.DBAction;
import app.hongs.db.Model;
import app.hongs.serv.medium.ALinkModel;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("handle/medium/comment")
public class CommentAction extends DBAction {

    @Override
    public void update(ActionHelper helper) {
        // Dont nead update
    }
    @Override
    public void isExists(ActionHelper helper) {

    }
    @Override
    public void isUnique(ActionHelper helper) {

    }

    @Override
    protected Model  getEntity(ActionHelper helper)
    throws HongsException {
        ALinkModel model = (ALinkModel) DB.getInstance("medium").getModel("comment");
        model.setLink("article");
        return model;
    }

    @Override
    protected  Map   getReqMap(ActionHelper helper, Model mod, String opr, Map req)
    throws HongsException {
        req = super.getReqMap(helper, mod, opr, req);

        if (!"retrieve".equals(opr)) {
            req.put("user_id", helper.getSessibute(Cnst.UID_SES));
        }

        return req;
    }

    @Override
    protected String getRspMsg(ActionHelper helper, Model mod, String opr, int num)
    throws HongsException {
        if ("delete".equals(opr)) {
            return "删除评论成功";
        }
        if ("create".equals(opr)) {
            return "添加评论成功";
        } else {
            return super.getRspMsg(helper, mod, opr, num);
        }
    }

}
