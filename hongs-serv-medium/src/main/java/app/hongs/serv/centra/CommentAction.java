package app.hongs.serv.centra;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.NaviMap;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Assign;
import app.hongs.db.DB;
import app.hongs.db.DBAction;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.serv.medium.LinkRecord;
import app.hongs.util.Synt;

/**
 * 文章评论
 * @author Hongs
 */
@Action("handle/news/comment")
@Assign(conf="saas_news", name="comment")
public class CommentAction extends DBAction {

    String link, linkId;

    @Override
    public void acting(ActionHelper helper, ActionRunner runner) throws HongsException {
        String uid = Synt.asString(helper.getSessibute(Cnst.UID_SES));
        if (uid == null || uid.length( ) == 0 ) {
            throw  new HongsException(0x1101, "");
        }

        // 有超级管理权限则不用理会数据是谁的
        NaviMap navi = NaviMap.getInstance("saas_news");
        if (navi.chkAuth( "saas/news/wholes" )) {
            return;
        }

        // 检验关联数据是否为当前用户所管理的
        link   = helper.getParameter("link"   );
        linkId = helper.getParameter("link_id");
        if ("article".equals(link)) {
            DB db = DB.getInstance("saas_news");
            Object oid = db.getTable("article").fetchCase()
                .filter("id = ?", linkId)
                .select("user_id")
                .one   (         )
                .get   ("user_id");
            if (! uid.equals(oid)) {
                throw new HongsException(0x1103, "No access to "+link+"("+linkId+")");
            }
        } else
        if ("dissent".equals(link)) {
            DB db = DB.getInstance("saas_news");
            Object oid = db.getTable("dissent").fetchCase()
                .join  ( db.getTable("article").tableName, "article",
                        "article.id = dissent.link_id AND dissent_link = 'article'" )
                .filter("dissent.id = ?", linkId)
                .select("article.user_id")
                .setOption("CLEVER_MODE", false )
                .one   (         )
                .get   ("user_id");
            if (! uid.equals(oid)) {
                throw new HongsException(0x1103, "No access to "+link+"("+linkId+")");
            }
        } else
        {
            throw new HongsException(0x1103, "Wrong link "+link);
        }
    }

    @Override
    public void create(ActionHelper helper) {
        // 禁止添加
    }
    @Override
    public void delete(ActionHelper helper) {
        // 禁止删除
    }
    @Override
    public void isExists(ActionHelper helper) {

    }
    @Override
    public void isUnique(ActionHelper helper) {

    }

    @Override
    public Model getEntity(ActionHelper helper) throws HongsException {
        LinkRecord model = (LinkRecord) DB.getInstance("saas_news").getModel("dissent");
        model.setLink  (link  );
        model.setLinkId(linkId);
        return model;
    }

}
