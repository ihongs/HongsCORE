package io.github.ihongs.serv.centre;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
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
     * @throws CruxException
     */
    @Action("create")
    @Verify(conf="master", form="sign")
    @CommitSuccess
    @Override
    public void signCreate(ActionHelper ah) throws CruxException {
        CoreConfig cc = CoreConfig.getInstance("master");
        if(!cc.getProperty("core.public.sign.open",true)) {
            throw new CruxException(404, "Sign in is not allowed");
        }

        super.signCreate(ah);
    }

    /**
     * 注册
     * @param ah
     * @throws CruxException
     */
    @Action("user/create")
    @Verify(conf="master", form="regs", type=0, trim=1)
    @CommitSuccess
    public void userCreate(ActionHelper ah) throws CruxException {
        CoreConfig cc = CoreConfig.getInstance("master");
        if(!cc.getProperty("core.public.regs.open",true)) {
            throw new CruxException(404, "Sign on is not allowed");
        }

        User uo = (User) DB.getInstance("master").getModel("user");
        Map  rd = ah.getRequestData();
        String uuid  =  uo.create(rd);
        String uname = Synt.declare(rd.get("name"), "");
        String uhead = Synt.declare(rd.get("head"), "");
        String roles ;

        // 加入公共部门
        roles = cc.getProperty("core.public.regs.unit", "");
        if (!roles.isEmpty()) {
            for (Object role : Synt.toSet(roles)) {
                Map sd = new HashMap(3);
                sd.put("user_id", uuid);
                sd.put("unit_id", role);
                sd.put("type"   ,  0  );
                uo.db.getTable("unit_user").insert(sd);
            }
        }

        // 赋予公共权限
        roles = cc.getProperty("core.public.regs.role", "");
        if (!roles.isEmpty()) {
            for (Object role : Synt.toSet(roles)) {
                Map sd = new HashMap(2);
                sd.put("user_id", uuid);
                sd.put("role"   , role);
                uo.db.getTable("user_role").insert(sd);
            }
        }

        Map  ad = AuthKit.userSign( ah, "*", uuid, uname, uhead ); // * 表示密码登录
        Map  sd = new HashMap(2);
        sd.put("info", ad);
        sd.put("ok", true);
        ah.reply (sd);
    }

    /**
     * 查重
     * @param ah
     * @throws CruxException
     */
    @Action("user/unique")
    public void userUnique(ActionHelper ah) throws CruxException {
        User uo = (User) DB.getInstance("master").getModel("user");
        Map  rd = ah.getRequestData();
        boolean  v = uo.unique ( rd );
        ah.reply ( null, v ? 1 : 0  );
    }

}
