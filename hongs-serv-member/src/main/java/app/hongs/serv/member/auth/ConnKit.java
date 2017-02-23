package app.hongs.serv.member.auth;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.HongsUnchecked;
import app.hongs.action.ActionHelper;
import app.hongs.util.Data;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class ConnKit {

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

    /**
     * 执行远程请求
     * 协议支持 HTTP,HTTPS
     * 数据支持 JSON,QueryString
     * @param url
     * @param req
     * @return
     * @throws HongsException
     */
    public static Map requests(String url, Map<String, String> req)
            throws HongsException {
        try {
            HttpPost post = new HttpPost();
            post.setURI   ( new URI(url) );

            List<NameValuePair> data = new ArrayList( );
            for (Map.Entry<String, String> et : req.entrySet()) {
                String n = et.getKey(  );
                String v = et.getValue();
                data.add(new BasicNameValuePair(n , v));
            }
            post.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));

            HttpResponse resp = (
                    ! url.startsWith("https://" )
                    ? HttpClients.createDefault()
                    : createHttps()
                    ).execute(post);
            if (resp.getStatusLine().getStatusCode() != 200) {
                throw new HongsException.Common("Call "+url+" Response code: "+resp.getStatusLine().getStatusCode());
            }

            String text = EntityUtils.toString(resp.getEntity()).trim();
            if (text.startsWith("{") && text.endsWith("}")) {
                return (Map) Data.toObject(text);
            } else
            if (text.startsWith("callback(")) { // QQ 获取 access_token 返回的数据格式
                return (Map) Data.toObject(text.substring(9,text.length()-2));
            } else {
                return ActionHelper.parseParam(ActionHelper.parseQuery(text));
            }
        } catch (UnsupportedEncodingException ex) {
            throw new HongsUnchecked.Common(ex);
        } catch (URISyntaxException ex) {
            throw new HongsUnchecked.Common(ex);
        } catch (IOException ex) {
            throw new HongsUnchecked.Common(ex);
        }
    }

    private static CloseableHttpClient createHttps() {
        try {
            SSLContext                 sslContext;
            SSLConnectionSocketFactory sslConnect;

            sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
                    return true;
                }
            }).build();

            sslConnect = new SSLConnectionSocketFactory(sslContext);
            return HttpClients
                    .custom()
                    .setSSLSocketFactory(sslConnect)
                    .build ();
        } catch (NoSuchAlgorithmException ex) {
            throw new HongsUnchecked.Common(ex);
        } catch (KeyManagementException ex) {
            throw new HongsUnchecked.Common(ex);
        } catch (KeyStoreException ex) {
            throw new HongsUnchecked.Common(ex);
        }
    }

}
