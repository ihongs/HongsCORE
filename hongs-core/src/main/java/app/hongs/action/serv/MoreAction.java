package app.hongs.action.serv;

import app.hongs.Cnst;
import app.hongs.CoreConfig;
import app.hongs.HongsCause;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.util.Data;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import java.io.IOException;
import java.util.Enumeration;
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
 * KEY.act=ACTION/PATH&KEY.req=PARAMS
 * PARAMS 为 JSON 或 URLEncoded 格式
 * .data  提供全部请求动作共同的参数
 * @author Hongs
 */
@Action("common/more")
public class MoreAction {

    @Action("__main__")
    public void pack(ActionHelper helper) {
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();
        Enumeration<String> nms = req.getParameterNames();
        Map                 re0 = helper.getRequestData();
        Map                 re1 = data(Dict.getDepth(re0, "", "data"));
        Map                 re2 ;
        Map                 rs0 = new HashMap();
        Map                 rs1 ;
        String              key ;
        String              uri ;

        while (nms.hasMoreElements(  )) {
            key = nms.nextElement (  );
            if( ! key.endsWith(".act")) {
                continue;
            }

            // 解析请求参数
            uri = helper.getParameter(key);
            uri = "/" + uri + Cnst.ACT_EXT;
            key = key.substring( 0, key.length() - 4 );
            re2 = data(Dict.getParam(re0, key+".req"));

            // 代理执行动作
            rs1 = new HashMap();
            rs1.putAll(  re1  );
            rs1.putAll(  re2  );
            helper.setRequestData ( rs1     );
            rs1 = call(helper, uri, req, rsp);
            Dict  .setParam  ( rs0, rs1, key);
        }

        helper.reply(rs0);
    }

    @Action("call")
    public void call(ActionHelper helper) throws HongsException {
        CoreConfig          cnf = CoreConfig.getInstance( );
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();

        // 许可及IP白名单
        boolean sw  = cnf.getProperty( "core.call.more.enable" , false);
        String  ia  = cnf.getProperty( "core.call.more.allows" );
        String  ip  = addr( req );
        Set     ias = Synt.toTerms( ia );
        if (ias == null || ias.isEmpty()) {
            ias =  new  HashSet();
            ias.add(       "::1"       );
            ias.add(    "127.0.0.1"    );
            ias.add( "0:0:0:0:0:0:0:1" );
        }
        if (! sw ) {
            throw new HongsException(0x1100, "Illegal request!");
        }
        if (! ias.contains(ip) ) {
            throw new HongsException(0x1100, "Illegal request.");
        }

        // 从参数提取参数
        Map map = helper.getRequestData();
        helper.setRequestData(data(map.get("request")));
        helper.setContextData(data(map.get("context")));
        helper.setSessionData(data(map.get("session")));
        helper.setCookiesData(data(map.get("cookies")));

        String uri = "/"+ map.get("act") + Cnst.ACT_EXT;

        call(helper, uri, req, rsp);
    }

    private Map call(ActionHelper helper, String uri,
            HttpServletRequest req, HttpServletResponse rsp) {
        helper.reply( new HashMap() );
        try {
            req.getRequestDispatcher(uri).include(req, rsp);
        } catch (ServletException ex) {
            if (ex.getCause( ) instanceof HongsCause) {
                HongsCause ez = (HongsCause) ex.getCause( );
                String msg = ez.getLocalizedMessage();
                String err = ez.getMessage();
                String ern = "Ex"+Integer.toHexString(ez.getErrno());
                helper.fault(ern, err, msg);
            } else {
                String msg = ex.getLocalizedMessage();
                String err = ex.getMessage();
                String ern = "Er500";
                helper.fault(ern, err, msg);
            }
        } catch (IOException ex) {
                String msg = ex.getLocalizedMessage();
                String err = ex.getMessage();
                String ern = "Er500";
                helper.fault(ern, err, msg);
        }
        return  helper.getResponseData();
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
            map = (  Map  ) Data.toObject(str);
        } else {
            map = ActionHelper.parseQuery(str);
        }
        return map;
    }

    private String addr(HttpServletRequest req) throws HongsException {
        /**
         * 代理会有安全隐患
         * 故不支持使用代理
         */
        String ip;
        ip = req.getHeader(     "Forwarded"    );
        if (ip != null && ip.length () != 0) {
            throw new HongsException(0x1100, "Illegal request!");
        }
        ip = req.getHeader(   "X-Forwarded-For");
        if (ip != null && ip.length () != 0) {
            throw new HongsException(0x1100, "Illegal request!");
        }
        ip = req.getHeader(   "Proxy-Client-IP");
        if (ip != null && ip.length () != 0) {
            throw new HongsException(0x1100, "Illegal request!");
        }
        ip = req.getHeader("WL-Proxy-Client-IP");
        if (ip != null && ip.length () != 0) {
            throw new HongsException(0x1100, "Illegal request!");
        }
        ip = req.getRemoteAddr();
        return ip;
    }

}
