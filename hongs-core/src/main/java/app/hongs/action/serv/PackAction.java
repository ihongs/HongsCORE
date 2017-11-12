package app.hongs.action.serv;

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
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 通用聚合动作
 * 可一次调用多个动作
 * 批量执行后返回数据
 * 请求格式举例:
 * ACTION/NAME.api=PARAMS&ACTION/NAME2.act#0=PARAMS&ACTION/NAME2.act#1=PARAMS&!data=PARAMS
 * PARAMS 为 JSON 或 URLEncoded 格式
 * !data  提供共同的参数
 * @author Hongs
 */
@Action("normal/pack")
public class PackAction {

    private static final Pattern EXT_PAT = Pattern.compile("\\.(act|api)(#.*?)?$");

    @Action("__main__")
    public void pack(ActionHelper helper) {
        Map<String, Object> ret = new LinkedHashMap( );
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();
        Enumeration<String> enu = req.getParameterNames();
        Map                 dat = data(Dict.get(helper.getRequestData(), null, "", "data"));
        Map                 raq ;
        Map                 rap ;
        String              uri ;
        String              urx ;
        Matcher             mat ;

        while (enu.hasMoreElements( )) {
            uri = enu.nextElement(   );
            mat = EXT_PAT.matcher(uri);
            if (! mat.find()) {
                continue;
            }
            urx = uri.substring(0,mat.end(1));

            // 解析请求参数
            raq = data(req.getParameter(uri));
            rap = new HashMap();
            rap.putAll(dat);
            rap.putAll(raq);
            helper.setRequestData(rap);

            // 代理执行动作
            rap = call(helper, urx, req, rsp);
            ret.put (uri , rap);
        }

        helper.reply(ret);
    }

    @Action("call")
    public void call(ActionHelper helper) throws HongsException {
        CoreConfig          cnf = CoreConfig.getInstance( );
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();

        // 许可及IP白名单
        boolean sw  = cnf.getProperty( "core.pack.call.enable" , false);
        String  ia  = cnf.getProperty( "core.pack.call.origin" );
        String  ip  = addr(req );
        Set     ias = Synt.toTerms( ia );
        if (ias == null || ias.isEmpty()) {
            ias  = new HashSet();
            ias.add("127.0.0.1");
            ias.add("0:0:0:0:0:0:0:1"  );
        }
        if (! sw ) {
            throw new HongsException(0x1100, "Illegal request!");
        }
        if (! ias.contains(ip) ) {
            throw new HongsException(0x1100, "Illegal request.");
        }

        // 从参数提取参数
        helper.setRequestData(data(req.getParameter("request")));
        helper.setContextData(data(req.getParameter("context")));
        helper.setSessionData(data(req.getParameter("session")));
        helper.setCookiesData(data(req.getParameter("cookies")));
        String uri = "/" + req.getParameter("act");
        if ( ! uri.endsWith(".act") && ! uri.endsWith(".api")) {
            uri += ".act";
        }

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
