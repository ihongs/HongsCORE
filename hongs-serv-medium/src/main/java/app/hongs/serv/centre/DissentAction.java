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
import app.hongs.serv.medium.LinkRecord;
import app.hongs.serv.medium.Statist;
import app.hongs.util.Synt;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("centre/medium/dissent")
@Assign(conf="medium", name="dissent")
public class DissentAction extends DBAction {

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
            helper.fault("您已经举报过了, 请等候处理或查看结果");
        }
        }
    }

    @Override
    protected Model  getEntity(ActionHelper helper)
    throws HongsException {
        LinkRecord model = (LinkRecord) DB.getInstance("medium").getModel("dissent");
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
            req.put(Cnst.WR_KEY, Synt.mapOf(
                "user_id" , uid,
                "state"   , 1
            ));
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
        String lnk = ext.getLink  ( );
        String lid = ext.getLinkId( );

        if ("create".equals(opr)) {
            Statist.create(lnk, lid, "comment_count", num);
            return "举报成功";
        } else
        if ("delete".equals(opr)) {
            int unm = 0 - num;
            Statist.update(lnk, lid, "comment_count", unm);
            return "取消举报";
        }

        return super.getRspMsg(helper, ett, opr, num);
    }

}
