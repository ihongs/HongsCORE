package app.hongs.util;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

/**
 * 远程请求工具集
 *
 * 使用 HTTP 协议
 *
 * @author Hongs
 */
public class Remote {

    public static enum METHOD { GET, PUT, POST, PATCH, DELETE };

    /**
     * 简单远程请求
     *
     * @param type
     * @param url
     * @param data
     * @param head
     * @param multipart
     * @return
     * @throws HongsException
     */
    public static String request(METHOD type, String url, Map<String, Object> data, Map<String, String> head, boolean multipart)
            throws HongsException {
        if (url == null) {
            throw new NullPointerException("Request url can not be null");
        }

        try {
            // 构建 HTTP 请求对象
            HttpRequestBase  http;
            switch (type) {
                case DELETE: http = new HttpDelete(); break;
                case PATCH: http = new HttpPatch(); break;
                case POST: http = new HttpPost(); break;
                case PUT: http = new HttpPut(); break;
                default : http = new HttpGet(); break;
            }

            // 设置报头
            if (head != null) {
                for(Map.Entry<String, String> et : head.entrySet()) {
                    http.setHeader( et.getKey( ) , et.getValue( ) );
                }
            }

            // 设置报文
            if (data != null) {
                if (http instanceof HttpEntityEnclosingRequest) {
                    HttpEntityEnclosingRequest htte = (HttpEntityEnclosingRequest) http;
                    if (multipart) {
                        htte.setEntity(buildPart(data));
                    } else {
                        htte.setEntity(buildPost(data));
                    }
                } else {
                    HttpEntity  ent  = buildPost(data) ;
                    try (
                        ByteArrayOutputStream buf = new ByteArrayOutputStream()
                    ) {
                        ent.writeTo(buf);
                        if (url.indexOf('?') == -1) {
                            url += "?" + buf.toString();
                        } else {
                            url += "&" + buf.toString();
                        }
                    }
                }
            }

            // 执行请求
            http.setURI(new URI(url));
            HttpResponse rsp = HttpClients
                     .createDefault()
                     .execute( http );

            // 判断结果
            int sta = rsp.getStatusLine().getStatusCode();
            if (sta >= 200 && sta <= 299) {
                String txt = EntityUtils.toString(rsp.getEntity(), "UTF-8").trim();
                return txt;
            } else
            if (sta >= 300 && sta <= 399) {
                Header hea = rsp.getFirstHeader( "Location" );
                String loc = hea != null ? hea.getValue(): "";
                throw  new StatusException(sta, url, loc);
            } else {
                String txt = EntityUtils.toString(rsp.getEntity(), "UTF-8").trim();
                if (sta >= 400 || sta <= 499) {
                    throw  new StatusException(sta, url, txt);
                } else
                if (sta >= 500 && sta <= 599) {
                    throw  new StatusException(sta, url, txt);
                } else
                if (sta >= 100 && sta <= 199) {
                    throw  new StatusException(sta, url, txt);
                } else {
                    throw  new StatusException(sta, url, txt);
                }
            }
        } catch (URISyntaxException|IOException ex) {
                throw  new SimpleException(url, ex);
        }
    }

    /**
     * 简单远程请求
     *
     * <p>
     * 协议支持 HTTP,HTTPS;
     * <p>
     *
     * @param url
     * @return
     * @throws HongsException
     */
    public static String get(String url)
            throws HongsException {
        return request(METHOD.GET, url, null, null, false);
    }

    /**
     * 简单远程请求
     *
     * <p>
     * 协议支持 HTTP,HTTPS;
     * 当 post 非 null 则采用 POST 反之为 GET,
     * 当一个键对多个值时, 值可以是数组或列表.
     * <p>
     *
     * @param url
     * @param post
     * @return
     * @throws HongsException
     */
    public static String post(String url, Map<String, Object> post)
            throws HongsException {
        return request(METHOD.POST, url, post, null, false);
    }

    /**
     * 简单远程请求
     *
     * <p>
     * 协议支持 HTTP,HTTPS;
     * 当 post 非 null 则采用 POST 反之为 GET,
     * 当一个键对多个值时, 值可以是数组或列表.
     * <p>
     *
     * @param url
     * @param post
     * @return
     * @throws HongsException
     */
    public static String upload(String url, Map<String, Object> post)
            throws HongsException {
        return request(METHOD.POST, url, post, null, true );
    }

    public static String download(String url, File file) {
        return null;
    }

    private static final Pattern JSONP = Pattern.compile("^\\w+\\s*\\((.*)\\)\\s*;?$");

    /**
     * 解析响应数据
     *
     * 可以识别 JSON,JSONP,FormData 形式数据
     *
     * @param resp
     * @return
     * @throws app.hongs.HongsException.Common
     */
    public static Map parseData(String resp)
            throws HongsException.Common {
        resp = resp.trim();

        // 识别是否为 JSONP 格式的数据
        Matcher mat = JSONP.matcher(resp);
        if (mat.matches()) {
            resp = mat.group(1).trim();
        }

        if (resp.length() == 0) {
            return new HashMap();
        }
        if (resp.startsWith("{") && resp.endsWith("}")) {
            return (Map) Data.toObject(resp);
        } else
        if (resp.startsWith("[") && resp.endsWith("]")) {
            throw  new HongsException.Common("Unsupported list: "+ resp );
        } else {
            return ActionHelper.parseParam(ActionHelper.parseQuery(resp));
        }
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
        List<NameValuePair> pair = new ArrayList();
        for (Map.Entry<String, Object> et : data.entrySet()) {
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
            return new UrlEncodedFormEntity(pair, HTTP.UTF_8);
        }
        catch ( UnsupportedEncodingException ex) {
            throw  new HongsException.Common(ex);
        }
    }

    /**
     * 构建上传实体
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
            throws HongsException{
        // TODO: 生成 multipart/form-data 数据
        return null;
    }

    /**
     * 请求状态异常
     *
     * getStatus() 对应 HTTP 的响应码
     * getUrl() 的 Url 为当前请求地址
     * getRsp() 在 3xx 代码时返回跳转地址, 其他均返回响应文本
     */
    public static class StatusException extends HongsException.Common {

        private final  int   sta;
        private final String url;
        private final String rsp;

        public StatusException(int sta, String url, String rsp) {
            super(sta >= 300 && sta <= 399
                ? "Redirect from "+url+" to " +rsp
                : "Error "+sta+" for request "+url);

            this.sta = sta;
            this.url = url;
            this.rsp = rsp;

            setLocalizedOptions(String.valueOf(sta), url, rsp);
        }

        public int getStatus() {
            return sta;
        }

        public String getUrl() {
            return url;
        }

        public String getRsp() {
            return rsp;
        }

    }

    /**
     * 一般请求异常
     *
     * 可使用 getUrl() 得到当前请求的网址,
     * 可通过 getCuase() 获取具体异常对象,
     * 通常为 URISyntaxException, IOException
     */
    public static class SimpleException extends HongsException.Common {

        private final String url;

        public SimpleException(String url, Throwable cause) {
            super( "Error request " + url, cause );

            this.url = url;

            setLocalizedOptions(url);
        }

        public String getUrl() {
            return url;
        }

    }

}
