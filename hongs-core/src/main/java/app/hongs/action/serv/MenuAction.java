package app.hongs.action.serv;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.NaviMap;
import app.hongs.action.anno.Action;
import app.hongs.util.Tool;
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

        String            href;
        NaviMap           site;
        Map<String, Map>  menu;
        site = NaviMap.getInstance(m);
        menu = site.getMenu(n);
        if (menu != null) {
            menu  = menu.get("menus");
            if (menu != null) {
                href  = getRedirect(site, menu);
                if (href != null) {
                    helper.redirect(Core.BASE_HREF + "/" + href);
                    return;
                }
            }

            helper.error403(getRedirect(3));
        } else {
            helper.error404(getRedirect(4));
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

    private String getRedirect(NaviMap site, Map<String, Map> menu)
    throws  HongsException {
        for (Map.Entry<String, Map> et : menu.entrySet()) {
            String  href  = et.getKey(  );
            if (href.startsWith("!")) {
                continue;
            }
            if (href.startsWith("#")
            ||  href.startsWith("common/menu.act?")) {
                Map item  = et.getValue();
                Map<String, Map> subs = (Map) item.get("menus");
                if (subs != null && !subs.isEmpty()) {
                    href  = getRedirect(site, subs);
                    if ( null != href ) {
                        return   href;
                    }
                }
            } else {
                if (site.chkMenu(href)) {
                        return   href;
                }
            }
        }
        return null;
    }

    private String getRedirect(int type) {
        CoreConfig conf = CoreConfig.getInstance();
        CoreLocale lang = CoreLocale.getInstance();
        String msg = lang.translate("core.error.no." + (type == 4 ? "found" : "power"));
        String uri = conf.getProperty("fore.Er40" + type + ".redirect");
        String err = conf.getProperty("core.error.redirect");
        if (uri == null || uri.length() == 0) {
            uri = Core.BASE_HREF + "/";
        }
        Map<String, String> rep = new HashMap();
        rep.put("msg", msg);
        rep.put("uri", uri);
        return  Tool.inject(err , rep);
    }

}
