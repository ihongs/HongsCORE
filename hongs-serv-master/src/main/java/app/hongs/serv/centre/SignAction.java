package app.hongs.serv.centre;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Preset;
import app.hongs.action.anno.Verify;
import app.hongs.db.DB;
import app.hongs.serv.auth.AuthKit;
import app.hongs.serv.member.User;
import app.hongs.util.Synt;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录动作
 * @author Hongs
 */
@Action("centra/sign")
public class SignAction extends app.hongs.serv.centra.SignAction {

    /**
     * 注册
     * @param ah
     * @throws app.hongs.HongsException
     */
    @Action("user/create")
    @Preset(conf="member", form="mine")
    @Verify(conf="member", form="mine")
    public void userCreate(ActionHelper ah) throws HongsException {
        Map  rd = ah.getRequestData();
        User uo = (User) DB.getInstance( "member" ).getModel( "user" );
        Map  sd = uo.create  (  rd  );

        // 提取登录信息
        String usrid = Synt.declare(sd.get(  "id" ), "");
        String uname = Synt.declare(sd.get( "name"), "");
        String uhead = Synt.declare(sd.get( "head"), "");
        long   utime = Synt.declare(sd.get("mtime"), 0L) * 1000;
        String appid = Synt.declare(ah.getParameter("appid"), "_WEB_");

        // 赋予公共权限
        sd = new HashMap();
        sd.put("user_id",  usrid  );
        sd.put("role"   , "public");
        uo.db.getTable("user_role").insert(sd);

        // 加入公共部门
        sd = new HashMap();
        sd.put("user_id",  usrid  );
        sd.put("dept_id", "PUBLIC");
        uo.db.getTable("user_dept").insert(sd);

        ah.reply(AuthKit.userSign(ah, null, appid, usrid, uname, uhead, utime));
    }

    /**
     * 注销
     * @param ah
     */
    @Action("user/delete")
    public void userDelete(ActionHelper ah) throws HongsException {
        String uuid = (String) ah.getSessibute(Cnst.UID_SES);
        if (null == uuid) {
            ah.reply(AuthKit.getWrong(null, "core.sign.off.invalid"));
            return;
        }

        User user = (User) DB.getInstance("member").getModel("user") ;
        user.del(uuid);

        signDelete(ah);
    }

}
