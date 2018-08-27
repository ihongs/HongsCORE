package io.github.ihongs.serv.auth;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Remote;
import java.util.Map;

public class ConnKit {

    /**
     * 执行远程请求
     * 协议支持 HTTP,HTTPS
     * 数据支持 JSON,JSONP,QueryString
     * @param url
     * @param req
     * @return
     * @throws HongsException
     */
    public static Map retrieve(String url, Map<String, Object> req)
    throws HongsException {
        if (req != null ) {
            return Remote.parseData(Remote.post(url, req));
        } else {
            return Remote.parseData(Remote.get (url     ));
        }
    }

    /**
     * 登录成功后跳转
     * 依此检查 Parameters,Cookies,Session 中是否有指定返回路径
     * 都没有指定时则跳转到默认地址
     * 默认地址缺失则跳转到网站首页
     * @param helper
     * @throws HongsException
     */
    public static void redirect(ActionHelper helper)
    throws HongsException {
        String k;
        String v;
        CoreConfig cc = CoreConfig.getInstance("oauth2");

        do {
            k = cc.getProperty("oauth2.bak.prm", "back");
            v = helper.getParameter(k);
            if (v != null && !v.isEmpty()) {
                break;
            }

            k = cc.getProperty("oauth2.bak.cok");
            if (k != null && !k.isEmpty()) {
                v = (String) helper.getCookibute(k);
                if (v != null && !v.isEmpty()) {
                    helper.setCookibute(k, null); // 清除 Cookies
                    break;
                }
            }

            k = cc.getProperty("oauth2.bak.ses");
            if (k != null && !k.isEmpty()) {
                v = (String) helper.getSessibute(k);
                if (v != null && !v.isEmpty()) {
                    helper.setSessibute(k, null); // 清除 Session
                    break;
                }
            }

            v = cc.getProperty("oauth2.bak.url", Core.BASE_HREF + "/");
        } while (false);

        // 跳转
        helper.redirect(v);
    }

}
