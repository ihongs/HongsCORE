package app.hongs.serv.centre;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Assign;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Verify;
import app.hongs.db.DB;
import app.hongs.db.DBAction;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.serv.medium.LinkRecord;
import app.hongs.serv.medium.Statist;
import app.hongs.util.Synt;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("centre/medium/endorse")
@Assign(conf="medium", name="endorse")
public class EndorseAction extends DBAction {

    String link  ;
    String linkId;

    @Override
    public void acting(ActionHelper helper, ActionRunner runner) throws HongsException {
        link   = helper.getParameter("link"   );
        linkId = helper.getParameter("link_id");
        if (link   == null) {
            throw new HongsException(0x1100, "link required");
        }
        if (linkId == null) {
            throw new HongsException(0x1100, "link_id required");
        }
    }

    @Override
    public void isExists(ActionHelper helper) {

    }
    @Override
    public void isUnique(ActionHelper helper) {

    }

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
        LinkRecord ett = (LinkRecord) getEntity(helper);
        Map    req = getReqMap(helper, ett, "update", helper.getRequestData());
        String uid = (String) req.get("user_id");
        String aid = ett.getLinkId();
        String lnk = ett.getLink(  );

        String whr = "link = ? AND link_id = ? AND user_id = ?";
        Map    row = ett.table
                .filter(whr, lnk, aid, uid)
                .select("id, state, score")
                .one( );

        if (row != null && ! row.isEmpty()) {
            Object id = row.get("id");
            int sv = Synt.declare(req.get("score"), 1);
            int sc = Synt.declare(row.get("score"), 1);
            int st = Synt.declare(row.get("state"), 1);
            helper.setAttribute("ln_id",id);
            helper.setAttribute("score",sc);
            if (sv != 0) {
                // 恢复或更新评分记录
                row.clear();
                row.put("state", 1 );
                row.put("score", sv);
                int an = ett.table
                   .filter("id = ?", id)
                   .limit (  1  )
                   .update( row );

                helper.reply(getRspMsg(helper, ett, "create", an));
            } else
            if (st == 0) {
                helper.fault("已取消过了");
            } else {
                delete(helper);
            }
        } else {
                create(helper);
        }
    }

    @Override
    protected Model  getEntity(ActionHelper helper)
    throws HongsException {
        LinkRecord model = (LinkRecord) DB.getInstance("medium").getModel("endorse");
        model.setLink  (link  );
        model.setLinkId(linkId);
        return model;
    }

    @Override
    protected  Map   getReqMap(ActionHelper helper, Model ett, String opr, Map req)
    throws HongsException {
        Object uid = helper.getSessibute(Cnst.UID_SES);
        req = super.getReqMap( helper, ett, opr, req );
        req.put("user_id" , uid);
        if ("delete".equals(opr)) {
            req.put(Cnst.AR_KEY, Synt.mapOf("", Synt.mapOf(
                "user_id" , uid
            )));
        }
        return req;
    }

    @Override
    protected String getRspMsg(ActionHelper helper, Model ett, String opr, int num)
    throws HongsException {
        if (num == 0) {
            return "操作失败";
        }

        LinkRecord ext = (LinkRecord) ett;
        String lnk = ext.getLink(  );
        String lid = ext.getLinkId();
        int oldSco = Synt.declare (helper.getAttribute("score"), 0);
        int newSco = Synt.declare (helper.getParameter("score"), 1);

        if ("create".equals(opr)) {
            Statist.create(lnk, lid, "endorse_count", num);
            Statist.update(lnk, lid, "endorse_score", newSco - oldSco);
            return "评分成功";
        } else
        if ("delete".equals(opr)) {
            int unm = 0 - num;
            Statist.update(lnk, lid, "endorse_count", unm);
            Statist.update(lnk, lid, "endorse_score", newSco - oldSco);
            return "取消评分成功";
        }

        return super.getRspMsg(helper, ett, opr, num);
    }

}
