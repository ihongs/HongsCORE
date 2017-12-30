package app.hongs.serv.centre;

import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Assign;
import app.hongs.db.DBAction;

/**
 *
 * @author Hongs
 */
@Action("centre/medium/statist")
@Assign(conf="medium", name="statist")
public class StatistAction extends DBAction {

    @Override
    public void isExists(ActionHelper helper) {

    }
    @Override
    public void isUnique(ActionHelper helper) {

    }
    @Override
    public void create(ActionHelper helper) {
        // 禁止创建
    }
    @Override
    public void update(ActionHelper helper) {
        // 禁止更新
    }
    @Override
    public void delete(ActionHelper helper) {
        // 禁止删除
    }

}
