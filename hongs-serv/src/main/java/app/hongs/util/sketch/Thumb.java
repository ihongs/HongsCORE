package app.hongs.util.sketch;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;
import net.coobird.thumbnailator.geometry.Position;
import net.coobird.thumbnailator.geometry.Positions;

/**
 * 缩略图工具
 * @author Hongs
 */
public class Thumb {

    private BufferedImage  src;
    private Position pos = Positions.CENTER;
    private Color    col = new Color(0, 0, 0, 0);

    // 扩展名:宽*高, 扩展名可以为空串
    private static final Pattern PAT = Pattern.compile("([\\w_]*):(\\d+)\\*(\\d+)");
    // keep:R,G,B,A, 取值0~255, A可选
    private static final Pattern PXT = Pattern.compile(";color:(\\d+(,\\d+){2,3})");

    public Thumb(BufferedImage img) {
        this.src = img;
    }

    public Thumb( File  src) throws IOException {
        this(ImageIO.read(src));
    }

    public Thumb(String src) throws IOException {
        this(ImageIO.read(new File(src)));
    }

    public Thumb setColor(Color col) {
        this.col = col;
        return this;
    }

    public Thumb setPosition(Position pos) {
        this.pos = pos;
        return this;
    }

    /**
     * 按比例保留
     * @param w
     * @param h
     * @return
     * @throws IOException
     */
    public Builder keep(int w, int h) throws IOException {
        int xw = src.getWidth (null);
        int xh = src.getHeight(null);
        int zw = xh * w / h;
        if (zw < xw) {
             h = xw * h / w;
             w = xw; // 宽度优先
        } else {
             w = zw;
             h = xh; // 高度优先
        }

        BufferedImage img = draw(src, col, pos, w, h, xw, xh);

        return Thumbnails.of(img);
    }

    /**
     * 按比例截取
     * @param w
     * @param h
     * @return
     */
    public Builder pick(int w, int h) {
        int xw = src.getWidth (null);
        int xh = src.getHeight(null);
        int zw = xh * w / h;
        if (zw > xw) {
             h = xw * h / w;
             w = xw; // 宽度优先
        } else {
             w = zw;
             h = xh; // 高度优先
        }

        BufferedImage img = draw(src, col, pos, w, h, xw, xh);

        return Thumbnails.of(img);
    }

    /**
     * 按尺寸缩放
     * @param w
     * @param h
     * @return
     */
    public Builder zoom(int w, int h) {
        BufferedImage img = draw(src, col);

        return Thumbnails.of(img)
                       .size(w,h);
    }

    /**
     * 转为构造器
     * @return
     */
    public Builder make() {
        BufferedImage img = draw(src, col);

        return Thumbnails.of(img);
    }

    private BufferedImage draw(BufferedImage img, Color col, Position pos, int w, int h, int xw, int xh) {
        int l , t;
        if (pos == Positions.TOP_LEFT) {
            l = 0;
            t = 0;
        } else
        if (pos == Positions.TOP_RIGHT) {
            l = (w - xw);
            t = 0;
        } else
        if (pos == Positions.BOTTOM_LEFT) {
            l = 0;
            t = (h - xh);
        } else
        if (pos == Positions.BOTTOM_RIGHT) {
            l = (w - xw);
            t = (h - xh);
        } else
        {
            l = (w - xw) / 2;
            t = (h - xh) / 2;
        }

        BufferedImage  buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics grp = buf.createGraphics();
        grp.setColor  (col);
        grp.fillRect  (0,0, w,h);
        grp.drawImage (img, l,t, null);
        grp.dispose   (   );
        return buf;
    }

    private BufferedImage draw(BufferedImage img, Color col) {
        int w = img.getWidth( );
        int h = img.getHeight();

        BufferedImage  buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics grp = buf.createGraphics();
        grp.setColor  (col);
        grp.fillRect  (0,0, w,h);
        grp.drawImage (img, 0,0, null);
        grp.dispose   (   );
        return buf;
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
        if ("".equals(ext)) {
            int pos = pth.lastIndexOf('.');
            if (pos > 0) {
                ext = pth.substring(pos+1);
            } else {
                throw new NullPointerException("Argument ext can not be empty");
            }
        }
        if ("".equals(rat) && "".equals(map)) {
            throw new NullPointerException("Argument rat or map can not be empty");
        }

        List<String> pts = new ArrayList();
        List<String> urs = new ArrayList();
        Thumb        thb = new Thumb(pth );
        Builder      bld ;
        Matcher      mat ;
        String       pre , prl , suf ;
        int          w, h;

        pth = new File( pth ).getAbsolutePath( );
        pre = pth.replaceFirst("\\.[^\\.]+$","");
        prl = url.replaceFirst("\\.[^\\.]+$","");

        // 截取图
        mat = PAT.matcher(rat);
        if  ( mat.find( ) ) {
            suf = mat.group(1);
            w   = Integer.parseInt(mat.group(2));
            h   = Integer.parseInt(mat.group(3));

            // 提取背景颜色(RGBA)
            mat = PXT.matcher(rat);
            if (mat.find()) {
                String[] x = mat.group(1).split(",");
                int r = Integer.parseInt( x[0] );
                int g = Integer.parseInt( x[1] );
                int b = Integer.parseInt( x[2] );
                int a = x.length == 3  ?  255
                      : Integer.parseInt( x[3] );
                thb.setColor(new Color(r, g, b, a) );
            }

            // 提取拼贴位置(TBLR)
            if (rat.contains(";bot-right")) {
                thb.setPosition(Positions.BOTTOM_RIGHT);
            } else
            if (rat.contains(";bot-left")) {
                thb.setPosition(Positions.BOTTOM_LEFT);
            } else
            if (rat.contains(";top-right")) {
                thb.setPosition(Positions.TOP_RIGHT);
            } else
            if (rat.contains(";top-left")) {
                thb.setPosition(Positions.TOP_LEFT);
            } else
            {
                thb.setPosition(Positions.CENTER);
            }

            bld = rat.contains(";keep")
                ? thb.keep(w,h)
                : thb.pick(w,h);
            if (! rat.contains(";temp")) {
                pth = pre + suf + "." + ext;
                url = prl + suf + "." + ext;
                bld.outputFormat(ext)
                   .toFile(pth);
                pts.add(pth);
                urs.add(url);
            }
        } else {
            bld = Thumbnails.of (pth);
            if (rat.length() != 0) {
                pth = pre + rat + "." + ext;
                url = prl + rat + "." + ext;
                bld.outputFormat(ext)
                   .toFile(pth);
                pts.add(pth);
                urs.add(url);
            } else {
                bld.outputFormat(ext)
                   .toFile(pth);
                pts.add(pth);
                urs.add(url);
            }
        }

        // 以上面截取的图为蓝本进行缩放
        thb = new Thumb(bld.asBufferedImage());

        // 缩放图
        mat = PAT.matcher(map);
        while (mat.find()) {
            suf = mat.group(1);
            w   = Integer.parseInt(mat.group(2));
            h   = Integer.parseInt(mat.group(3));

            pth = pre + suf + "." + ext;
            url = prl + suf + "." + ext;
            bld = thb.zoom  (w,h);
            bld.outputFormat(ext)
               .toFile(pth);
            pts.add(pth);
            urs.add(url);
        }

        // 没截取或缩放则用指定格式路径
        if (pts.isEmpty()) {
            pts.add(pre + "." + ext);
            urs.add(url + "." + ext);
        }

        return new String[][] {
            pts.toArray(new String[] {}),
            urs.toArray(new String[] {})
        };
    }

}
