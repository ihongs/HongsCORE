package io.github.ihongs.action;

import io.github.ihongs.Core;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.Tool;
import io.github.ihongs.util.verify.Wrong;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.Part;
//import eu.medsea.mimeutil.MimeUtil;
//import eu.medsea.mimeutil.detector.MagicMimeMimeDetector;

/**
 * 文件上传助手
 * @author Hongs
 */
public class UploadHelper {
    private String uploadTemp = "${BASE_PATH}/static/upload/tmp";
    private String uploadPath = "static/upload";
    private String uploadHref = "static/upload";
    private String resultName = null;
    private String requestKey = null;
    private Set<String> allowTypes = null;
    private Set<String> allowExtns = null;

//  static {
//      MimeUtil.registerMimeDetector(MagicMimeMimeDetector.class.getClassName());
//  }

    /**
     * 设置上传临时目录
     * @param path
     * @return
     */
    public UploadHelper setUploadTemp(String path) {
        this.uploadTemp = path;
        return this;
    }

    /**
     * 设置上传目标目录
     * @param path
     * @return
     */
    public UploadHelper setUploadPath(String path) {
        this.uploadPath = path;
        return this;
    }

    /**
     * 设置目标链接前缀
     * @param href
     * @return
     */
    public UploadHelper setUploadHref(String href) {
        this.uploadHref = href;
        return this;
    }

    /**
     * 设置请求参数名称
     * 仅用于 upload(Map, UploadHelper...) 方法中为每个 UploaderHelper 指定请求参数
     * @param name
     * @return
     */
    public UploadHelper setRequestKey(String name) {
        this.requestKey = name;
        return this;
    }

    /**
     * 设置许可的类型(Mime-Type)
     * @param type
     * @return
     */
    public UploadHelper setAllowTypes(String... type) {
        this.allowTypes = new HashSet(Arrays.asList(type));
        return this;
    }

    /**
     * 设置许可的类型(扩展名)
     * @param extn
     * @return
     */
    public UploadHelper setAllowExtns(String... extn) {
        this.allowExtns = new HashSet(Arrays.asList(extn));
        return this;
    }

    private void chkTypeOrExtn(String type, String extn) throws Wrong {
        /**
         * 检查文件类型
         */
        if (this.allowTypes != null
        && !this.allowTypes.contains(type)) {
            // 文件类型不对
            throw new Wrong("fore.form.invalid.types", this.allowTypes.toString());
        }

        /**
         * 检查扩展名
         */
        if (this.allowExtns != null
        && !this.allowExtns.contains(extn)) {
            // 扩展名不对
            throw new Wrong("fore.form.invalid.extns", this.allowExtns.toString());
        }
    }

    private void setResultName(String name, String extn) {
        /**
         * 当文件名含点和斜杠时将作为完整文件名
         * 当文件名以点结尾表示同上但需加扩展名
         * 默认情况下会把文件等分拆解成多级目录
         */
        if (name.endsWith(".")) {
            int l = name.length ( ) - 1 ;
            name  = name.substring(0, l);
        } else
        if (name.contains(".")
        ||  name.contains("/")) {
            extn  = null;
        } else
        {
            name  = Tool.splitPath(name);
        }

        if (extn != null
        &&  extn.length() != 0) {
            name += "." + extn;
        }

        this.resultName = name;
    }

    private String getUploadTemp(String path) {
        Map m = new HashMap();
        m.put("BASE_PATH", Core.BASE_PATH);
        m.put("CORE_PATH", Core.CORE_PATH);
        m.put("CONF_PATH", Core.CONF_PATH);
        m.put("DATA_PATH", Core.DATA_PATH);
        path = Tool.inject(path, m );

        if (! new File(path).isAbsolute()) {
            path = Core.BASE_PATH  + "/" + path;
        }

        return path;
    }

    private String getResultPath(String path) {
        Map m = new HashMap();
        m.put("BASE_PATH", Core.BASE_PATH);
        m.put("CORE_PATH", Core.CORE_PATH);
        m.put("CONF_PATH", Core.CONF_PATH);
        m.put("DATA_PATH", Core.DATA_PATH);
        path = Tool.inject(path, m );

        if (! new File(path).isAbsolute()) {
            path = Core.BASE_PATH  + "/" + path;
        }

        return path;
    }

    private String getResultHref(String href) {
        String host = System.getProperty("host.url");
        if (host == null || host.length()!=0) {
            ActionHelper  help  ;
            help  = /**/  Core  .getInstance  (ActionHelper.class);
            host  = ActionDriver.getSchemeHost(help.getRequest( ));
        }
        String hrel = host+Core.BASE_HREF ;

        Map m = new HashMap();
        m.put("BASE_HREF", Core.BASE_HREF);
        m.put("SERV_HREF", hrel);
        m.put("SERV_HREF", host);
        href = Tool.inject(href, m );
        return href;
    }

    /**
     * 获取完整目标路径
     * @return
     */
    public String getResultPath() {
        String path = this.resultName;
        if (this.uploadPath != null) {
            path = this.uploadPath + "/" + path;
        }
        return getResultPath( path );
    }

    /**
     * 获取完整目标链接
     * @return
     */
    public String getResultHref() {
        String href = this.resultName;
        if (this.uploadHref != null) {
            href = this.uploadHref + "/" + href;
        }
        return getResultHref( href );
    }

    /**
     * 检查文件流并写入目标目录
     * @param xis  上传文件输入流
     * @param type 上传文件类型
     * @param extn 上传文件扩展
     * @param subn 目标文件名称
     * @return
     * @throws Wrong
     */
    public File upload(InputStream xis, String type, String extn, String subn) throws Wrong {
        if (extn.contains( "." )) {
//          extn = MimeUtil.getExtension(extn);
            extn = extn.substring(extn.lastIndexOf('.') + 1);
        }

        chkTypeOrExtn(type, extn);
        setResultName(subn, extn);

        File file = new File(getResultPath());
        File fdir = file.getParentFile();
        if (!fdir.exists()) {
             fdir.mkdirs();
        }

        // 拷贝数据
        try {
            try (
                FileOutputStream     fos = new FileOutputStream    (file);
                BufferedInputStream  bis = new BufferedInputStream (xis );
                BufferedOutputStream bos = new BufferedOutputStream(fos );
            ) {
                byte[] buf = new byte[1024];
                int    ovr ;
                while((ovr = bis.read(buf )) != -1) {
                    bos.write(buf, 0, ovr );
                }
            }
        } catch (IOException ex) {
            throw new Wrong(ex, "fore.form.upload.failed");
        }

        return file;
    }

    /**
     * 检查文件流并写入目标目录
     * @param xis  上传文件输入流
     * @param type 上传文件类型
     * @param extn 上传文件扩展
     * @return
     * @throws Wrong
     */
    public File upload(InputStream xis, String type, String extn) throws Wrong {
        return  upload(xis, type, extn, Core.newIdentity());
    }

    /**
     * 检查上传对象并写入目标目录
     * @param part
     * @param subn
     * @return
     * @throws Wrong
     */
    public File upload(Part part, String subn) throws Wrong {
        if (part == null) {
            setResultName("", null);
            return  null;
        }

        String type = part.getContentType();
        String extn = part.getSubmittedFileName();

        /**
         * 从 Mime-Type 和原始文件名中分解出类型和扩展名
         */
        int pos  = type.indexOf(',');
        if (pos == -1) {
            type = "";
        } else {
            type = type.substring(0 , pos);
        }
        pos  = extn.lastIndexOf('.');
        if (pos == -1) {
            extn = "";
        } else {
            extn = extn.substring(1 + pos);
        }

        try {
            return upload(part.getInputStream(), type, extn, subn);
        }
        catch ( IOException ex) {
            throw new Wrong(ex, "fore.form.upload.failed");
        }
    }

    /**
     * 检查上传对象并写入目标目录
     * @param part
     * @return
     * @throws Wrong
     */
    public File upload(Part part) throws Wrong {
        return  upload(part, Core.newIdentity());
    }

    /**
     * 检查文件对象并写入目标目录
     * 检查当前文件
     * @param file
     * @param subn
     * @return
     * @throws Wrong
     */
    public File upload(File file, String subn) throws Wrong {
        if (file == null) {
            setResultName("", null);
            return  null;
        }

        if (file.exists() == false) {
            throw new Wrong("core.file.upload.not.exists");
        }

        /**
         * 从文件名中解析类型和提取扩展名
         */
//      type = MimeUtil.getMimeTypes (file).toString(   );
//      extn = MimeUtil.getExtension (file);
        FileNameMap nmap = URLConnection.getFileNameMap();
        String      extn = file.getName(  );
        String      type = nmap.getContentTypeFor( extn );
        extn = extn.substring(extn.lastIndexOf('.') + 1 );

        chkTypeOrExtn(type, extn);
        setResultName(subn, extn);

        /**
         * 原始文件与目标文件不同才需移动
         */
        File dist = new File(getResultPath());
        if (!dist.equals(file)) {
            File dirt = dist.getParentFile( );
            if (!dirt.isDirectory()) {
                 dirt.mkdirs( );
            }
            file.renameTo(dist);
        }

        return dist;
    }

    /**
     * 检查文件对象并写入目标目录
     * @param file
     * @return
     * @throws Wrong
     */
    public File upload(File file) throws Wrong {
        return  upload(file, Core.newIdentity());
    }

    /**
     * 检查临时文件或目标链接情况
     * 参数为临时文件名或结果链接
     * @param name
     * @return
     * @throws Wrong
     */
    public File upload(String name) throws Wrong {
        if (name == null || name.length( ) == 0) {
            setResultName("", null);
            return  null;
        }

        String subn = name;
        String extn =  "" ;
        name = name.replace( '\\', '/' ); // 避免 Windows 异常
        int i  = subn.lastIndexOf( '/' );
        if (i != -1) {
            subn = subn.substring(i + 1);
        }
        int j  = subn.lastIndexOf( '.' );
        if (j != -1) {
            extn = subn.substring(j + 1);
            subn = subn.substring(0 , j);
        }

        /*
         * 修改时文件未重新上传则回传原路径
         * 此时传入名称和扩展应得到相同结果
         * 这种情况无需额外的操作
         */
        setResultName(subn, extn);
        String href = getResultHref();
        String path = getResultPath();
        if  (  name.equals (href)) {
            return new File(path);
        }

        /*
         * 不能直接当作文件路径来处理
         * 这会导致严重的安全漏洞
         * 如给的是某重要配置文件路径
         * 则可能导致敏感数据泄露
         */
        if (i != -1) {
            throw new Wrong( "core.file.upload.not.allows" );
//          return upload(new File(subn),Core.getUniqueId());
        }

        /**
         * 尝试移动临时目录的文件
         */
        return upload(
                new File(getUploadTemp(uploadTemp)
                        + File.separator  +  name),
                Core.newIdentity());
    }

    /**
     * 批量处理上传数据
     * @param request
     * @param uploads
     * @throws Wrong
     */
    public static void upload(Map request, UploadHelper... uploads) throws Wrong {
        for(UploadHelper upload : uploads) {
            String n =   upload.requestKey != null? upload.requestKey: "file";
            Object v = Dict.getParam(request, null, n);
            String u ;
            File   f ;

            //** 单个文件 **/

            if(!(v instanceof Collection)
            && !(v instanceof Map)) {
                if (v instanceof Part) {
                    f = upload.upload((Part) v);
                } else
                if (v instanceof File) {
                    f = upload.upload((File) v);
                } else {
                    u = Synt.declare (v, "");
                    f = upload.upload(u);
                }
                if (f != null) {
                    u  = upload.getResultHref();
                    Dict.setParam(request, u,n);
                } else {
                    Dict.setParam(request,"",n);
                }
                continue;
            }

            //** 多个文件 **/

            List s = new ArrayList();
            List a = Synt.asList(v );
            n = n.replaceFirst("(\\.|\\[\\])$", "");

            for (Object x : a) {
                if (x instanceof Part) {
                    f = upload.upload((Part) x);
                } else
                if (x instanceof File) {
                    f = upload.upload((File) x);
                } else {
                    u = Synt.declare (x, "");
                    f = upload.upload(u);
                }
                if (f != null) {
                    s.add(upload.getResultHref());
                }
            }

            Dict.setParam(request, s, n );
        }
    }

}
