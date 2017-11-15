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
 * NAME1/ACTION/PATH1=PARAMS&NAME2/ACTION/PATH2=PARAMS
 * PARAMS 为 JSON 或 URLEncoded 格式
 * .data  提供共同的参数
 * @author Hongs
 */
@Action("normal/more")
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
        String              nm0 ;
        String              nm1 ;
        String              uri ;
        int                 pos ;

        while (nms.hasMoreElements( )) {
            nm0 = nms.nextElement (  );
            pos = nm0.indexOf("/");
            if (0 > pos) continue ;
            nm1 = nm0.substring(0,pos);
            uri = nm0.substring(  pos);
            uri = uri  +  Cnst.ACT_EXT;

            // 解析请求参数
            re2 = data(re0.get( nm0 ));
            rs1 = new HashMap();
            rs1.putAll(re1);
            rs1.putAll(re2);
            helper.setRequestData(rs1);

            // 代理执行动作
            rs1 = call(helper, uri, req, rsp);
            rs0.put (nm1 , rs1);
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
                helper.fault( msg, ern, err);
            } else {
                String msg = ex.getLocalizedMessage();
                String err = ex.getMessage();
                String ern = "Er500";
                helper.fault( msg, ern, err);
            }
        } catch (IOException ex) {
                String msg = ex.getLocalizedMessage();
                String err = ex.getMessage();
                String ern = "Er500";
                helper.fault( msg, ern, err);
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
