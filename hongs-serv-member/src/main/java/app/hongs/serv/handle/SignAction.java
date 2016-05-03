package app.hongs.serv.handle;

import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.UploadHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Preset;
import app.hongs.action.anno.Verify;
import app.hongs.db.DB;
import app.hongs.serv.auth.AuthKit;
import app.hongs.serv.member.User;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import app.hongs.util.image.Thumb;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录动作
 * @author Hongs
 */
@Action("handle/sign")
public class SignAction extends app.hongs.serv.manage.SignAction {

    /**
     * 注册
     * @param helper
     * @throws app.hongs.HongsException
     */
    @Action("user/create")
    @Preset(conf="member", envm="mine")
    @Verify(conf="member", form="mine")
    public void memberCreate(ActionHelper helper) throws HongsException {
        User   mod = (User) DB.getInstance("member").getModel("user");
        Map     rd = helper.getRequestData();

        if (rd.containsKey("head")) {
            // 上传头像
            UploadHelper uh = new UploadHelper()
                .setUploadHref("upload/member/head")
                .setUploadPath("upload/member/head")
                .setAllowTypes("image/jpeg", "image/png", "image/gif")
                .setAllowExtns("jpeg", "jpg", "png", "gif");
            File fo  = uh.upload(rd.get("head").toString());

            // 缩略头像
            if ( fo != null) {
                String fn = uh.getResultPath();
                String fu = uh.getResultHref();
                try {
                    fu = Thumb.toThumbs( fn, fu )[1][0];
                } catch (IOException ex) {
                    throw new HongsException.Common(ex);
                }
                rd.put("head" , fu);
            } else {
                rd.put("head" , "");
            }
        }

        Map     sd = mod.create(rd);

        // 提取登录信息
        String usrid = Synt.asserts(sd.get(  "id" ), "");
        String uname = Synt.asserts(sd.get( "name"), "");
        String uhead = Synt.asserts(sd.get( "head"), "");
        long   utime = Synt.asserts(sd.get("mtime"), 0L) * 1000;
        String appid = Synt.declare(helper.getParameter("appid"), "_WEB_");

        // 赋予公共权限
        sd = new HashMap();
        sd.put("user_id",  usrid  );
        sd.put("role"   , "public");
        mod.db.getTable("user_role").insert(sd);

        // 加入公共部门
        sd = new HashMap();
        sd.put("user_id",  usrid  );
        sd.put("dept_id", "PUBLIC");
        mod.db.getTable("user_dept").insert(sd);

        helper.reply(AuthKit.userSign(helper, appid, usrid, uname, uhead, utime));
    }

    /**
     * 注销
     * @param helper
     */
    @Action("user/delete")
    public void memberDelete(ActionHelper helper) throws HongsException {
        User   mod = (User) DB.getInstance("member").getModel("user");
        Map     rd = helper.getRequestData();
        int     sd = mod.delete(rd);

        CoreLocale   ln  ;
        ln = CoreLocale.getInstance().clone();
        ln.load("member");
        String msg = ln.translate("core.delete.user.success");

        helper.reply(msg, sd);
    }

}
