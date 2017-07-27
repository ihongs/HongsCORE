package app.hongs.serv.oauth2;

import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.serv.auth.AuthKit;
import app.hongs.serv.auth.ConnKit;
import app.hongs.util.Data;
import app.hongs.util.Synt;
import java.util.HashMap;
import java.util.Map;

/**
 * 微博关联登录
 * @author Hongs
 */
@Action("handle/oauth2/wx")
public class WBAction {

    @Action("web/create")
    public void inWeb(ActionHelper helper) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("oauth2");
        String  appId = cc.getProperty("oauth2.wb.web.app.id" );
        String  appSk = cc.getProperty("oauth2.wb.web.app.key");
        String   rurl = cc.getProperty("oauth2.wb.wap.bak.url");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error500("Not support this mode");
        }

        Map info = getUserInfo(code, appId, appSk, rurl);
        String  opnId = (String) info.get("opnid");
        String   name = (String) info.get( "name");
        String   head = (String) info.get( "head");

        AuthKit.openSign(helper, "wb", appId, opnId, name, head, System.currentTimeMillis());

        ConnKit.redirect(helper);
    }

    @Action("wap/create")
    public void inWap(ActionHelper helper) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("oauth2");
        String  appId = cc.getProperty("oauth2.wb.wap.app.id" );
        String  appSk = cc.getProperty("oauth2.wb.wap.app.key");
        String   rurl = cc.getProperty("oauth2.wb.wap.bak.url");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error500("Not support this mode");
        }

        Map info = getUserInfo(code, appId, appSk, rurl);
        String  opnId = (String) info.get("opnid");
        String   name = (String) info.get( "name");
        String   head = (String) info.get( "head");

        AuthKit.openSign(helper, "wb", appId, opnId, name, head, System.currentTimeMillis());

        ConnKit.redirect(helper);
    }

    public static Map getUserInfo(String code, String appId, String appSk, String rurl)
    throws HongsException {
        Map    req;
        Map    rsp;
        int    err;
        String url;
        String token;
        String opnId;

        url = "https://api.weibo.com/oauth2/access_token";
        req = new HashMap();
        req.put("code"          , code );
        req.put("client_id"     , appId);
        req.put("client_secret" , appSk);
        req.put("grant_type"    , "authorization_code");
        rsp = ConnKit.retrieve(url, req);

        err = Synt.declare(rsp.get("errcode"), 0);
        if (err != 0) {
            throw new HongsException.Common("Get token and opnId error\r\n"+Data.toString(rsp));
        }
        token = (String) rsp.get("access_token");
        opnId = (String) rsp.get("uid");

        url = "https://api.weibo.com/2/eps/user/info.json";
        req = new HashMap();
        req.put("uid", opnId);
        req.put("access_token", token);
        rsp = ConnKit.retrieve(url, req);

        err = Synt.declare(rsp.get("errcode"), 0);
        if (err != 0) {
            throw new HongsException.Common("Get user info error\r\n"+Data.toString(rsp));
        }

        req = new HashMap();
        req.put("appid", "wb" );
        req.put("opnid", opnId);
        req.put("name" , rsp.get("nickname"));
        req.put("head" , rsp.get("headimgurl"));

        return null;
    }

}
