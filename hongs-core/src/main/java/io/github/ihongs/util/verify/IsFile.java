package io.github.ihongs.util.verify;

import io.github.ihongs.Core;
import io.github.ihongs.CruxExemption;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.Part;

/**
 * 文件校验
 * <pre>
 * 规则参数:
 *  pass-exists yes|no 是否跳过本地路径(本地网络存在的)
 *  pass-remote yes|no 是否跳过远程链接(开头为 http://)
 *  down-remote yes|no 是否下载远程文件
 *  drop-origin yes|no 抛弃原始文件, 仅使用 checks 中新创建的
 *  keep-origin yes|no 返回原始路径, 不理会 checks 中新创建的
 *  keep-naming yes|no 保持原文件名, 会对网址末尾的文件名编码(将废弃,请改用 hash-status)
 *  hash-status yes|no 末尾附加信息, #n=文件名称&s=文件大小等
 *  temp 上传临时目录, 可用变量 ${DATA_PATH}, ${BASE_PATH} 或 ${字段名} ${字段名:split} 等, :split 的将拆分路径
 *  path 上传目标目录, 可用变量 ${BASE_PATH}, ${DATA_PATH} 或 ${字段名} ${字段名:split} 等, :split 的将拆分路径
 *  href 上传文件链接, 可用变量 ${SERV_PATH}, ${SERV_HREF} 或 ${字段名} ${字段名:split} 等, :split 的将拆分路径
 *  size 文件大小限制, 字节单位
 *  accept 类型许可表, 逗号分隔, Mime-Type 或 .extension
 *  reject 类型禁止表, 逗号分隔, Mime-Type 或 .extension
 *  naming 文件名算法, 摘要算法, 如: MD5, SHA-1, SHA-256; 或者保留名称 keep; 也可指定名称 keep:xxx.xxx
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
        Map cleans = watch.getCleans ( );

        if (value instanceof String) {

        // 跳过资源路径, 兼容旧的 "pass-locals"
        if (Synt.declare(getParam("pass-exists"), false)
        ||  Synt.declare(getParam("pass-locals"), false)) {
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
                    x  = getUrl(x, cleans); // 补全
                    if (u.startsWith(x)) {
                        value = u ;
                        break ;
                    }
                }
                // 如果有临时目录则下载到这
                x = (String) getParam ("temp");
                if (x != null && !"".equals(x)) {
                    x  = getDir(x, cleans); // 补全
                } else {
                    x  = Core.BASE_PATH
                       + "/static/upload/tmp" ;
                }
                value  = stores(value.toString(), x);
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
        String lead;
        String name;
        long   size;

        para = getParam("temp");
        if (para != null && !"".equals(para)) hlpr.setUploadTemp(getDir(Synt.asString(para), cleans));
        para = getParam("path");
        if (para != null && !"".equals(para)) hlpr.setUploadPath(getDir(Synt.asString(para), cleans));
        para = getParam("href");
        if (para != null && !"".equals(para)) hlpr.setUploadHref(getUrl(Synt.asString(para), cleans));
        para = getParam("accept");
        if (para != null && !"".equals(para)) hlpr.setAccept(Synt.toSet(para));
        para = getParam("reject");
        if (para != null && !"".equals(para)) hlpr.setReject(Synt.toSet(para));

        // 命名方式, 兼容旧版
        para = Synt.defoult(getParam("naming"), getParam("digest"));
        if (para != null && !"".equals(para)) {
            lead  = Synt.asString (para);
            if (lead.startsWith("keep:")) {
                lead = lead.substring(5);
            } else
            if (lead.equals    ("keep" )) {
                lead =  "" ;
            } else {
                hlpr.setDigestType(lead);
                lead = null;
            }
        } else {
            lead  = null;
        }

        String hash = "";
        if (value instanceof Part ) {
            Part part =(Part) value;
            name = part.getSubmittedFileName();
            size = part.getSize ( );
            if (lead == null) {
                hlpr.upload( part );
            } else
            if (lead.isEmpty()) {
                hlpr.upload( part, name );
            } else
            {
                hlpr.upload( part, lead );
            }
        } else
        if (value instanceof File ) {
            File file =(File) value;
            name = file.getName ( );
            size = file.length  ( );
            if (lead == null) {
                hlpr.upload( file );
            } else
            if (lead.isEmpty()) {
                hlpr.upload( file, name );
            } else
            {
                hlpr.upload( file, lead );
            }
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
            file = hlpr.upload(href, lead);
            size = file.length();
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

        /**
         * 保留原始的文件名
         * 建议 hash-status
         */
        if (Synt.declare(getParam("keep-naming"), false)) {
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
                throw new CruxExemption("Wrong path/href setting");
            }
            catch (IOException ex) {
                throw new CruxExemption(ex);
            }

            /**
             * UploadHelper
             * 不对路径进行转码
             * 故需自行编码处理
             */
            href = encode(href);
        }

        /**
         * 文件名尾追加参数
         * 如原始名称大小等
         */
        if (Synt.declare(getParam("hash-status"), false)) {
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
        return new DownPart(href).name(name);
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
            throw new CruxExemption(e);
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
            throw new CruxExemption(e);
        }

        name = name.replace("+","%20");
        return path + name;
    }

    private String getUrl(String href, Map vals) throws Wrong {
        Map vars = new HashMap(4);
        String CURR_SERV_HREF = Core.SERVER_HREF.get();
        String CORE_SERV_PATH = Core.SERVER_PATH.get();
        vars.put("SERVER_ID", Core.SERVER_ID);
        vars.put("SERV_HREF", CURR_SERV_HREF);
        vars.put("SERV_PATH", CORE_SERV_PATH);
        vars.put("BASE_HREF", CURR_SERV_HREF
                            + CORE_SERV_PATH);

        href = inject (href , vals , vars);

        return href;
    }

    private String getDir(String path, Map vals) throws Wrong {
        Map vars = new HashMap(4);
        vars.put("SERVER_ID", Core.SERVER_ID);
        vars.put("BASE_PATH", Core.BASE_PATH);
        vars.put("CORE_PATH", Core.CORE_PATH);
        vars.put("CONF_PATH", Core.CONF_PATH);
        vars.put("DATA_PATH", Core.DATA_PATH);

        path = inject (path , vals , vars);

        // 对相对路径进行补全
        if (! new File(path).isAbsolute()) {
            path = Core.BASE_PATH  + "/" + path;
        }

        return path;
    }

    private String inject(String path, Map vals, Map vars) throws Wrong {
        Matcher matcher = PATH_PATT.matcher(path);
        StringBuffer sb = new  StringBuffer();
        Object       ob;
        String       st;
        String       sp;

        while  ( matcher.find() ) {
            st = matcher.group(1);

            if (! "$".equals(st)) {
                if (st.startsWith("{")) {
                    st = st.substring(1, st.length() - 1);
                    // 默认值
                    int p  = st.indexOf  (":");
                    if (p != -1) {
                        sp = st.substring(1+p);
                        st = st.substring(0,p);
                    } else {
                        sp = null;
                    }
                } else {
                        sp = null;
                }

                // 取值
                    ob  = vars.get (st);
                if (ob != null) {
                    st  = ob.toString();
                } else {
                    ob  = vals.get (st);
                if (ob != null) {
                    st  = ob.toString();
                } else {
                    throw new Wrong(st + " required for file");
                } // End if vals
                } // End if vars

                // 拆分
                if (sp != null) {
                try {
                    if (sp.equals    ("split" )) {
                        st = Syno.splitPath(st);
                    } else
                    if (sp.startsWith("split,")) {
                        st = Syno.splitPath(st, Synt.declare(sp.substring(6), 3));
                    } else
                    {
                        throw new Wrong(    "Unsupported file code ending :"+ sp);
                    }
                } catch (ClassCastException ex ) {
                        throw new Wrong(ex, "Unsupported file code ending :"+ sp);
                }}
            }

            st = Matcher.quoteReplacement(st);
            matcher.appendReplacement(sb, st);
        }
        matcher.appendTail(sb);

        return sb.toString(  );
    }

    private static final Pattern HREF_PATT = Pattern.compile("^(https?:)?//.*");
    private static final Pattern NAME_PATT = Pattern.compile( "[\\/<>*\":?|]" );
    private static final Pattern PATH_PATT = Pattern.compile("\\$(\\$|\\w+|\\{.+?\\})");

}
