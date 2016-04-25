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
        } catch (HongsException ex) {
        if (ex.getCode() == 0x104e) {
            helper.fault("您已经举报过了, 无法重复举报");
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
        if ("create".equals(opr)) {
            String lnk = ((ALinkModel) mod).getLink();
            Table  art =  mod.db.getTable(lnk);
            String sql = "UPDATE `"+art.tableName+"` SET `count_dissent` = `count_dissent` + 1";
            mod.db.execute(sql);
            return "举报成功";
        } else {
            return super.getRspMsg(helper, mod, opr, num);
        }
    }

}
