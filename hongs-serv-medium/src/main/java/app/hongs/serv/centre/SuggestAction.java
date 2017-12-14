package app.hongs.serv.centre;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Assign;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Preset;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Verify;
import app.hongs.db.DB;
import app.hongs.db.DBAction;
import app.hongs.serv.medium.Suggest;
import app.hongs.util.Synt;
import java.util.Map;

@Action("centre/medium/suggest")
@Assign(conf="medium", name="suggest")
public class SuggestAction extends DBAction {

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
    public void delete(ActionHelper helper) {
        // 禁止删除
    }

    @Action("search")
    @Preset(conf="", form="")
    @Select(conf="", form="")
    @Override
    public void search(ActionHelper helper)
    throws HongsException {
        String uit = helper.getParameter("unit");
        String uid = Synt.asString (helper.getSessibute(Cnst.UID_SES));
        helper.getRequestData().put("user_id", uid);

        Map row = DB.getInstance ( "medium" ).getTable("suggest")
                    .filter("`unit`=? AND `user_id`=?", uit, uid)
                    .select("`count` AS `size`")
                    .one();
        if (row != null && !row.isEmpty()) {
            helper.reply( row );
        } else {
            helper.reply("", 0);
        }
    }

    @Action("update")
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void update(ActionHelper helper)
    throws HongsException {
        String uit = helper.getParameter("unit");
        String uid = Synt.asString (helper.getSessibute(Cnst.UID_SES));
        int    num = Synt.asInt(helper.getRequestData( ).get("count"));

        Suggest.update(uit, uid, num);

        helper.reply("");
    }

}
