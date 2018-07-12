package io.github.ihongs.serv.centra;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.db.Table;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.serv.auth.RoleSet;
import io.github.ihongs.util.Synt;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpSession;

/**
 * 管理区登录操作
 * @author Hongs
 */
@Action("centra/sign")
public class SignAction {

    /**
     * 登录
     * @param ah
     * @throws HongsException
     */
    @Action("create")
    @Verify(conf="master",form="sign")
    public void signCreate(ActionHelper ah) throws HongsException {
        String appid    = Synt.declare(ah.getParameter("appid"), "_WEB_" );
        String place    = Synt.declare(ah.getParameter("place"), "public");
        String username = Synt.declare(ah.getParameter("username"), "");
        String password = Synt.declare(ah.getParameter("password"), "");
        String passcode ;

        DB        db = DB.getInstance("master");
        Table     tb = db.getTable("user");
        FetchCase fc;
        Map       ud;

        // 验证密码
        fc = new FetchCase( )
            .from   ( tb.tableName )
            .select ( "password, passcode, id, name, head, mtime, state" )
            .filter ( "username = ?" , username );
        ud = db.fetchLess(fc);
        if ( ud.isEmpty() ) {
            ah.reply(AuthKit.getWrong("username", "core.username.invalid"));
            return;
        }
        passcode=Synt.declare( ud.get("passcode"),"" );
        password=AuthKit.getCrypt(password + passcode);
        if (! password.equals( ud.get("password") )) {
            ah.reply(AuthKit.getWrong("password", "core.password.invalid"));
            return;
        }

        String usrid = (String) ud.get( "id" );
        String uname = (String) ud.get("name");
        String uhead = (String) ud.get("head");
        int    state = Synt.declare(ud.get("state"), 0 ) ;
        long   utime = Synt.declare(ud.get("mtime"), 0L) * 1000 ;

        // 验证状态
        if (1 != state) {
            ah.reply(AuthKit.getWrong("state" , "core.sign.state.invalid"));
            return;
        }

        // 验证区域
        Set rs = RoleSet.getInstance ( usrid );
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
