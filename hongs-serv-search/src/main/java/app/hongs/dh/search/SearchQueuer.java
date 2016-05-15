package app.hongs.dh.search;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLogger;
import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.HongsUnchecked;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.util.Async;
import java.util.Map;

/**
 * 索引队列
 * 防止同时写入而造成锁损坏
 * @author Hongs
 */
public class SearchQueuer extends Async<Map> implements Core.GlobalSingleton {

    private final LuceneRecord that;

    private SearchQueuer(LuceneRecord that) throws HongsException {
        super(SearchQueuer.class.getName(),
            CoreConfig.getInstance().getProperty("serv.search.max.tasks", Integer.MAX_VALUE),
            CoreConfig.getInstance().getProperty("serv.search.max.servs", 1/*Single Mode*/));
        this.that = that;
    }

    public static SearchQueuer getInstance(LuceneRecord that) throws HongsException {
        Map<String,Map> form = that.getFields();
            String      name = that.getDbName();
            String      path = that.getDbPath();
                        name = SearchQueuer.class.getName() +"::"+ name ;
        SearchQueuer    inst = (SearchQueuer) Core.GLOBAL_CORE.got(name);
        if (inst == null) {
            that =  new LuceneRecord(path, form);
            inst =  new SearchQueuer(that  /**/);
            /**/Core.GLOBAL_CORE.put(name, inst);
        }
        return inst;
    }

    @Override
    public void run(Map rd) {
        try {
            String act = (String) rd.get( "__action__" );
            if ("set".equals(act)) {
                String id = (String) rd.get(Cnst.ID_KEY);
                that.setDoc ( id  , that.map2Doc( rd ) );
            } else
            if ("del".equals(act)) {
                String id = (String) rd.get(Cnst.ID_KEY);
                that.delDoc ( id );
            } else
            {
                CoreLogger.getLogger("search.queuer").error("Can not run action: "+act);
            }
        } catch (HongsException ex) {
            CoreLogger.getLogger("search.queuer").error(ex.getMessage());
        } catch (HongsUnchecked ex) {
            CoreLogger.getLogger("search.queuer").error(ex.getMessage());
        } catch (HongsError er) {
            CoreLogger.getLogger("search.queuer").error(er.getMessage());
        }
    }

}
