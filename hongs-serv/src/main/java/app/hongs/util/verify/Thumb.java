package app.hongs.util.verify;

import app.hongs.CoreConfig;
import app.hongs.util.Synt;
import java.io.IOException;

/**
 * 缩略图
 * 规则参数:
 *  thumb-extn 格式名称, 如 jpg
 *  thumb-pick 截取比例, 如 _bg:1*1, 或 _bg:1*1;keep
 *  thumb-zoom 缩放尺寸, 如 _lg:256*256,_md:128*128,_sm:64*64
 * 其中冒号前为该尺寸对应的文件名后缀, 在扩展名之前
 * 以上 thumb-pick 可以只给后缀, 表示仅转换格式, 不按比例截取也不按比例扩展
 * keep 后也可以跟背景色"(红,绿,蓝,透明度)", 取值均为 0~255, 缺省为黑色透明
 * @author Hongs
 */
public class Thumb extends IsFile {

    @Override
    public String checks(String href, String path) throws Wrong {
        CoreConfig cc = CoreConfig.getInstance();
        String ext = cc.getProperty("core.util.thumb.extn", "jpg");
        String rat = cc.getProperty("core.util.thumb.pick", "_bg:1*1");
        String map = cc.getProperty("core.util.thumb.zoom", "_lg:256*256,_md:128*128,_sm:64*64");

        ext = Synt.declare(params.get("thumb-extn"), ext);
        rat = Synt.declare(params.get("thumb-pick"), rat);
        map = Synt.declare(params.get("thumb-zoom"), map);

        // 已经是截取图了则不再继续截取
        if (href.endsWith(rat.replaceFirst(":.*$","")+"."+ext)) {
            return href;
        }

        try {
            String[][] a = app.hongs.util.sketch.Thumb.toThumbs(path, href, ext, rat, map);
            return a[1][0];
        } catch (IOException ex) {
            throw new Wrong( ex, "Can not get thumbs");
        }
    }

}
