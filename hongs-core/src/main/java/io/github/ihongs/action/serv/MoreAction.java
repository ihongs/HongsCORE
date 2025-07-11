package io.github.ihongs.action.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CruxCause;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.dh.MergeMore;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 通用聚合动作
 *
 * <p>
 * 可一次调用多个动作
 * 批量执行后返回数据
 * 请求格式举例:
 * </p>
 * <pre>
 * {
 *   at: "path/to/action",
 *   in: {type: 1},
 *   sub1: {
 *     at: "path/to/sub1/action",
 *     on: "id=main_id", // main has many sub1
 *     in: {rb: ["id", "name"]},
 *     sub2: {
 *       at: "path/to/sub2/action",
 *       on: "sub2_id=id", // sub1 belongs to sub2
 *       in: {ob: ["age!", "id"]}
 *     }
 *   }
 * }
 * </pre>
 * <p>
 * 下层用 on 关联上层, 缺省为层级名加 _id;
 * 当顶层 at 未给出时, 顶层资源平行无关联.
 * </p>
 *
 * @author Hongs
 */
@Action("common/more")
public class MoreAction {

    @Action("__main__")
    public void more(ActionHelper helper) {
        helper.reply("");

        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();
        Map    re0 = helper.getRequestData( );
        Map    rs0 = helper.getResponseData();
        Core  core = Core.getInstance();
        Wrap  wrap = new Wrap( helper );
        String act = null;

        try {
            act = Core.ACTION_NAME.get();
            core.put(ActionHelper.class.getName(),  wrap );

            more(wrap, null, req, rsp, re0, rs0, null, 0 );
        } finally {
            Core.ACTION_NAME.set ( act );
            core.put(ActionHelper.class.getName(), helper);
        }

        helper.reply(rs0);
    }

    private void more(ActionHelper helper, String sub, HttpServletRequest req, HttpServletResponse rsp, Map re0, Map rs0, MergeMore meg, int l) {
        String uri;
        String key;
        String col;
        Map    re1;
        Map    rs1;

        try {
            uri = (String) re0.remove("at");
            key = (String) re0.remove("on");
            re1 = (Map   ) re0.remove("in");
        }
        catch (ClassCastException e) {
            return;
        }

        if (uri != null) {
            if (meg != null) {
                if (key != null) {
                    int  p  = key. indexOf ('=');
                    if ( p >= 0) {
                        col = key.substring(1+p);
                        key = key.substring(0,p);
                    } else {
                        col = Cnst.ID_KEY;
                    }
                } else {
                        col = Cnst.ID_KEY;
                        key = sub +"_"+ Cnst.ID_KEY;
                }

                // 映射参数
                Map<Object,List> map ;
                map = meg.mapped(key);
                if (map.isEmpty()) {
                    return;
                }

                // 请求参数
                if (re1 == null) {
                    re1  = new  HashMap( );
                }
                re1.put(col, map.keySet());

                // 执行请求
                helper.reply( (Map) null );
                helper.setRequestData(re1);
                call( helper, uri , req, rsp );
                rs1 = helper.getResponseData();

                if (rs1 == null) {
                    return;
                }

                // 获取列表
                List<Map> list = (List) rs1.get("list");
                if (list == null) {
                     Map  info = (Map ) rs1.get("info");
                if (info != null) {
                    list  = Synt.listOf (info);
                } else {
                    return;
                }}

                // 预设关联
                for(Map.Entry<Object, List> lr : map.entrySet()) {
                    List<Map> lst = lr . getValue( );
                    for (Map  row : lst) {
                      row.put(sub , new ArrayList());
                    }
                }

                // 执行关联
                meg.append(list, map, col, sub);

                // 下级关联
                meg = new MergeMore (list);
            } else {
                // 请求参数
                if (re1 == null) {
                    re1  = new  HashMap( );
                }

                // 执行请求
                helper.reply( (Map) null );
                helper.setRequestData(re1);
                call( helper, uri , req, rsp );
                rs1 = helper.getResponseData();

                // 响应数据
                if (rs1 == null) {
                    return;
                }
                if (sub == null) {
                    rs0.putAll ( rs1);
                } else {
                    rs0.put(sub, rs1);

                    // 首个错误上移
                    if (Synt.declare(rs0.get("ok"), true)
                    && !Synt.declare(rs1.get("ok"), true)) {
                        rs0.put("ok", false);
                        if (rs1.containsKey("ern")) {
                            rs0.put("ern", rs1.get("ern"));
                        }
                        if (rs1.containsKey("err")) {
                            rs0.put("err", rs1.get("err"));
                        }
                        if (rs1.containsKey("msg")) {
                            rs0.put("msg", rs1.get("msg"));
                        }
                    }
                }

                // 获取列表
                List<Map> list = (List) rs1.get("list");
                if (list == null) {
                     Map  info = (Map ) rs1.get("info");
                if (info != null) {
                    list  = Synt.listOf (info);
                } else {
                    return;
                }}

                // 下级关联
                meg = new MergeMore(list);
            }
        } else
        if ( l == 0) {
            rs1=rs0;
        } else {
            return ;
        }

        /* 下级数据 */

        l ++;

        for(Object ot : re0.entrySet()) {
            Map.Entry  et = (Map.Entry) ot;
            Object k = et.getKey  ();
            Object v = et.getValue();
            if (v instanceof Map
            &&  k instanceof String) {
                re1 = (Map   ) v;
                sub = (String) k;
                more(helper, sub, req, rsp, re1, rs1, meg, l);
            }
        }
    }

    private void call(ActionHelper helper, String act, HttpServletRequest req, HttpServletResponse rsp) {
        // 重设路径
        act = act + Cnst.ACT_EXT ;
        Core.ACTION_NAME.set(act);
        helper.setAttribute(Cnst.ACTION_ATTR, null);

        try {
            req.getRequestDispatcher("/" + act).include(req, rsp);
        } catch (ServletException | IOException ex) {
            if (ex.getCause() instanceof CruxCause) {
                CruxCause ez = (CruxCause) ex.getCause();
                String en = Integer.toHexString(ez.getErrno());
                Map map = new HashMap(4);
                map.put("ok" ,  false  );
                map.put("ern", "Ex"+en );
                map.put("err", ez.getMessage());
                map.put("msg", ez.getLocalizedMessage());
                helper.reply(map);
            } else {
                Map map = new HashMap(4);
                map.put("ok" ,  false  );
                map.put("ern", "Er500" );
                map.put("err", ex.getMessage());
                map.put("msg", ex.getLocalizedMessage());
                helper.reply(map);
            }
        }
    }

    private static class Wrap extends ActionHelper {

        public Wrap(ActionHelper helper) {
            super(helper.getRequest(), helper.getResponse());
        }

        @Override
        public void flush() {
            // Nothing to do
        }

    }

}
