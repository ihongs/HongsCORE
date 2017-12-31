package app.hongs.serv.centre;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.FormSet;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Assign;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Verify;
import app.hongs.db.DB;
import app.hongs.db.DBAction;
import app.hongs.db.Model;
import app.hongs.serv.medium.Mlink;
import app.hongs.serv.medium.Mstat;
import app.hongs.util.Synt;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("centre/medium/endorse")
@Assign(conf="medium", name="endorse")
public class EndorseAction extends DBAction {

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
        Mlink ett = (Mlink) getEntity(helper);
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
            int sv = Synt.declare(req.get("score"), 1);
            int sc = Synt.declare(row.get("score"), 1);
            int st = Synt.declare(row.get("state"), 1);
            Object id = row.get(   "id"   );
            helper.setAttribute("ln_id",id);
            helper.setAttribute("score",sc);
            if (sv != 0) {
                // 恢复或更新评分记录
                int an  = 0 ;
                if (sv != sc) {
                    row.clear();
                    row.put("state", 1 );
                    row.put("score", sv);
                    an = ett.table
                       .filter("id = ?",id)
                       .limit ( 1 )
                       .update(row);
                }

                helper.reply(getRspMsg(helper, ett, "update", an));
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
        String link, linkId;
        link   = helper.getParameter("link"   );
        linkId = helper.getParameter("link_id");
        if (link == null || linkId == null) {
            throw new HongsException(0x1100, "link and link_id required");
        }
        Mlink model = (Mlink) DB.getInstance("medium").getModel("endorse");
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
            return "评分完成";
        }

        Mlink  lin = (Mlink) ett;
        Mstat  sta = (Mstat) ett.db.getModel("statist");
        Map    ena = FormSet.getInstance( "medium" )
                            .getEnum("statist_link");
        String lnk = lin.getLink(  );
        String lid = lin.getLinkId();
               sta.setLink   ( lnk );
               sta.setLinkId ( lid );
        if (ena.containsKey(lnk)) {
            int  c = Synt.declare(helper.getParameter("score"), 1);
            int  o = Synt.declare(helper.getAttribute("score"), 0);
        if ("update".equals(opr)) {
            sta.put("endorse_score", c-o);
            return "更新评分";
        }
        if ("create".equals(opr)) {
            sta.add("endorse_count", num);
            sta.put("endorse_score", c-o);
            return "评分成功";
        }
        if ("delete".equals(opr)) {
            int unm = 0 - num;
            sta.put("endorse_count", unm);
            sta.put("endorse_score", c-o);
            return "取消评分";
        }
        }

        return super.getRspMsg(helper, ett, opr, num);
    }

}
