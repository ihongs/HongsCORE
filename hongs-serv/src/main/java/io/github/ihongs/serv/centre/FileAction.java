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

        // 取文件扩展名
        int pos = ext.lastIndexOf( '.' );
        if (pos > -1 ) {
            ext = ext.substring  ( pos );
        } else {
            ext = "" ;
        }

        // 传到临时目录
        UploadHelper  uh = new UploadHelper( );
        uh.setUploadPath("static/upload/temp");
        uh.setUploadHref("static/upload/temp");
        String href = uh.getResultHref();
        String name = (uid + "-" + fid );
        uh.upload(prt, name);

        // 组织绝对路径
        String link = System.getProperty("server.host");
        if (link == null || link.length () == 0 ) {
            HttpServletRequest sr = helper.getRequest();
            link = sr.getScheme()+"://"+sr.getServerName();
            int port  = sr.getServerPort();
            if (port != 80 && port != 443) {
                link += ":" + port;
            }
        }
        link += Core.BASE_HREF +"/"+ href ;

        helper.reply("" , Synt.mapOf(
            "name", name + ext,
            "href", href,
            "link", link
        ));
    }

}
