package app.hongs.action.serv;

import app.hongs.Cnst;
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
 * 参数 n 是导航路径名
 * m 与 n 相同时省略 n
 * 对应的菜单节点的 href 写作 common/menu.act?m=XXX&n=XXX
 *
 * @author Hong
 */
@Action("common/menu")
public class MenuAction {

    private static final String MENU_ACT_URI = "common/menu" + Cnst.ACT_EXT;

    @Action("__main__")
    public void menu(ActionHelper helper)
    throws HongsException {
        String m = helper.getParameter("m"); // 配置名称
        String n = helper.getParameter("n"); // 活动区域
        String x = helper.getParameter("x"); // 附加标识
        String u = MENU_ACT_URI;

        if (m == null || "".equals(m)) {
            m  = "default";
        }   u += "?m=" + m;
        if (n != null) {
            u += "&n=" + n;
        }
        if (x != null) {
            u += "&x=" + x;
        }

        // 检查是否有可以进入的下级菜单
        NaviMap site = NaviMap.getInstance(m);
        if (site.chkMenu(u) ) {
            String  href  ;
            Map<String, Map> menu = site.getMenu(u);
            if (menu != null) {
                menu  = menu.get(  "menus"  );
                if (menu != null) {
                    href  = getRedirect(site, menu);
                    if (href != null) {
                        helper.redirect(Core.BASE_HREF + "/" + href);
                        return;
                    }
                }
            }
        }

        // 没有权限则跳到指定目录或首页
        if (n == null) {
            if (!"default".equals(m)) {
                n = m ;
            } else {
                n = "";
            }
        }
        helper.redirect(Core.BASE_HREF + "/" + n);
    }

    @Action("list")
    public void list(ActionHelper helper)
    throws HongsException {
        String m = helper.getParameter("m");
        String n = helper.getParameter("n");
        String d = helper.getParameter("d");
        List   l;
        int    b;

        if (m == null || m.length() == 0) {
            m  = "default";
        }
        if (d != null && d.length() != 0) {
            b  = Integer.parseInt(d);
        } else {
            b  = 1;
        }
        if (n != null && n.length() != 0) {
            l  = NaviMap.getInstance(m).getMenuTranslated(n, b);
        } else {
            l  = NaviMap.getInstance(m).getMenuTranslated(   b);
        }

        Map data = new HashMap();
        data.put( "list", l );
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
            ||  href.startsWith(MENU_ACT_URI + "?")) {
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
