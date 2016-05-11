package app.hongs.util.verify;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.action.ActionHelper;
import app.hongs.util.Synt;

/**
 * 验证码
 * 可选参数有
 * vcode.sess 会话键
 * vcode.time 有效期(秒)
 * @author Hongs
 */
public class Vcode extends Rule {

    @Override
    public Object verify(Object value) throws Wrong {
        ActionHelper ah = Core.getInstance(ActionHelper.class);
        String ss = Synt.declare(params.get("vcode.sess"), "captcha");
        String cc = Synt.declare(ah.getSessibute(ss + "_code"), ""  );
        long   ct = Synt.declare(ah.getSessibute(ss + "_time"), 0L  );
        long   xt = Synt.declare(params.get("vcode.time"),
                CoreConfig.getInstance()
                          .getProperty("core.captcha.timeout", 600L))
                          * 1000;

        // 人机校验
        if (ct < System.currentTimeMillis()-xt) {
            throw new Wrong("fore.form.captcha.timeout");
        }
        if (cc.equals("") || !cc.equals(value)) {
            throw new Wrong("fore.form.captcha.invalid");
        }

        // 销毁记录
        ah.setSessibute(ss + "_code", null);
        ah.setSessibute(ss + "_time", null);

        return AVOID;
    }

}
