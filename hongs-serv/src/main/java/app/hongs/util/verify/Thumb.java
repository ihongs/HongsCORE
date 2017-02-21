package app.hongs.util.verify;

import app.hongs.CoreConfig;
import app.hongs.util.Synt;
import java.io.File;
import java.io.IOException;

/**
 * 缩略图
 * 规则参数:
 *  thumb-extn 格式名称, 如 jpg
 *  thumb-pick 截取比例, 如 _bg:1*1, 或 _bg:1*1;keep
 *  thumb-zoom 缩放尺寸, 如 _lg:256*256,_md:128*128,_sm:64*64
 *  drop-origin yes|no 抛弃原始文件
 *  keep-origin yes|no 保留原始路径
 * 其中冒号前为该尺寸对应的文件名后缀, 在扩展名之前
 * 以上 thumb-pick 可以只给后缀, 表示仅转换格式, 不按比例截取也不按比例扩展
 * keep 后也可以跟背景色"(红,绿,蓝,透明度)", 取值均为 0~255, 缺省为黑色透明
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
        if (Synt.declare(params.get("keep-origin"), false)) {
            durl = href;
        }

        return durl;
    }

}
