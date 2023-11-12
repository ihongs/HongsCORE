package io.github.ihongs.serv.centra;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CruxException;
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

    @Action("info")
    @Preset(conf="master", form="mine")
    public void mineInfo(ActionHelper ah)
    throws CruxException {
        Object id = ah.getSessibute(Cnst.UID_SES);
        if (id == null || "".equals(id) ) {
            throw new  CruxException ( 401 , "" );
        }

        UserAction ua = new UserAction( );
        Map rd = ah.getRequestData();
        rd.put( "id", id );

        ua. getInfo ( ah );
    }

    @Action("same")
    @Preset(conf="master", form="mine")
    public void sameName(ActionHelper ah)
    throws CruxException {
        Object id = ah.getSessibute(Cnst.UID_SES);
        if (id == null || "".equals(id) ) {
            throw new  CruxException ( 401 , "" );
        }

        UserAction ua = new UserAction( );
        Map rd = ah.getRequestData();
        rd.put( "id", id );
        ua.isUnique ( ah );
    }

    @Action("save")
    @Preset(conf="master", form="mine")
    @Verify(conf="master", form="mine", type=1, trim=1)
    @CommitSuccess
    public void mineSave(ActionHelper ah)
    throws CruxException {
        Object id = ah.getSessibute(Cnst.UID_SES);
        if (id == null || "".equals(id) ) {
            throw new  CruxException ( 401 , "" );
        }

        Map rd = ah.getRequestData();
        rd.put( "id", id );

        // 禁止危险修改, 其实校验已经做过限制了. 这是以防万一
        rd.remove("units");
        rd.remove("roles");
        rd.remove("state");

        // 验证原始密码
        Table  ut = DB.getInstance ( "master" ).getTable("user");
        String pw = (String) rd.get("password");
        String po = (String) rd.get("passolde");
        if (pw != null && ! "".equals(pw)) {
            Map row = ut.fetchCase( )
                .filter("id = ?", id)
                .select("password , passcode")
                .getOne( );
            String ps = (String) row.get("password");
            String pc = (String) row.get("passcode");
            Map xd = new HashMap();
            Map ed = new HashMap();
            xd.put("ok", false);
            xd.put("errs", ed );
            xd.put("msg" , CoreLocale.getInstance().translate("fore.form.invalid"));
            if (po != null && ! "".equals(po)) {
                if (ps == null) {
                    ps  = "";
                }
                if (pc != null) {
                    pw += pc;
                    po += pc;
                }
                po = AuthKit.getCrypt(po);
                if (! po.equals(ps)) {
                    ed.put("passolde", CoreLocale.getInstance("master").translate("core.passolde.invalid"));
                    ah.reply(xd);
                    return;
                }
                pw = AuthKit.getCrypt(pw);
                if (!!po.equals(pw)) {
                    ed.put("password", CoreLocale.getInstance("master").translate("core.passolde.unchged"));
                    ah.reply(xd);
                    return;
                }
            } else {
                if (ps != null && !"".equals(ps)) {
                    ed.put("passolde", CoreLocale.getInstance("master").translate("core.passolde.undefed"));
                    ah.reply(xd);
                    return;
                }
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

        UserAction ua = new UserAction( );
        ua.doSave( ah );
    }

}
