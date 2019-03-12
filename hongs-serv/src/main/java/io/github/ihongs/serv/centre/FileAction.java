package io.github.ihongs.serv.centre;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
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
        List  list = new  LinkedList ();
        List  fils = Synt.asList (helper.getRequestData( ).get("file") );
        String uid = Synt.declare(helper.getSessibute(Cnst.UID_SES),"0");
        String nid = Core.newIdentity();
        String fmt = "%0"+ String.valueOf ( fils.size( ) ).length()+"d" ;
        int    idx = 0;

        for(Object item  :  fils) {
        if (item instanceof Part) {
            Part   part  = (Part) item;
            String name  = ( uid +"-"+ nid ) +"-"+
            String.format  ( fmt , + + idx ) +".";

            // 传到临时目录
            UploadHelper  uh = new UploadHelper();
            uh.setUploadPath("static/upload/tmp");
            uh.setUploadHref("static/upload/tmp");
            name = uh.upload(part,name).getName();
            String href = uh . getResultHref(   );

            // 组织绝对路径
            String link = Core.SITE_HREF + Core.BASE_HREF + "/" + href;

            list.add(Synt.mapOf(
                "name", name,
                "href", href,
                "link", link
            ));
        }}

        helper.reply(Synt.mapOf("list", list));
    }

}
