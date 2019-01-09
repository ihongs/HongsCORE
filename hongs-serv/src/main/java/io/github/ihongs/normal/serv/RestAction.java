package io.github.ihongs.normal.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.serv.ApisAction;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * REST 接口适配
 * 将 /api/abc/def 转换为 /abc/def.api 的形式
 * 同 API 的规则 , 未来有  URL rewrite 的功能
 * @author Hongs
 */
public class RestAction extends ApisAction {

    private static final String   METHOD_GET    = "GET";
    private static final String   METHOD_ADD    = "ADD";
    private static final String   METHOD_PUT    = "PUT";
    private static final String   METHOD_POST   = "POST";
    private static final String   METHOD_PATCH  = "PATCH";
    private static final String   METHOD_DELETE = "DELETE";

    private static final String[] ACTION_GET    = new String[] {"search", "list", "info"};
    private static final String[] ACTION_GOT    = new String[] {"search", "info", "list"};
    private static final String[] ACTION_POST   = new String[] {"create", "save"};
    private static final String[] ACTION_PATCH  = new String[] {"update", "save"};
    private static final String[] ACTION_DELETE = new String[] {"delete", "drop"};

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse rsp)
            throws ServletException, IOException {
        switch(req.getMethod()) {
            case METHOD_GET   :
                reset(req, ACTION_GET   );
                super.service( req, rsp );
                break;
            case METHOD_ADD   :
            case METHOD_POST  :
                req.setAttribute(Cnst.UPDATE_MODE, false);
                reset(req, ACTION_POST  );
                super.service( req, rsp );
                break;
            case METHOD_PUT   :
            case METHOD_PATCH :
                req.setAttribute(Cnst.UPDATE_MODE, true );
                reset(req, ACTION_PATCH );
                super.service( req, rsp );
                break;
            case METHOD_DELETE:
                reset(req, ACTION_DELETE);
                super.service( req, rsp );
                break;
            default:
                rsp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                req.getMethod() + " is not allowed for this resource!");
        }
    }

    private static  void  reset(HttpServletRequest req, String... mts) {
        /**
         * RestAction 中 ServletPath 为 /api/, PathInfo 为 /abc/def
         * ApisAction 中 ServletPath 为 /abc/def.api, 并无 PathInfo
         * 故需将前者转换为后者的形式, 然后交由 ApisAction 继续处理
         */

        String uri = parse (req, mts);

        req.setAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH , uri);
        req.removeAttribute(RequestDispatcher.INCLUDE_PATH_INFO);

        req.setAttribute(RequestDispatcher.FORWARD_SERVLET_PATH , uri);
        req.removeAttribute(RequestDispatcher.FORWARD_PATH_INFO);
    }

    private static String parse(HttpServletRequest req, String... mts) {
        // 去掉前缀
        String url = req.getPathInfo();
        String act = url.substring(1);

        // 是否动作
        Map atx = ActionRunner.getActions();
        if (atx.containsKey(act)) {
            return url + Cnst.ACT_EXT;
        }

        Map         dat = new HashMap (   );
        String[]    ats = act.split ( "/" );
        StringBuilder u = new StringBuilder();
        String        m = mts [0];
        String        w ;

        // 分解路径
        for(int i = 0; i < ats.length; i ++ ) {
            String  x  =  ats[ i ];
            int p = x.indexOf('=');
            if (p > 0) {
                String n, v;
                v = x.substring(1 + p);
                x = x.substring(0 , p);

                /**
                 * 当这是最后一个参数时
                 * 将 info 加到 list 前
                 * 最后一个总是叫 id
                 * 其他外键则叫 x_id
                 */
                if (i == ats.length - 1) {
                    if (m.equals(ACTION_GOT[0])) {
                        mts  =   ACTION_GOT   ;
                    }
                    n = Cnst.ID_KEY;
                } else {
                    n = x.replace('-', '_') + '_'
                      + Cnst.ID_KEY;
                }
                dat.put(n, v);

                u.append('/').append(x);
            } else {
                u.append('/').append(x);
            }
        }

        // 微调字串
        if (0 < u.length()) {
            w = u.substring( 1 );
        } else {
            w = u.toString (   );
        }

        // 路径参数
        if (!dat.isEmpty()) {
            req.setAttribute(Cnst.REQUES_ATTR, dat);
        }

        // 逐个对比
        for(String x : mts) {
            x = w + "/" + x ;
            if (atx.containsKey(x)) {
                x = "/" + x + Cnst.ACT_EXT;
                return    x ;
            }
        }

        /**
         * 当已知的动作中无法匹配时
         * 可能是使用了 AutoFilter 的原因
         * 可尝试检查当前动作或方法
         */
        for(String x : mts) {
            if (w.endsWith("/" + x)) {
                x = "/" + w + Cnst.ACT_EXT;
                return    x ;
            }
        }
        if (m.equals(ACTION_GET   [0])) {
            return "/" + w + "/search" + Cnst.ACT_EXT;
        }
        if (m.equals(ACTION_POST  [0])) {
            return "/" + w + "/create" + Cnst.ACT_EXT;
        } else
        if (m.equals(ACTION_PATCH [0])) {
            return "/" + w + "/update" + Cnst.ACT_EXT;
        } else
        if (m.equals(ACTION_DELETE[0])) {
            return "/" + w + "/delete" + Cnst.ACT_EXT;
        } else
        {
            return null;
        }
    }

}
