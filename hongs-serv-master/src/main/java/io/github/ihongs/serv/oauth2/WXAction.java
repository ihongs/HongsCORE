package io.github.ihongs.serv.oauth2;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
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
 * 微信关联登录
 * @author Hongs
 */
@Action("centre/sign/open/wx")
public class WXAction {

    /**
     * 微信 Web 登录回调
     * @param helper
     * @throws CruxException
     */
    @Action("web/create")
    @CommitSuccess
    public void inWeb(ActionHelper helper) throws CruxException {
        CoreConfig cc = CoreConfig.getInstance("auth");
        String  appId = cc.getProperty("oauth2.wx.web.app.id" );
        String  appSk = cc.getProperty("oauth2.wx.web.app.key");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error(400, "Not support this mode");
            return;
        }

        try {
            Map info = getUserInfo(code, appId, appSk);
            String  opnId = (String) info.get("opnid");
            String  opuId = (String) info.get("opuid");
            String   name = (String) info.get( "name");
            String   head = (String) info.get( "head");

            Map back = AuthKit.openSign(helper, "wx", Synt.defoult(opuId, opnId), name, head);

            // 登记 openId
            if (opnId != null && opuId != null) {
                String usrId = (String) back.get(Cnst.UID_SES);
                AuthKit.setUserSign ( "wx.web", opnId, usrId );
            }

            AuthKit.redirect(helper, back);
        } catch (CruxException exc) {
            AuthKit.redirect(helper, exc );
            CoreLogger.error(/*****/ exc );
        }
    }

    /**
     * 微信 WAP 登录回调
     * @param helper
     * @throws CruxException
     */
    @Action("wap/create")
    @CommitSuccess
    public void inWap(ActionHelper helper) throws CruxException {
        CoreConfig cc = CoreConfig.getInstance("auth");
        String  appId = cc.getProperty("oauth2.wx.wap.app.id" );
        String  appSk = cc.getProperty("oauth2.wx.wap.app.key");
        String   code = helper.getParameter ("code");

        if (appId == null || appSk == null) {
            helper.error(400, "Not support this mode");
            return;
        }

        try {
            Map info = getUserInfo(code, appId, appSk);
            String  opnId = (String) info.get("opnid");
            String  opuId = (String) info.get("opuid");
            String   name = (String) info.get( "name");
            String   head = (String) info.get( "head");

            Map back = AuthKit.openSign(helper, "wx", Synt.defoult(opuId, opnId), name, head);

            // 登记 openId
            if (opnId != null && opuId != null) {
                String usrId = (String) back.get(Cnst.UID_SES);
                AuthKit.setUserSign ( "wx.wap", opnId, usrId );
            }

            AuthKit.redirect(helper, back);
        } catch (CruxException exc) {
            AuthKit.redirect(helper, exc );
            CoreLogger.error(/*****/ exc );
        }
    }

    public static Map getUserInfo(String code, String appId, String appSk)
    throws CruxException {
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
            throw new CruxException("Get token error\r\n"+Dist.toString(rsp));
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
            throw new CruxException("Get user info error\r\n"+Dist.toString(rsp));
        }
        opuId = (String) rsp.get("unionid");

        req = new HashMap();
        req.put("opnid", opnId);
        req.put("opuid", opuId);
        req.put("name" , rsp.get("nickname"));
        req.put("head" , rsp.get("headimgurl"));

        return req;
    }

}
