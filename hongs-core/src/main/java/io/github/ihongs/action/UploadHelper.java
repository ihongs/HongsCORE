package io.github.ihongs.action;

import io.github.ihongs.Core;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.verify.Wrong;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.Part;

/**
 * 文件上传助手
 *
 * <h3>异常代码</h3>
 * <pre>
 * 代码区间 1120~1124
 * 1120=缺少命名摘要算法
 * 1121=命名摘要处理失败
 * </pre>
 *
 * @author Hongs
 */
public class UploadHelper {
    private String uploadTemp = "static/upload/tmp";
    private String uploadPath = "static/upload";
    private String uploadHref = "static/upload";
    private String digestType = null;
    private String resultName = null;
    private String requestKey = null;
    private Set <String> accept = null ;
    private Set <String> reject = null ;

    private static final char[] DIGITS = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

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
     * 设置命名摘要算法, 如: MD5, SHA-1, SHA-256
     * 注意, 仅适用于未指定文件名的上传, 如 upload(Part),upload(File)
     * @param type
     * @return
     */
    public UploadHelper setDigestType(String type) {
        this.digestType = type;
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
     * 设置许可的类型
     * 可以使用扩展名, 如 .png
     * 也可用MimeType, 如 image/png 或 image/*
     * @param types
     * @return
     */
    public UploadHelper setAccept(String  ...  types) {
        return setAccept(new HashSet(Arrays.asList(types)));
    }
    public UploadHelper setAccept(Set <String> types) {
        this.accept = types;
        return this;
    }

    /**
     * 设置拒绝的类型
     * 可以使用扩展名, 如 .png
     * 也可用MimeType, 如 image/png 或 image/*
     * @param types
     * @return
     */
    public UploadHelper setReject(String  ...  types) {
        return setReject(new HashSet(Arrays.asList(types)));
    }
    public UploadHelper setReject(Set <String> types) {
        this.reject = types;
        return this;
    }

    private void chkTypeOrKind(String type, String kind) throws Wrong {
        /**
         * 通配类型, 如 image/*
         */
        String  typa = null;
        if (type != null
        && (this.accept != null
        ||  this.reject != null ) )  {
            int gash = type.indexOf  (  "/"  );
            if (gash > 0) {
                typa = type.substring(0, gash) + "/*";
            }
        }

        /**
         * 许可类型
         */
        if (this.accept != null
        && !this.accept.contains(kind)
        && !this.accept.contains(type)
        && !this.accept.contains(typa) ) {
            throw new Wrong("@core.file.type.invalid", Syno.concat(",", this.accept));
        }

        /**
         * 禁止类型
         */
        if (this.reject != null
        && (this.reject.contains(kind)
        ||  this.reject.contains(type)
        ||  this.reject.contains(typa))) {
            throw new Wrong("@core.file.type.illegal", Syno.concat(",", this.reject));
        }
    }

    private void setResultName(String name, String kind) {
        /**
         * 当文件名以点结尾表示同上但需加扩展名
         * 当文件名含点和斜杠时将作为完整文件名
         * 默认情况下会把文件等分拆分成多级目录
         * 拆分后加上服务器 ID 规避同步同名问题
         */
        if (name.endsWith(".") == true ) {
            int l = name.length ( ) - 1 ;
            name  = name.substring(0, l);
        } else
        if (name.contains(".") == false
        &&  name.contains("/") == false) {
            name  = Syno.splitPath(name);
            name  = Core.SERVER_ID +"/"+ name;
        } else
        {
            kind  = null;
        }

        if (kind != null && ! kind.isEmpty()) {
            name += kind;
        }

        this.resultName = name;
    }

    private String getUploadTemp(String path) {
        Map m = new HashMap(4);
        m.put("BASE_PATH", Core.BASE_PATH);
        m.put("CORE_PATH", Core.CORE_PATH);
        m.put("CONF_PATH", Core.CONF_PATH);
        m.put("DATA_PATH", Core.DATA_PATH);
        path = Syno.inject(path, m );

        if (! new File(path).isAbsolute()) {
            path = Core.BASE_PATH  + "/" + path;
        }

        return path;
    }

    private String getResultPath(String path) {
        Map m = new HashMap(4);
        m.put("BASE_PATH", Core.BASE_PATH);
        m.put("CORE_PATH", Core.CORE_PATH);
        m.put("CONF_PATH", Core.CONF_PATH);
        m.put("DATA_PATH", Core.DATA_PATH);
        path = Syno.inject(path, m );

        if (! new File(path).isAbsolute()) {
            path = Core.BASE_PATH  + "/" + path;
        }

        return path;
    }

    private String getResultHref(String href) {
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

    private String getTypeByName(String name) {
        return URLConnection.getFileNameMap()
                     .getContentTypeFor(name);
    }

    private String getTypeByMime(String type) {
        int pos  = type. indexOf ( ';' );
        if (pos >= 1 ) {
            return type.substring(0,pos);
        } else {
            return type;
        }
    }

    private String getKindByName(String name) {
        int pos  = name.lastIndexOf('/');
        if (pos >= 0 ) {
            name = name.substring(1+pos);
        }
            pos  = name.lastIndexOf('.');
        if (pos >= 1 ) {
            return name.substring(0+pos);
        } else {
            return  "" ;
        }
    }

    private String getDigestName(File file) {
        if (digestType == null) {
            return Core.newIdentity();
        }

        // 摘要计算
        byte[] a ;
        long   l = file.length();
        try (
            FileInputStream  in = new FileInputStream(file);
            FileChannel      fc = in.getChannel();
        ) {
            MappedByteBuffer bb = fc.map (FileChannel.MapMode.READ_ONLY, 0, l);
            MessageDigest m ;
            m = MessageDigest.getInstance(digestType);
                m.update(bb);
            a = m.digest(  );
        }
        catch (NoSuchAlgorithmException e) {
            throw new CruxExemption(e, 1120);
        }
        catch (IOException e) {
            throw new CruxExemption(e, 1121);
        }

        // 转为 16 进制
        int  i = 0 ;
        int  j = a . length;
        StringBuilder s = new StringBuilder(2 * j);
        for (  ; i < j; i ++  ) {
            byte b = a[ i ];
            char y = DIGITS[b       & 0xf];
            char x = DIGITS[b >>> 4 & 0xf];
            s.append(x);
            s.append(y);
        }
        return s.toString();
    }

    private File getDigestFile(File file) {
        String name = file. getName();
        String kind = getKindByName(name);
        String dige = getDigestName(file);

        setResultName ( dige , kind );
        String path = getResultPath();

        // 移动文件
        File dist = new  File  (  path  );
        File dirt = dist.getParentFile( );
        if (!dirt.isDirectory()) {
             dirt.mkdirs( );
        }
        file.renameTo(dist);

        return dist;
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
     * @param type 上传文件类型, 如 image/png
     * @param kind 上传文件扩展, 如 .png
     * @param lead 目标文件名称
     * @return
     * @throws Wrong
     */
    public File upload(InputStream xis, String type, String kind, String lead) throws Wrong {
        int p  = type.indexOf(';');
        if (p != -1) {
            type = type.substring(0 , p);
        }
        p  = kind.lastIndexOf('.');
        if (p != -1) {
            kind = kind.substring(0 + p);
        }

        chkTypeOrKind(type, kind);
        setResultName(lead, kind);

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
            throw new Wrong(ex, "@fore.file.upload.failed");
        }

        return file;
    }

    /**
     * 检查文件流并写入目标目录
     * @param xis  上传文件输入流
     * @param type 上传文件类型, 如 image/png
     * @param kind 上传文件扩展, 如 .png
     * @return
     * @throws Wrong
     */
    public File upload(InputStream xis, String type, String kind) throws Wrong {
        if (digestType == null) {
            return upload(xis, type, kind, Core.newIdentity());
        }

        File tmp = upload(xis, type, kind, Core.newIdentity()+".tmp.");

        return getDigestFile(tmp);
    }

    /**
     * 检查上传对象并写入目标目录
     * @param part
     * @param lead
     * @return
     * @throws Wrong
     */
    public File upload(Part part, String lead) throws Wrong {
        if (part == null) {
            setResultName("", null);
            return  null;
        }

        /**
         * 从上传项中获取类型并提取扩展名
         */
        String type = part.getContentType( /**/ );
               type = getTypeByMime( type );
        String kind = part.getSubmittedFileName();
               kind = getKindByName( kind );

        try {
            return upload(part.getInputStream(), type, kind, lead);
        }
        catch ( IOException ex) {
            throw new Wrong(ex, "@fore.file.upload.failed");
        }
    }

    /**
     * 检查上传对象并写入目标目录
     * @param part
     * @return
     * @throws Wrong
     */
    public File upload(Part part) throws Wrong {
        if (digestType == null) {
            return upload(part, Core.newIdentity());
        }

        File tmp = upload(part, Core.newIdentity()+".tmp.");

        return getDigestFile(tmp);
    }

    /**
     * 检查文件对象并写入目标目录
     *
     * 文件在临时目录里则移动过去
     * 文件不在上传目录则复制过去
     *
     * @param file
     * @param lead
     * @return
     * @throws Wrong
     */
    public File upload(File file, String lead) throws Wrong {
        if (file == null) {
            setResultName("", null);
            return  null;
        }

        if (file.exists() == false) {
            throw new Wrong("@core.file.upload.not.exists");
        }

        /**
         * 从文件名中解析类型和提取扩展名
         */
        String name = file. getName();
        String type = getTypeByName(name);
        String kind = getKindByName(name);

        chkTypeOrKind(type, kind);
        setResultName(lead, kind);

        try {
            Path srce = file.toPath().toAbsolutePath();
            Path dist = Paths.get(getResultPath()).toAbsolutePath();
            Path temp = Paths.get(getUploadTemp(uploadTemp)).toAbsolutePath();
            if ( srce.startsWith (temp) ) {
                File  dir = dist.getParent( ).toFile();
                if (! dir.exists ()) {
                      dir.mkdirs ();
                }
                Files.move( srce, dist, StandardCopyOption.REPLACE_EXISTING );
            } else {
            Path path = Paths.get(getResultPath(uploadPath)).toAbsolutePath();
            if (!srce.startsWith (path) ) {
                File  dir = dist.getParent( ).toFile();
                if (! dir.exists ()) {
                      dir.mkdirs ();
                }
                Files.copy( srce, dist, StandardCopyOption.REPLACE_EXISTING );
            }}
            return dist.toFile();
        } catch (IOException ex) {
            throw new Wrong( ex , "@core.file.upload.failed");
        }
    }

    /**
     * 检查文件对象并写入目标目录
     * @param file
     * @return
     * @throws Wrong
     */
    public File upload(File file) throws Wrong {
        if (digestType == null) {
            return upload(file, Core.newIdentity());
        }

        File tmp = upload(file, Core.newIdentity()+".tmp.");

        return getDigestFile(tmp);
    }

    /**
     * 检查临时文件或目标链接情况
     * 参数为临时文件名或结果链接
     * 注意: 如果有 URL encode 务必先 decode
     * @param name
     * @return
     * @throws Wrong
     */
    public File upload(String name) throws Wrong {
        if (name == null || name.length( ) == 0) {
            setResultName("", null);
            return  null;
        }

        name = name.replace('\\', '/'); // 避免 Windows 异常

        /**
         * 值与目标网址的前导路径如果一致,
         * 视为修改表单且没有重新上传文件;
         * 但需规避用相对路径访问敏感文件,
         * 目录不得以点结尾, 如 ./ 和 ../
         *
         * 反之将其视为传递来的临时文件名,
         * 此时不得含斜杠从而规避同上问题.
         *
         * 问: 为何此处不支持非 uploadHerf 或 uploadPath 之外的本服务器路径?
         * 答: 虽然内部能够读到, 但也可能会对文件访问设置过滤器进行权限校验;
         * 故, 为保安全, 只能处理 uploadHref,uploadPath,uploadTemp 三个位置.
         */

        String href = getResultHref(uploadHref) + "/";
        String path = getResultPath(uploadPath) + "/";
        String lead ;

        if (name.startsWith(href)) {
            lead = name.substring(href.length());
            if (lead.contains("./" )) {
                throw new Wrong("@core.file.upload.not.allows");
            }
            name = path + lead;
            return upload(new File(name), lead );
        }

        if (name.startsWith(path)) {
            lead = name.substring(path.length());
            if (lead.contains("./" )) {
                throw new Wrong("@core.file.upload.not.allows");
            }
        //  name = path + lead;
            return upload(new File(name), lead );
        }

        /* 临时文件 */

        String temp = getUploadTemp(uploadTemp) + "/";

        {
            if (name.contains("./" )) {
                throw new Wrong("@core.file.upload.not.allows");
            }
            name = temp + name;
            return upload(new File(name));
        }
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

            List a = Synt . asList(v);
            List s = new ArrayList(a.size());
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
