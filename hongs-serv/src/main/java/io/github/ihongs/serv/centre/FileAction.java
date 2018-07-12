package io.github.ihongs.serv.centre;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.UploadHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.util.Synt;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

/**
 * 公共文件上传
 * @author Hongs
 */
@Action("centre/file")
public class FileAction {

    @Action("create")
    public void create(ActionHelper helper) throws HongsException {
        Part   prt = (Part  ) helper.getRequestData().get("file");
        String uid = (String) helper.getSessibute( Cnst.UID_SES );
        String ext =  prt .getSubmittedFileName();
        String fid =  Core.newIdentity();

        int pos = ext.lastIndexOf( '.' );
        if (pos > -1 ) {
            ext = ext.substring(pos + 1);
        } else {
            ext = "" ;
        }

        // 传到临时目录
        UploadHelper  uh = new UploadHelper( );
        uh.setUploadPath("static/upload/temp");
        uh.setUploadHref("static/upload/temp");
        String name = uid +"-"+ fid +"."+ ext ;
        String href = uh.getResultHref();
        uh.upload(prt, name);

        // 组织绝对路径
        HttpServletRequest sr = helper.getRequest();
        String host = sr.getServerName();
        int    port = sr.getServerPort();
        if (port != 80 && port != 443) {
            host += ":" + port;
        }
        String link = sr.getScheme ( ) +"://"+ host
                    + sr.getContextPath()+"/"+ href;

        helper.reply("", Synt.mapOf(
            "name", name,
            "href", href,
            "link", link
        ));
    }

}
