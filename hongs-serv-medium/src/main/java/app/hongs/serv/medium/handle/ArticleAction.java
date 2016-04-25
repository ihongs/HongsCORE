package app.hongs.serv.medium.handle;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.dh.search.SearchAction;
import app.hongs.serv.medium.ABaseModel;
import app.hongs.serv.medium.Article;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Hongs
 */
@Action("handle/medium/article")
public class ArticleAction extends SearchAction {

    @Action("basics/retrieve")
    public void basics(ActionHelper helper) throws HongsException {
        ABaseModel mod = (ABaseModel) ((Article) getEntity(helper)).getModel();
        Map rd = helper.getRequestData();
        Set rb = new HashSet( );
        rb.add("count_browses");
        rb.add("count_consent");
        rb.add("count_dissent");
        rd.put("rb", rb);
        Map rs= mod.getInfo(rd);
        helper.reply(rs);
    }

    @Override
    public LuceneRecord getEntity(ActionHelper helper) throws HongsException {
        return Article.getInstance("default");
    }

}
