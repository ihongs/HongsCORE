package io.github.ihongs.util.verify;

import io.github.ihongs.util.Synt;
import java.awt.image.BufferedImage;
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
 *  thumb-size      缩放尺寸, 如 _lg:80*40, _md:60*30, _sm:40*20
 *  thumb-mode      处理模式, 如 pick 截取, keep 保留, test 检查(此时 thumb-size 不加后缀)
 *  thumb-index     返回索引, 默认为 0, 即首个
 *  thumb-color     背景颜色
 *  thumb-align     停靠位置
 *
 * @see io.github.ihongs.util.sketch.Thumb toThumbs
 * @author Hongs
 */
public class Thumb extends IsFile {

    @Override
    public String[] checks(String href, String path)
    throws Wrong {
        String extn = Synt.declare(getParam("thumb-extn" ), "");
        String size = Synt.declare(getParam("thumb-size" ), "");
        String mode = Synt.declare(getParam("thumb-mode" ), "");
        String col  = Synt.declare(getParam("thumb-color"), "");
        String pos  = Synt.declare(getParam("thumb-align"), "");
        int    idx  = Synt.declare(getParam("thumb-index"), 0 );

        try {
            String [][] hps = exec(href, path, extn, size, mode, col, pos);
            return hps[ idx ];
        } catch (IndexOutOfBoundsException ex) {
            throw new Wrong( ex, "Thumb index out of bounds." );
        } catch (IOException ex) {
            throw new Wrong( ex, "Can not create the thumbs." );
        }
    }

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
    throws  Wrong, IOException {
        BufferedImage img = Thumbnails.of(nrl).scale(1).asBufferedImage();
        int rw = img.getWidth ();
        int rh = img.getHeight();

        // 没有指定扩展名则无需改变格式
        if (ext.length() == 0) {
            int idx = nrl.lastIndexOf('.');
            if (idx > 0) {
                ext = nrl.substring(1+idx);
            } else {
                ext = "png";
            }
        }

        /**
         * 仅校验尺寸或比例,
         * 不相符将抛出异常.
         * 会取出配置的第一个尺寸或比例,
         * 多个尺寸后面的会进行缩放处理.
         */
        if ("test".equals( mod )) {
            String[] sia = suf.split (",");
            boolean  mat = false;
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
                if (rat = siz.contains("/")) {
                    arr = siz.split( "/" , 2 );
                } else {
                    arr = siz.split("\\*", 2 );
                }
                w   = Integer.parseInt(arr[0]);
                h   = Integer.parseInt(arr[1]);
            } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                throw new Wrong("Wrong thumb size `"+siz+"`. Usage: W*H W/H");
            }

            /**
             * 为了应对发长图的需求,
             * 可限制单一的宽或者高.
             */
            if (! rat ) {
                if (w == 0 && h == 0) {
                    w  = rw ; h  = rh ;
                } else
                if (w == 0) {
                    w  = rw * h  / rh ;
                } else
                if (h == 0) {
                    h  = rh * w  / rw ;
                }
            } else {
                if (w == 0 || h == 0) {
                throw new Wrong("Wrong thumb size `"+siz+"`. Usage: W*0 0/H");
                }
            }

            if (! rat ) {
                if (w == rw && h == rh) {
                    mat = true;
                    break;
                }
            } else {
                if (w *  rh == h *  rw) {
                    mat = true;
                    break;
                }
            }
        }

            if (! mat ) {
                throw new Wrong("fore.size.invalid").setLocalizedOptions(suf);
            }

            // 尺寸匹配, 无需改变
            suf = "" ;
            mod = "" ;
        }

        List<String[]> hps = new ArrayList();
        Builder        bui = null;
        String         pre , pro ;

        nrl = new File( nrl ).getAbsolutePath( );
        pre = nrl.replaceFirst("\\.[^\\.]+$","");
        pro = url.replaceFirst("\\.[^\\.]+$","");

        if (suf.contains("*")
        ||  suf.contains("/")
        ||  suf.contains(",")) {

            String[] sia = suf.split (",");
            String   src = nrl;
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
                if (  siz.contains ( ":" ) ) {
                    arr = siz.split( ":" , 2 );
                    suf = arr[0].trim();
                    siz = arr[1].trim();
                } else
                if (! siz.contains ( "*" )
                &&  ! siz.contains ( "/" ) ) {
                    suf = siz;
                    siz = "" ;
                } else {
                    suf = "" ;
                }
                if (! siz.isEmpty( ) ) {
                    if (rat = siz.contains("/")) {
                        arr = siz.split( "/" , 2 );
                    } else {
                        arr = siz.split("\\*", 2 );
                    }
                    w   = Integer.parseInt(arr[0]);
                    h   = Integer.parseInt(arr[1]);
                } else {
                    rat = false;
                    w   = 0;
                    h   = 0;
                }
            } catch ( IndexOutOfBoundsException | NumberFormatException e ) {
                throw new Wrong("Wrong thumb size `"+siz+"`. Usage: Suffix:W*H Suffix:W/H W*H W/H");
            }

            /**
             * 为了应对发长图的需求,
             * 可限制单一的宽或者高.
             */
            if (! rat ) {
                if (w == 0 && h == 0) {
                    w  = rw ; h  = rh ;
                } else
                if (w == 0) {
                    w  = rw * h  / rh ;
                } else
                if (h == 0) {
                    h  = rh * w  / rw ;
                }
            } else {
                if (w == 0 || h == 0) {
                throw new Wrong("Wrong thumb size `"+siz+"`. Usage: Suffix:W*0 Suffix:0*H W*0 0*H");
                }
            }

            bui = make(src, col, pos, mod, w, h, ! rat );

            // 保存到文件
            nrl = pre + suf + "." + ext;
            url = pro + suf + "." + ext;
            try {
                bui.outputFormat(ext);
            } catch (IllegalArgumentException ex) {
                throw new Wrong(ex, "fore.type.invalid").setLocalizedOptions(ext);
            }
            bui.toFile(file(nrl));
            hps.add(new String [] {url, nrl, "w="+w+"&h="+h});
        }} else {
            /**
             * 如果没有指定缩放尺寸,
             * 那就认为仅需转换格式.
             */
            int w = rw;
            int h = rh;
            
            bui = make(nrl, col );

            // 保存到文件
            nrl = pre + suf + "." + ext;
            url = pro + suf + "." + ext;
            try {
                bui.outputFormat(ext);
            } catch (IllegalArgumentException ex) {
                throw new Wrong(ex, "fore.type.invalid").setLocalizedOptions(ext);
            }
            bui.toFile(file(nrl));
            hps.add(new String [] {url, nrl, "w="+w+"&h="+h});
        }

        return hps.toArray(new String [][]{});
    }

    private Builder make(String nrl, String col, String pos, String mod, int w, int h, boolean f)
    throws  IOException {
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

    private Builder make(String nrl, String col)
    throws  IOException {
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