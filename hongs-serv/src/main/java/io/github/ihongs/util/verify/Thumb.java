package io.github.ihongs.util.verify;

import io.github.ihongs.util.Synt;
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

/**
 * 缩略图
 *
 * 规则参数:
 *  thumb-extn      格式名称, 如 jpg
 *  thumb-size      缩放尺寸, 如 _lg:80*40, _md:60*30, _sm:40*20
 *  thumb-mode      处理模式, 如 pick 截取, keep 保留, test 检查
 *  thumb-index     返回索引, 默认为 0, 即首个
 *  thumb-color     背景颜色
 *  thumb-align     停靠位置
 *
 * @see io.github.ihongs.util.sketch.Thumb toThumbs
 * @author Hongs
 */
public class Thumb extends IsFile {

    @Override
    public String checks(String href, String path) throws Wrong {
        String extn = Synt.declare(getParam("thumb-extn" ), "");
        String size = Synt.declare(getParam("thumb-size" ), "");
        String mode = Synt.declare(getParam("thumb-mode" ), "");
        String col  = Synt.declare(getParam("thumb-color"), "");
        String pos  = Synt.declare(getParam("thumb-align"), "");
        int    idx  = Synt.declare(getParam("thumb-index"), 0 );

        try {
            return exec(href, path, extn, size, mode, col, pos)[0][idx];
        } catch (IndexOutOfBoundsException ex) {
            throw new Wrong( ex, "Thumb index out of bounds." );
        } catch (IOException ex) {
            throw new Wrong( ex, "Can not create the thumbs." );
        }
    }

    private static final Pattern TEST_PATT = Pattern.compile("(\\d+)([\\*/])(\\d+)");

    /**
     * 生成缩略图
     * @param url 原始图片链接
     * @param nrl 原始图片路径
     * @param ext 输出格式: png,gif,jpg,bmp 之类
     * @param suf 截取比例: _bg:1/1,_sm:9*9 等等
     * @param mod 处理模式: pick 截取, keep 保留
     * @param col 背景颜色: R,G,B[,A] 取值 0~255
     * @param pos 停靠位置: 9 宫格式
     * @return 缩略图 链接, 路径
     * @throws Wrong
     * @throws IOException
     */
    private String[][] exec(String url, String nrl, String ext, String suf, String mod, String col, String pos)
    throws Wrong, IOException {
        // 没有指定扩展名则无需改变格式
        if (ext.length() == 0 ) {
            int idx = nrl.lastIndexOf('.');
            if (idx > 0) {
                ext = nrl.substring(1+idx);
            } else {
                throw new Wrong( "Missing extension." );
            }
        }

        /**
         * 仅校验尺寸或比例,
         * 不相符将抛出异常.
         * 会取出配置的第一个尺寸或比例,
         * 多个尺寸后面的会进行缩放处理.
         */
        if ("test".equals(mod)) {
            BufferedImage img = ImageIO.read(new File(nrl));
            Matcher mat = TEST_PATT.matcher (suf);
            int w = img.getWidth ();
            int h = img.getHeight();

            if (mat.matches()) {
                String sc = mat.group(2);
                int w2 = Integer.parseInt(mat.group(1));
                int h2 = Integer.parseInt(mat.group(3));

                if ("*".equals(sc)) {
                    if (w != w2 || h != h2) {
                        throw new Wrong("fore.size.unmatch")
                            .setLocalizedOptions(mat.group(1), mat.group(3));
                    }
                } else {
                    if (w *  h2 != h *  w2) {
                        throw new Wrong("fore.scal.unmatch")
                            .setLocalizedOptions(mat.group(1), mat.group(3));
                    }
                }
            } else {
                throw new Wrong("Thumb size config can not be used for test mode");
            }

            mod = ""; // 尺寸匹配, 无需截取
        }

        List<String> nrs = new ArrayList();
        List<String> urs = new ArrayList();
        Builder      bui = null;
        String       pre , pro ;

        nrl = new File( nrl ).getAbsolutePath( );
        pre = nrl.replaceFirst("\\.[^\\.]+$","");
        pro = url.replaceFirst("\\.[^\\.]+$","");

        if (suf.contains("*")
        ||  suf.contains("/")
        ||  suf.contains(",")) {

            String[] sia = suf.split (",");
            String   src = nrl;
            double   scl = 0;
            boolean  rat;
            int      w;
            int      h;

        for (String siz : sia) {
            /**
             * 解析后缀和缩放尺寸,
             * 除号为仅按比例裁剪.
             */
            try {
                String[ ] arr;
                siz = siz.trim();
                if (/***/ siz.contains(":")) {
                    arr = siz.split( ":" , 2 );
                    suf = arr[0].trim();
                    siz = arr[1].trim();
                } else {
                    suf = "" ;
                }
                if (rat = siz.contains("/")) {
                    arr = siz.split( "/" , 2 );
                } else {
                    arr = siz.split("\\*", 2 );
                }
                w   = Integer.parseInt(arr[0]);
                h   = Integer.parseInt(arr[1]);
            } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                throw new Wrong("Wrong thumb size `"+siz+"`. Usage: Suffix:W*H Suffix:W/H W*H W/H");
            }

            /**
             * 第一个或比例有了变化,
             * 才需要特别去裁剪铺贴.
             */
            if (bui == null || scl != w / h) {
                bui = make(src, col, pos, mod, w, h, !rat);
                scl = w / h;
            } else {
                bui = Thumbnails.of(bui.asBufferedImage());
                if (! rat ) bui.size (w , h);
            }

            // 保存到文件
            nrl = pre + suf + "." + ext;
            url = pro + suf + "." + ext;
            try {
                bui.outputFormat(ext);
            } catch (IllegalArgumentException ex) {
                throw new Wrong ("Unsupported format: " + ext);
            }
            bui.toFile(file(nrl));
            nrs.add(nrl);
            urs.add(url);
        }} else {
            /**
             * 如果没有指定缩放尺寸,
             * 那就认为仅需转换格式.
             */
            bui = make(nrl , col);

            // 保存到文件
            nrl = pre + suf + "." + ext;
            url = pro + suf + "." + ext;
            try {
                bui.outputFormat(ext);
            } catch (IllegalArgumentException ex) {
                throw new Wrong ("Unsupported format: " + ext);
            }
            bui.toFile(file(nrl));
            nrs.add(nrl);
            urs.add(url);
        }

        return new String[][] {
            urs.toArray(new String[] {}),
            nrs.toArray(new String[] {})
        };
    }

    private Builder make(String nrl, String col, String pos, String mod, int w, int h, boolean f) throws IOException {
        io.github.ihongs.util.sketch.Thumb thb = new io.github.ihongs.util.sketch.Thumb(nrl);

        // 设置背景颜色
        thb.setColor(col);

        // 设置拼贴位置
        thb.setAlign(pos);

        // 拼贴或者裁剪
        if ("keep".equals(mod)) {
            return thb.keep(w, h, f);
        } else
        if ("pick".equals(mod)) {
            return thb.pick(w, h, f);
        } else
        {
            return thb.size(w, h, false); // 不对图片进行裁剪或者补充, 那只可能缩放到一定尺寸内
        }
    }

    private Builder make(String nrl, String col) throws IOException {
        io.github.ihongs.util.sketch.Thumb thb = new io.github.ihongs.util.sketch.Thumb(nrl);

        if (col != null) {
            thb.setColor(col);

            return thb.make().scale(1);
        } else {
            return thb.made().scale(1);
        }
    }

    private File file(String nrl) {
        File file = new File(nrl);
        File dir  = file.getParentFile();
        if (!dir.exists()) {
             dir.mkdirs();
        }
        return file;
    }

}
