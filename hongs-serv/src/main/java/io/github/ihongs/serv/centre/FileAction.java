package io.github.ihongs.serv.centre;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.UploadHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.util.Synt;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.Part;

/**
 * 公共文件上传
 * @author Hongs
 */
@Action("centre/file")
public class FileAction {

    @Action("create")
    public void create(ActionHelper helper) throws HongsException {
        String uid = ( String )  helper.getSessibute  ( Cnst.UID_SES );
        List  fils = Synt.asList(helper.getRequestData( ).get("file"));
        List  list = new LinkedList();

        for(Object  item  : fils) {
        if (item instanceof Part) {
            Part  part = (Part) item ;
            String ext =  part.getSubmittedFileName();
            String nid =  Core.newIdentity();

            // 取文件扩展名
            int pos = ext.lastIndexOf( '.' );
            if (pos > -1 ) {
                ext = ext.substring  ( pos );
            } else {
                ext = "" ;
            }

            // 传到临时目录
            UploadHelper  uh = new UploadHelper();
            uh.setUploadPath("static/upload/tmp");
            uh.setUploadHref("static/upload/tmp");
            String href = uh.getResultHref();
            String name = uid +"-"+ nid +".";
            uh.upload(part, name);

            // 组织绝对路径
            String link = Core.SCHEME_HOST.get() + Core.BASE_HREF + "/" + href;

            list.add(Synt.mapOf(
                "name", name + ext,
                "href", href,
                "link", link
            ));
        }}

        helper.reply(Synt.mapOf("list", list));
    }

}
