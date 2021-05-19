package io.github.ihongs.action.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.NaviMap;
import io.github.ihongs.action.anno.Action;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用菜单动作
 *
 * 请求参数说明:
 * 参数 m 是导航配置名
 * 参数 n 是节点标识名
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
        String n = helper.getParameter("n"); // 节点标识
        String x = helper.getParameter("x"); // 附加标识(已废弃)
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

        NaviMap site = NaviMap.getInstance(m);
        Map menu  = (Map) site.getMenu( u );
        if (menu != null) {
        Map mens  = (Map) menu.get("menus");
        if (mens != null) {
            // 寻找其下有权限的菜单
            u = getRedirect( site, mens );
            if (u != null) {
                helper.ensue(Core.SERV_PATH + "/" + u);
                return;
            }
        }
            // 找不到则转入后备地址
            // 默认等同模块配置路径
            u = (String) menu.get("hrel");
            if (u != null) {
                helper.ensue(Core.SERV_PATH + "/" + u);
                return;
            } else {
                u = "default".equals(m) ? "" : m ;
                helper.ensue(Core.SERV_PATH + "/" + u);
                return;
            }
        }

        throw new HongsException(404, "Can not find the menu " + m);
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

    private String getRedirect(NaviMap site, Map<String, Map> mens)
    throws  HongsException {
        for (Map.Entry<String, Map> et : mens.entrySet()) {
            Map    item = et.getValue();
            String href = et.getKey  ();
            String hrel = (String) item.get("hrel");
            if (hrel != null
            &&  hrel.startsWith("!")) {
                continue;
            }
            if (href.startsWith("!")) {
                continue;
            }
            if (href.startsWith(MENU_ACT_URI + "?")) {
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
