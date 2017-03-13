package app.hongs.serv.manage;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Verify;
import app.hongs.db.DB;
import app.hongs.db.util.FetchCase;
import app.hongs.db.Table;
import app.hongs.serv.auth.AuthKit;
import app.hongs.serv.auth.RoleSet;
import app.hongs.util.Synt;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpSession;

/**
 * 管理区登录操作
 * @author Hongs
 */
@Action("manage/sign")
public class SignAction {

    /**
     * 登录
     * @param ah
     * @throws HongsException
     */
    @Action("create")
    @Verify(conf="member",form="sign")
    public void signCreate(ActionHelper ah) throws HongsException {
        String appid    = Synt.declare(ah.getParameter("appid"), "_WEB_" );
        String place    = Synt.declare(ah.getParameter("place"), "public");
        String username = Synt.declare(ah.getParameter("username"), "");
        String password = Synt.declare(ah.getParameter("password"), "");
               password = AuthKit.getCrypt(password);

        DB        db = DB.getInstance("member");
        Table     tb = db.getTable("user");
        FetchCase fc;
        Map       ud;

        // 验证密码
        fc = new FetchCase( )
            .from   (tb.tableName)
            .select ("password, id, name, head, mtime, state")
            .filter ("username = ? " , username);
        ud = db.fetchLess(fc);
        if ( ud.isEmpty() ) {
            ah.reply(AuthKit.getWrong("username", "core.username.invalid"));
            return;
        }
        if (! password.equals( ud.get("password") )) {
            ah.reply(AuthKit.getWrong("passowrd", "core.password.invalid"));
            return;
        }

        String usrid = (String) ud.get( "id" );
        String uname = (String) ud.get("name");
        String uhead = (String) ud.get("head");
        int    state = Synt.asserts(ud.get("state"), 0 ) ;
        long   utime = Synt.asserts(ud.get("mtime"), 0L) * 1000 ;

        // 验证状态
        if (state != 1) {
            ah.reply(AuthKit.getWrong("state" , "core.sign.state.invalid"));
            return;
        }

        // 验证区域
        Set rs = RoleSet.getInstance(usrid);
        if (0 != place.length() && !rs.contains(place)) {
            ah.reply(AuthKit.getWrong("place" , "core.sign.place.invalid"));
            return;
        }

        ah.reply(AuthKit.userSign(ah, place, appid, usrid, uname, uhead, utime));
    }

    /**
     * 登出
     * 此动作可以清除会话数据
     * @param ah
     * @throws HongsException
     */
    @Action("delete")
    public void signDelete(ActionHelper ah) throws HongsException {
        HttpSession ss = ah.getRequest().getSession(false);
        if (null == ss) {
            ah.reply(AuthKit.getWrong(null, "core.sign.phase.invalid"));
            return;
        }

        AuthKit.signOut(ss);

        ah.reply ( "" );
    }

    /**
     * 更新
     * 此动作可维持会话不过期
     * @param ah
     * @throws HongsException
     */
    @Action("update")
    public void signUpdate(ActionHelper ah) throws HongsException {
        HttpSession ss = ah.getRequest().getSession(false);
        if (null == ss) {
            ah.reply(AuthKit.getWrong(null, "core.sign.phase.invalid"));
            return;
        }

        AuthKit.signUpd(ss);

        ah.reply ( "" );
    }

}
