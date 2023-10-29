package io.github.ihongs.util.verify;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.DownPart;
import io.github.ihongs.action.UploadHelper;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.http.Part;

/**
 * 文件校验
 * <pre>
 * 规则参数:
 *  pass-locals yes|no 是否跳过本地路径(本地网络存在的)
 *  pass-remote yes|no 是否跳过远程链接(开头为 http://)
 *  down-remote yes|no 是否下载远程文件
 *  drop-origin yes|no 抛弃原始文件, 仅使用 checks 中新创建的
 *  keep-origin yes|no 返回原始路径, 不理会 checks 中新创建的
 *  keep-naming yes|no 保持原文件名, 会对网址末尾的文件名编码(将废弃,请改用 hash-status)
 *  hash-status yes|no 末尾附加信息, #n=文件名称&s=文件大小等
 *  name-digest 命名摘要算法, 如: MD5,SHA-1,SHA-256
 *  name-add-id 路径要增加ID, 将会从 cleans 提取 id
 *  temp 上传临时目录, 可用变量 $DATA_PATH, $BASE_PATH 等
 *  path 上传目标目录, 可用变量 $BASE_PATH, $DATA_PATH 等
 *  href 上传文件链接, 可用变量 $SERV_PATH, $SERV_HREF 等, 后者包含域名
 *  size 文件大小限制
 *  accept 类型的许可, 逗号分隔, Mime-Type 或 .extension
 * </pre>
 * @author Hongs
 */
public class IsFile extends Rule {
    @Override
    public Object verify(Value watch) throws Wrong {
        // 跳过空值和空串
        Object value = watch.get();
        if (value  ==  null ) {
            return PASS;
        }
        if (value.equals("")) {
            return PASS;
        }

        if (value instanceof String) {

        // 跳过资源路径
        if (Synt.declare(getParam("pass-locals"), false)) {
            String u = value.toString( );
            if (new File(Core.BASE_PATH +"/"+u).isFile()) {
                return u;
            }
        }

        // 跳过远程地址
        if (Synt.declare(getParam("pass-remote"), false)) {
            String u = value.toString( );
            if (HREF_PATT.matcher(u).matches()) {
                return value;
            }
        }

        // 下载远程文件
        if (Synt.declare(getParam("down-remote"), false)) {
            String u = value.toString( );
            if (HREF_PATT.matcher(u).matches()) {
            do {
                String x;
                // 如果是本地路径则不再下载
                x = Core.SERVER_HREF.get()+"/";
                if (x != null && !"".equals(x)) {
                    if (u.startsWith(x)) {
                        value = u ;
                        break ;
                    }
                }
                // 如果是目标路径则不再下载
                x = (String) getParam ("href");
                if (x != null && !"".equals(x)) {
                    x  = getUrl(x); // 补全
                    if (u.startsWith(x)) {
                        value = u ;
                        break ;
                    }
                }
                // 如果有临时目录则下载到这
                x = (String) getParam ("temp");
                if (x != null && !"".equals(x)) {
                    x  = getDir(x); // 补全
                } else {
                    x  = Core.BASE_PATH
                       + "/static/upload/tmp" ;
                }
                value  = stores(value.toString( ), x);
                if (value == null) {
                    return   null;
                }
            } while(false);
            }
        }

        } // End If

        UploadHelper hlpr = new UploadHelper();
        Object para;
        String path;
        String href;
        String name;
        long   size;

        para = getParam("accept");
        if (para != null && !"".equals(para)) hlpr.setAccept(Synt.toSet(para));
        para = getParam("temp");
        if (para != null && !"".equals(para)) hlpr.setUploadTemp(Synt.declare(para, String.class));
        para = getParam("path");
        if (para != null && !"".equals(para)) hlpr.setUploadPath(Synt.declare(para, String.class));
        para = getParam("href");
        if (para != null && !"".equals(para)) hlpr.setUploadHref(Synt.declare(para, String.class));
        para = getParam("name-digest");
        if (para != null && !"".equals(para)) hlpr.setDigestType(Synt.declare(para, String.class));

        /**
         * 给当前路径末尾追加记录 ID
         * 以便检测和清理无效文件
         */
        if (Synt.declare(getParam("name-add-id"), false)) {
            String id = Synt.declare(watch.getCleans().get(Cnst.ID_KEY), String.class);
            if (id == null || id.isEmpty()) {
                throw new Wrong(Cnst.ID_KEY +" required for file");
            }
            id   = Syno.splitPath(id); // 避免单层目录数量过多
            path = Synt.declare(getParam("path"), "static/upload");
            href = Synt.declare(getParam("href"), "static/upload");
            hlpr.setUploadPath(path + "/" + id );
            hlpr.setUploadHref(href + "/" + id );
        }

        String hash = "";
        if (value instanceof Part ) {
            Part part =(Part) value;
            name = part.getSubmittedFileName();
            size = part.getSize ( );
            hlpr.upload(part);
        } else
        if (value instanceof File ) {
            File file =(File) value;
            name = file.getName ( );
            size = file.length  ( );
            hlpr.upload(file);
        } else
        {
            href = value.toString();
            name = href;

            /**
             * 井号后为附加选项
             * 竖杆后为真实名称
             * 有斜杠为旧的记录
             * 外部记录的是网址
             * 必须进行解码才行
             */
            int p;
            p = href.indexOf ("#");
            if (p != -1) {
                hash = href.substring(  p);
                href = href.substring(0,p);
            }
            p = href.indexOf ("|");
            if (p != -1) {
                name = href.substring(1+p);
                href = href.substring(0,p);
            } else
            if (href.contains("/")) {
                href = decode(href);
                name = null; // 不在此返回是因 upload 方法还会做检查
            }

            File file;
            file = hlpr.upload(href);
            size = file.length(/**/);
        }

        path = hlpr.getResultPath();
        href = hlpr.getResultHref();

        // 没新上传, 不必检查
        if (null == name) {
            return  href  +  hash  ;
        }

        // 大小检查, 超限报错
        long max = getParam("size" , Long.MAX_VALUE);
        if ( max < size ) {
            throw new Wrong("@core.file.size.invalid", String.valueOf(max));
        }

        /**
         * 检查新上传的文件
         * 可以返回原始路径
         * 或者抛弃原始文件
         */
        String[] hp = checks(href, path);
        if (Synt.declare(getParam("keep-origin"), false)) {
            // Keep to return origin href
        } else
        if (Synt.declare(getParam("drop-origin"), false)) {
            try {
                if (! Files.isSameFile(Paths.get(path), Paths.get(hp[1]))) {
                      new File ( path ).delete();
                }
            } catch (IOException e) {
                throw new Wrong( e.getMessage());
            }
            href = hp[0];
            path = hp[1];
        } else {
            href = hp[0];
            path = hp[1];
        }

        if (getParam("keep-naming", false)) {
            // 检查外部文件名是否合法
            if (name.getBytes( ).length > 256 ) {
                throw new Wrong("@fore.file.name.toolong", name);
            }
            if (NAME_PATT.matcher(name).find()) {
                throw new Wrong("@fore.file.name.illegal", name);
            }

            try {
                int    pos ;
                String nick;
                String dist;

                // 新的网址
                pos  = href.lastIndexOf('/');
                nick = href.substring(1+pos);
                href = href.substring(0,pos);
                pos  = nick.lastIndexOf('.');
                if (pos <= 0) { // 没有扩展名
                    href = href +"/"+ nick +".d/"+ name;
                } else {
                    href = href +"/"+ nick.substring(0 , pos) +"/"+ name;
                }

                // 新的路径
                pos  = path.lastIndexOf('/');
                nick = path.substring(1+pos);
                path = path.substring(0,pos);
                pos  = nick.lastIndexOf('.');
                if (pos <= 0) { // 没有扩展名
                    path = path +"/"+ nick +".d/"+ name;
                } else {
                    path = path +"/"+ nick.substring(0 , pos) +"/"+ name;
                }

                // 指向上级原始文件
                dist = "../" + nick;

                File d = new File(path).getParentFile();
                if (!d.exists()) {
                     d.mkdir ();
                }

                Files.createSymbolicLink(Paths.get(path), Paths.get(dist));
            }
            catch (StringIndexOutOfBoundsException ex) {
                throw new HongsExemption("Wrong path/href setting");
            }
            catch (IOException ex) {
                throw new HongsExemption(ex);
            }

            /**
             * UploadHelper
             * 不对路径进行转码
             * 故需自行编码处理
             */
            href = encode(href);
        }

        if (getParam("hash-status", false)) {
            /**
             * 附加上原始名称、文件大小
             * 也可能由 checks 附加信息
             * 如图片的宽高, 影音的时长
             */
            name  = encode( name );
            href += "#n=" + name  ;
            href += "&s=" + size  ;
            if (hp.length > 2) {
                href += "&"+ hp[2];
            }
        }

        return href;
    }

    /**
     * 远程文件下载到本地
     * 返回临时名称, 或者临时文件, 或者包装了的 Part
     * @param href 文件链接
     * @param temp 临时目录
     * @return
     * @throws Wrong
     */
    protected Object stores(String href, String temp) throws Wrong {
        String name = Synt.asString(getParam("__name__"));
        return new DownPart(href , name);
    }

    /**
     * 上传成功后的进一步检查
     * 用于后续处理, 如生成缩略图, 或视频截图等, 返回新的链接和路径
     * @param href 文件链接
     * @param path 文件路径
     * @return
     * @throws Wrong
     */
    protected String[] checks(String href, String path) throws Wrong {
        return new String [] {href , path};
    }

    /**
     * 仅对 URL 的文件名部分进行解码
     * @param name
     * @return
     */
    private String decode(String name) {
        String   path;
        int p  = name.lastIndexOf("/");
        if (p != -1) {
            p +=  1;
            path = name.substring(0,p);
            name = name.substring(  p);
        } else {
            path = "";
        }

        try {
            name = URLDecoder.decode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new HongsExemption(e);
        }

//      name = name.replace("%20","+");
        return path + name;
    }

    /**
     * 仅对 URL 的文件名部分进行编码
     * @param name
     * @return
     */
    private String encode(String name) {
        String   path;
        int p  = name.lastIndexOf("/");
        if (p != -1) {
            p +=  1;
            path = name.substring(0,p);
            name = name.substring(  p);
        } else {
            path = "";
        }

        try {
            name = URLEncoder.encode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new HongsExemption(e);
        }

        name = name.replace("+","%20");
        return path + name;
    }

    private String getUrl(String href) {
        String CURR_SERV_HREF = Core.SERVER_HREF.get();
        String CORE_SERV_PATH = Core.SERVER_PATH.get();

        Map m = new HashMap(3);
        m.put("SERV_HREF", CURR_SERV_HREF);
        m.put("SERV_PATH", CORE_SERV_PATH);
        m.put("BASE_HREF", CURR_SERV_HREF
                         + CORE_SERV_PATH);
        href = Syno.inject(href, m );

        return href;
    }

    private String getDir(String path) {
        Map m = new HashMap(4);
        m.put("BASE_PATH", Core.BASE_PATH);
        m.put("CORE_PATH", Core.CORE_PATH);
        m.put("CONF_PATH", Core.CONF_PATH);
        m.put("DATA_PATH", Core.DATA_PATH);
        path = Syno.inject(path, m );

        // 对相对路径进行补全
        if (! new File(path).isAbsolute()) {
            path = Core.BASE_PATH  + "/" + path;
        }

        return path;
    }

    private static final Pattern HREF_PATT = Pattern.compile("^(https?:)?//.*");
    private static final Pattern NAME_PATT = Pattern.compile( "[\"\\/<>*:?|]" );

}
