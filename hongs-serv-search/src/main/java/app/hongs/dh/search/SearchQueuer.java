package app.hongs.dh.search;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLogger;
import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.util.Async;
import java.util.Map;

/**
 * 索引队列
 * 防止同时写入而造成锁损坏
 * <p>
 * 注意:
 * 此类中为调用 setDoc 和 delDoc 进行最终的写操作,
 * 切记不要覆盖 setDoc 或 delDoc 来做加入索引操作.
 * </p>
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
//      Map<String,Map> form = that.getFields();
//          String      path = that.getDbPath();
            String      name = that.getDbName();
                        name = SearchQueuer.class.getName() +"::"+ name ;
        SearchQueuer    inst = (SearchQueuer) Core.GLOBAL_CORE.got(name);
        if (inst == null) {
//          that =  new LuceneRecord(path, form);
            that =  that.clone(); that.close();
            inst =  new SearchQueuer ( that);
            Core.GLOBAL_CORE.put(name, inst);
        }
        return inst;
    }

    @Override
    public void run(Map rd) {
        try {
            String act = (String) rd.get("__action__");
            String  id = (String) rd.get(Cnst.ID_KEY );
            if ("set".equals(act)) {
                that.begin  (    );
                that.setDoc ( id , that.map2Doc( rd ));
                that.commit (    );
            } else
            if ("del".equals(act)) {
                that.begin  (    );
                that.delDoc ( id );
                that.commit (    );
            } else
            {
                CoreLogger.getLogger("search.queuer").error("Unrecognized action `"+act+"` for "+that.getDbName());
            }
            CoreLogger.getLogger("search.queuer").trace(act+" '"+id+"' for "+that.getDbName());
        } catch (HongsException ex) {
            CoreLogger.getLogger("search.queuer").error(ex.getMessage());
        } catch (HongsExpedient ex) {
            CoreLogger.getLogger("search.queuer").error(ex.getMessage());
        } catch (HongsError er) {
            CoreLogger.getLogger("search.queuer").error(er.getMessage());
        }
    }

}
