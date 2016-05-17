package app.hongs.serv.oauth2;

import app.hongs.CoreLogger;
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

public class Client {

    public static Map request(String url, Map<String, String> req)
            throws HongsException.Common {
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

            HttpResponse resp = prepare().execute(post);
            if (resp.getStatusLine().getStatusCode() != 200) {
                throw new HongsException.Common("Call "+url+" Response code: "+resp.getStatusLine().getStatusCode());
            }

            String text = EntityUtils.toString(resp.getEntity());
            if (text.startsWith("{") && text.endsWith("}")) {
                return (Map) Data.toObject(text);
            } else
            if (text.startsWith("callback(")) {
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

    public static CloseableHttpClient prepare() {
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
        } catch (NoSuchAlgorithmException e) {
            CoreLogger.error(e);
        } catch (KeyManagementException e) {
            CoreLogger.error(e);
        } catch (KeyStoreException e) {
            CoreLogger.error(e);
        }

        return HttpClients.createDefault();
    }

}
