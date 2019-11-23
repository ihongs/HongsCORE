package io.github.ihongs.util.verify;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.UploadHelper;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.Part;

/**
 * 文件校验
 * <pre>
 * 规则参数:
 *  pass-source yes|no 是否跳过资源链接(存在有"/"的字符)
 *  pass-remote yes|no 是否跳过远程链接(开头为"http://")
 *  down-remote yes|no 是否下载远程文件
 *  drop-origin yes|no 抛弃原始文件, 仅使用 checks 中新创建的
 *  keep-origin yes|no 返回原始路径, 不理会 checks 中新创建的
 *  keep-naming yes|no 保持原文件名, 会对网址末尾的文件名编码
 *  temp 上传临时目录, 可用变量 $DATA_PATH, $BASE_PATH 等
 *  path 上传目标目录, 可用变量 $BASE_PATH, $DATA_PATH 等
 *  href 上传文件链接, 可用变量 $BASE_HREF, $SERV_HREF 等, 后者包含域名
 *  type 文件类型限制, 逗号分隔 (Mime-Type)
 *  extn 扩展名称限制, 逗号分隔
 * </pre>
 * @author Hongs
 */
public class IsFile extends Rule {
    @Override
    public Object verify(Value watch) throws Wrong {
        // 跳过空值和空串
        Object value = watch.get();
        if (value  ==  null ) {
            return STAND;
        }
        if (value.equals("")) {
            return STAND;
        }

        if (value instanceof String) {

        // 跳过资源路径
        if (Synt.declare(getParam("pass-source"), false)) {
            String u = value.toString( );
            if (u.indexOf( '/' ) != -1 ) {
                return value;
            }
        }

        // 跳过远程地址
        if (Synt.declare(getParam("pass-remote"), false)) {
            String u = value.toString( );
            if (u.matches("^\\w+://.*")) {
                return value;
            }
        }

        // 下载远程文件
        if (Synt.declare(getParam("down-remote"), false)) {
            String u = value.toString( );
            if (u.matches("^\\w+://.*")) {
            do {
                String x;
                // 如果是本地路径则不再下载
                x = Core.SITE_HREF + "/";
                if (x != null && !"".equals(x)) {
                    if (u.startsWith(x)) {
                        value = u;
                        break ;
                    }
                }
                x = (String) getParam("href");
                if (x != null && !"".equals(x)) {
                    if (u.startsWith(x)) {
                        value = u;
                        break ;
                    }
                }
                // 如果有临时目录则下载到这
                x = (String) getParam("temp");
                if (x == null ||  "".equals(x)) {
                    x = Core.BASE_PATH + "/static/upload/tmp";
                }
                value = stores(value.toString(), x);
            } while(false);
            }
        }

        } // End If

        UploadHelper hlpr = new UploadHelper();
        String href;
        String path;
        Object para;

        para = getParam("temp");
        if (para != null && !"".equals(para)) hlpr.setUploadTemp(Synt.declare(para, String.class));
        para = getParam("path");
        if (para != null && !"".equals(para)) hlpr.setUploadPath(Synt.declare(para, String.class));
        para = getParam("href");
        if (para != null && !"".equals(para)) hlpr.setUploadHref(Synt.declare(para, String.class));
        para = getParam("type");
        if (para != null && !"".equals(para)) hlpr.setAllowTypes(Synt.toArray(para, String.class));
        para = getParam("extn");
        if (para != null && !"".equals(para)) hlpr.setAllowExtns(Synt.toArray(para, String.class));

        if (value instanceof Part) {
            Part part =(Part) value;
            hlpr.upload(part, bename(part.getSubmittedFileName()));
        } else
        if (value instanceof File) {
            File file =(File) value;
            hlpr.upload(file, bename(file.getName()));
        } else
        {
            // 外部记录的是网址, 必须进行解码才行
            href = value.toString();
            if (getParam("keep-naming", false)
            &&  href.contains("/")) {
                href = decode(href);
            }
            hlpr.upload(href);
        }

        path = hlpr.getResultPath();
        href = hlpr.getResultHref();

        /**
         * UploadHelper
         * 不对路径进行转码
         * 故需自行编码处理
         */
        if (getParam("keep-naming", false)) {
            href = encode(href);
        }

        /**
         * 检查新上传的文件
         * 可以返回原始路径
         * 或者抛弃原始文件
         */
        if ( ! href.equals(value) ) {
            String  x = checks(href , path);
            if ( ! href.equals(x) ) {
                if (Synt.declare(getParam("keep-origin"), false)) {
                    // Keep to return origin href
                } else
                if (Synt.declare(getParam("drop-origin"), false)) {
                    new File(path).delete();
                    href = x;
                } else {
                    href = x;
                }
            }
        }

        return href;
    }

    /**
     * 远程文件预先下载到本地
     * 返回临时名称
     * @param href 文件链接
     * @param temp 临时目录
     * @return
     * @throws Wrong
     */
    protected String stores(String href, String temp) throws Wrong {
        URL url = null;
        try {
            url = new URL(href);
        } catch (MalformedURLException ex) {
            throw new Wrong(ex, "file.url.has.error", href);
        }

           URLConnection cnn ;
             InputStream ins = null;
        FileOutputStream out = null;
        try {
            cnn = url.openConnection( );
            ins = cnn.getInputStream( );

            // 从响应头中获取到名称
            String name;
            do {
                Pattern pat;
                Matcher mat;

                name = cnn.getHeaderField("Content-Disposition");
                if (name != null) {
                    pat = Pattern.compile("filename=\"(.*?)\"" );
                    mat = pat.matcher(name);
                    if (mat.find()) {
                        name = mat.group(1);
                        break;
                    }
                }

                name = cnn.getHeaderField("Content-Type");
                if (name != null) {
                    pat = Pattern.compile( "^\\w+/\\w+" );
                    mat = pat.matcher(name);
                    if (mat.find()) {
                        name = mat.group(0).replace( "/" , "." );
                        break;
                    }
                }

                name = cnn.getURL().getPath().replaceAll("[\\?#].*", "");
            }
            while (false);

            // 重组名称避免无法存储
            int i  = name.lastIndexOf( '/' );
            if (i != -1) {
                name = name.substring(i + 1);
            }
            int j  = name.lastIndexOf( '.' );
            if (j != -1) {
                String extn;
                extn = name.substring(j + 1);
                name = name.substring(0 , j);

                // 检查扩展名
                CoreConfig c = CoreConfig.getInstance( "default" );
                String d = c.getProperty("fore.upload.deny.extns");
                if (Synt.toTerms(d).contains(extn)) {
                    throw new Wrong("fore.form.upload.failed");
                }

                name = URLEncoder.encode(name, "UTF-8")
                     + "."
                     + URLEncoder.encode(extn, "UTF-8");
            } else {
                name = URLEncoder.encode(name, "UTF-8");
            }
                name = Core.newIdentity () + "!" + name; // 加上编号避免重名

            // 检查目录避免写入失败
            File file = new File(temp + File.separator + name);
            File fdir = file.getParentFile();
            if (!fdir.exists()) {
                 fdir.mkdirs();
            }

            // 将上传的存入临时文件
            out  = new FileOutputStream( file );
            byte[] buf = new byte[1204];
            int    ovr ;
            while((ovr = ins.read(buf )) != -1) {
                out.write(buf, 0, ovr );
            }

            return name;
        } catch (IOException ex) {
            throw new Wrong( ex, "core.file.can.not.fetch", href, temp);
        } finally {
            if (out != null) {
                try {
                    out.close( );
                } catch (IOException ex) {
                    throw new Wrong( ex, "core.file.can.not.close", temp);
                }
            }
            if (ins != null) {
                try {
                    ins.close( );
                } catch (IOException ex) {
                    throw new Wrong( ex, "core.file.can.not.close", href);
                }
            }
        }
    }

    /**
     * 上传成功后的进一步检查
     * 用于后续处理, 如生成缩略图, 或视频截图等
     * @param href 文件链接
     * @param path 文件路径
     * @return
     * @throws Wrong
     */
    protected String checks(String href, String path) throws Wrong {
        return href;
    }

    private static final Pattern NAME_PATT = Pattern.compile("[\\/<>:?*\"|]");

    /**
     * 命名文件, 规避存储路径重合
     * @param name
     * @return
     * @throws Wrong
     */
    private String bename(String name) throws Wrong {
        if (getParam("keep-naming", false)) {
            if (255 < name.getBytes(  ).length) {
                throw new Wrong("fore.file.name.toolong", name);
            }
            if (NAME_PATT.matcher(name).find()) {
                throw new Wrong("fore.file.name.illegal", name);
            }
            return Syno.splitPn36(
                   Core.newIdentity()
                   ) + "/" + name;
        } else {
            return Core.newIdentity();
        }
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
}
