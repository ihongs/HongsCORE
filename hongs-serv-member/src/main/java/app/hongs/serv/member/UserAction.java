package app.hongs.serv.member;

import app.hongs.Cnst;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.NaviMap;
import app.hongs.action.UploadHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.db.DB;
import app.hongs.db.FetchCase;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import app.hongs.util.image.Thumb;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户动作接口
 * @author Hongs
 */
@Action("manage/member/user")
public class UserAction {

    private final User model;

    public UserAction()
    throws HongsException {
        model = (User) DB.getInstance("member").getModel("user");
    }

    @Action("list")
    public void getList(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        FetchCase fc  =  new FetchCase();
        fc.setOption("INCLUDE_REMOVED", Synt.declare(rd.get("include-removed"), false));
        rd = model.getList(rd, fc);

        // Remove the password field, don't show password in page
        List<Map> list = (List) rd.get("list");
        for (Map  info :  list) {
            info.remove("password");
        }

        helper.reply(rd);
    }

    @Action("info")
    public void getInfo(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        String id = helper.getParameter( "id");
        String wr = helper.getParameter("-with-roles");
        String ud = (String)helper.getSessibute("uid");

        if ( id != null && id.length() != 0 ) {
            rd = model.getInfo(rd);
        } else {
            rd =  new  HashMap(  );
        }

        // Remove the password field, don't show password in page
        Map info  = (Map) rd.get("info");
        if (info != null) {
            info.remove("password");
        }

        // With all roles
        if (Synt.declare(wr, false)) {
            List rs = ! "1".equals(ud) ?
                    NaviMap.getInstance("manage").getRoleTranslated(0, 0):
                    NaviMap.getInstance("manage").getRoleTranslated(0, 0, null);
            Dict.put(rd, rs, "enum", "roles..role");
        }

        helper.reply(rd);
    }

    @Action("save")
    public void doSave(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();

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
                rd.put("head", fu);
            } else {
                rd.put("head", "");
            }
        }

        // Ignore empty password in update
        if ("".equals(rd.get("password"))) {
            rd.remove("password");
        }

        String id = model.set(rd);

        Map sd = new HashMap();
        sd.put( "id" , id);
        sd.put("name", rd.get("name"));
        sd.put("head", rd.get("head"));
        sd.put("username", rd.get("username"));

        CoreLocale  ln = CoreLocale.getInstance().clone( );
                    ln.load("member" );
        String ms = ln.translate("core.save.user.success");
        helper.reply(ms, sd);
    }

    @Action("delete")
    @CommitSuccess
    public void doDelete(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();

        // 不能删除自己和超级管理员
        Set rs = Synt.declare(rd.get(model.table.primaryKey), Set.class);
        if (rs != null) {
            if (rs.contains(helper.getSessibute(Cnst.UID_SES))) {
                helper.fault("不能删除当前登录用户");
                return;
            }
            if (rs.contains(Cnst.ADM_UID)) {
                helper.fault("不能删除超级管理账号");
                return;
            }
        }

        int rn = model.delete(rd);
        CoreLocale  ln = CoreLocale.getInstance().clone( );
                    ln.load("member" );
        String ms = ln.translate("core.delete.user.success", Integer.toString(rn));
        helper.reply(ms, rn);
    }

    @Action("unique")
    public void isUnique(ActionHelper helper)
    throws HongsException {
        Map rd = helper.getRequestData();
        FetchCase fc  =  new FetchCase();
        fc.setOption("INCLUDE_REMOVED", Synt.declare(rd.get("include-removed"), false));
        boolean rv = model.unique(rd,fc);
        helper.reply("", rv);
    }

}
