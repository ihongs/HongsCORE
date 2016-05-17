package app.hongs.serv.oauth2;

import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.serv.auth.AuthKit;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信关联登录
 * @author Hongs
 */
@Action("handle/oauth2/wx")
public class WXAction {

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
    }

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
    }

    public static Map getUserInfo(String code, String appId, String appSk)
    throws HongsException {
        String url;
        Map    req;
        Map    rsp;

        url = "https://api.weixin.qq.com/sns/oauth2/access_token";
        req = new HashMap();
        req.put("code", code);
        req.put("appid", appId);
        req.put("secret", appSk);
        req.put("grant_type", "authorization_code");
        rsp = Client.request(url, req);

        String token = (String) rsp.get("access_token");
        String opnId = (String) rsp.get("openid");
        if (opnId == null || token == null) {
            throw new HongsException.Common("Get openid and access token error");
        }

        url = "https://graph.qq.com/user/get_user_info";
        req = new HashMap();
        req.put("openid", opnId);
        req.put("access_token", token);
        rsp = Client.request(url, req);

        req = new HashMap();
        req.put("appid", "wx" );
        req.put("opnid", rsp.get("unionid" ));
        req.put("name" , rsp.get("nickname"));
        req.put("head" , rsp.get("headimgurl"));

        return null;
    }

}
