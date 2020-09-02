package io.github.ihongs.util.sketch;

import io.github.ihongs.HongsExemption;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;
import net.coobird.thumbnailator.geometry.Position;
import net.coobird.thumbnailator.geometry.Positions;

/**
 * 缩略图工具
 *
 * 此工具为对 Thumbnails.Builder 的一点补充,
 * 增加背景颜色设置, 规避 png 转 jpg 后透明部分成黑色,
 * 增加贴图方位设置, 截取等操作后可指定源图停靠的方位,
 * 类似 Thumbnails.Builder 的 sourceRegion 方法.
 * 这里的 make, size, pick, keep 均会创建新图片,
 * 需注意 made 仅仅是 Thumbnails.of(src) 的别名,
 * 另注意 made, make 并未设定尺寸, 需自行 scale.
 *
 * @author Hongs
 */
public class Thumb {

    private final BufferedImage src;
    private Position pos = null;
    private Color    col = null;

    public Thumb (BufferedImage img) {
        this.src = img;
    }

    public Thumb (File   src) throws IOException {
        this.src = Thumbnails.of(src)
            .useExifOrientation(true)               // 规避图片旋转
            .imageType(BufferedImage.TYPE_INT_ARGB) // 规避透明变黑
            .scale(1)
            .asBufferedImage();
    }

    public Thumb (String src) throws IOException {
        this.src = Thumbnails.of(src)
            .useExifOrientation(true)               // 规避图片旋转
            .imageType(BufferedImage.TYPE_INT_ARGB) // 规避透明变黑
            .scale(1)
            .asBufferedImage();
    }

    public Thumb (InputStream src) throws IOException {
        this.src = Thumbnails.of(src)
            .useExifOrientation(true)               // 规避图片旋转
            .imageType(BufferedImage.TYPE_INT_ARGB) // 规避透明变黑
            .scale(1)
            .asBufferedImage();
    }

    /**
     * 设置背景颜色
     * @param col
     * @return
     */
    public Thumb setColor(Color col) {
        this.col = col;
        return this;
    }

    /**
     * 设置停靠位置
     * @param pos
     * @return
     */
    public Thumb setAlign(Position pos) {
        this.pos = pos;
        return this;
    }

    /**
     * 设置背景颜色
     * 可以用 #RRGGBB 或 #AARRGGBB 或 R,G,B 或 R,G,B,A 的形式
     * @param str
     * @return
     */
    public Thumb setColor(String str) {
        if (str == null) {
            col  = null;
            return this;
        }
        str = str.trim();
        if (str.length() == 0 ) {
            col  = null;
            return this;
        }
        try {
            if (str.startsWith("#")) {
                // 16 进制表示法
                str = str.substring(1);
                int c = Integer.parseInt (str, 16);
                if (str.length() == 6) {
                  return setColor(new Color(c, false));
                } else
                if (str.length() == 8) {
                  return setColor(new Color(c, true ));
                }
            } else {
                // RGBA 表示方法
                String[] x = str.split(",");
                int r = Integer.parseInt (x[0].trim());
                int g = Integer.parseInt (x[1].trim());
                int b = Integer.parseInt (x[2].trim());
                int a = (x.length == 3 ) ? 255
                      : Integer.parseInt (x[3].trim());
                return setColor(new Color(r, g, b, a));
            }
            throw new HongsExemption("Unable to parse color value: "+str);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new HongsExemption("Unable to parse color value: "+str);
        }
    }

    /**
     * 设置停靠位置
     * 类似于 HTML align 的形式, 如 center 或 center right 等
     * @param str
     * @return
     */
    public Thumb setAlign(String str) {
        if (str == null) {
            pos  = null;
            return this;
        }
        str = str.trim();
        if (str.length() == 0) {
            pos  = null;
            return this;
        }
        switch (str) {
            case "center center": case "center":
                return setAlign(Positions.CENTER       );
            case "top center"   : case "top"   :
                return setAlign(Positions.TOP_CENTER   );
            case "center left"  : case "left"  :
                return setAlign(Positions.CENTER_LEFT  );
            case "center right" : case "right" :
                return setAlign(Positions.CENTER_RIGHT );
            case "bototm center": case "bottom":
                return setAlign(Positions.BOTTOM_CENTER);
            case "top left"     :
                return setAlign(Positions.TOP_LEFT     );
            case "top right"    :
                return setAlign(Positions.TOP_RIGHT    );
            case "bottom left"  :
                return setAlign(Positions.BOTTOM_LEFT  );
            case "bottom right" :
                return setAlign(Positions.BOTTOM_RIGHT );
            default:
                throw new HongsExemption("Unsupported place value: "+str);
        }
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
        int tw , th;
        int sw = src.getWidth( );
        int sh = src.getHeight();
        int dw = sh * w / h; // 注意: 先乘再除能避免整形除法的精度损失
        if (dw < sw) {
            th = sw * h / w;
            tw = sw; // 宽度优先
        } else {
            tw = dw;
            th = sh; // 高度优先
        }

        BufferedImage img = draw(src, col, pos, tw, th, sw, sh);

        Builder bud = Thumbnails.of(img);
        if (f) {
            return  bud.forceSize  (w,h);
        } else {
            return  bud.scale(1);
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
        int tw , th;
        int sw = src.getWidth ();
        int sh = src.getHeight();
        int dw = sh * w / h; // 注意: 先乘再除能避免整形除法的精度损失
        if (dw > sw) {
            th = sw * h / w;
            tw = sw; // 宽度优先
        } else {
            tw = dw;
            th = sh; // 高度优先
        }

        BufferedImage img = draw(src, col, pos, tw, th, sw, sh);

        Builder bud = Thumbnails.of(img);
        if (f) {
            return  bud.forceSize  (w,h);
        } else {
            return  bud.scale(1);
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
            return  bud.forceSize  (w,h);
        } else {
            return  bud.size(w,h);
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

    /**
     * 创建图层
     * @param img 源图
     * @param col 背景颜色
     * @param pos 停靠位置
     * @param w   目标宽
     * @param h   目标高
     * @param x   源图宽
     * @param y   源图高
     * @return    新的图层
     */
    private BufferedImage draw(BufferedImage img, Color col, Position pos, int w, int h, int x, int y) {
        if (pos == Positions.TOP_LEFT) {
            x = 0;
            y = 0;
        } else
        if (pos == Positions.TOP_RIGHT) {
            x = (w - x);
            y = 0;
        } else
        if (pos == Positions.TOP_CENTER) {
            x = (w - x) / 2;
            y = 0;
        } else
        if (pos == Positions.BOTTOM_LEFT) {
            x = 0;
            y = (h - y);
        } else
        if (pos == Positions.BOTTOM_RIGHT) {
            x = (w - x);
            y = (h - y);
        } else
        if (pos == Positions.BOTTOM_CENTER) {
            x = (w - x) / 2;
            y = (h - y);
        } else
        if (pos == Positions.CENTER_LEFT) {
            x = 0;
            y = (h - y) / 2;
        } else
        if (pos == Positions.CENTER_RIGHT) {
            x = (w - x);
            y = (h - y) / 2;
        } else /* Default is CENTER CENTER */
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

    /**
     * 创建图层
     * @param img 源图
     * @param col 背景颜色
     * @param w   目标宽
     * @param h   目标高
     * @return    新的图层
     */
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
