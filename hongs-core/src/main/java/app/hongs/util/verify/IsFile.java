package app.hongs.util.verify;

import app.hongs.Core;
import app.hongs.action.UploadHelper;
import app.hongs.util.Synt;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件校验
 * <pre>
 * 规则参数:
 *  pass-remote yes|no 是否跳过远程文件
 *  down-remote yes|no 是否下载远程文件
 *  drop-origin yes|no 抛弃原始文件, 此参数在本类中没有用上, 其他文件转换中可能用到
 *  temp 上传临时目录, 可用变量 $DATA_PATH, $BASE_PATH 等
 *  path 上传目标目录, 可用变量 $BASE_PATH, $DATA_PATH 等
 *  href 上传文件链接, 可用变量 $BASE_HREF, $BASE_LINK 等, 后者带域名前缀
 *  type 文件类型限制(Mime-Type), 逗号分隔
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
                    x  = Core.DATA_PATH + File.separator + "upload" ;
                }
                value = stores(value.toString(), x );
            } while(false);
            }
        }

        String name = Synt.declare(params.get("name"), String.class);
        if (name == null || "".equals(name)) {
            name = Synt.declare(params.get("__name__"), "");
        }

        UploadHelper u = new UploadHelper();
        String x;
        x = (String) params.get("temp");
        if (x != null) u.setUploadTemp(x);
        x = (String) params.get("path");
        if (x != null) u.setUploadPath(x);
        x = (String) params.get("href");
        if (x != null) u.setUploadHref(x);
        x = (String) params.get("type");
        if (x != null) u.setAllowTypes(x.trim().split(","));
        x = (String) params.get("extn");
        if (x != null) u.setAllowExtns(x.trim().split(","));

        u.upload(value.toString());

        // 仅检查新上传的文件
        String y;
        x = u.getResultHref();
        y = u.getResultPath();
        if (! x.equals(value)) {
            x = checks(x , y);
        }

        return x;
    }

    /**
     * 远程文件预先下载到本地
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

            // 获取类型名称
            String type = HttpURLConnection.guessContentTypeFromStream(ins);
            String name = cnn.getHeaderField("Content-Disposition");
            Pattern pat = Pattern.compile   ("filename=\"(.*?)\"" );
            Matcher mat = pat.matcher( name );
            String  fid = Core.getUniqueId( );
            if (mat.find()) {
                name = mat.group(1);
            } else {
                name = cnn.getURL().getFile();
            }

            // 写入文件内容
            out  = new FileOutputStream(temp + File.separator + fid + ".tmp");
            byte[] buf = new byte[1204];
            int    siz = 0;
            int    ovr = 0;
            while((ovr = ins.read(buf )) != -1) {
                out.write(buf, 0, ovr );
                siz += ovr;
            }
            out.close();
            out  = null;
            ins.close();
            ins  = null;

            // 写入文件信息
            out  = new FileOutputStream(temp + File.separator + fid + ".tnp");
            out.write((name + "\r\n" + type + "\r\n" + siz).getBytes());
            out.close();
            out  = null;

            return fid ;
        } catch (IOException ex) {
            throw new Wrong(ex, "file.can.not.fetch", href, temp);
        } finally {
            if (out != null) {
                try {
                    out.close( );
                } catch (IOException ex) {
                    throw new Wrong(ex, "file.can.not.close", temp);
                }
            }
            if (ins != null) {
                try {
                    ins.close( );
                } catch (IOException ex) {
                    throw new Wrong(ex, "file.can.not.close", href);
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
