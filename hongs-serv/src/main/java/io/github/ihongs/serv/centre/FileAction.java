package io.github.ihongs.serv.centre;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.UploadHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.util.Synt;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.http.Part;

/**
 * 公共文件上传
 * @author Hongs
 */
@Action("common/file")
public class FileAction {

    @Action("create")
    public void create(ActionHelper helper) throws HongsException {
        List  list = new  ArrayList  ();
        List  fils = Synt.asList (helper.getRequestData( ).get("file") );
        String uid = Synt.declare(helper.getSessibute(Cnst.UID_SES),"0");
        String nid = Core.newIdentity();
        int    idx = 0;

        if (fils == null
        ||  fils.size() < 1) {
            helper.reply(Synt.mapOf(
                "ok" , false,
                "ern", "Er400",
                "err", "file required"
            ));
            return;
        }
        if (fils.size() > 9) {
            helper.reply(Synt.mapOf(
                "ok" , false,
                "ern", "Er400",
                "err", "up to 9 files"
            ));
            return;
        }

        UploadHelper uh = new UploadHelper( );
        uh.setUploadPath("static/upload/tmp");
        uh.setUploadHref("static/upload/tmp");

        for(Object item :   fils) {
        if (item instanceof Part) {
            Part   part = ( Part) item;
            String name = ( uid +"-"+ nid ) +"-"+ (idx++) + "." ;

            File   file = uh.upload(part , name);
            String href = uh.getResultHref(/**/);
            String link = Core.SERVER_HREF.get()
                        + Core.SERVER_PATH.get()
                        + "/" + href;
            name = file.getName( ) + "|" + part.getSubmittedFileName();

            list.add(Synt.mapOf(
                "name", name,
                "href", href,
                "link", link
            ));
        }}

        helper.reply(Synt.mapOf("list", list));
    }

}
