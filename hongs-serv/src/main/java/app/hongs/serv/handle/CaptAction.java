package app.hongs.serv.handle;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.util.Synt;
import app.hongs.util.image.Vcode;
import java.io.IOException;

/**
 *
 * @author Hongs
 */
@Action("handle/capt")
public class CaptAction {

    @Action("create")
    public void create(ActionHelper helper) throws HongsException, IOException {
        int    h  = Synt.declare(helper.getParameter("h"), 40);
        String b  = Synt.declare(helper.getParameter("b"), "");
        String f  = Synt.declare(helper.getParameter("f"), "");
        String e  = Synt.declare(helper.getParameter("e"), "png");

        Vcode  vc = Vcode.captcha(h, b, f, e);

        // 设置会话
        helper.setSessibute("captcha_code", vc.getCode());
        helper.setSessibute("captcha_time", System.currentTimeMillis());

        // 禁止缓存
        helper.getResponse().setContentType("image/" + e);
        helper.getResponse().setDateHeader("Expires" , 0);
        helper.getResponse().setHeader("Pragma", "no-cache");
        helper.getResponse().setHeader("Cache-Control", "no-cache");

        vc.write(e, helper.getResponse( ).getOutputStream());
    }

}
