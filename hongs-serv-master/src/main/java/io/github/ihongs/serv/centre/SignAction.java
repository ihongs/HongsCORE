package io.github.ihongs.serv.centre;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Preset;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.DB;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.serv.master.User;
import io.github.ihongs.util.Synt;
import java.util.HashMap;
import java.util.Map;

/**
 * 登录动作
 * @author Hongs
 */
@Action("centre/sign")
public class SignAction extends io.github.ihongs.serv.centra.SignAction {

    /**
     * 登录
     * @param ah
     * @throws HongsException
     */
    @Action("create")
    @Verify(conf="master", form="sign")
    @Override
    public void signCreate(ActionHelper ah) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("master");
        if(!cc.getProperty("core.public.sign.open",true)) {
            throw new HongsException(0x1104,"Sign in is not allowed!");
        }

        super.signCreate(ah);
    }

    /**
     * 注册
     * @param ah
     * @throws HongsException
     */
    @Action("user/create")
    @Preset(conf="master", form="mine")
    @Verify(conf="master", form="mine")
    public void userCreate(ActionHelper ah) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("master");
        if(!cc.getProperty("core.public.regs.open",true)) {
            throw new HongsException(0x1104,"Sign on is not allowed!");
        }

        User uo = (User) DB.getInstance( "master" ).getModel( "user" );
        Map  rd = ah.getRequestData(  );
        Map  sd = (Map ) uo.create (rd);

        // 提取登录信息
        String usrid = Synt.declare(sd.get(  "id" ), "");
        String uname = Synt.declare(sd.get( "name"), "");
        String uhead = Synt.declare(sd.get( "head"), "");
        long   utime = Synt.declare(sd.get("mtime"), 0L) * 1000;
        String appid = Synt.declare(ah.getParameter("appid"), "_WEB_");
        String place = Synt.declare(ah.getParameter("place"),"centre");

        // 赋予公共权限
        sd = new HashMap();
        sd.put("user_id", usrid);
        sd.put("role"   , cc.getProperty("core.public.regs.role", "centre"));
        uo.db.getTable("user_role").insert(sd);

        // 加入公共部门
        sd = new HashMap();
        sd.put("user_id", usrid);
        sd.put("dept_id", cc.getProperty("core.public.regs.dept", "CENTRE"));
        uo.db.getTable("user_dept").insert(sd);

        ah.reply(AuthKit.userSign(ah, place, appid, usrid, uname, uhead, utime));
    }

    /**
     * 注销
     * @param ah
     * @throws HongsException
     */
    @Action("user/delete")
    public void userDelete(ActionHelper ah) throws HongsException {
        String id = (String) ah.getSessibute(Cnst.UID_SES);
        if (id == null) {
            ah.reply(AuthKit.getWrong("","core.sign.phase.invalid"));
            return;
        }

        User user = (User) DB.getInstance("master").getModel("user");
        user. del (id); // 删除当前用户
        signDelete(ah); // 消除登录状态
    }

    /**
     * 查重
     * @param ah
     * @throws HongsException
     */
    @Action("user/unique")
    public void userUnique(ActionHelper ah) throws HongsException {
        User user = (User) DB.getInstance("master").getModel("user");
        Map  data =  ah.getRequestData();
        boolean v =  user.unique( data );
        ah.reply  (  null,  v ? 1 : 0  );
    }

}
