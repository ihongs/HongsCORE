package io.github.ihongs.serv.oauth2;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.util.Data;
import io.github.ihongs.util.Remote;
import io.github.ihongs.util.Synt;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信关联登录
 * @author Hongs
 */
@Action("centre/oauth2/wx")
public class WXAction {

    /**
     * 微信 Web 登录回调
     * @param helper
     * @throws HongsException 
     */
    @Action("web/create")
    public void inWeb(ActionHelper helper) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("oauth2");
        String  appId = cc.getProperty("oauth2.wx.web.app.id" );
        String  appSk = cc.getProperty("oauth2.wx.web.app.key");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error500("Not support this mode");
        }

        Map info = getUserInfo(code, appId, appSk);
        String  opnId = (String) info.get("opnid");
        String  opuId = (String) info.get("opuid");
        String   name = (String) info.get( "name");
        String   head = (String) info.get( "head");

        Map back = AuthKit.openSign(helper, "wx", Synt.defoult(opuId, opnId), name, head);

        AuthKit.redirect(helper, back);
    }

    /**
     * 微信 WAP 登录回调
     * @param helper
     * @throws HongsException 
     */
    @Action("wap/create")
    public void inWap(ActionHelper helper) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("oauth2");
        String  appId = cc.getProperty("oauth2.wx.wap.app.id" );
        String  appSk = cc.getProperty("oauth2.wx.wap.app.key");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error500("Not support this mode");
        }

        Map info = getUserInfo(code, appId, appSk);
        String  opnId = (String) info.get("opnid");
        String  opuId = (String) info.get("opuid");
        String   name = (String) info.get( "name");
        String   head = (String) info.get( "head");

        Map back = AuthKit.openSign(helper, "wx", Synt.defoult(opuId, opnId), name, head);

        AuthKit.redirect(helper, back);
    }

    /**
     * 微信小程序登录回调
     * @param helper
     * @throws HongsException 
     */
    @Action("wxa/create")
    public void inWxa(ActionHelper helper) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("oauth2");
        String  appId = cc.getProperty("oauth2.wx.wap.app.id" );
        String  appSk = cc.getProperty("oauth2.wx.wap.app.key");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error500("Not support this mode");
        }

        Map info = getWxauInfo(code, appId, appSk, helper.getRequestData());
        String  opnId = (String) info.get("opnid");
        String  opuId = (String) info.get("opuid");
        String   name = (String) info.get( "name");
        String   head = (String) info.get( "head");

        Map back = AuthKit.openSign(helper, "wx", Synt.defoult(opuId, opnId), name, head);

        AuthKit.redirect(helper, back);
    }

    public static Map getUserInfo(String code, String appId, String appSk)
    throws HongsException {
        Map    req;
        Map    rsp;
        int    err;
        String url;
        String token;
        String opnId;
        String opuId;

        url = "https://api.weixin.qq.com/sns/oauth2/access_token";
        req = new HashMap();
        req.put("code"         , code );
        req.put("appid"        , appId);
        req.put("secret"       , appSk);
        req.put("grant_type"   , "authorization_code");
        rsp = Remote.parseData(Remote.get(url, req));

        err = Synt.declare(rsp.get("errcode"), 0);
        if (err != 0) {
            throw new HongsException.Common("Get token error\r\n"+Data.toString(rsp));
        }
        token = (String) rsp.get("access_token");
        opnId = (String) rsp.get("openid");

        url = "https://api.weixin.qq.com/sns/userinfo";
        req = new HashMap();
        req.put("openid"       , opnId);
        req.put("access_token" , token);
        req.put("lang"         , "zh_CN");
        rsp = Remote.parseData(Remote.get(url, req));

        err = Synt.declare(rsp.get("errcode"), 0);
        if (err != 0) {
            throw new HongsException.Common("Get user info error\r\n"+Data.toString(rsp));
        }
        opuId = (String) rsp.get("unionid");

        req = new HashMap();
        req.put("opnid", opnId);
        req.put("opuid", opuId);
        req.put("name" , rsp.get("nickname"));
        req.put("head" , rsp.get("headimgurl"));

        return req;
    }

    public static Map getWxauInfo(String code, String appId, String appSk, Map data)
    throws HongsException {
        Map    rsp;
        int    err;
        String url;

        url = "https://api.weixin.qq.com/sns/jscode2session?appid="+appId+"&secret="+appSk+"&js_code="+code+"&grant_type=authorization_code";
        rsp = Remote.parseData(Remote.get(url));

        err = Synt.declare(rsp.get("errcode"), 0);
        if (err != 0) {
            throw new HongsException.Common("Get session error\r\n"+Data.toString(rsp));
        }

        String opnId = (String)rsp.get( "openid");
        String opuId = (String)rsp.get("unionid");

        Map   ud;
        DB    db;
        Table tb;

        ud  = (Map) data.get("user_info");
        if (ud != null && ! ud.isEmpty()) {
            return  Synt.mapOf(
                "opnid", opnId,
                "opuid", opuId,
                "name" , ud.get("name"),
                "head" , ud.get("head")
            );
        }

        db  = DB.getInstance( "master");
        tb  = db.getTable ("user_sign");
        ud  = tb.fetchCase()
                .filter("`unit` = ? AND `code` = ?", "wx", Synt.defoult(opuId, opnId))
                .select("`user_id`")
                .getOne(   );
        if (ud != null && !ud.isEmpty()) {
            tb  = db.getTable ( "user");
            ud  = tb.fetchCase()
                    .filter("`id` = ?" , ud.get("user_id"))
                    .select("`name`,`head`")
                    .getOne(   );
            return  Synt.mapOf(
                "opnid", opnId,
                "opuid", opuId,
                "name" , ud.get("name"),
                "head" , ud.get("head")
            );
        }

        return null;
    }
}
