package app.hongs.serv.centre;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Assign;
import app.hongs.db.DB;
import app.hongs.db.DBAction;
import app.hongs.db.Model;
import app.hongs.serv.medium.LinkRecord;
import app.hongs.serv.medium.Statist;
import app.hongs.serv.medium.Suggest;
import app.hongs.util.Synt;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("centre/medium/comment")
@Assign(conf="medium", name="comment")
public class CommentAction extends DBAction {

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
    @Override
    public void update(ActionHelper helper) {
        // 禁止更新
    }

    @Override
    protected Model  getEntity(ActionHelper helper)
    throws HongsException {
        LinkRecord model = (LinkRecord) DB.getInstance("medium").getModel("comment");
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
                "user_id" , uid,
                "state"   , 1
            )));
        }
        return req;
    }

    @Override
    protected String getRspMsg(ActionHelper helper, Model ett, String opr, int num)
    throws HongsException {
        if (num <= 0) {
            return "操作失败";
        }

        LinkRecord ext = (LinkRecord) ett;
        String lnk = ext.getLink  ( );
        String lid = ext.getLinkId( );
        String uid = Synt.asString(helper.getSessibute(Cnst.UID_SES));

        if ("create".equals(opr)) {
            Statist.create(lnk, lid, "comment_count", num);
            Suggest.create(lnk, uid, num);
            return "评论成功";
        } else
        if ("delete".equals(opr)) {
            int unm = 0 - num;
            Statist.update(lnk, lid, "comment_count", unm);
            Suggest.update(lnk, uid, unm);
            return "删除评论";
        }

        return super.getRspMsg(helper, ett, opr, num);
    }

}
