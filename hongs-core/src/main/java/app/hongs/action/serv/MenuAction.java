package app.hongs.action.serv;

import app.hongs.Core;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.NaviMap;
import app.hongs.action.anno.Action;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用菜单动作
 * @author Hong
 */
@Action("common/menu")
public class MenuAction {

    @Action("__main__")
    public void menu(ActionHelper helper)
    throws HongsException {
        String m = helper.getParameter("m");
        String n = helper.getParameter("n");
        if (null == m || "".equals(m)) {
            m = "default";
        }
        if (null == n || "".equals(n)) {
            n = "common/menu.act?m=" + m ;
        } else {
            n = "common/menu.act?m=" + m + "&n=" + n ;
        }

            NaviMap         site =  NaviMap.getInstance(m);
            Map<String,Map> menu =  site.getMenu( n );
        if (menu != null && menu.containsKey("menus")) {
            Map<String,Map> manu =  menu.get("menus");
                String mu = null ;
            for(String mn : manu.keySet()) {
                if (site.chkMenu(mn)) {
                    helper.redirect(Core.BASE_HREF+"/"+mn);
                    return;
                }
                if (mu == null) {
                    mu  =  mn ;
                }
            }
                if (mu != null) {
                    helper.redirect(Core.BASE_HREF+"/"+mu);
                    return;
                }

            helper.error403(CoreLocale.getInstance().translate("core.error.no.power"));
        } else {
            helper.error404(CoreLocale.getInstance().translate("core.error.no.found"));
        }
    }

    @Action("list")
    public void list(ActionHelper helper)
    throws HongsException {
        String m = helper.getParameter("m");
        String n = helper.getParameter("n");
        String level = helper.getParameter("l");
        String depth = helper.getParameter("d");

        int l, d;
        if (m  == null || m .length() == 0) {
            m  = "default";
        }
        if (level == null || level.length() == 0) {
            l = 1;
        } else {
            l = Integer.parseInt(level);
        }
        if (depth == null || depth.length() == 0) {
            d = 1;
        } else {
            d = Integer.parseInt(depth);
        }

        List list = n != null && n.length() != 0
                  ? NaviMap.getInstance(m).getMenuTranslated(n, d)
                  : NaviMap.getInstance(m).getMenuTranslated(l, d);

        Map data = new HashMap();
        data.put( "list", list );
        helper.reply(data);
    }

}
