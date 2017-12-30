package app.hongs.serv.centre;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
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
import app.hongs.util.verify.Capts;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("centre/medium/comment")
@Assign(conf="medium", name="comment")
public class CommentAction extends DBAction {

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

    @Action("create")
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void create(ActionHelper helper)
    throws HongsException {
        // 评论必须要验证码
        new app.hongs.util.verify.Verify( )
           .addRule( "capt" , new Capts ())
           .verify(helper.getRequestData());

        super.create(helper);
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
        Mlink model = (Mlink) DB.getInstance("medium").getModel("comment");
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
        
        Mstat sta = (Mstat) ett.db.getModel("statist");

        if ("create".equals(opr)) {
            sta.add("comment_count", num);
            return "评论成功";
        }
        if ("delete".equals(opr)) {
            int unm = 0 - num;
            sta.put("comment_count", unm);
            return "删除评论";
        }

        return super.getRspMsg(helper, ett, opr, num);
    }

}
