package app.hongs.serv.medium.manage;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Select;
import app.hongs.db.DB;
import app.hongs.db.FetchCase;
import app.hongs.dh.lucene.LuceneAction;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.serv.medium.ABaseModel;
import app.hongs.serv.medium.Article;
import app.hongs.util.Synt;
import java.util.Map;

/**
 * 文章管理
 * @author Hongs
 */
@Action("manage/medium/article")
public class ArticleAction extends LuceneAction {

    /**
     * 文章管理直接从数据库获取
     * @param helper
     * @throws HongsException
     */
    @Action("retrieve")
    @Select(conf="", form="")
    @Override
    public void retrieve(ActionHelper helper) throws HongsException {
        ABaseModel art = (ABaseModel) DB.getInstance("medium").getModel("article");
                   art.setType("default");

        Map     req = helper.getRequestData();
                req = getReqMap(helper, art, "retrieve", req);

        FetchCase c = new FetchCase();
        if (req.containsKey("sect_id")) {
            Object sid = req.get("sect_id");
            c.join(art.db.getTable("segment").tableName)
             .on  ("link_id = :id AND link = 'article'")
             .by  (FetchCase.INNER)
             .where("sect_id IN (?)", sid );
        }
        c.setOption("INCLUDE_REMOVED", Synt.declare(req.get("include-removed"), false));
        
        Map     rsp = art.retrieve(req, c);
                rsp = getRspMap(helper, art, "retrieve", rsp);
        helper.reply(rsp);
    }

    @Override
    public LuceneRecord getEntity(ActionHelper helper) throws HongsException {
        return Article.getInstance("default");
    }

}
