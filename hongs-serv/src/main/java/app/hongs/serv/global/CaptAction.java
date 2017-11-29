package app.hongs.serv.global;

import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.util.Synt;
import app.hongs.util.sketch.Capts;
import java.io.IOException;

/**
 * 简单验证图片
 * @author Hongs
 */
@Action("global/capt")
public class CaptAction {

    @Action("create")
    public void create(ActionHelper helper) throws HongsException, IOException {
        int    h  = Synt.declare(helper.getParameter("h"), 40);
        String b  = Synt.declare(helper.getParameter("b"), "");
        String f  = Synt.declare(helper.getParameter("f"), "");
        String e  = Synt.declare(helper.getParameter("e"), "png" );

        Capts  vc = Capts.captcha(h, b, f, e);

        // 设置会话
        String ss = CoreConfig.getInstance( )
              .getProperty ("core.capt.session" , "capt");
        helper.setSessibute(ss+"_code", /**/vc.getCode());
        helper.setSessibute(ss+"_time", System.currentTimeMillis());

        // 禁止缓存
        helper.getResponse().setContentType("image/" + e);
        helper.getResponse().setDateHeader("Expires" , 0);
        helper.getResponse().setHeader("Pragma", "no-cache");
        helper.getResponse().setHeader("Cache-Control", "no-cache");

        vc.write(e, helper.getResponse( ).getOutputStream());
    }

}
