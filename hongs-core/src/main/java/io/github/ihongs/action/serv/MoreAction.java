package io.github.ihongs.action.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsCause;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 通用聚合动作
 * 可一次调用多个动作
 * 批量执行后返回数据
 * 请求格式举例:
 * a.KEY=ACTION&e.KEY=PARAMS&foo=bar
 * PARAMS 为 JSON 或 URLEncoded 格式
 * 其他参数为共用参数
 * @author Hongs
 */
@Action("common/more")
public class MoreAction {

    @Action("__main__")
    public void pack(ActionHelper helper) {
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();
        Map<String, Object> re0 = helper.getRequestData( );
        Map<String, String> acs = Synt.asMap(re0.get("a"));
        Map<String, Object> das = Synt.asMap(re0.get("d"));
        Map                 re1;
        Map                 rs0;
        Map                 rs1;
        String              key;
        String              uri;
        String              act;

        if (acs == null) {
            acs = new HashMap();
        }
        if (das == null) {
            das = new HashMap();
        }
            rs0 = new HashMap();
            act = Core.ACTION_NAME.get();

        Wrap wrap = new Wrap( helper );
        Core core = Core.getInstance();
        core.put(ActionHelper.class.getName(), wrap);

        try {
            for(Map.Entry<String, String> et : acs.entrySet()) {
                key = et.getKey(  );
                uri = et.getValue();
                uri = uri + Cnst.ACT_EXT;
                re1 = new HashMap( re0 );
                re1.putAll( data ( das.get (key) ) );

                Core.ACTION_NAME.set ( uri );
                wrap.setRequestData  ( re1 );
                wrap.setAttribute(Cnst.ACTION_ATTR, null);
                wrap.setAttribute(Cnst.ORIGIN_ATTR, null);
                wrap.reply((Map) null );
                call( wrap, uri , req , rsp);
                rs1 = wrap.getResponseData();
                rs0 . put ( key , rs1 );

                if (rs1 != null
                && !rs0.containsKey("ok")
                && !Synt.declare(rs1.get("ok"), true)) {
                    rs0.put("ok" , false);
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
        } finally {
            Core.ACTION_NAME.set(act);
            core.put(ActionHelper.class.getName(), helper);
        }

        helper.reply(rs0);
    }

    @Action("call")
    public void call(ActionHelper helper) throws HongsException {
        CoreConfig          cnf = CoreConfig.getInstance();
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();

        // 许可及IP白名单
        boolean sw  = cnf.getProperty( "core.call.more.enable" , false);
        String  ia  = cnf.getProperty( "core.call.more.allows" );
        String  ip  = ActionDriver.getClientAddr (req);
        Set     ias = Synt.toTerms( ia );
        if (ias == null || ias.isEmpty()) {
            ias =  new  HashSet();
            ias.add(       "::1"       );
            ias.add(    "127.0.0.1"    );
            ias.add( "0:0:0:0:0:0:0:1" );
        }
        if (! sw ) {
            throw new HongsException(400, "Illegal request!");
        }
        if (! ias.contains(ip) ) {
            throw new HongsException(400, "Illegal request.");
        }

        // 从参数提取参数
        Map map = helper.getRequestData();
        helper.setRequestData(data(map.get("request")));
        helper.setContextData(data(map.get("context")));
        helper.setSessionData(data(map.get("session")));
        helper.setCookiesData(data(map.get("cookies")));

        String act = Core.ACTION_NAME.get();
        String uri = map.get("act") + Cnst.ACT_EXT;

        try {
            Core.ACTION_NAME.set(uri);
            call(helper, uri,req,rsp);
        } finally {
            Core.ACTION_NAME.set(act);
        }
    }

    private void call(ActionHelper helper, String uri,
            HttpServletRequest req, HttpServletResponse rsp) {
        try {
            req.getRequestDispatcher("/" + uri).include(req, rsp);
        } catch (ServletException ex) {
            if (ex.getCause() instanceof HongsCause) {
                HongsCause  ez = ( HongsCause ) ex.getCause( );
                String en = Integer.toHexString(ez.getErrno());
                Map map = new HashMap();
                map.put("ok" ,  false );
                map.put("ern", "Ex"+en);
                map.put("err", ez.getMessage());
                map.put("msg", ez.getLocalizedMessage());
                helper.reply(map);
            } else {
                Map map = new HashMap();
                map.put("ok" ,  false );
                map.put("ern", "Er500");
                map.put("err", ex.getMessage());
                map.put("msg", ex.getLocalizedMessage());
                helper.reply(map);
            }
        } catch (IOException ex) {
                Map map = new HashMap();
                map.put("ok" ,  false );
                map.put("ern", "Er500");
                map.put("err", ex.getMessage());
                map.put("msg", ex.getLocalizedMessage());
                helper.reply(map);
        }
    }

    private Map data(Object obj) {
        if (obj == null || "".equals(obj)) {
            return new HashMap();
        }
        if (obj instanceof Map ) {
            return ( Map ) obj ;
        }
        String str = Synt.declare(obj, "");
        Map map;
        if (str.startsWith("{") && str.endsWith("}")) {
            map = (  Map  ) Dawn.toObject(str);
        } else {
            map = ActionHelper.parseQuery(str);
        }
        return map;
    }

    private static class Wrap extends ActionHelper {

        public Wrap(ActionHelper helper) {
            super(helper.getRequest(), helper.getResponse());
        }

        @Override
        public void responed() {
            // Nothing to do
        }

    }

}
