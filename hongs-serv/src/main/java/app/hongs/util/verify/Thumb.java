package app.hongs.util.verify;

import app.hongs.CoreConfig;
import app.hongs.util.Synt;
import java.io.File;
import java.io.IOException;

/**
 * 缩略图
 * 规则参数:
 *  thumb-extn 格式名称, 如 jpg
 *  thumb-pick 截取比例, 如 _bg, _bg:1*1 或 _bg:1*1;keep;temp
 *  thumb-zoom 缩放尺寸, 如 _lg:256*256,_md:128*128,_sm:64*64
 *  back-origin yes|no 返回原始路径
 *  drop-origin yes|no 抛弃原始文件
 * 冒号前为尺寸对应的文件名后缀, 在扩展名的前面
 * back-origin,drop-origin 互斥, 二者仅能选一个; keep,temp 均是可选项
 * thumb-pick 可只给后缀, 表示仅转换格式而不截取
 * keep 后可跟背景色":R,G,B[,A]", 缺省为黑色透明
 * temp 表示此截取图片仅作为模板, 将在缩放后删除
 * @author Hongs
 */
public class Thumb extends IsFile {

    @Override
    public String checks(String href, String path) throws Wrong {
        CoreConfig cc = CoreConfig.getInstance();
        String ext = cc.getProperty("core.thumb.extn", "jpg");
        String rat = cc.getProperty("core.thumb.pick", "_bg:1*1");
        String map = cc.getProperty("core.thumb.zoom", "_lg:256*256,_md:128*128,_sm:64*64");

        ext = Synt.declare(params.get("thumb-extn"), ext);
        rat = Synt.declare(params.get("thumb-pick"), rat);
        map = Synt.declare(params.get("thumb-zoom"), map);

        // 已是截图了则不再继续
        // 验证中已查是否新传的
        /*
        String xxt = rat.length() != 0 && !rat.contains(";temp")
                   ? rat.replaceFirst(":.*$", "")
                   : map.replaceFirst(":.*$", "");
        if (href.endsWith(xxt + "." + ext)) {
            return href;
        }
        */

        String dest, durl;
        try {
            String[][] ph = app.hongs.util.sketch.Thumb.toThumbs(path, href, ext, rat, map);
            dest = ph[0][0];
            durl = ph[1][0];
        } catch (IOException ex) {
            throw new Wrong( ex, "Can not create thumbs." );
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

}
