package io.github.ihongs.action.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CruxCause;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.dh.MergeMore;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
 * <p>
 * 还可以通过 common/more/call 调用 JSON-RPC 2.0 接口:
 * </p>
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "path/to/action",
 *   "params": {"a": 1, "b": 2},
 *   "id": "0"
 * }
 * </pre>
 * <p>
 * 支持一次调用多个:
 * </p>
 * <pre>
 * [
 *   {
 *     "jsonrpc": "2.0",
 *     "method": "path/to/action",
 *     "params": {"a": 1, "b": 2},
 *     "id": "0"
 *   }
 * ]
 * </pre>
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

    @Action("call")
    public void call(ActionHelper helper) {
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();
        Core  core = Core.getInstance();
        Wrap  wrap = new Wrap( helper );
        String act = null;

        try {
            act = Core.ACTION_NAME.get();
            core.put(ActionHelper.class.getName(),  wrap );

            Object data;
            String c = helper.getParameter ( Cnst.CB_KEY );
            if ("~".equals(c)) {
                data = helper.getRequestData().get("data");
            } else {
                data = helper.getRequestData();
            }

            if (data instanceof Map ) {
                Map  dict  = (Map ) data;
                Map  rsto  = call(helper, dict, req, rsp );
                if ( rsto != null ) {
                    helper.reply(Synt.mapOf(
                        Cnst.CB_KEY, "~",
                        "data", rsto
                    ));
                }
            } else
            if (data instanceof List) {
                List list  = (List) data;
                List rsts  = call(helper, list, req, rsp );
                if ( rsts != null ) {
                    helper.reply(Synt.mapOf(
                        Cnst.CB_KEY, "~",
                        "data", rsts
                    ));
                }
            } else
            {
                throw new CruxExemption(400, "Wrong request, must be array or object");
            }
        } finally {
            Core.ACTION_NAME.set ( act );
            core.put(ActionHelper.class.getName(), helper);
        }
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
        String url = "/" + act + Cnst.ACT_EXT ;
        Core.ACTION_NAME.set(url.substring(1));
        helper.setAttribute(Cnst.ACTION_ATTR, null);

        try {
            req.getRequestDispatcher(url).include(req, rsp);
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

    private Map  call(ActionHelper helper, Map  dict, HttpServletRequest req, HttpServletResponse rsp) {
        String id = null;
        try {
            id = Synt.asString(dict.get("id"));
            float ver = Synt.declare(dict.get("jsonrpc"), 2.0f);
            if (ver != 2.0f) {
                throw new CruxExemption(400, "Only support jsonrpc 2.0");
            }

            String method = Synt.asString(dict.get("method"));
            Map    params = Synt.asMap   (dict.get("params"));

            helper.reply ( ( Map ) null );
            helper.setRequestData(params);

            // 重设路径
            String url = "/"+ method+ Cnst.ACT_EXT;
            Core.ACTION_NAME.set(url.substring(1));
            helper.setAttribute(Cnst.ACTION_ATTR, null);

            // 请求转发
            req.getRequestDispatcher(url).include( req, rsp );

            Map    result = helper.getResponseData();

            if (id != null) {
                return Synt.mapOf(
                    "jsonrpc", "2.0",
                    "result", result,
                    "id", id
                );
            } else {
                return Synt.mapOf(
                    "jsonrpc", "2.0",
                    "result", result
                );
            }
        }
        catch (Exception ex) {
            int sc ;
            if (ex instanceof CruxCause) {
                switch (((CruxCause) ex).getState()) {
                    case 400:
                        sc = -32600;
                    case 401:
                        sc = -32001;
                    case 403:
                        sc = -32003;
                    case 404:
                        sc = -32601;
                    case 500:
                    default :
                        sc = -32603;
                }
            } else {
                sc = -32603;
            }
            if (id != null) {
                return Synt.mapOf(
                    "jsonrpc", "2.0",
                    "error", Synt.mapOf(
                        "code", sc,
                        "message", ex.getMessage()
                    ),
                    "id", id
                );
            } else {
                return Synt.mapOf(
                    "jsonrpc", "2.0",
                    "error", Synt.mapOf(
                        "code", sc,
                        "message", ex.getMessage()
                    )
                );
            }
        }
    }

    private List call(ActionHelper helper, List list, HttpServletRequest req, HttpServletResponse rsp) {
        List rsts = new ArrayList(list.size());
        for (Object item : list) {
            Map red = Synt.asMap(item);
            Map rst = call(helper, red, req, rsp);
            if (rst != null) {
                rsts.add(rst);
            }
        }
        return rsts;
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
