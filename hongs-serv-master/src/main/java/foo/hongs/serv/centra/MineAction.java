package foo.hongs.serv.centra;

import foo.hongs.Cnst;
import foo.hongs.CoreLocale;
import foo.hongs.HongsException;
import foo.hongs.action.ActionHelper;
import foo.hongs.action.anno.Action;
import foo.hongs.action.anno.Preset;
import foo.hongs.action.anno.Verify;
import foo.hongs.db.DB;
import foo.hongs.serv.auth.AuthKit;
import foo.hongs.serv.master.UserAction;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理区我的信息
 * @author Hongs
 */
@Action("centra/mine")
public class MineAction {

    @Action("info")
    @Preset(conf="master", form="mine")
    public void mineInfo(ActionHelper ah)
    throws HongsException {
        Map rd = ah.getRequestData();
        String id = (String) ah.getSessibute(Cnst.UID_SES);
        rd.put( "id", id );

        UserAction ua = new UserAction();
        ua.getInfo(ah);
    }

    @Action("update")
    @Verify(conf="master", form="mine", type=1, trim=1)
    public void mineUpdate(ActionHelper ah)
    throws HongsException {
        Map rd = ah.getRequestData();
        String id = (String) ah.getSessibute(Cnst.UID_SES);

        // 禁止危险修改
        rd.put( "id", id );
        rd.remove("roles");
        rd.remove("rtime");
        rd.remove("mtime");
        rd.remove("ctime");
        rd.remove("state");

        // 验证原始密码
        String pw = (String) rd.get("password");
        String po = (String) rd.get("passolde");
        if (pw != null && !"".equals(pw)) {
            Map xd = new HashMap();
            Map ed = new HashMap();
            xd.put("ok",false);
            xd.put("errs", ed);
            xd.put("msg", CoreLocale.getInstance().translate("fore.form.invalid"));
            if (po != null && !"".equals(po)) {
                Map row = DB.getInstance("master").getTable ("user").fetchCase( )
                    .filter("id = ?", id)
                    .select( "password" )
                    .getOne( );
                po = AuthKit.getCrypt(po);
                if (! po.equals(row.get("password")) ) {
                    ed.put("passolde", "旧密码不正确");
                    ah.reply(xd);
                    return;
                }
            } else {
                    ed.put("passolde", "请填写旧密码");
                    ah.reply(xd);
                    return;
            }
        }

        UserAction ua = new UserAction();
        ua.doSave( ah );
    }

}
