package app.hongs.action.serv;

import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.NaviMap;
import app.hongs.action.anno.Action;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用菜单动作
 *
 * 请求参数说明:
 * 参数 m 是导航配置名
 * 参数 n 是菜单节点名
 * 如果 m,n 一致则仅给 m
 * 对应的菜单节点的 href 写作 common/menu.act?m=&n=
 *
 * @author Hong
 */
@Action("common/menu")
public class MenuAction {

    @Action("__main__")
    public void menu(ActionHelper helper)
    throws HongsException {
        String m = helper.getParameter("m");
        String n = helper.getParameter("n");
        String x = n;
        if (null == m || "".equals(m)) {
            m = "default";
        }
        if (null == n || "".equals(n)) {
            n = "common/menu.act?m=" + m ;
            x = !"deafult".equals(m) ? m : "" ;
        } else {
            n = "common/menu.act?m=" + m + "&n=" + n ;
        }

        NaviMap site  =  NaviMap.getInstance(m);
        Map<String, Map> menu = site.getMenu(n);
        String  href  ;
        if (menu != null) {
            menu  = menu.get("menus");
            if (menu != null) {
                href  = getRedirect(site, menu);
                if (href != null) {
                    helper.redirect(Core.BASE_HREF +"/"+ href);
                    return;
                }
            }
        }

        helper.redirect(Core.BASE_HREF +"/"+ x);
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

}
