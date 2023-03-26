package io.github.ihongs.serv.centra;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.action.serv.AuthFilter;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.serv.Record;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.serv.auth.RoleSet;
import io.github.ihongs.util.Synt;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
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
    @Verify(conf="master", form="sign")
    @CommitSuccess
    public void signCreate(ActionHelper ah) throws HongsException {
        String place    = Synt.declare(ah.getParameter("place"), "centre");
        String username = Synt.declare(ah.getParameter("username"), "");
        String password = Synt.declare(ah.getParameter("password"), "");
        String passcode ;

        DB        db = DB.getInstance("master");
        Table     tb = db.getTable   ( "user" );
        FetchCase fc;
        Map       ud;
        String    id;
        String    tt;
        int       at;
        int       rt;

        // 检查账号
        fc = new FetchCase( )
            .from   ( tb.tableName )
            .filter ( "username = ?" , username )
            .select ( "password, passcode, id, name, head, state, ptime" );
        ud = db.fetchLess(fc);
        if ( ud.isEmpty() ) {
            ah.reply(AuthKit.getWrong("username", "core.username.invalid"));
            return;
        }

        // 重试限制
        CoreConfig cc = CoreConfig.getInstance ("master");
        id = (String) ud.get("id");
        tt = Synt.declare(cc.getProperty("core.sign.retry.token"), "");
        at = Synt.declare(cc.getProperty("core.sign.retry.times"), 5 );
        if ( Synt.declare(Record.get("sign.retry.allow."+id), false) ) {
            tt = "id";
        }
        switch (tt) {
            case "id": break;
            case "ip": id = Core.CLIENT_ADDR.get(); break;
            default  : id = id+"-"+Core.CLIENT_ADDR.get();
        }
        rt = Synt.declare(Record.get("sign.retry.times."+id), 0 );
        if (rt >= at) {
            ah.reply(AuthKit.getWrong("password", "core.password.timeout"));
            ah.getResponseData( ).put("allow_times" , at);
            ah.getResponseData( ).put("retry_times" , rt);
            return;
        } else {
            rt ++ ;
        }

        // 校验密码
        passcode=Synt.declare( ud.get("passcode"),"" );
        password=AuthKit.getCrypt(password + passcode);
        if (! password.equals( ud.get("password") )) {
            ah.reply(AuthKit.getWrong("password", "core.password.invalid"));
            ah.getResponseData( ).put("allow_times" , at);
            ah.getResponseData( ).put("retry_times" , rt);

            // 记录错误次数
            ZoneId zi = Core.getZoneId( );
            long   et = LocalDateTime.of(
                    LocalDate.now(zi).plusDays(1), LocalTime.MIN
                ).atZone(zi).toInstant( ).toEpochMilli( ) / 1000;
            Record.set ("sign.retry.times." + id, rt, et);
            return;
        } else {
            Record.del ("sign.retry.times." + id/*Drop*/);
        }

        String uuid  = (String) ud.get( "id" );
        String uname = (String) ud.get("name");
        String uhead = (String) ud.get("head");
        int    state = Synt.declare( ud.get("state"), 0 );
        long   ptime = Synt.declare( ud.get("ptime"), 0L);

        // 验证状态
        if (0 >= state) {
            ah.reply(AuthKit.getWrong("state" , "core.sign.state.invalid"));
            return;
        }

        // 规避自定 RoleSet 附加判断
        ah.setSessibute(Cnst.UID_SES, null);
        ah.setSessibute(Cnst.USK_SES, null);
        ah.setSessibute(Cnst.UST_SES, null);

        // 验证区域
        Set rs  = RoleSet.getInstance(uuid);
        if (rs != null && ! place.isEmpty() && ! rs.contains(place)) {
            ah.reply(AuthKit.getWrong("place" , "core.sign.place.invalid"));
            return;
        }

        Map ad  = AuthKit.userSign( ah, "*", uuid, uname, uhead ); // * 表示密码登录
        Map sd  = new HashMap(5);
        sd.put("info", ad);
        sd.put("ok", true);

        // 密码时效
        int xd  = cc.getProperty("core.passwd.worn.days", 0);
        if (xd  > 0 ) {
        int pd  = (int)(System.currentTimeMillis() / 1000L - ptime) / 86400;
        if (pd  > xd) {
            if (0 == ptime) pd = xd; // 首批导入可能为 0
            sd.put("ern", "password.expired");
            sd.put("err", "Password expired");
            sd.put("msg", CoreLocale.getInstance("master").translate("core.password.expired", pd, xd));
        }}

        ah.reply (sd);
    }

    /**
     * 更新
     * 此动作可维持会话不过期
     * @param ah
     * @throws HongsException
     */
    @Action("update")
    public void signUpdate(ActionHelper ah) throws HongsException {
        HttpSession ss  =  ah.getRequest( ).getSession( false );
        if (null == ss || null == ss.getAttribute(Cnst.UID_SES) ) {
            ah.reply(AuthKit.getWrong(null, "core.sign.phase.invalid"));
            return;
        }

        // 登录超时
        String curr_exp_key = AuthFilter.class.getName()+":expire";
        long exp = Synt.declare(ah.getAttribute(curr_exp_key), 0L);
        long ust = Synt.declare(ss.getAttribute(Cnst.UST_SES), 0L);
        long now = System.currentTimeMillis() / 1000;
        if ( exp != 0 && exp <= now - ust ) {
            ah.reply(AuthKit.getWrong(null, "core.sign.phase.invalid"));
            return;
        }

        // 重设时间
        if ( exp != 0 ) {
            ss.setAttribute(Cnst.UST_SES,now);
        }

        ah.reply ( "" );
    }

    /**
     * 登出
     * 此动作可以清除会话数据
     * @param ah
     * @throws HongsException
     */
    @Action("delete")
    public void signDelete(ActionHelper ah) throws HongsException {
        HttpSession ss  =  ah.getRequest( ).getSession( false );
        if (null == ss || null == ss.getAttribute(Cnst.UID_SES) ) {
            ah.reply(AuthKit.getWrong(null, "core.sign.phase.invalid"));
            return;
        }

        ss.invalidate();

        ah.reply ( "" );
    }

}
