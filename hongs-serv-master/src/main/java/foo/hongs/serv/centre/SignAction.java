package foo.hongs.serv.centre;

import foo.hongs.Cnst;
import foo.hongs.HongsException;
import foo.hongs.action.ActionHelper;
import foo.hongs.action.anno.Action;
import foo.hongs.action.anno.Preset;
import foo.hongs.action.anno.Verify;
import foo.hongs.db.DB;
import foo.hongs.db.util.FetchCase;
import foo.hongs.serv.auth.AuthKit;
import foo.hongs.serv.master.User;
import foo.hongs.util.Synt;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录动作
 * @author Hongs
 */
@Action("centre/sign")
public class SignAction extends foo.hongs.serv.centra.SignAction {

    /**
     * 注册
     * @param ah
     * @throws foo.hongs.HongsException
     */
    @Action("user/create")
    @Preset(conf="master", form="mine")
    @Verify(conf="master", form="mine")
    public void userCreate(ActionHelper ah) throws HongsException {
        Map  rd = ah.getRequestData();
        User uo = (User) DB.getInstance( "master" ).getModel( "user" );
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
        sd.put("role"   , "centre");
        uo.db.getTable("user_role").insert(sd);

        // 加入公共部门
        sd = new HashMap();
        sd.put("user_id",  usrid  );
        sd.put("dept_id", "CENTRE");
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

        User user = (User) DB.getInstance("master").getModel("user") ;
        user.del(uuid);

        signDelete(ah);
    }

    /**
     * 检查
     * @param ah
     * @throws HongsException
     */
    @Action("user/unique")
    public void userUnique(ActionHelper ah) throws HongsException {
        User user = (User) DB.getInstance("master").getModel("user") ;
        Map  data = ah . getRequestData( );
        FetchCase caze = user.fetchCase( );

        // 密码等不可检测
        String  n = (String) data.get("n");
        if ("password".equals( n ) || "passcode".equals( n )) {
            throw new HongsException(0x1100, "Colume " + n + " is not allowed");
        }

        boolean v = user.unique(data,caze);
        ah.reply(null, v ? 1 : 0 );
    }

}
