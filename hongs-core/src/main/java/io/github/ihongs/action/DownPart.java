package io.github.ihongs.action;

import io.github.ihongs.util.Synt;
import io.github.ihongs.util.verify.Wrong;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
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
    private final String href;
    private final String name;

    public DownPart (String href, String name) throws Wrong {
        // 允许不加 http 或 https
        if (href.startsWith("/")) {
            href = "http:"+ href;
        }

        try {
            this. name = name;
            this. href = href;
            this. conn = new URL(href).openConnection();
        } catch (MalformedURLException ex) {
            throw new Wrong( ex, "core.file.url.has.error", href );
        } catch (IOException ex) {
            throw new Wrong( ex, "core.file.url.get.error", href );
        }
    }

    public DownPart (String href) throws Wrong {
        this(href, null);
    }

    public String getHref() {
        return this.href;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public  long  getSize() {
        String size = this.conn.getHeaderField("Content-Length");
        return Synt.declare(size, 0L);
    }

    @Override
    public String getContentType() {
        String type = this.conn.getHeaderField("Content-Type"  );
        if (type != null) {
            Pattern pat;
            Matcher mat;
            pat = Pattern.compile("^[^/]+/[^/; ]+");
            mat = pat.matcher(type);
            if (mat.find()) {
                return mat.group(0);
            }
        }
        type = this.href.replaceAll("[\\?#].*", "").replaceAll(".*/", "");
        return URLConnection.getFileNameMap( ).getContentTypeFor ( type );
    }

    @Override
    public String getSubmittedFileName() {
        String name = this.conn.getHeaderField("Content-Disposition");
        if (name != null) {
            Pattern pat;
            Matcher mat;
            pat = Pattern.compile("filename=\"(.*?)\"");
            mat = pat.matcher(name);
            if (mat.find()) {
                return mat.group(1);
            }
        }
        return this.href.replaceAll("[\\?#].*", "").replaceAll(".*/", "");
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.conn.getInputStream();
    }

    @Override
    public void write(String path) throws IOException {
        File file = new File(path);
        File fdir = file.getParentFile();
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

}
