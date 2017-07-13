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
 * 参数 n 是导航路径名
 * m 与 n 相同时省略 n
 * 对应的菜单节点的 href 写作 common/menu.act?m=XXX&n=XXX
 *
 * @author Hong
 */
@Action("common/menu")
public class MenuAction {

    @Action("__main__")
    public void menu(ActionHelper helper)
    throws HongsException {
        String m = helper.getParameter("m"); // 配置名称
        String n = helper.getParameter("n"); // 活动区域
        String x = helper.getParameter("x"); // 附加标识
        String u = "common/menu.act";

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
