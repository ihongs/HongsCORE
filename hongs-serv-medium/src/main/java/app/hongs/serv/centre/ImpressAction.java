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
@Action("centre/medium/endorse")
@Assign(conf="medium", name="impress")
public class ImpressAction extends DBAction {
    
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
    public void delete(ActionHelper helper) {
        // 禁止删除
    }

    @Action("create")
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void create(ActionHelper helper)
    throws HongsException {
        try {
            super.create(helper);
            helper.reply("谢谢到访");
        } catch (HongsException ex ) {
            helper.reply("浏览过了");
        }
    }

    @Override
    protected Model  getEntity(ActionHelper helper)
    throws HongsException {
        LinkRecord model = (LinkRecord) DB.getInstance("medium").getModel("impress");
        model.setLink  (link  );
        model.setLinkId(linkId);
        return model;
    }

    @Override
    protected  Map   getReqMap(ActionHelper helper, Model ett, String opr, Map req)
    throws HongsException {
        Object sid = helper.getRequest().getSession().getId();
        Object uid = helper.getSessibute(Cnst.UID_SES);
        req = super.getReqMap( helper, ett, opr, req );
        if (uid == null) {
            req.put("user_id", null);
            req.put("sess_id", sid );
        } else {
            req.put("user_id", uid );
            req.put("sess_id", null);
        }
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
        if (num <= 0) {
            return "操作失败";
        }

        LinkRecord ext = (LinkRecord) ett;
        String lnk = ext.getLink  ( );
        String lid = ext.getLinkId( );

        if ("create".equals(opr)) {
            Statist.create(lnk, lid, "impress_count", num);
            return "记录成功";
        }

        return super.getRspMsg(helper, ett, opr, num);
    }
    
}
