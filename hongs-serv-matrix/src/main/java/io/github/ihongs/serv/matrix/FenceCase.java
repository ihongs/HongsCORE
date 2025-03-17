package io.github.ihongs.serv.matrix;

import io.github.ihongs.CruxException;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import java.util.Map;

/**
 * 数据限定表
 *
 * 注意: filter/assort/select 并非追加, 多次调会覆盖
 *
 * @author Hongs
 */
public class FenceCase {

    protected final Data  data ;
    protected final Table table;

    protected String   orders;
    protected String   fields;
    protected String   wheres;
    protected Object[] params;

    public FenceCase(Data data, Table table) {
        super();

        this.data  = data ;
        this.table = table;
    }

    public FenceCase select(String fields) {
        this.fields = fields;
        return this;
    }

    public FenceCase assort(String orders) {
        this.orders = orders;
        return this;
    }

    public FenceCase filter(String wheres, Object... params) {
        this.wheres = wheres;
        this.params = params;
        return this;
    }

    protected FetchCase fetchCase() {
        return table.fetchCase().filter("`form_id`=?", data.getFormId());
    }

    public Map getOne() throws CruxException {
        FetchCase  fc = fetchCase();
        if (wheres != null) {
            fc.filter(wheres, params != null ? params : new Object[] {});
        }
        if (orders != null) {
            fc.assort(orders);
        }
        if (fields != null) {
            fc.select(fields);
        }
        return fc.getOne( );
    }

    public int update(Map<String, Object> dat) throws CruxException {
        return table.update(dat, wheres, params);
    }

    public int insert(Map<String, Object> dat) throws CruxException {
        dat.put( "user_id", data.getUserId() );
        dat.put( "form_id", data.getFormId() );
        return table.insert(dat);
    }

}
