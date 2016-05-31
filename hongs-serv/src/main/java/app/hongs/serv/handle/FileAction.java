package app.hongs.serv.handle;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.UploadHelper;
import app.hongs.action.anno.Action;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import app.hongs.util.Tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.awt.Image;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.imageio.ImageIO;

import eu.medsea.mimeutil.MimeUtil;
import eu.medsea.mimeutil.detector.MagicMimeMimeDetector;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;

/**
 * 通用上传动作
 * @author Hongs
 */
@Action("handle/file")
public class FileAction {

    static {
        MimeUtil.registerMimeDetector(MagicMimeMimeDetector.class.getName());
    }
  
    @Action("create")
    public void uploadFile(ActionHelper helper) throws HongsException {
        Map sd = new HashMap();
        Map rd = helper.getRequestData();
        int md = Synt.declare(rd.get (Cnst.MD_KEY), 0);
        Set<String> sk = Synt.declare(helper.getAttribute(Cnst.UPLOAD_ATTR), Set.class);

        if (sk == null) {
            helper.reply(sd);
            return;
        }

        if (md ==  2  ) {
            for(String pn : sk) {
                Object pv = Dict.getParam(rd, pn);
                if (pv == null) {
                    helper.fault(pn + " upload failed");
                    return;
                }
                Dict.setParam(sd, pv, pn);
            }
        } else {
            for(String pn : sk) {
                Object pv = Dict.getParam(rd, pn);
                if (pv == null) {
                    helper.fault(pn + " upload failed");
                    return;
                }
                sd.put(pn , pv);
            }
        }

        helper.reply(sd);
    }

    @Action("image/create")
    public void uploadImage(ActionHelper helper) throws HongsException {
        CoreConfig c = CoreConfig.getInstance();
        String t , e , p , u , v = null;
        t = c.getProperty("fore.upload.image.types", "image/jpeg,image/png,image/gif,image/bmp");
        e = c.getProperty("fore.upload.image.extns", "jpeg,jpg,png,gif,bmp");
        p = c.getProperty("core.upload.image.path" , "upload/image");
        u = c.getProperty("core.upload.image.href" , "upload/image");

        UploadHelper h = new UploadHelper();
        h.setAllowTypes(t.split(","));
        h.setAllowExtns(e.split(","));
        h.setUploadPath(p);
        h.setUploadHref(u);
        h.upload(v);
        u = h.getResultHref();

        // 补全 URL
        if (!u.startsWith("http:") && !u.startsWith("https:")) {
            u = helper.getRequest().getServerName() + Core.BASE_HREF +"/"+ u;
        }

        Map m = new HashMap();
        m.put("id" , v);
        m.put("url", u);
        helper.reply(m);
    }

    @Action("image/retrieve")
    public void outputImage(ActionHelper helper) throws HongsException {
        String id = helper.getParameter("id");
        String  t = helper.getParameter( "t");
        String  s = helper.getParameter( "s");
        int     w = Synt.declare(helper.getParameter("w"), 0);
        int     h = Synt.declare(helper.getParameter("h"), 0);
        String wh = w+"x"+h;

        if (id == null || "".equals(id)) {
            helper.error400("Param id required");
            return;
        }

        CoreConfig c = CoreConfig.getInstance();
        String e , p , v;
        e = c.getProperty("fore.upload.image.extns", "jpeg,jpg,png,gif,bmp");
        p = c.getProperty("core.upload.image.path" , "upload/image");
        v = c.getProperty("core.upload.image.save" , "");

        List<String> es = Arrays.asList(e.split(","));
        List<String> vs = Arrays.asList(v.split(","));
        if (!es.contains(t)) {
            helper.error400("Wrong value '"+ t +"' for param tp, must be "+ e);
            return;
        }
        if (t == null || "".equals(t)) {
            t =  es.get(0);
        }

        // 文件基本路径
        String fp = Core.BASE_PATH +"/"+ p +"/"+ Tool.splitPath(id);

        // 检查原始文件
        File sf = null;
        for (int i = 0, j = es.size(); i < j; i ++) {
            sf = new File(fp +"."+ es.get(i));
            if (sf.exists()) {
                break ;
            }
            if (i == j - 1 ) {
                helper.error400("Image file '"+ t +"' not exists");
                return;
            }
        }

        // 计算图片尺寸
        if (w != 0 || h != 0) {
        if (w == 0 || h == 0) {
            Image img;
            try {
                img = ImageIO.read(sf);
            } catch (IOException ex) {
                throw new HongsException.Common(ex);
            }
            if (w != 0) {
                h  = (int) (((float) w) / img.getWidth (null) * img.getHeight(null));
            } else
            if (h != 0) {
                w  = (int) (((float) h) / img.getHeight(null) * img.getWidth (null));
            }
        }
            fp += "_"+ w +"x"+ h;
        }

        // 检查目标文件
        File df = new File(fp+"."+t);
        if ( df.exists()) {
            long st, dt;
            st = sf.lastModified();
            dt = df.lastModified();
            if (dt > st) {
                String sl, dl;
                SimpleDateFormat sdf = new SimpleDateFormat(
                                "EEE, d MMM yyyy HH:mm:ss z",
                                            Locale.ENGLISH );
                sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                dl = sdf.format(  new  Date(dt));
                sl = helper.getRequest(  ).getHeader("If-Modified-Since");
                if (sl != null && sl.equals(dl)) {
                    helper.getResponse().setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                } else {
                    outputFile(df, helper.getResponse()); // 目标文件存在且未过期则输出.
                    return;
                }
            }
        }

        try {
            Builder<File> bf = Thumbnails.of(sf).size(w,h).outputFormat(t);
            if (!vs.contains(wh)) {
                bf.toOutputStream( helper.getResponse().getOutputStream() );
                return;
            } else {
                bf.toFile  ( df );
            }
        } catch (IOException ex ) {
            throw new HongsException.Common(ex);
        }

        outputFile( df , helper.getResponse() );
    }

    private void outputFile(File df, HttpServletResponse rs) throws HongsException {
        rs.reset();
        rs.setContentLengthLong(df.length());
        rs.setContentType(MimeUtil.getMimeTypes(df).toString());

        try {
             InputStream ips;
            OutputStream ops;
            ips = new FileInputStream(df);
            ops =  rs.getOutputStream(  );
            byte[] b =  new  byte[ 1024 ];
            int    i ;
            while (-1 != (i=ips.read(b))) {
                ops.write(b, 0, i);
            }
            ips.close();
            ops.flush();
            ops.close();
        } catch (FileNotFoundException e) {
            throw new HongsException.Common(e);
        } catch (IOException e) {
            throw new HongsException.Common(e);
        }
    }

}
