package app.hongs.util.verify;

import app.hongs.Core;
import app.hongs.action.UploadHelper;
import app.hongs.util.Synt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.net.MalformedURLException;
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
 *  temp 上传临时目录, 可用变量 $DATA_PATH, $BASE_PATH 等
 *  path 上传目标目录, 可用变量 $BASE_PATH, $DATA_PATH 等
 *  href 上传文件链接, 可用变量 $BASE_HREF, $FULL_HREF 等, 后者带域名前缀
 *  type 文件类型限制, 逗号分隔 (Mime-Type)
 *  extn 扩展名称限制, 逗号分隔
 * </pre>
 * @author Hongs
 */
public class IsFile extends Rule {
    @Override
    public Object verify(Object value) throws Wrong {
        if (value == null || "".equals(value)) {
            return   null; // 允许为空
        }

        if (value instanceof String) {

        // 跳过资源路径
        if (Synt.declare(params.get("pass-source"), false)) {
            String u = value.toString( );
            if (u.indexOf( '/' ) != -1 ) {
                return value;
            }
        }

        // 跳过远程地址
        if (Synt.declare(params.get("pass-remote"), false)) {
            String u = value.toString( );
            if (u.matches("^\\w+://.*")) {
                return value;
            }
        }

        // 下载远程文件
        if (Synt.declare(params.get("down-remote"), false)) {
            String u = value.toString( );
            if (u.matches("^\\w+://.*")) {
            do {
                String x;
                // 如果是本地路径则不再下载
                x = (String) params.get("href");
                if (x != null && !"".equals(x)) {
                    if (u.startsWith(x)) {
                        value = u;
                        break ;
                    }
                }
                // 如果有临时目录则下载到这
                x = (String) params.get("temp");
                if (x == null ||  "".equals(x)) {
                    x = Core.DATA_PATH + File.separator + "tmp";
                }
                value = stores(value.toString(), x);
            } while(false);
            }
        }

        } // End If

        UploadHelper hlpr = new UploadHelper();
        String href, path;
        String   x;
        String[] y;

        x = (String) params.get("temp");
        if (x != null) hlpr.setUploadTemp(x);
        x = (String) params.get("path");
        if (x != null) hlpr.setUploadPath(x);
        x = (String) params.get("href");
        if (x != null) hlpr.setUploadHref(x);
        y = (String[]) Synt.toArray(params.get("type"));
        if (y != null) hlpr.setAllowTypes(y);
        y = (String[]) Synt.toArray(params.get("extn"));
        if (y != null) hlpr.setAllowExtns(y);

        if (value instanceof Part) {
            hlpr.upload((Part) value);
        } else
        if (value instanceof File) {
            hlpr.upload((File) value);
        } else {
            hlpr.upload(value.toString());
        }

        href = hlpr.getResultHref();
        path = hlpr.getResultPath();

        /**
         * 检查新上传的文件
         * 可以返回原始路径
         * 或者抛弃原始文件
         */
        if ( ! href.equals(value) ) {
            x = checks(href , path);
            if ( ! href.equals(x) ) {
                if (Synt.declare(params.get("keep-origin"), false)) {
                    // Keep to return origin href
                } else
                if (Synt.declare(params.get("drop-origin"), false)) {
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
            String name = cnn.getHeaderField("Content-Disposition");
            Pattern pat = Pattern.compile("filename=\"(.*?)\"");
            Matcher mat = pat.matcher( name );
            if (mat.find()) {
                name = mat.group(1);
            } else {
                name = cnn.getURL().getPath();
            }

            // 重组名称避免无法存储
            int i  = name.lastIndexOf( '/' );
            if (i != -1) {
                name = name.substring(i + 1);
            }
            int j  = name.lastIndexOf( '.' );
            if (j != -1) {
                name = URLEncoder.encode(name.substring(i , j), "UTF-8")
                     + "."
                     + URLEncoder.encode(name.substring(j + 1), "UTF-8");
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

}
