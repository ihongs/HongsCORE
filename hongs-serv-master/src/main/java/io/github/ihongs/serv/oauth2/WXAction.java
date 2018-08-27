package io.github.ihongs.serv.oauth2;

import io.github.ihongs.serv.auth.ConnKit;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.util.Data;
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
        String   name = (String) info.get( "name");
        String   head = (String) info.get( "head");

        AuthKit.openSign(helper, "wx", appId, opnId, name, head, System.currentTimeMillis());

        ConnKit.redirect(helper);
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
        String   name = (String) info.get( "name");
        String   head = (String) info.get( "head");

        AuthKit.openSign(helper, "wx", appId, opnId, name, head, System.currentTimeMillis());

        ConnKit.redirect(helper);
    }

    public static Map getUserInfo(String code, String appId, String appSk)
    throws HongsException {
        Map    req;
        Map    rsp;
        int    err;
        String url;
        String token;
        String opnId;

        url = "https://api.weixin.qq.com/sns/oauth2/access_token";
        req = new HashMap();
        req.put("code"          , code );
        req.put("appid"         , appId);
        req.put("secret"        , appSk);
        req.put("grant_type"    , "authorization_code");
        rsp = ConnKit.retrieve(url, req);

        err = Synt.declare(rsp.get("errcode"), 0);
        if (err != 0) {
            throw new HongsException.Common("Get token and opnId error\r\n"+Data.toString(rsp));
        }
        token = (String) rsp.get("access_token");
        opnId = (String) rsp.get("openid");

        url = "https://graph.qq.com/user/get_user_info";
        req = new HashMap();
        req.put("openid"        , opnId);
        req.put("access_token"  , token);
        rsp = ConnKit.retrieve(url, req);

        err = Synt.declare(rsp.get("errcode"), 0);
        if (err != 0) {
            throw new HongsException.Common("Get user info error\r\n"+Data.toString(rsp));
        }
        opnId = Synt.declare(rsp.get("unionid"), opnId);

        req = new HashMap();
        req.put("appid", "wx" );
        req.put("opnid", opnId);
        req.put("name" , rsp.get("nickname"));
        req.put("head" , rsp.get("headimgurl"));

        return req;
    }

}
