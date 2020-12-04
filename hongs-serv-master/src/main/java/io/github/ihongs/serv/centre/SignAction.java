package io.github.ihongs.serv.centre;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
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
     * @throws HongsException
     */
    @Action("create")
    @Verify(conf="master", form="sign")
    @CommitSuccess
    @Override
    public void signCreate(ActionHelper ah) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("master");
        if(!cc.getProperty("core.public.sign.open",true)) {
            throw new HongsException(404,"Sign in is not allowed");
        }

        super.signCreate(ah);
    }

    /**
     * 注册
     * @param ah
     * @throws HongsException
     */
    @Action("user/create")
    @Verify(conf="master", form="regs", type=0, trim=1)
    @CommitSuccess
    public void userCreate(ActionHelper ah) throws HongsException {
        CoreConfig cc = CoreConfig.getInstance("master");
        if(!cc.getProperty("core.public.regs.open",true)) {
            throw new HongsException(404,"Sign on is not allowed");
        }

        User uo = (User) DB.getInstance("master").getModel("user");
        Map  rd = ah.getRequestData();
        String uuid  =  uo.create(rd);
        String uname = Synt.declare(rd.get("name"), "");
        String uhead = Synt.declare(rd.get("head"), "");

        // 加入公共部门
        Map  sd = new HashMap();
        sd.put("user_id", uuid);
        sd.put("dept_id", cc.getProperty("core.public.regs.dept", "CENTRE"));
        uo.db.getTable("dept_user").insert(sd);

        // 赋予公共权限. 仅用部门即可(2019/02/28)
//      Map  sd = new HashMap();
//      sd.put("user_id", uuid);
//      sd.put("role"   , cc.getProperty("core.public.regs.role", "centre"));
//      uo.db.getTable("user_role").insert(sd);

        ah.reply(AuthKit.userSign(ah, uuid, uname, uhead));
    }

    /**
     * 查重
     * @param ah
     * @throws HongsException
     */
    @Action("user/unique")
    public void userUnique(ActionHelper ah) throws HongsException {
        User uo = (User) DB.getInstance("master").getModel("user");
        Map  rd = ah.getRequestData();
        boolean  v = uo.unique ( rd );
        ah.reply ( null, v ? 1 : 0  );
    }

}
