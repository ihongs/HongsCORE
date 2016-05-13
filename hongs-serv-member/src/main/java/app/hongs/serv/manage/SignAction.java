package app.hongs.serv.manage;

import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Verify;
import app.hongs.db.DB;
import app.hongs.db.FetchCase;
import app.hongs.db.Table;
import app.hongs.serv.auth.AuthKit;
import app.hongs.serv.auth.RoleSet;
import app.hongs.util.Synt;
import app.hongs.util.verify.Wrong;
import app.hongs.util.verify.Wrongs;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpSession;

/**
 * 管理区登录操作
 * @author Hongs
 */
@Action("manage/sign")
public class SignAction {

    @Action("create")
    @Verify(conf="member",form="sign")
    public void create(ActionHelper ah) throws HongsException {
        String appid    = Synt.declare(ah.getParameter("appid"), "_WEB_" );
        String place    = Synt.declare(ah.getParameter("place"), "public");
        String username = Synt.declare(ah.getParameter("username"), "");
        String password = Synt.declare(ah.getParameter("password"), "");
               password = AuthKit.getCrypt(password);

//        // 人机校验
//        String captcha  = Synt.declare(ah.getParameter("captcha"), "").toUpperCase();
//        String captcode = Synt.declare(ah.getSessibute("captcha_code"), "");
//        long   capttime = Synt.declare(ah.getSessibute("captcha_time"), 0L);
//        if (capttime < System.currentTimeMillis( ) - 600000 ) {
//            ah.reply(getWrong("captcha", "core.captcha.timeout"));
//            return;
//        }
//        if (captcode.equals("") || !captcode.equals(captcha)) {
//            ah.reply(getWrong("captcha", "core.captcha.invalid"));
//            return;
//        }
//        // 销毁验证码
//        ah.setSessibute("captcha_code", null);
//        ah.setSessibute("captcha_time", null);

        DB        db = DB.getInstance("member");
        Table     tb = db.getTable("user");
        FetchCase fc;
        Map       ud;

        // 验证密码
        fc = new FetchCase( )
            .from   (tb.tableName)
            .select ("password, id, name, head, mtime, state")
            .where  ("username = ?", username);
        ud = db.fetchLess(fc);
        if ( ud.isEmpty() ) {
            ah.reply(getWrong("username", "core.username.invalid"));
            return;
        }
        if (! password.equals( ud.get("password") )) {
            ah.reply(getWrong("passowrd", "core.password.invalid"));
            return;
        }

        String usrid = (String) ud.get( "id" );
        String uname = (String) ud.get("name");
        String uhead = (String) ud.get("head");
        int    state = Synt.asserts(ud.get("state"), 0 ) ;
        long   utime = Synt.asserts(ud.get("mtime"), 0L) * 1000 ;

        // 验证状态
        if (state != 1) {
            CoreLocale lang = CoreLocale.getInstance( "member" );
            ah.fault(lang.translate( "core.sign.state.invalid"));
            return;
        }

        // 验证区域
        Set rs = RoleSet.getInstance(usrid);
        if (0 != place.length() && !rs.contains(place)) {
            CoreLocale lang = CoreLocale.getInstance( "member" );
            ah.fault(lang.translate( "core.sign.place.invalid"));
            return;
        }

        ah.reply(AuthKit.userSign(ah, place, appid, usrid, uname, uhead, utime));
    }

    @Action("delete")
    public void delete(ActionHelper ah) throws HongsException {
        HttpSession sess = ah.getRequest().getSession();
        if (null == sess) {
            CoreLocale lang = CoreLocale.getInstance("member" );
            ah.fault ( lang.translate("core.sign.out.invalid"));
            return;
        }

        // 清除登录
        DB.getInstance("member")
          .getTable("user_sign")
          .delete("`sesid` = ?", sess.getId());

        // 清除会话
        ah.getRequest()
          .getSession()
          .invalidate();

        ah.reply("");
    }

    protected final Map getWrong(String k, String w) throws HongsException {
        Map m = new HashMap();
        Map e = new HashMap();
        CoreLocale lang = CoreLocale.getInstance ("member") ;
        m.put(k, new Wrong(w).setLocalizedSection("member"));
        e.put("errs", new Wrongs(m).getErrors());
        e.put("msg", lang.translate("core.sign.in.invalid"));
        e.put("ok", false);
        return e;
    }

}
