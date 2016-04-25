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
import app.hongs.util.Synt;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("handle/medium/consent")
public class ConsentAction extends DBAction {

    /**
     * 没则添加有则取消
     * @param helper
     * @throws HongsException
     */
    @Action("update")
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void update(ActionHelper helper)
    throws HongsException {
        Model  mod = getEntity(helper);
        Map    req = helper.getRequestData();
        String aid = helper.getParameter("link_id");
        String lnk = ( (ALinkModel) mod ).getLink();
        String whr = "link = ? AND link_id = ?";
        Map    row = mod.table.fetchCase()
                .select("id,state,score" )
                .where ( whr , lnk , aid )
                .one( );
        if (row != null && !row.isEmpty()) {
            helper.setAttribute("score", row.get("score"));
            if (Synt.asserts( row.get("state"), 1 ) == 0 ) {
                // 恢复
                row.clear();
                row.put("score",Synt.declare(req.get("socre"),1));
                row.put("state",  1 );
                mod.table.fetchCase()
                        .where (whr, aid)
                        .update(row );
                helper.reply(getRspMsg(helper, mod, "create", 1));
            } else {
                helper.getRequestData ().put("id", row.get("id"));
                delete(helper);
            }
        } else {
            create(helper);
        }
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
        ALinkModel model = (ALinkModel) DB.getInstance("medium").getModel("consent");
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
        int newScore = Synt.declare(helper.getParameter("score"), 1);
        int oldScore = Synt.declare(helper.getAttribute("score"), 0);
        String  lnk  = ((ALinkModel) mod).getLink();
        Table   art  =  mod.db.getTable(lnk);
        if (newScore > 0) {
            if ("create".equals(opr)) {
                String sql = "UPDATE `"+art.tableName+"` SET `count_consent` = `count_consent` + 1";
                mod.db.execute(sql);
                return "赞";
            } else
            if ("delete".equals(opr)) {
                String sql = "UPDATE `"+art.tableName+"` SET `count_consent` = `count_consent` - 1 WHERE `count_consent` > 1";
                mod.db.execute(sql);
                return "取消赞";
            }
        } else {
            if (oldScore > 0) {
                String sql = "UPDATE `"+art.tableName+"` SET `count_consent` = `count_consent` - 1 WHERE `count_consent` > 1";
                mod.db.execute(sql);
            }
            if ("create".equals(opr)) {
                return "踩";
            } else
            if ("delete".equals(opr)) {
                return "取消踩";
            }
        }
        return super.getRspMsg(helper, mod, opr, num);
    }

}
