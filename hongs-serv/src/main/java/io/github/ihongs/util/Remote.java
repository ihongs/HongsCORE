package io.github.ihongs.util;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * 远程请求工具
 * @author Hongs
 */
public final class Remote {

    private Remote () {}

    public static enum METHOD { GET, PUT, POST, PATCH, DELETE };

    public static enum FORMAT { JSON, FORM, PART };

    /**
     * 简单远程请求
     *
     * <p>
     * 协议支持 HTTP,HTTPS.
     * <p>
     *
     * @param url
     * @return
     * @throws HongsException
     * @throws StatusException
     * @throws SimpleException
     */
    public static String get (String url)
            throws HongsException, StatusException, SimpleException {
        return request(METHOD.GET , FORMAT.FORM, url, null, null);
    }

    /**
     * 简单远程请求
     *
     * <p>
     * 协议支持 HTTP,HTTPS;
     * 一个键对多个值时可以用数组或列表.
     * <p>
     *
     * @param url
     * @param data
     * @return
     * @throws HongsException
     * @throws StatusException
     * @throws SimpleException
     */
    public static String get (String url, Map<String, Object> data)
            throws HongsException, StatusException, SimpleException {
        return request(METHOD.GET , FORMAT.FORM, url, data, null);
    }

    /**
     * 简单远程请求
     *
     * <p>
     * 协议支持 HTTP,HTTPS;
     * 一个键对多个值时可以用数组或列表.
     * Content-Type: application/x-www-from-urlencoded
     * <p>
     *
     * @param url
     * @param data
     * @return
     * @throws HongsException
     * @throws StatusException
     * @throws SimpleException
     */
    public static String post(String url, Map<String, Object> data)
            throws HongsException, StatusException, SimpleException {
        return request(METHOD.POST, FORMAT.FORM, url, data, null);
    }

    /**
     * 简单远程请求
     *
     * <p>
     * 协议支持 HTTP,HTTPS;
     * 一个键对多个值时可以用数组或列表.
     * Content-Type: multipart/form-data
     * <p>
     *
     * @param url
     * @param data
     * @return
     * @throws HongsException
     * @throws StatusException
     * @throws SimpleException
     */
    public static String part(String url, Map<String, Object> data)
            throws HongsException, StatusException, SimpleException {
        return request(METHOD.POST, FORMAT.PART, url, data, null);
    }

    /**
     * 简单文件下载
     *
     * @param url
     * @param file
     * @throws HongsException
     * @throws StatusException
     * @throws SimpleException
     */
    public static void save(String url, File file)
            throws HongsException, StatusException, SimpleException {
        request( METHOD.GET , FORMAT.FORM, url, null, null, file);
    }

    /**
     * 简单远程请求
     *
     * @param type
     * @param kind
     * @param url
     * @param data
     * @param head
     * @return
     * @throws HongsException
     * @throws StatusException
     * @throws SimpleException
     */
    public static String request(METHOD type, FORMAT kind, String url, Map data, Map head)
            throws HongsException, StatusException, SimpleException {
        final String         [ ] txt = new String         [1];
        final SimpleException[ ] err = new SimpleException[1];
        request(type, kind, url, data, head, (rsp) -> {
            try {
                txt[0] = EntityUtils.toString(rsp.getEntity(), "UTF-8").trim();
            } catch (IOException | ParseException ex) {
                err[0] = new SimpleException(url, ex);
            }
        });
        if (null != err[0]) {
            throw   err[0];
        }   return  txt[0];
    }

    /**
     * 简单下载请求
     *
     * @param type
     * @param kind
     * @param url
     * @param data
     * @param head
     * @param file
     * @throws HongsException
     * @throws StatusException
     * @throws SimpleException
     */
    public static void request(METHOD type, FORMAT kind, String url, Map data, Map head, File file)
            throws HongsException, StatusException, SimpleException {
        final SimpleException[ ] err = new SimpleException[1];
        request(type, kind, url, data, head, (rsp) -> {
            // 建立目录
            File dir = file.getParentFile();
            if (!dir.exists()) {
                 dir.mkdirs();
            }

            // 保存文件
            try (
                OutputStream out = new FileOutputStream(file);
            ) {
                rsp.getEntity( ).writeTo(out);
            } catch (IOException ex) {
                err[0] = new SimpleException (url, ex);
            }
        });
        if (null != err[0]) {
            throw   err[0];
        }
    }

    /**
     * 简单输出请求
     *
     * @param type
     * @param kind
     * @param url
     * @param data
     * @param head
     * @param out
     * @throws HongsException
     * @throws StatusException
     * @throws SimpleException
     */
    public static void request(METHOD type, FORMAT kind, String url, Map data, Map head, OutputStream out)
            throws HongsException, StatusException, SimpleException {
        final SimpleException[ ] err = new SimpleException[1];
        request(type, kind, url, data, head, (rsp) -> {
            try {
                rsp.getEntity( ).writeTo(out);
            }
            catch (IOException ex) {
                err[0] = new SimpleException (url, ex);
            }
        });
        if (null != err[0]) {
            throw   err[0];
        }
    }

    /**
     * 简单请求
     *
     * @param type
     * @param kind
     * @param url
     * @param data
     * @param head
     * @param con
     * @throws HongsException
     * @throws StatusException
     * @throws SimpleException
     */
    public static void request(METHOD type, FORMAT kind, String url, Map data, Map head, Consumer<HttpResponse> con)
            throws HongsException, StatusException, SimpleException {
        if (url == null) {
            throw new NullPointerException("Request url can not be null");
        }

        HttpRequestBase http = null;
        try {
            // 构建 HTTP 请求对象
            switch (type) {
                case DELETE: http = new HttpDelete(); break;
                case PATCH : http = new HttpPatch( ); break;
                case POST  : http = new HttpPost(  ); break;
                case PUT   : http = new HttpPut(   ); break;
                default    : http = new HttpGet(   ); break;
            }

            // 设置报文
            if (data != null) {
                if (http instanceof HttpEntityEnclosingRequest) {
                    HttpEntityEnclosingRequest htte = (HttpEntityEnclosingRequest) http;
                    if (null != kind) switch ( kind ) {
                    case JSON:
                        htte.setEntity(buildJson(data));
                        break;
                    case PART:
                        htte.setEntity(buildPart(data));
                        break;
                    default:
                        htte.setEntity(buildPost(data));
                        break;
                    } else {
                        htte.setEntity(buildPost(data));
                    }
                } else {
                    String qry = EntityUtils.toString(buildPost(data), "UTF-8");
                    if (url.indexOf('?') == -1) {
                        url += "?" + qry;
                    } else {
                        url += "&" + qry;
                    }
                }
            }

            // 设置报头
            if (head != null) {
                String k , v ;
                for(Object o : head.entrySet()) {
                    Map.Entry e = ( Map.Entry ) o;
                    k = Synt.asString(e.getKey  ());
                    if (k.startsWith(":")) continue;
                    v = Synt.asString(e.getValue());
                    http.setHeader(k, v);
                }
            }

            // 设置超时
            CoreConfig cc = CoreConfig.getInstance ("manage");
            RequestConfig.Builder cb = RequestConfig.custom();
            int tt;
            tt = Synt.declare(head != null ? head.get(":WAIT-TIMEOUT") : null, cc.getProperty("core.remote.request.wait.timeout", 0));
            if (tt > 0) cb.setConnectionRequestTimeout ( tt );
            tt = Synt.declare(head != null ? head.get(":CONN-TIMEOUT") : null, cc.getProperty("core.remote.request.conn.timeout", 0));
            if (tt > 0) cb.setConnectTimeout(tt);
            tt = Synt.declare(head != null ? head.get(":SOCK-TIMEOUT") : null, cc.getProperty("core.remote.request.sock.timeout", 0));
            if (tt > 0) cb.setSocketTimeout (tt);
            tt = Synt.declare(head != null ? head.get(":MAX-REDIRECT") : null, cc.getProperty("core.remote.request.max.redirect", 0));
            if (tt > 0) { cb.setMaxRedirects(tt); cb.setRedirectsEnabled(true); }
            http.setConfig( cb.build() );

            // 执行请求
            http.setURI(new URI(url));
            HttpResponse rsp = HttpClients
                     .createDefault()
                     .execute( http );

            // 异常处理
            int sta = rsp.getStatusLine().getStatusCode();
            if (sta >= 300 && sta <= 399) {
                Header hea = rsp.getFirstHeader( "Location" );
                String loc = hea != null ? hea.getValue(): "";
                throw  new StatusException(url, loc, sta);
            }
            if (sta >= 400 || sta <= 199) {
                HttpEntity t = rsp.getEntity();
                String txt = EntityUtils.toString(t, "UTF-8");
                throw  new StatusException(url, txt, sta);
            }

            con.accept( rsp );
        } catch (URISyntaxException | IOException ex) {
            throw new SimpleException(url, ex);
        } finally {
            if (http != null) {
                http.releaseConnection( );
            }
        }
    }

    private static final Pattern JSONP = Pattern.compile("^\\w+\\s*\\((.*)\\)\\s*;?$", Pattern.DOTALL);

    /**
     * 解析响应数据
     *
     * 可以识别 JSON,JSONP,form-urlencode 形式数据
     *
     * @param resp
     * @return
     */
    public static Map parseData(String resp) {
        resp = resp.trim();

        // 识别是否为 JSONP 格式的数据
        Matcher mat = JSONP.matcher(resp);
        if (mat.matches()) {
            resp = mat.group( 1 ).trim( );
        }

        if (resp.length( ) == 0) {
            return new HashMap();
        }
        if (resp.startsWith("{") && resp.endsWith("}")) {
            return (  Map  ) Dist.toObject(resp);
        } else
        if (resp.startsWith("[") && resp.endsWith("]")) {
            throw  new UnsupportedOperationException("Unsupported list: "+ resp);
        } else
        if (resp.startsWith("<") && resp.endsWith(">")) {
            throw  new UnsupportedOperationException("Unsupported html: "+ resp);
        } else {
            return ActionHelper.parseQuery(resp);
        }
    }

    /**
     * 构建JSON实体
     *
     * 讲数据处理成JSON格式,
     * Content-Type: application/json; charset=utf-8
     *
     * @param data
     * @return
     * @throws HongsException
     */
    public static HttpEntity buildJson(Map<String, Object> data)
            throws HongsException {
        StringEntity enti = new StringEntity(Dist.toString(data), "UTF-8");
                     enti.setContentType/**/("application/json");
                     enti.setContentEncoding("UTF-8");
        return enti;
    }

    /**
     * 构建请求实体
     *
     * 采用键值对的数据结构,
     * 当一个键对应多个值时,
     * 值可以是数组或者集合.
     *
     * @param data
     * @return
     * @throws HongsException
     */
    public static HttpEntity buildPost(Map<String, Object> data)
            throws HongsException {
        List<NameValuePair> pair = new ArrayList(data.size());
        for (Map.Entry<String, Object> et : data.entrySet ()) {
            String n = et.getKey(  );
            Object o = et.getValue();
            if (o == null) {
                // continue;
            } else
            if (o instanceof Object [ ]) { // 针对 Servlet 参数格式
                for(Object v : (Object [ ]) o) {
                    String s = String.valueOf(v);
                    pair.add(new BasicNameValuePair(n, s));
                }
            } else
            if (o instanceof Collection) { // 针对 WebSocket 的格式
                for(Object v : (Collection) o) {
                    String s = String.valueOf(v);
                    pair.add(new BasicNameValuePair(n, s));
                }
            } else {
                    String s = String.valueOf(o);
                    pair.add(new BasicNameValuePair(n, s));
            }
        }
        try {
            return new UrlEncodedFormEntity(pair, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw  new HongsException(e, 1111);
        }
    }

    /**
     * 构建复合实体
     *
     * 采用键值对的数据结构,
     * 当一个键对应多个值时,
     * 值可以是数组或者集合;
     * 值可以是字符串或文件.
     *
     * @param data
     * @return
     * @throws HongsException
     */
    public static HttpEntity buildPart(Map<String, Object> data)
            throws HongsException {
        MultipartEntityBuilder part = MultipartEntityBuilder.create();
        part.setMode( HttpMultipartMode.BROWSER_COMPATIBLE );
        part.setCharset( Charset.forName("UTF-8") );
        for (Map.Entry<String, Object> et : data.entrySet()) {
            String n = et.getKey(  );
            Object o = et.getValue();
            if (o == null) {
                // continue;
            } else
            if (o instanceof Object [ ]) { // 针对 Servlet 参数格式
                for(Object v : (Object [ ]) o) {
                    buildPart(part, n, v);
                }
            } else
            if (o instanceof Collection) { // 针对 WebSocket 的格式
                for(Object v : (Collection) o) {
                    buildPart(part, n, v);
                }
            } else {
                    buildPart(part, n, o);
            }
        }
        return part.build();
    }
    private static void buildPart(MultipartEntityBuilder part, String n, Object v) {
        if (v instanceof ContentBody) {
            part.addPart(n, (ContentBody) v);
        } else
        if (v instanceof File) {
            part.addBinaryBody(n , (File) v);
        } else
        if (v instanceof byte[]) {
            part.addBinaryBody(n , (byte[]) v);
        } else
        if (v instanceof InputStream) {
            part.addBinaryBody(n , (InputStream) v);
        } else
        {
            part.addTextBody(n , String.valueOf(v));
        }
    }

    /**
     * 请求状态异常
     *
     * getStatus() 对应 HTTP 的响应码
     * getUrl() 的 Url 为当前请求地址
     * getRsp() 在 3xx 时返回跳转地址, 其他情况则返回响应文本
     */
    public static class StatusException extends HongsException {

        private final String url;
        private final String rsp;
        private final  int   sta;

        public StatusException(String url, String rsp, int sta) {
            super(sta >= 300 && sta <= 399
                ? "@manage:core.remote.request.status.refer"
                : "@manage:core.remote.request.status.error"
                , url, rsp, sta
            );

            this.sta = sta;
            this.url = url;
            this.rsp = rsp;
        }

        public String getUrl() {
            return url;
        }

        public String getRsp() {
            return rsp;
        }

        public int getStatus() {
            return sta;
        }

    }

    /**
     * 一般请求异常
     *
     * 可使用 getUrl() 得到当前请求的网址,
     * 可通过 getCuase() 获取具体异常对象,
     * 通常为 URISyntaxException, IOException
     */
    public static class SimpleException extends HongsException {

        private final String url;

        public SimpleException(String url, Throwable cause) {
            super(cause, "@manage:core.remote.request.simple.error", url);

            this.url = url;
        }

        public String getUrl() {
            return url;
        }

    }

}
