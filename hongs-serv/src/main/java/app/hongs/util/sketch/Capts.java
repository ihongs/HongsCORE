package app.hongs.util.sketch;

import app.hongs.CoreConfig;
import app.hongs.HongsUnchecked;
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

public class Capts {

    private int width = 160;
    private int height = 40;
    private int codeCount = 4 ;
    private int maskCount = 17;
    private float fontRatio = 0.80f;
    private float mendRatio = 0.167f;
    private float maskRatio = 0.025f;
    private Color backColor = Color.WHITE;
    private Color fontColor = Color.BLACK;
    private String fontFile ="!5TH AVENUE STENCIL.ttf";
    private char[] fontDict = new char[] {
        '1', '3', '4', '5', '6', '7', '8', '9',
        'A', 'B', 'C', 'D', 'E', 'F', 'G',
        'H', 'I', 'J', 'K', 'L', 'M', 'N',
        'P', 'Q', 'R', 'S', 'T',
        'U', 'V', 'W', 'X', 'Y'
    };

    private String          code  =  null;
    private BufferedImage   buff  =  null;

    public static Capts captcha(int h, String b, String f, String e) {
        if (h < 40 || h > 200) {
            throw new HongsUnchecked.Common("h must be 40 ~ 200 px");
        }

        // 获取配置
        CoreConfig cc = CoreConfig.getInstance();
        String ff = cc.getProperty("core.captcha.font.file", "!Anja Eliane.ttf");
        String cs = cc.getProperty("core.captcha.code.dict", "13456789ABCDEFGHIJKLMNPQRSTUVWXY");
        int    cn = cc.getProperty("core.captcha.code.count", 4 );
        int    mn = cc.getProperty("core.captcha.mask.count", 17);
        float  sr = cc.getProperty("core.captcha.size.ratio", 0.6f);
        float  fr = cc.getProperty("core.captcha.font.ratio", 0.8f);
        float  mr = cc.getProperty("core.captcha.mend.ratio", 0.167f);
        float  xr = cc.getProperty("core.captcha.mask.ratio", 0.025f);
        int    w  = (int) ((float) h * sr * (cn + 1));

        char[] cd = cs.toCharArray();
        Color  bc = "".equals(b) ? new Color(0xffffff) : new Color(Integer.parseInt(b, 16));
        Color  fc = "".equals(f) ? new Color(0x000000) : new Color(Integer.parseInt(f, 16));

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

    public void savCode ( ) {
        buff = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int fw = width / (1 + codeCount);            // 单位宽度
        int ls = (int) ((float) height * maskRatio); // 干扰线框
        int fs = (int) ((float) height * fontRatio); // 最大字号
        int fz = (int) ((float)   fs   *   0.80f  ); // 最小字号

        Random        rd = new  Random( );
        StringBuilder sb = new  StringBuilder ();
        Graphics2D    gd = buff.createGraphics();

        gd.setColor(backColor);
        gd.fillRect(0, 0, width, height );
        gd.setColor(fontColor);
        gd.setStroke(new BasicStroke(ls));
        gd.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Font font;
        try {
            InputStream is;
            if (fontFile.startsWith("!")) {
                is = Capts.class.getResourceAsStream(fontFile.substring(1));
            } else {
                is =  new  FileInputStream( new File(fontFile));
            }
            font = Font.createFont ( Font.TRUETYPE_FONT , is ) ;
        } catch (FontFormatException ex) {
            throw new HongsUnchecked.Common(ex);
        } catch (IOException ex) {
            throw new HongsUnchecked.Common(ex);
        }

        for(int i  = 0; i < codeCount; i ++) {
            int fn = rd.nextInt ( fs - fz  ) + fz  ;
            int fe = (int) ((float) fn * mendRatio); // 误差修正
            int j  = rd.nextInt ( fontDict.length );
            String ss = String.valueOf(fontDict[j]);
            sb.append(ss );
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

    public String getCode() {
        if (code == null) {
            savCode();
        }
        return code;
    }

    public BufferedImage getBuff() {
        if (buff == null) {
            savCode();
        }
        return buff;
    }

    public void write(String ext, OutputStream out) throws IOException {
        ImageIO.write(getBuff( ), ext, out);
        out.close();
    }

    private void shearX(int w, int h, Graphics g, Random r, Color c) {
        int period = r.nextInt((int) ((float) h * 0.2f));
        int phases = r.nextInt((int) ((float) h * 0.2f));
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
        int period = r.nextInt((int) ((float) h * 0.2f));
        int phases = r.nextInt((int) ((float) h * 0.2f));
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

}
