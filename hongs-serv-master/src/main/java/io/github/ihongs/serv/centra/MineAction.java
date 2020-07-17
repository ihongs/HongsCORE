package io.github.ihongs.serv.centra;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Preset;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.serv.auth.AuthKit;
import io.github.ihongs.serv.master.UserAction;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理区我的信息
 * @author Hongs
 */
@Action("centra/mine")
public class MineAction {

    @Action("same")
    @Preset(conf="master", form="mine")
    public void sameName(ActionHelper ah)
    throws HongsException {
        Map rd = ah.getRequestData();
        String id = (String) ah.getSessibute(Cnst.UID_SES);
        rd.put( "id", id );

        UserAction ua = new UserAction();
        ua.isUnique ( ah );
    }

    @Action("info")
    @Preset(conf="master", form="mine")
    public void mineInfo(ActionHelper ah)
    throws HongsException {
        Map rd = ah.getRequestData();
        String id = (String) ah.getSessibute(Cnst.UID_SES);
        rd.put( "id", id );

        UserAction ua = new UserAction();
        ua.getInfo(ah);
    }

    @Action("save")
    @Preset(conf="master", form="mine")
    @Verify(conf="master", form="mine", type=1, trim=1)
    @CommitSuccess
    public void mineSave(ActionHelper ah)
    throws HongsException {
        Map rd = ah.getRequestData();
        String id = (String) ah.getSessibute(Cnst.UID_SES);
        rd.put( "id", id );

        // 禁止危险修改, 其实校验已经做过限制了. 这是以防万一
        rd.remove("depts");
        rd.remove("roles");
        rd.remove("state");

        // 验证原始密码
        Table  ut = DB.getInstance ( "master" ).getTable("user");
        String pw = (String) rd.get("password");
        String po = (String) rd.get("passolde");
        if (pw != null && ! "".equals(pw)) {
            Map xd = new HashMap();
            Map ed = new HashMap();
            xd.put("ok",false);
            xd.put("errs", ed);
            xd.put("msg", CoreLocale.getInstance().translate("fore.form.invalid"));
            if (po != null && !"".equals(po)) {
                Map row = ut.fetchCase( )
                    .filter("id = ?", id)
                    .select("password , passcode")
                    .getOne( );
                String ps = (String) row.get("password");
                String pc = (String) row.get("passcode");
                if (pc != null) po += pc ;
                po = AuthKit.getCrypt(po);
                if (! po.equals(ps)) {
                    ed.put("passolde", "旧密码不正确");
                    ah.reply(xd);
                    return;
                }
            } else {
                    ed.put("passolde", "请填写旧密码");
                    ah.reply(xd);
                    return;
            }
        }

        // 附加验证标识, 当要验证的字段值改变时, 重设为未验证
        Map<String, Object> fs = ut.getFields();
        Map<String, Object> fz = new HashMap ();
        for(String  fn : fs.keySet()) {
            String  fx ;
            if (fn.endsWith("_checked")) {
                fx =fn.substring(0 , fn.length() - 8 );
                if (fs.containsKey(fx)
                &&  rd.containsKey(fx) ) {
                    fz.put(fx, rd.get(fx));
                }
            }
        }
        if (! fz.isEmpty()) {
            StringBuilder sb = new StringBuilder (   );
            for(String fn : fz.keySet()) {
                sb.append(",`").append(fn).append("`");
            }
            Map ud = ut.fetchCase()
                .filter("`id` = ?" , id)
                .select(sb.substring(1))
                .getOne( );
            for(Map.Entry<String, Object> et : fz.entrySet()) {
                String fn = et.getKey(  );
                Object fv = et.getValue();
                Object fo = ud.get   (fn);
                if (fv == null
                ||  fv.equals("")
                || !fv.equals(fo) ) {
                    rd.put(et.getKey()+"_checked","0");
                }
            }
        }

        UserAction ua = new UserAction();
        ua.doSave( ah );
    }

}
