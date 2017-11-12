package app.hongs.serv.handle;

import app.hongs.Cnst;
import app.hongs.CoreConfig;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import java.util.HashMap;
import java.util.Map;

/**
 * 我的信息
 * @author Hongs
 */
@Action("handle/mine")
public class MineAction extends app.hongs.serv.bundle.MineAction {

    @Action("sign/info")
    public void signInfo(ActionHelper ah) {
        Map        sd = new HashMap();
        String     sk = CoreConfig.getInstance().getProperty("core.api.ssid");
        sd.put(sk, ah.getRequest().getRequestedSessionId());
        sd.put(Cnst.UID_SES, ah.getSessibute(Cnst.UID_SES));
        sd.put(Cnst.UST_SES, ah.getSessibute(Cnst.UST_SES));
        sd.put(Cnst.USL_SES, ah.getSessibute(Cnst.USL_SES));
        sd.put("appid", ah.getSessibute("appid"));
        sd.put("uname", ah.getSessibute("uname"));
        sd.put("uhead", ah.getSessibute("uhead"));
        ah.reply(   "", sd);
    }

}
