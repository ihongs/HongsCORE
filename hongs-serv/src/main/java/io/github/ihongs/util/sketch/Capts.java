package io.github.ihongs.util.sketch;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreRoster;
import io.github.ihongs.HongsExemption;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import javax.imageio.ImageIO;

/**
 * 图像验证码
 * @author Hongs
 */
public class Capts {

    private int width = 100;
    private int height = 40;
    private int codeCount = 4;
    private int maskCount = 8;
    private float fontRatio = 0.80f;
    private float mendRatio = 0.10f;
    private float maskRatio = 0.05f;
    private Color backColor = Color.WHITE;
    private Color fontColor = Color.BLACK;
    private String fontFile ="@Capts.ttf";
    private char[] fontDict = new char[] {
        '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G',
        'H', 'I', 'J', 'K', 'L', 'M', 'N',
        'P', 'Q', 'R', 'S', 'T',
        'U', 'V', 'W', 'X', 'Y'
    };

    private String          code  =  null;
    private BufferedImage   buff  =  null;

    public void setSize(int w, int h) {
        this.width  = w;
        this.height = h;
    }

    public void setCodeCount(int n) {
        this.codeCount = n;
    }

    public void setMaskCount(int n) {
        this.maskCount = n;
    }

    public void setFontRatio(float n) {
        this.fontRatio = n;
    }

    public void setMendRatio(float n) {
        this.mendRatio = n;
    }

    public void setMaskRatio(float n) {
        this.maskRatio = n;
    }

    public void setBackColor(Color c) {
        this.backColor = c;
    }

    public void setFontColor(Color c) {
        this.fontColor = c;
    }

    public void setFontFile(String n) {
        this.fontFile  = n;
    }

    public void setCodeDict(char[] a) {
        this.fontDict  = a;
    }

    public BufferedImage getBuff() {
        if (buff == null) {
            newCapt();
        }
        return buff;
    }

    public String getCode() {
        if (code == null) {
            newCapt();
        }
        return code;
    }

    public void newCapt() {
        buff = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int fw = width / (1 + codeCount);            // 单位宽度
        int ls = (int) ((float) height * maskRatio); // 干扰线框
        int fs = (int) ((float) height * fontRatio); // 最大字号
        int fz = (int) ((float)   fs   *   0.80f  ); // 最小字号

        Random        rd = new  Random();
        StringBuilder sb = new  StringBuilder ();
        Graphics2D    gd = buff.createGraphics();

        gd.setColor(backColor);
        gd.fillRect(0, 0, width, height);
        gd.setColor(fontColor);
        gd.setStroke(new BasicStroke(ls, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        gd.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Font font;
        try {
            InputStream is;
            if (fontFile.startsWith("@")) {
                is = Capts.class.getResourceAsStream(fontFile.substring(1));
            } else {
                is =  new  FileInputStream( new File(fontFile) );
            }
            font = Font.createFont(Font.TRUETYPE_FONT, is);
        } catch (FontFormatException ex) {
            throw new HongsExemption(ex);
        } catch (IOException ex) {
            throw new HongsExemption(ex);
        }

        for(int i  = 0; i < codeCount; i ++) {
            /**
             * medRatio 是字号误差系数
             * 实际可能偏大, 需要缩减尺寸,
             * 修正高度误差, 避免超出画布.
             */
            int fn = rd.nextInt ( fs - fz  ) + fz  ;
            int fe = (  int  )  ( fn * mendRatio  );
            int j  = rd.nextInt ( fontDict.length );
            String ss = String.valueOf(fontDict[j]);
            sb.append(ss);
            gd.setFont(font.deriveFont(Font.CENTER_BASELINE , fn) );
            int fx = (fw - gd.getFontMetrics().stringWidth(ss)) / 2;
            gd.drawString(ss, fw / 2 + fw * i + fx, (height - fn) / 2 + fn - fe);
        }

        for(int i  = 0; i < maskCount; i ++) {
            int xs = rd.nextInt(width );
            int ys = rd.nextInt(height);
            int xe = xs + rd.nextInt(fw * 2);
            int ye = ys + rd.nextInt(fs / 2);
            if (i % 3 == 0) {
                gd.setColor(fontColor );
            } else {
                gd.setColor(backColor );
            }
            gd.drawLine(xs, ys, xe, ye);
        }

        // 扭曲画面
        shearX(width, height, gd, rd, backColor);
        shearY(width, height, gd, rd, backColor);

        code = sb.toString();
    }

    /**
     * 输出图片
     * 注意: 请在调用后自行决定关闭 out
     * @param ext
     * @param out
     * @throws IOException 
     */
    public void write(String ext, OutputStream out) throws IOException {
        ImageIO.write(getBuff( ), ext, out);
    }

    private void shearX(int w, int h, Graphics g, Random r, Color c) {
        int x  = (int) (0.2f * h);
        int period = r.nextInt(x)+ x;
        int phases = r.nextInt(x)+ x;
        int frames = r.nextInt(w - 2) + 2;

        for (int i = 0; i < h; i ++) {
            double d = (double) (period >> 1)
                    * Math.sin((double) i / (double) period
                            + (6.2831853071795862D * (double) phases)
                            / (double) frames);
            g.copyArea(0, i, w, 1, (int) d, 0);
            g.setColor(c);
            g.drawLine((int) d, i, 0, i   );
            g.drawLine((int) d+ w, i, w, i);
        }
    }

    private void shearY(int w, int h, Graphics g, Random r, Color c) {
        int x  = (int) (0.2f * h);
        int period = r.nextInt(x)+ x;
        int phases = r.nextInt(x)+ x;
        int frames = r.nextInt(w - 2) + 2;

        for (int i = 0; i < w; i ++) {
            double d = (double) (period >> 1)
                    * Math.sin((double) i / (double) period
                            + (6.2831853071795862D * (double) phases)
                            / (double) frames);
            g.copyArea(i, 0, 1, h, 0, (int) d);
            g.setColor(c);
            g.drawLine(i, (int) d, i, 0   );
            g.drawLine(i, (int) d+ h, i, h);
        }
    }

    /**
     * 生成验证码
     * @param h 图片高(px)
     * @param b 背景色
     * @param f 前景色
     * @return 
     */
    public static Capts captcha(int h, String b, String f) {
        if (h < 24 || h > 96) {
            throw new HongsExemption(400 , "h must be 24~96 (px)");
        }

        // 获取配置
        CoreConfig  cc = CoreConfig.getInstance();
        String ff = cc.getProperty("core.capts.font.file", "@Capts.ttf");
        String cs = cc.getProperty("core.capts.code.dict", "1234567890");
        int    cn = cc.getProperty("core.capts.code.count", 4);
        int    mn = cc.getProperty("core.capts.mask.count", 8);
        float  sr = cc.getProperty("core.capts.size.ratio", 0.40f);
        float  fr = cc.getProperty("core.capts.font.ratio", 0.80f);
        float  mr = cc.getProperty("core.capts.mend.ratio", 0.10f);
        float  xr = cc.getProperty("core.capts.mask.ratio", 0.05f);
        int    w  = (int) ((float) h * sr * (cn + 1));

        char[] cd = cs.toCharArray();
        Color  bc = "".equals(b) ? new Color(0xffffff, true ) : new Color(Integer.parseInt(b, 16));
        Color  fc = "".equals(f) ? new Color(0x000000, false) : new Color(Integer.parseInt(f, 16));

        // 构建实例
        Capts vc = new Capts( );
        vc.setSize (w , h );
        vc.setCodeCount(cn);
        vc.setMaskCount(mn);
        vc.setFontRatio(fr);
        vc.setMendRatio(mr);
        vc.setMaskRatio(xr);
        vc.setBackColor(bc);
        vc.setFontColor(fc);
        vc.setFontFile (ff);
        vc.setCodeDict (cd);

        return vc;
    }

}
