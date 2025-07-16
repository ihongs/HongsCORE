package io.github.ihongs.util;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.action.ActionHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.ContentBody;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.util.Timeout;

/**
 * 远程请求工具
 *
 * <pre>
 * 默认配置(default.properties):
 *  core.remote.request.wait.timeout=等待超时
 *  core.remote.request.conn.timeout=连接超时
 *  core.remote.request.sock.timeout=套接超时
 *  core.remote.request.max.redirect=最大重定向数
 * 单位毫秒, 为 0 不限, 默认均为 0.
 * 在请求 head 中设置 :WAIT-TIMEOUT/:CONN-TIMEOUT/:SOCK-TIMEOUT/:MAX-REDIRECT 亦可
 * </pre>
 *
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
     * @throws CruxException
     * @throws StatusException
     * @throws SimpleException
     */
    public static String get (String url)
            throws CruxException, StatusException, SimpleException {
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
     * @throws CruxException
     * @throws StatusException
     * @throws SimpleException
     */
    public static String get (String url, Map<String, Object> data)
            throws CruxException, StatusException, SimpleException {
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
     * @throws CruxException
     * @throws StatusException
     * @throws SimpleException
     */
    public static String post(String url, Map<String, Object> data)
            throws CruxException, StatusException, SimpleException {
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
     * @throws CruxException
     * @throws StatusException
     * @throws SimpleException
     */
    public static String part(String url, Map<String, Object> data)
            throws CruxException, StatusException, SimpleException {
        return request(METHOD.POST, FORMAT.PART, url, data, null);
    }

    /**
     * 简单文件下载
     *
     * @param url
     * @param file
     * @throws CruxException
     * @throws StatusException
     * @throws SimpleException
     */
    public static void save(String url, File file)
            throws CruxException, StatusException, SimpleException {
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
     * @throws CruxException
     * @throws StatusException
     * @throws SimpleException
     */
    public static String request(METHOD type, FORMAT kind, String url, Map data, Map head)
            throws CruxException, StatusException, SimpleException {
        final String         [ ] txt = new String         [1];
        final SimpleException[ ] err = new SimpleException[1];
        request(type, kind, url, data, head, (rsp) -> {
            try {
                txt[0] = EntityUtils.toString(rsp.getEntity(), StandardCharsets.UTF_8).trim();
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
     * @throws CruxException
     * @throws StatusException
     * @throws SimpleException
     */
    public static void request(METHOD type, FORMAT kind, String url, Map data, Map head, File file)
            throws CruxException, StatusException, SimpleException {
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
     * @throws CruxException
     * @throws StatusException
     * @throws SimpleException
     */
    public static void request(METHOD type, FORMAT kind, String url, Map data, Map head, OutputStream out)
            throws CruxException, StatusException, SimpleException {
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
     * @throws CruxException
     * @throws StatusException
     * @throws SimpleException
     */
    public static void request(METHOD type, FORMAT kind, String url, Map data, Map head, Consumer<ClassicHttpResponse> con)
            throws CruxException, StatusException, SimpleException {
        if (null == url) {
            throw new NullPointerException("Request url can not be null");
        }

        // 将 GET 参数拼到 URL 上
        if (null != data
        && (null == type
        ||  type == METHOD.GET )) {
            String que = queryText(data);
            if ( ! que.isEmpty()) {
                if (! url.contains("?")) {
                    url = url +"?"+ que ;
                } else {
                    url = url +"&"+ que ;
                }
            }
            data  = null;
        }

        try {
            // 构建 HTTP 请求对象
            ClassicHttpRequest req ;
            URI uri = new URI (url);
            if (null != type) {
                switch (type) {
                case DELETE: req = new HttpDelete(uri); break;
                case PATCH : req = new HttpPatch (uri); break;
                case POST  : req = new HttpPost  (uri); break;
                case PUT   : req = new HttpPut   (uri); break;
                default    : req = new HttpGet   (uri); break;
            }} else {
                req = new HttpGet (uri);
            }

            // 设置报文
            if (null != data) {
            if (null != kind) {
                switch (kind) {
                case JSON  : req.setEntity(buildJson(data)); break;
                case PART  : req.setEntity(buildPart(data)); break;
                default    : req.setEntity(buildPost(data)); break;
            }} else {
                req.setEntity(buildPost(data));
            }}

            // 设置报头
            if (null != head) {
                String k , v ;
                for(Object o : head.entrySet()) {
                    Map.Entry e = ( Map.Entry ) o;
                    k = Synt.asString(e.getKey  ());
                    if (k.startsWith(":")) continue;
                    v = Synt.asString(e.getValue());
                    req.setHeader(k, v);
                }
            }

            // 设置超时
            CoreConfig cc = CoreConfig.getInstance();
            RequestConfig.Builder rb = RequestConfig.custom();
            ConnectionConfig.Builder cb = ConnectionConfig.custom();
            BasicHttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
            int tt;
            tt = Synt.declare(head != null ? head.get(":SOCK-TIMEOUT") : null, cc.getProperty("core.remote.request.sock.timeout", -1));
            if (tt > -1) cb.setSocketTimeout (Timeout.ofMilliseconds(tt));
            tt = Synt.declare(head != null ? head.get(":CONN-TIMEOUT") : null, cc.getProperty("core.remote.request.conn.timeout", -1));
            if (tt > -1) cb.setConnectTimeout(Timeout.ofMilliseconds(tt));
            tt = Synt.declare(head != null ? head.get(":WAIT-TIMEOUT") : null, cc.getProperty("core.remote.request.wait.timeout", -1));
            if (tt > -1) rb.setConnectionRequestTimeout(Timeout.ofMilliseconds(tt));
            tt = Synt.declare(head != null ? head.get(":MAX-REDIRECT") : null, cc.getProperty("core.remote.request.max.redirect", -1));
            if (tt > -1) { rb.setRedirectsEnabled(true); rb.setMaxRedirects(tt); }
            cm.setConnectionConfig( cb.build() );

            // 执行请求
            try (
                CloseableHttpClient hc = HttpClientBuilder.create ( )
                    .setDefaultRequestConfig(rb.build())
                    .setConnectionManager(cm)
                    .build();
            ) {
                final String rel = url ;
                final StatusException[ ] se = new StatusException [1];
                hc.execute ( req , rsp -> {
                    // 异常处理
                    int sta  = rsp.getCode();
                    if (sta >= 300 && sta <= 399) {
                        Header hea = rsp.getFirstHeader( "Location" );
                        String loc = hea != null ? hea.getValue(): "";
                        se[0]= new StatusException(rel, loc, sta);
                        return null;
                    }
                    if (sta >= 400 || sta <= 199) {
                        HttpEntity t = rsp.getEntity();
                        String txt = EntityUtils.toString(t, "UTF-8");
                        se[0]= new StatusException(rel, txt, sta);
                        return null;
                    }

                    con.accept(rsp);

                    return null;
                });
                if (null != se [0]) {
                    throw   se [0];
                }
            }
        } catch (URISyntaxException | IOException ex) {
            throw new SimpleException(url, ex);
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
     * 构建查询字串
     * @param data
     * @return
     */
    public static String queryText(Map data) {
        try {
            return EntityUtils.toString(buildPost(data), StandardCharsets.UTF_8);
        } catch (ParseException | IOException ex) {
            throw  new CruxExemption ( ex, 1111 );
        }
    }

    /**
     * 构建JSON实体
     *
     * 将数据处理成JSON格式,
     * Content-Type: application/json; charset=utf-8
     *
     * @param data
     * @return
     */
    public static HttpEntity buildJson(Map<String, Object> data) {
        return new StringEntity(Dist.toString(data), ContentType.APPLICATION_JSON);
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
     */
    public static HttpEntity buildPost(Map<String, Object> data) {
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
        return new UrlEncodedFormEntity(pair, StandardCharsets.UTF_8);
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
     */
    public static HttpEntity buildPart(Map<String, Object> data) {
        MultipartEntityBuilder part = MultipartEntityBuilder.create();
        part.setMode(HttpMultipartMode.EXTENDED);
        part.setCharset(StandardCharsets.UTF_8 );
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
     * event-stream 协议支持
     */
    public static class EventStream extends OutputStream {

        private final static int END = "\n".getBytes()[0];
        private              int end = 0; // 连续换行计数
        private              int cnt = 0; // 有效的行计数
        private final List<Byte> buf = new ArrayList();
        private final Charset    chs ;
        private final Map<String, Consumer<String>> aps = new HashMap(1);

        public EventStream () {
            this(StandardCharsets.UTF_8);
        }

        public EventStream (Charset chs) {
            super();
            this.chs = chs;
        }

        @Override
        public void write(int b) throws IOException {
            if (END == b) {
                end += 1;

                if (end == 2) {
                    flush();
                    cnt = 0;
                    return ;
                }
                if (end >= 3) {
                    return ;
                }

                cnt += 1;
            } else {
                end  = 0;
            }

            buf.add((byte) b);
        }

        @Override
        public void flush() {
            if (buf.isEmpty()) {
                return;
            }

            int i = 0;
            byte[] bts = new byte[buf.size()];
            for(Byte c : buf ) {
                bts[i++] = c ;
            }

            String str = new String(bts, chs);
            accept(str);

            buf.clear();
        }

        @Override
        public void close() {
            flush();
            end = 0;
            cnt = 0;
        }

        public void accept(String str) {
            Map<String, String> map = new HashMap(cnt);
            StringBuilder dat = new StringBuilder(   );

            String  s, k, v;
            int b = 0, d, p;
            while (true) {
                d = str.indexOf("\n", b);
                if (d < 0) {
                    break;
                }
                d = d + 1;
                s = str.substring(b , d);
                b = d + 0;

                // 拆分键值, 跳过冒号开头的注释
                p = s.indexOf(":");
                if (p < 1) {
                    continue;
                }

                k = s.substring(0 , p).trim( );
                v = s.substring(1 + p).trim( );
                if ( "data".equals (k)) {
                    dat.append(v).append("\n");
                } else {
                    map.put(k, v);
                }
            }

            // 一个块可以有多个 data
            if (! dat.isEmpty() ) {
                dat.setLength(dat.length() - 1);
                map.put("data", dat.toString());
            }

            // 仅注释则可能为空
            if (! map.isEmpty() ) {
                accept ( map );
            }
        }

        public void accept(Map<String, String> map) {
            String evt = map.get("event");
            String dat = map.get( "data");
            if (evt == null) {
                evt = "message";
            }

            Consumer app = aps.get( evt );
            if (app != null) {
                app.accept(dat);
            }
        }

        public EventStream on (String evt, Consumer<String> app) {
            aps.put(evt, app);
            return this;
        }

    }

    /**
     * 请求状态异常
     *
     * getStatus() 对应 HTTP 的响应码
     * getUrl() 的 Url 为当前请求地址
     * getRsp() 在 3xx 时返回跳转地址, 其他情况则返回响应文本
     */
    public static class StatusException extends CruxException {

        private final String url;
        private final String rsp;
        private final  int   sta;

        public StatusException(String url, String rsp, int sta) {
            super(sta >= 300 && sta <= 399
                ? "@normal:core.remote.request.status.refer"
                : "@normal:core.remote.request.status.error"
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
    public static class SimpleException extends CruxException {

        private final String url;

        public SimpleException(String url, Throwable cause) {
            super(cause, "@normal:core.remote.request.simple.error", url);

            this.url = url;
        }

        public String getUrl() {
            return url;
        }

    }

}
