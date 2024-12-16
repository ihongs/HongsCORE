package io.github.ihongs.action;

import io.github.ihongs.util.Synt;
import io.github.ihongs.util.verify.Wrong;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.net.URLConnection;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.Part;

/**
 * 反向下载 Part
 * @author Hongs
 */
public class DownPart implements Part {

    private final URLConnection conn;
    private final String  href;
    private String name = null;
    private String file = null;
    private String type = null;
    private  long  size = -1L ;

    /**
     * 反向下载 Part
     * @param href
     * @param httpOnly true 仅支持 http 和 https
     * @throws io.github.ihongs.util.verify.Wrong
     */
    public DownPart (String href, boolean httpOnly) throws Wrong {
        // 允许不加 http 或 https
        if (href.startsWith("//")) {
            href = "http:"+ href;
        } else
        if (href.startsWith("/" )) {
            href = "file:"+ href;
        } else
        if (href.matches("^[a-zA-Z]:")) {
            href = "file:"+ href.replaceAll("\\\\", "/"); // DOS/Windows
        }

        try {
            URL url = new URI(href).toURL();
            String pro =  url.getProtocol();
            if (httpOnly
            && !pro.equalsIgnoreCase("https")
            && !pro.equalsIgnoreCase("http")) {
                throw new Wrong("@core.file.url.not.allow", href);
            }

            this. href = href;
            this. conn = url.openConnection();
        } catch (URISyntaxException|MalformedURLException e) {
            throw new Wrong( e, "@core.file.url.has.error", href);
        } catch (IOException e) {
            throw new Wrong( e, "@core.file.url.get.error", href);
        }
    }

    /**
     * 反向下载 Part, 仅支持 http 和 https
     * @param href
     * @throws io.github.ihongs.util.verify.Wrong
     */
    public DownPart (String href) throws Wrong {
        this(href, true);
    }

    /**
     * 字段名
     * @param  name
     * @return this
     */
    public DownPart name(String name) {
        this.name = name;
        return this;
    }

    /**
     * 文件名
     * @param  name
     * @return this
     */
    public DownPart file(String name) {
        this.file = name;
        return this;
    }

    /**
     * 类型
     * @param  type
     * @return this
     */
    public DownPart type(String type) {
        this.type = type;
        return this;
    }

    /**
     * 大小
     * @param  size
     * @return this
     */
    public DownPart size( long  size) {
        this.size = size;
        return this;
    }

    public String getHref() {
        return this.href;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getSubmittedFileName() {
        if (file != null) return file;
        String fil2 = this.conn.getHeaderField("Content-Disposition");
        if (fil2 != null) {
            Pattern pat;
            Matcher mat;
            pat = Pattern.compile("filename=\"(.*?)\"");
            mat = pat.matcher(fil2);
            if (mat.find()) {
                return mat.group(1);
            }
        }
        return this.href.replaceAll("[\\?#].*", "").replaceAll(".*/", "");
    }

    @Override
    public String getContentType() {
        if (type != null) return type;
        String typ2 = this.conn.getHeaderField("Content-Type"  );
        if (typ2 != null) {
            Pattern pat;
            Matcher mat;
            pat = Pattern.compile("^[^/]+/[^/; ]+");
            mat = pat.matcher(typ2);
            if (mat.find()) {
                return mat.group(0);
            }
        }
        typ2 = this.href.replaceAll("[\\?#].*", "").replaceAll(".*/", "");
        return URLConnection.getFileNameMap( ).getContentTypeFor ( typ2 );
    }

    @Override
    public  long  getSize() {
        if (size != -1L ) return size;
        String size = this.conn.getHeaderField("Content-Length");
        return Synt.declare(size, 0L);
    }

    @Override
    public String getHeader(String name) {
        return this.conn.getHeaderField (name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return this.conn.getHeaderFields( ).get(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return this.conn.getHeaderFields( ).keySet( );
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.conn.getInputStream();
    }

    @Override
    public void write(String path) throws IOException {
        File file = new File(path);
        File fdir = file.getParentFile( );
        if (!fdir.exists()) {
             fdir.mkdirs();
        }

        try (
            FileOutputStream out = new FileOutputStream(file);
                 InputStream ins = this.conn.getInputStream();
        ) {
            byte[] buf = new byte[1024];
            int    ovr ;
            while((ovr = ins.read(buf )) != -1) {
                out.write(buf, 0, ovr );
            }
        }
    }

    @Override
    public void delete() throws IOException {
        // Nothing to do
    }

}
