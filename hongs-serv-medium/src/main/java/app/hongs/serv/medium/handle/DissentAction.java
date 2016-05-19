package app.hongs.serv.medium.handle;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Verify;
import app.hongs.db.DB;
import app.hongs.db.DBAction;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.serv.medium.ALinkModel;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("handle/medium/dissent")
public class DissentAction extends DBAction {

    @Action("create")
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void create(ActionHelper helper)
    throws HongsException {
        try {
            super.create(helper);
        } catch (HongsException ex ) {
        if (ex.getErrno() == 0x104e) {
            helper.fault("您已经举报过了, 请等候或查看处理结果");
        }
        }
    }

    @Override
    public void update(ActionHelper helper) {
        // Dont nead update
    }
    @Override
    public void delete(ActionHelper helper) {
        // Dont nead delete
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
    protected  Map   getReqMap(ActionHelper helper, Model ett, String opr, Map req)
    throws HongsException {
        req = super.getReqMap(helper, ett, opr, req);

        if (!"retrieve".equals(opr)) {
            req.put("user_id", helper.getSessibute(Cnst.UID_SES));
        }

        return req;
    }

    @Override
    protected String getRspMsg(ActionHelper helper, Model ett, String opr, int num)
    throws HongsException {
        String lid = helper.getParameter("link_id");
        String lnk = (( ALinkModel ) ett).getLink();
        Table  art = ett.db.getTable(lnk);

        if ("create".equals(opr)) {
            String sql = "UPDATE `"+art.tableName+"` SET `count_dissent` = `count_dissent` + 1 WHERE id = ?";
            art.db.execute (sql, lid);
            return "举报成功";
        }

        return super.getRspMsg(helper, ett, opr, num);
    }

}
