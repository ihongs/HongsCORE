package app.hongs.serv.oauth2;

import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.serv.auth.AuthKit;
import java.util.HashMap;
import java.util.Map;

/**
 * QQ关联登录
 * @author Hongs
 */
public class QQAction {

    @Action("web/create")
    public void inWeb(ActionHelper helper) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("oauth2");
        String  appId = cc.getProperty("oauth2.wx.web.app.id" );
        String  appSk = cc.getProperty("oauth2.wx.web.app.key");
        String   rurl = cc.getProperty("oauth2.qq.wap.bak.url");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error500("Not support this mode");
        }

        Map info = getUserInfo(code, appId, appSk, rurl, false);
        String  opnId = (String) info.get("opnid");
        String   name = (String) info.get( "name");
        String   head = (String) info.get( "head");

        AuthKit.openSign(helper, "wx", appId, opnId, name, head, System.currentTimeMillis());
    }

    @Action("wap/create")
    public void inWap(ActionHelper helper) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("oauth2");
        String  appId = cc.getProperty("oauth2.wx.wap.app.id" );
        String  appSk = cc.getProperty("oauth2.wx.wap.app.key");
        String   rurl = cc.getProperty("oauth2.qq.wap.bak.url");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error500("Not support this mode");
        }

        Map info = getUserInfo(code, appId, appSk, rurl, true );
        String  opnId = (String) info.get("opnid");
        String   name = (String) info.get( "name");
        String   head = (String) info.get( "head");

        AuthKit.openSign(helper, "wx", appId, opnId, name, head, System.currentTimeMillis());
    }

    public static Map getUserInfo(String code, String appId, String appSk, String rurl, boolean inQQ)
    throws HongsException {
        String url;
        Map    req;
        Map    rsp;

        url = inQQ
            ? "https://graph.z.qq.com/moc2/token"
            : "https://graph.qq.com/oauth2.0/token";
        req = new HashMap();
        req.put("code"          , code );
        req.put("client_id"     , appId);
        req.put("client_secret" , appSk);
        req.put("redirect_uri"  , rurl );
        req.put("grant_type"    , "authorization_code");
        rsp = Client.request(url, req);

        String token = (String) rsp.get("access_token");
        if (token == null) {
            throw new HongsException.Common("Get access token error");
        }

        url = inQQ
            ? "https://graph.z.qq.com/moc2/me"
            : "https://graph.qq.com/oauth2.0/me";
        req = new HashMap();
        req.put("access_token", token);
        rsp = Client.request(url, req);

        String opnId = (String) rsp.get("open_id");
        if (opnId == null) {
            throw new HongsException.Common("Get openid error");
        }

        url = "https://graph.qq.com/user/get_user_info";
        req = new HashMap();
        req.put("openid", opnId);
        req.put("access_token", token);
        req.put("oauth_consumer_key", appId);
        rsp = Client.request(url, req);

        req = new HashMap();
        req.put("appid", "qq" );
        req.put("opnid", opnId);
        req.put("name" , rsp.get("nikename"));
        req.put("head" , rsp.get("figureurl_qq_2"));

        return req;
    }

}
