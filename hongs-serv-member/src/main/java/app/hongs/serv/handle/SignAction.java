package app.hongs.serv.handle;

import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;

/**
 * 登录动作
 * @author Hongs
 */
@Action("handle/sign")
public class SignAction extends app.hongs.serv.manage.SignAction {

    /**
     * 注册
     * @param helper
     */
    @Action("member/create")
    public void memberCreate(ActionHelper helper) {

    }

    /**
     * 注销
     * @param helper
     */
    @Action("member/delete")
    public void memberDelete(ActionHelper helper) {

    }

}
