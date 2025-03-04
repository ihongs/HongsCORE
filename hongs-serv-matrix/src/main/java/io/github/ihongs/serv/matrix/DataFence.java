package io.github.ihongs.serv.matrix;

import io.github.ihongs.CruxException;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import java.util.Map;

/**
 * 数据限定表
 *
 * @author Hongs
 */
public class DataFence extends FetchCase {

    protected final Data  data ;
    protected final Table table;

    public DataFence(Data data, Table table) {
        super();

        this.data  = data ;
        this.table = table;

        this.use   ( table.db )
            .from  ( table.tableName )
            .filter("`form_id`=?", data.getFormId());
    }

    @Override
    public int delete() throws CruxException {
        return table.delete(/**/ wheres.substring(5), wparams.toArray());
    }

    @Override
    public int update(Map<String, Object> dat) throws CruxException {
        return table.update(dat, wheres.substring(5), wparams.toArray());
    }

    @Override
    public int insert(Map<String, Object> dat) throws CruxException {
        dat.put( "user_id", data.getUserId() );
        dat.put( "form_id", data.getFormId() );
        return table.insert(dat);
    }

}
