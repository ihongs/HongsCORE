package app.hongs.util.sketch;

import app.hongs.CoreConfig;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import static net.coobird.thumbnailator.geometry.Positions.CENTER;

/**
 *
 * @author Hongs
 */
public class Thumb {

    String pth = null;
    String pre = null;
    String ext = null;

    // 扩展名:宽*高...
    private static final Pattern pat = Pattern.compile("([\\w_]+):(\\d+)\\*(\\d+)");
    // keep:(R,G,B,A),取值0~255,A可选
    private static final Pattern pxt = Pattern.compile(";keep\\((\\d+,\\d+,\\d+(,\\d+)?)\\)", Pattern.CASE_INSENSITIVE);

    /**
     * 设置原始图片路径
     * @param pth
     */
    public void setPath(String pth) {
        this.pth = pth ;
        this.pre = null;
    }

    /**
     * 设置规定输出格式
     * @param ext
     */
    public void setExtn(String ext) {
        this.ext = ext ;
        this.pre = null;
    }

    /**
     * 检查路径和扩展名
     */
    protected final void chkPathAndExtn() {
        if (pth == null) {
            throw new NullPointerException("Must setPath");
        }
        if (ext == null) {
            throw new NullPointerException("Must setExtn");
        }
        if (pre == null) {
            pth = new File(pth).getAbsolutePath();
            pre = pth.replaceFirst("\\.\\w+$","");
        }
    }

    /**
     * 按比例保留
     * @param suf
     * @param w
     * @param h
     * @param b RGBA
     * @return
     * @throws IOException
     */
    public String keep(String suf, int w, int h, Color b) throws IOException {
        chkPathAndExtn();

        Image img = ImageIO.read ( new File(pth) );
        if (null == img) {
            throw new IOException("Can not read image '"+pth+"'");
        }

        int xw = img.getWidth (null);
        int xh = img.getHeight(null);
        int zw = xh * w / h;
        if (zw < xw) {
             h = xw * h / w;
             w = xw; // 宽度优先
        } else {
             w = zw;
             h = xh; // 高度优先
        }

        // 构建目标尺寸的空白图
        // 把当前图片粘贴在中央
        int  l = (w - xw) / 2;
        int  t = (h - xh) / 2;
        BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g =  buf.createGraphics();
        g.setColor (b);
        g.fillRect (0,0, w,h);
        g.drawImage(img, l,t , null);
        g.dispose  ( );

        String dst =  pre  +  suf + "." + ext ;
        Thumbnails.of(buf)
                .size(w,h)
                .outputFormat(ext).toFile(dst);
        return dst;
    }

    /**
     * 按比例截取
     * @param suf
     * @param w
     * @param h
     * @return
     * @throws IOException
     */
    public String pick(String suf, int w, int h) throws IOException {
        chkPathAndExtn();

        Image img = ImageIO.read ( new File(pth) );
        if (null == img) {
            throw new IOException("Can not read image '"+pth+"'");
        }

        int xw = img.getWidth (null);
        int xh = img.getHeight(null);
        int zw = xh * w / h;
        if (zw > xw) {
             h = xw * h / w;
             w = xw; // 宽度优先
        } else {
             w = zw;
             h = xh; // 高度优先
        }

        String dst =  pre  +  suf + "." + ext ;
        Thumbnails.of(pth)
                .sourceRegion(CENTER , w , h )
                .size(w,h)
                .outputFormat(ext).toFile(dst);
        return dst;
    }

    /**
     * 按尺寸缩放
     * @param suf
     * @param w
     * @param h
     * @return
     * @throws IOException
     */
    public String zoom(String suf, int w, int h) throws IOException {
        chkPathAndExtn();

        String dst =  pre  +  suf + "." + ext ;
        Thumbnails.of(pth)
                .size(w,h)
                .outputFormat(ext).toFile(dst);
        return dst;
    }

    /**
     * 仅转换格式
     * @param suf
     * @return
     * @throws IOException
     */
    public String conv(String suf) throws IOException {
        chkPathAndExtn();

        String dst =  pre  +  suf + "." + ext ;
        Thumbnails.of(pth)
                .outputFormat(ext).toFile(dst);
        return dst;
    }

    /**
     * 生成缩略图
     * @param pth 原始图片路径
     * @param url 原始图片链接
     * @return 缩略图路径,链接
     * @throws IOException
     */
    public static String[][] toThumbs(String pth, String url) throws IOException {
        CoreConfig cc = CoreConfig.getInstance();
        String ext = cc.getProperty("core.util.thumb.extn", "jpg");
        String rat = cc.getProperty("core.util.thumb.pick", "_bg:1*1");
        String map = cc.getProperty("core.util.thumb.zoom", "_lg:256*256,_md:128*128,_sm:64*64");
        return toThumbs(pth, url, ext, rat, map);
    }

    /**
     * 生成缩略图
     * @param pth 原始图片路径
     * @param url 原始图片链接
     * @param ext 规定输出格式
     * @param rat 截取比例格式: 后缀:宽*高;keep#RGBA
     * @param map 缩放尺寸格式: 后缀:宽*高,后缀:宽*高...
     * @return 缩略图路径,链接
     * @throws IOException
     */
    public static String[][] toThumbs(String pth, String url, String ext, String rat, String map) throws IOException {
        if (url == null) url = "";
        if (ext == null) ext = "";
        if (rat == null) rat = "";
        if (map == null) map = "";
        if ("".equals(rat) && "".equals(map)) {
            throw new NullPointerException("Argument rat or map can not be empty");
        }
        if ("".equals(ext)) {
            throw new NullPointerException("Argument ext can not be empty");
        }

        List<String> pts = new ArrayList();
        List<String> urs = new ArrayList();
        Thumb        thb = new Thumb();
        Matcher      mat , mxt;
        String       suf;
        int          w,h;

        url = url.replaceFirst( "\\.\\w+$", "" );

        thb.setExtn(ext);
        thb.setPath(pth);

        // 原始图
        mat = pat.matcher(rat);
        if  ( mat.find() ) {
            suf = mat.group(1);
            w   = Integer.parseInt(mat.group(2));
            h   = Integer.parseInt(mat.group(3));
            mxt = pxt.matcher(rat);
            if (mxt.find() ) {
                // 提取背景色(RGBA)
                String[] x = mxt.group(1).split(",");
                int r  = Integer.parseInt (x[0]);
                int g  = Integer.parseInt (x[1]);
                int b  = Integer.parseInt (x[2]);
                int a  = x.length == 3  ?  255
                       : Integer.parseInt (x[3]);
                pth = thb.keep(suf, w, h, new Color(r, g, b, a));
            } else
            if (rat.endsWith(";keep")) {
                // 默认黑色或者透明
                pth = thb.keep(suf, w, h, new Color(0, 0, 0, 0));
            } else {
                pth = thb.pick(suf, w, h);
            }
            pts.add(pth);
            urs.add(url + suf+"."+ext);
        } else {
            pth = thb.conv(rat);
        }

        // 以上面截取的图为蓝本进行缩放
        thb.setPath(pth);

        // 缩放图
        mat = pat.matcher(map);
        while(mat.find() ) {
            suf = mat.group(1);
            w   = Integer.parseInt(mat.group(2));
            h   = Integer.parseInt(mat.group(3));
            pth = thb.zoom(suf, w, h );
            pts.add(pth);
            urs.add(url + suf+"."+ext);
        }

        return new String[][] {
            pts.toArray(new String[] {}),
            urs.toArray(new String[] {})
        };
    }

}
