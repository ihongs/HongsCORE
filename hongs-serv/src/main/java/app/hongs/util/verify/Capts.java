package app.hongs.util.verify;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.action.ActionHelper;
import app.hongs.util.Synt;

/**
 * 验证码
 * 规则参数
 *  capts-sess 会话键
 *  capts-time 有效期(秒)
 * @author Hongs
 */
public class Capts extends Rule {

    @Override
    public Object verify(Object value) throws Wrong {
        ActionHelper ah = Core.getInstance(ActionHelper.class);
        String ss = Synt.declare(params.get("capts-sess"),
            CoreConfig.getInstance()
                      .getProperty("core.capt.session","capt"));
        long   xt = Synt.declare(params.get("capts-time"),
            CoreConfig.getInstance()
                      .getProperty("core.capt.timeout", 600L ));
        String cc = Synt.declare(ah.getSessibute(ss + "_code"), "");
        long   ct = Synt.declare(ah.getSessibute(ss + "_time"), 0L);
        String vs = Synt.declare(value, "");

        try {
            // 人机校验
            if (cc.equals("") || !cc.equalsIgnoreCase( vs )) {
                throw new Wrong("fore.capt.invalid");
            }
            if (ct + xt * 1000 < System.currentTimeMillis()) {
                throw new Wrong("fore.capt.timeout");
            }
        }
        finally {
            // 销毁记录
            ah.setSessibute(ss + "_code", null);
            ah.setSessibute(ss + "_time", null);
        }

        return BLANK;
    }

}
