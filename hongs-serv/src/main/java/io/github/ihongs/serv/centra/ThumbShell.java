package io.github.ihongs.serv.centra;

import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.util.verify.Thumb;
import io.github.ihongs.util.verify.Wrong;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Map;

/**
 * 缩略图批量处理
 *
 * 命令参数:
 *  --thumb-extn    格式名称, 如 jpg
 *  --thumb-size    缩放尺寸, 如 _lg:80*40, _md:60*30, _sm:40*20
 *  --thumb-mode    处理模式, 如 pick 截取, keep 保留, test 检查(此时 thumb-size 不加后缀)
 *  --thumb-color   背景颜色
 *  --thumb-align   停靠位置
 *
 * 后面可跟多个图片的文件路径,
 * 否则从管道读取每行文件路径.
 *
 * @author hongs
 */
@Cmdlet("thumb")
public class ThumbShell {

    @Cmdlet("__main__")
    public static void exec(String[] args) throws IOException {
        Map opts = CmdletHelper.getOpts(args, new String[] {
            "thumb-size:s",
            "thumb-extn:s",
            "thumb-mode:s",
            "thumb-color:s",
            "thumb-align:s",
            "!A"
        });
        String [] files = (String []) opts.remove("");

        InputStream in  = CmdletHelper.IN .get();
        PrintStream out = CmdletHelper.OUT.get();
        PrintStream err = CmdletHelper.ERR.get();
        Thumb thu = new Thumb( );
              thu.config( opts );

        if  ( files.length > 0 ) {
            for (String path : files) {
                if (path == null
                ||  path.isEmpty( ) ) {
                    break;
                }
                try {
                    thu.checks ("", path.trim());
                    out.println(path + "\t[OK]");
                } catch (Wrong wr) {
                    err.println(path + "\t" + wr.getLocalizedMistake());
                }
            }
        } else
        try (
            BufferedReader inp = new BufferedReader(new InputStreamReader(in));
        ) {
            String path;
            while (true) {
                path = inp.readLine();
                if (path == null
                ||  path.isEmpty( ) ) {
                    break;
                }
                try {
                    thu.checks ("", path.trim());
                    out.println(path + "\t[OK]");
                } catch (Wrong wr) {
                    err.println(path + "\t" + wr.getLocalizedMistake());
                }
            }
        }
    }

}
