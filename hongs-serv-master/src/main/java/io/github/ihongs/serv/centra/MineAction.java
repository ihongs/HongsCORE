package io.github.ihongs.serv.centra;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Preset;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.DB;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.serv.master.UserAction;
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

    @Action("save")
    @Verify(conf="master", form="mine", type=1, trim=1)
    public void mineSave(ActionHelper ah)
    throws HongsException {
        Map rd = ah.getRequestData();
        String id = (String) ah.getSessibute(Cnst.UID_SES);

        // 禁止危险修改. 这是以防万一, 其实校验已经做过限制了
        rd.put( "id", id );
        rd.remove("depts");
        rd.remove("roles");
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
