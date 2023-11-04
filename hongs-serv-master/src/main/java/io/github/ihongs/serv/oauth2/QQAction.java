package io.github.ihongs.serv.oauth2;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Remote;
import io.github.ihongs.util.Synt;
import java.util.HashMap;
import java.util.Map;

/**
 * QQ关联登录
 * @author Hongs
 */
@Action("centre/sign/open/qq")
public class QQAction {

    /**
     * QQ Web 登录回调
     * @param helper
     * @throws HongsException
     */
    @Action("web/create")
    @CommitSuccess
    public void inWeb(ActionHelper helper) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("oauth2");
        String  appId = cc.getProperty("oauth2.qq.web.app.id" );
        String  appSk = cc.getProperty("oauth2.qq.web.app.key");
        String   rurl = cc.getProperty("oauth2.qq.wap.bak.url");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error(400, "Not support this mode");
        }

        try {
            Map info = getUserInfo(code, appId, appSk, rurl, false);
            String  opnId = (String) info.get("opnid");
            String  opuId = (String) info.get("opuid");
            String   name = (String) info.get( "name");
            String   head = (String) info.get( "head");

            Map back = AuthKit.openSign(helper, "qq", Synt.defoult(opuId, opnId), name, head);

            // 登记 openId
            if (opnId != null && opuId != null) {
                String usrId = (String) back.get(Cnst.UID_SES);
                AuthKit.setUserSign ( "qq.web", opnId, usrId );
            }

            AuthKit.redirect(helper, back);
        } catch (HongsException exc) {
            AuthKit.redirect(helper, exc );
            CoreLogger.error(/*****/ exc );
        }
    }

    /**
     * QQ WAP 登录回调
     * @param helper
     * @throws HongsException
     */
    @Action("wap/create")
    @CommitSuccess
    public void inWap(ActionHelper helper) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("oauth2");
        String  appId = cc.getProperty("oauth2.qq.wap.app.id" );
        String  appSk = cc.getProperty("oauth2.qq.wap.app.key");
        String   rurl = cc.getProperty("oauth2.qq.wap.bak.url");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error(400, "Not support this mode");
        }

        try {
            Map info = getUserInfo(code, appId, appSk, rurl, true );
            String  opnId = (String) info.get("opnid");
            String  opuId = (String) info.get("opuid");
            String   name = (String) info.get( "name");
            String   head = (String) info.get( "head");

            Map back = AuthKit.openSign(helper, "qq", Synt.defoult(opuId, opnId), name, head);

            // 登记 openId
            if (opnId != null && opuId != null) {
                String usrId = (String) back.get(Cnst.UID_SES);
                AuthKit.setUserSign ( "qq.wap", opnId, usrId );
            }

            AuthKit.redirect(helper, back);
        } catch (HongsException exc) {
            AuthKit.redirect(helper, exc );
            CoreLogger.error(/*****/ exc );
        }
    }

    public static Map getUserInfo(String code, String appId, String appSk, String rurl, boolean inQQ)
    throws HongsException {
        Map    req;
        Map    rsp;
        int    err;
        String url;
        String token;
        String opnId;
        String opuId;

        url = inQQ
            ? "https://graph.z.qq.com/moc2/token"
            : "https://graph.qq.com/oauth2.0/token";
        req = new HashMap();
        req.put("code"         , code );
        req.put("client_id"    , appId);
        req.put("client_secret", appSk);
        req.put("redirect_uri" , rurl );
        req.put("grant_type"   , "authorization_code");
        rsp = Remote.parseData(Remote.get(url, req));

        err = Synt.declare(rsp.get("code"), 0);
        if (err != 0) {
            throw new HongsException("Get token error\r\n"+Dist.toString(rsp));
        }
        token = (String) rsp.get("access_token");

        url = inQQ
            ? "https://graph.z.qq.com/moc2/me"
            : "https://graph.qq.com/oauth2.0/me";
        req = new HashMap();
        req.put("access_token" , token);
        rsp = Remote.parseData(Remote.get(url, req));

        err = Synt.declare(rsp.get("code"), 0);
        if (err != 0) {
            throw new HongsException("Get open id error\r\n"+Dist.toString(rsp));
        }
        opnId = (String) rsp.get("openid");

        url = "https://graph.qq.com/user/get_user_info";
        req = new HashMap();
        req.put("openid"       , opnId);
        req.put("access_token" , token);
        req.put("oauth_consumer_key", appId);
        rsp = Remote.parseData(Remote.get(url, req));

        err = Synt.declare(rsp.get("ret"), 0);
        if (err != 0) {
            throw new HongsException("Get user info error\r\n"+Dist.toString(rsp));
        }
        opuId = (String) rsp.get("unionid");

        req = new HashMap();
        req.put("opnid", opnId);
        req.put("opuid", opuId);
        req.put("name" , rsp.get("nickname"));
        req.put("head" , rsp.get("figureurl_qq_2"));

        return req;
    }

}
