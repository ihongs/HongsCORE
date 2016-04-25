package app.hongs.serv.manage;

import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Verify;
import app.hongs.db.DB;
import app.hongs.serv.auth.AuthKit;
import app.hongs.serv.member.UserAction;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理区我的信息
 * @author Hongs
 */
@Action("manage/mine")
public class MineAction {

    @Action("retrieve")
    public void mineRetrieve(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        String id = (String) helper.getSessibute("uid");
        rd.put( "id", id );

        UserAction ua = new UserAction();
        ua.getInfo(helper);
    }

    @Action("update")
    @Verify(conf="member",form="mine",save=1,tidy=true)
    public void mineUpdate(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        String id = (String) helper.getSessibute("uid");

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
            xd.put("errs", ed);
            xd.put("ok", false);
            xd.put("msg", CoreLocale.getInstance().translate("fore.form.invalid"));
            if (po != null && !"".equals(po)) {
                Map row = DB.getInstance("member").getTable("user").fetchCase()
                    .where ("id = ?", id)
                    .select( "password" )
                    .one();
                po = AuthKit.getCrypt(po);
                if (! po.equals(row.get("password")) ) {
                    ed.put("passolde", "旧密码不正确");
                    helper.reply(xd);
                    return;
                }
            } else {
                    ed.put("passolde", "请填写旧密码");
                    helper.reply(xd);
                    return;
            }
        }

        UserAction ua = new UserAction();
        ua.doSave(helper);
    }

}
