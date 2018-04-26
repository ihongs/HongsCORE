package app.hongs.serv.centre;

import app.hongs.Cnst;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import java.util.HashMap;
import java.util.Map;

/**
 * 我的信息
 * @author Hongs
 */
@Action("centre/mine")
public class MineAction extends app.hongs.serv.centra.MineAction {

    @Action("sign/info")
    public void signInfo(ActionHelper ah) {
        Map sd = new HashMap();
        sd.put(Cnst.UID_SES, ah.getSessibute(Cnst.UID_SES));
        sd.put(Cnst.STM_SES, ah.getSessibute(Cnst.STM_SES));
        sd.put(Cnst.SAE_SES, ah.getSessibute(Cnst.SAE_SES));
        sd.put("sesid", ah.getRequest().getRequestedSessionId());
        sd.put("appid", ah.getSessibute("appid"));
        sd.put("uname", ah.getSessibute("uname"));
        sd.put("uhead", ah.getSessibute("uhead"));
        ah.reply(   "", sd);
    }

}
