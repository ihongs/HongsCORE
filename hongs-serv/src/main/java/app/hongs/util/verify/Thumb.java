package app.hongs.util.verify;

import app.hongs.HongsExpedient;
import app.hongs.util.Synt;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;

/**
 * 缩略图
 *
 * 规则参数:
 *  thumb-extn      格式名称, 如 jpg
 *  thumb-size      缩放尺寸, 如 _lg:80*40; _md:60*30
 *  thumb-mode      处理模式, 如 pick 截取, keep 保留
 *  thumb-index     返回索引, 默认为 0, 即首个
 *  thumb-color     背景颜色
 *  thumb-place     粘贴位置
 *  back-origin     yes|no 返回原始路径
 *  drop-origin     yes|no 抛弃原始文件
 *
 * @see app.hongs.util.sketch.Thumb toThumbs
 * @author Hongs
 */
public class Thumb extends IsFile {

    @Override
    public String checks(String href, String path) throws Wrong {
        String extn = Synt.declare(params.get("thumb-extn" ), "jpg");
        String size = Synt.declare(params.get("thumb-size" ), "");
        String mode = Synt.declare(params.get("thumb-mode" ), "");
        String col  = Synt.declare(params.get("thumb-color"), "");
        String pos  = Synt.declare(params.get("thumb-place"), "");
        int    idx  = Synt.declare(params.get("thumb-index"), 0 );

        String dest , durl;
        try {
            String[][] ph = exec(path, href, extn, size, mode, col, pos);
            dest = ph[0][idx];
            durl = ph[1][idx];
        } catch (IOException|HongsExpedient ex) {
            throw new Wrong(ex, "Can not create the thumbs.");
        } catch ( IndexOutOfBoundsException ex) {
            throw new Wrong(ex, "Thumb index out of bounds.");
        }

        // 可以选择抛弃原始文件
        // 亦或保留返回原始路径
        if (Synt.declare(params.get("drop-origin"), false)) {
            if (!dest.equals(path)) new File(path).delete();
        } else
        if (Synt.declare(params.get("back-origin"), false)) {
            durl = href;
        }

        return durl;
    }

    /**
     * 生成缩略图
     * @param pth 原始图片路径
     * @param url 原始图片链接
     * @param ext 输出格式: png,gif,jpg,bmp 之类
     * @param suf 截取比例: _bg:1/1,_sm:9*9 等等
     * @param mod 处理模式: pick 截取, keep 保留
     * @param col 背景颜色: R,G,B[,A] 取值 0~255
     * @param pos 贴图位置: center 中央, top-left 左上, top-right 右上, bot-left 左下, bot-right 右下
     * @return 缩略图路径,链接
     * @throws IOException
     */
    private String[][] exec(String pth, String url, String ext, String suf, String mod, String col, String pos) throws IOException {
//        if (pth == null || pth.equals("")) {
//                throw new NullPointerException("Path can not be empty");
//        }
//        if (ext == null || ext.equals("")) {
//            int idx = pth.lastIndexOf('.');
//            if (idx > 0) {
//                ext = pth.substring(idx+1);
//            } else {
//                throw new NullPointerException("Extn can not be empty");
//            }
//        }
//        if (url == null) url = "";
//        if (suf == null) suf = "";

        List<String> pts = new ArrayList();
        List<String> urs = new ArrayList();
        Builder      bld = null;
        String       pre , prl ;

        pth = new File( pth ).getAbsolutePath( );
        pre = pth.replaceFirst("\\.[^\\.]+$","");
        prl = url.replaceFirst("\\.[^\\.]+$","");

        if (suf.contains(";")
        &&  suf.contains(":")) {

            String[] sia = suf.split (";");
            String   src = pth;
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
                String[] arr;
                siz = siz.trim (  );
                arr = siz.split(":", 2);
                suf = arr[0].trim();
                siz = arr[1].trim();
                if (rat = siz.contains("/")) {
                    arr = siz.split("\\/", 2 );
                } else {
                    arr = siz.split("\\*", 2 );
                }
                w   = Integer.parseInt(arr[0]);
                h   = Integer.parseInt(arr[1]);
            } catch (IndexOutOfBoundsException ex) {
                throw new HongsExpedient.Common("Wrong thumb size `"+siz+"`. Usage: Suffix:W*H or Suffix:Scale");
            } catch (/**/NumberFormatException ex) {
                throw new HongsExpedient.Common("Wrong thumb size `"+siz+"`. Usage: Suffix:W*H or Suffix:Scale");
            }

            /**
             * 第一个或比例有了变化,
             * 才需要特别去裁剪铺贴.
             */
            if (bld != null && scl != w / h) {
                bld  = Thumbnails.of(bld.asBufferedImage());
            } else {
                bld  = make(src, col, pos, mod, w, h);
                scl  =  (w / h);
            }
            if (! rat) {
                bld.size(w , h);
            }

            // 保存到文件
            pth = pre + suf + "." + ext;
            url = prl + suf + "." + ext;
            bld.outputFormat(ext)
               .toFile(pth);
            pts.add(pth);
            urs.add(url);
        }} else {
            /**
             * 如果没有指定缩放尺寸,
             * 那就认为仅需转换格式.
             */
            bld = make(pth , col);

            // 保存到文件
            pth = pre + suf + "." + ext;
            url = prl + suf + "." + ext;
            bld.outputFormat(ext)
               .toFile(pth);
            pts.add(pth);
            urs.add(url);
        }

        return new String[][] {
            pts.toArray(new String[] {}),
            urs.toArray(new String[] {})
        };
    }

    private Builder make(String pth, String col, String pos, String mod, int w, int h) throws IOException {
        app.hongs.util.sketch.Thumb thb = new app.hongs.util.sketch.Thumb(pth);

        // 设置背景颜色
        thb.setColor(col);

        // 设置拼贴位置
        thb.setPlace(pos);

        // 拼贴或者裁剪
        if ("keep".equals(mod)) {
            return thb.keep(w, h);
        } else
        if ("pick".equals(mod)) {
            return thb.pick(w, h);
        } else
        {
            return thb.make();
        }
    }

    private Builder make(String pth, String col) throws IOException {
        app.hongs.util.sketch.Thumb thb = new app.hongs.util.sketch.Thumb(pth);

        if (col != null) {
            thb.setColor(col);

            return thb.make();
        } else {
            return thb.made();
        }
    }

}
