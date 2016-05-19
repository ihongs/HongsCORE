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
        Model  ett = getEntity(helper);
        Map    req = helper.getRequestData();
        String aid = helper.getParameter("link_id");
        String lnk = ( (ALinkModel) ett ).getLink();

        String whr = "link = ? AND link_id = ?";
        Map    row = ett.table
                .filter(whr , lnk , aid )
                .select("id,state,score")
                .one( );

        if (row != null && !row.isEmpty()) {
            helper.setAttribute("score", row.get("score"));
            if (Synt.asserts( row.get("state"), 1 ) == 0 ) {
                // 恢复
                int s = Synt.declare(req.get("socre"), 1 );
                row.clear();
                row.put("state", 1);
                row.put("score", s);
                ett.table
                   .filter(whr, lnk, aid)
                   .update(row);

                helper.reply(getRspMsg(helper, ett, "create", 1));
            } else {
                req.put("id" , row.get("id"));

                delete (helper);
            }
        } else {
                create (helper);
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
        String     type  = Synt.declare(helper.getParameter("type"), "");
        if ( ! "".equals(type)) {
            model.setLink("article_"+type);
        } else {
            model.setLink("article");
        }
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
        int newScore = Synt.declare(helper.getParameter("score"), 1);
        int oldScore = Synt.declare(helper.getAttribute("score"), 0);
        String lid = helper.getParameter("link_id");
        String lnk = ( (ALinkModel) ett ).getLink();
        String tab = lnk;
        String cnt ;
        String opn ;

        if (lnk.endsWith("_collect")) {
            tab = tab.substring(0, tab.length() - 8);
            cnt = "count_collect";
            opn = "收藏";
        } else {
            cnt = "count_consent";
            opn = "赞";
        }

        Table  art = ett.db.getTable(tab);

        if (newScore > 0) {
            if ("create".equals(opr)) {
                String sql = "UPDATE `"+art.tableName+"` SET `"+cnt+"` = `"+cnt+"` + 1 WHERE id = ?";
                art.db.execute (sql, lid);
                return opn ;
            } else
            if ("delete".equals(opr)) {
                String sql = "UPDATE `"+art.tableName+"` SET `"+cnt+"` = `"+cnt+"` - 1 WHERE `"+cnt+"` > 1 AND id = ?";
                art.db.execute (sql, lid);
                return "取消" + opn ;
            }
        } else {
            if (oldScore > 0) {
                String sql = "UPDATE `"+art.tableName+"` SET `"+cnt+"` = `"+cnt+"` - 1 WHERE `"+cnt+"` > 1 AND id = ?";
                art.db.execute (sql, lid);
            }
            if ("create".equals(opr)) {
                return opn ;
            } else
            if ("delete".equals(opr)) {
                return "取消" + opn ;
            }
        }

        return super.getRspMsg(helper, ett, opr, num);
    }

}
