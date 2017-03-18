package app.hongs.util.sketch;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;

/**
 * 缩略图工具
 * @author Hongs
 */
public class Thumb {

    private BufferedImage src;
    private Color col  = null;
    private Place pos  = null;

    public static enum Place { CENTER, TOP_LEFT, TOP_RIGHT, BOT_LEFT, BOT_RIGHT };

    public Thumb(BufferedImage img) {
        this.src = img;
    }

    public Thumb(File src) throws IOException {
        this(ImageIO.read(src));
    }

    public Thumb(String src) throws IOException {
        this(  new File(src)  );
    }

    public Thumb(Builder tmp) throws IOException {
        this(tmp.asBufferedImage( ));
    }

    public Thumb setColor(Color col) {
        this.col = col;
        return this;
    }

    public Thumb setPlace(Place pos) {
        this.pos = pos;
        return this;
    }

    public Thumb setColor(String col) {
        if (col == null) {
            this.col = null;
            return this;
        }

        String[] x = col.split(  ","  );
        int r = Integer.parseInt (x[0]);
        int g = Integer.parseInt (x[1]);
        int b = Integer.parseInt (x[2]);
        int a = (x.length == 3)  ?  255
              : Integer.parseInt (x[3]);
        setColor(new Color(r, g, b, a));
        return this;
    }

    public Thumb setPlace(String pos) {
        if ("top-right".equals(pos)) {
            setPlace(Place.TOP_RIGHT);
        } else
        if ("top-left".equals(pos)) {
            setPlace(Place.TOP_LEFT);
        } else
        if ("bot-right".equals(pos)) {
            setPlace(Place.BOT_RIGHT);
        } else
        if ("bot-left".equals(pos)) {
            setPlace(Place.BOT_LEFT);
        } else
        {
            setPlace(Place.CENTER);
        }
        return this;
    }

    /**
     * 按比例保留
     * 当 f 为 true 时强制缩放到 w*h, 为 false 仅按比例处理
     * @param w
     * @param h
     * @param f
     * @return
     */
    public Builder keep(int w, int h, boolean f) {
        int sw = src.getWidth( );
        int sh = src.getHeight();
        int dw = sh * w / h;
        if (dw < sw) {
             h = sw * h / w;
             w = sw; // 宽度优先
        } else {
             w = dw;
             h = sh; // 高度优先
        }

        BufferedImage img = draw(src, col, pos, w, h, sw, sh);

        Builder bud = Thumbnails.of(img);
        if (f) {
            return  bud.forceSize  (h,w);
        } else {
            return  bud;
        }
    }

    public Builder keep(int w, int h) {
        return keep(w, h, false);
    }

    /**
     * 按比例截取
     * 当 f 为 true 时强制缩放到 w*h, 为 false 仅按比例处理
     * @param w
     * @param h
     * @param f
     * @return
     */
    public Builder pick(int w, int h, boolean f) {
        int sw = src.getWidth ();
        int sh = src.getHeight();
        int dw = sh * w / h;
        if (dw > sw) {
             h = sw * h / w;
             w = sw; // 宽度优先
        } else {
             w = dw;
             h = sh; // 高度优先
        }

        BufferedImage img = draw(src, col, pos, w, h, sw, sh);

        Builder bud = Thumbnails.of(img);
        if (f) {
            return  bud.forceSize  (h,w);
        } else {
            return  bud;
        }
    }

    public Builder pick(int w, int h) {
        return pick(w, h, false);
    }

    /**
     * 按尺寸缩放
     * 当 f 为 true 时强制拉伸到 w*h, 为 false 则等比缩放到 w*h
     * @param w
     * @param h
     * @param f
     * @return
     */
    public Builder size(int w, int h, boolean f) {
        int sw = src.getWidth ();
        int sh = src.getHeight();

        BufferedImage img = draw(src, col, sw, sh);

        Builder bud = Thumbnails.of(img);
        if (f) {
            return  bud.forceSize  (h,w);
        } else {
            return  bud.size(h,w);
        }
    }

    public Builder size(int w, int h) {
        return size(w, h, false);
    }

    /**
     * 转为构造器
     * 会创建新图, 会铺背景色
     * @return
     */
    public Builder make() {
        int sw = src.getWidth ();
        int sh = src.getHeight();

        BufferedImage img = draw(src, col, sw, sh);

        return Thumbnails.of(img);
    }

    /**
     * 转为构造器
     * 不创建新图, 不铺背景色
     * @return
     */
    public Builder made() {
        return Thumbnails.of(src);
    }

    private BufferedImage draw(BufferedImage img, Color col, Place pos, int w, int h, int x, int y) {
        if (pos == Place.TOP_LEFT ) {
            x = 0;
            y = 0;
        } else
        if (pos == Place.TOP_RIGHT) {
            x = (w - x);
            y = 0;
        } else
        if (pos == Place.BOT_LEFT ) {
            x = 0;
            y = (h - y);
        } else
        if (pos == Place.BOT_RIGHT) {
            x = (w - x);
            y = (h - y);
        } else
        {
            x = (w - x) / 2;
            y = (h - y) / 2;
        }

        BufferedImage  buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics grp = buf.createGraphics();
        if (col != null) {
            grp.setColor(col);
            grp.fillRect(0,0, w,h);
        }
        grp.drawImage(img, x,y, null);
        grp.dispose();
        return buf;
    }

    private BufferedImage draw(BufferedImage img, Color col, int w, int h) {
        BufferedImage  buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics grp = buf.createGraphics();
        if (col != null) {
            grp.setColor(col);
            grp.fillRect(0,0, w,h);
        }
        grp.drawImage(img, 0,0, null);
        grp.dispose();
        return buf;
    }

}
