package io.github.ihongs.serv.matrix.util;

import io.github.ihongs.Core;
import io.github.ihongs.CruxException;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.serv.matrix.Data;
import java.util.Map;

/**
 * 数据限定表
 *
 * 注意: filter/select/assort 并非追加, 多次调会覆盖.
 * 如需增加限定和补充记录可重写 Data.fenceCase(),
 * 返回新限定对象继承此类并重写 fetchCase,insert.
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

    public FetchCase fetchCase() {
        return table.fetchCase().filter("form_id = ?", data.getFormId());
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

    public int delete() throws CruxException {
        FetchCase  fc = fetchCase();
        if (wheres != null) {
            fc.filter(wheres, params != null ? params : new Object[] {});
        }
        return table.delete(/**/ fc.getWhere(), fc.getWheres());
    }

    public int update(Map<String, Object> dat) throws CruxException {
        FetchCase  fc = fetchCase();
        if (wheres != null) {
            fc.filter(wheres, params != null ? params : new Object[] {});
        }
        return table.update(dat, fc.getWhere(), fc.getWheres());
    }

    public int insert(Map<String, Object> dat) throws CruxException {
        dat.put( "form_id", data.getFormId() );
        dat.put( "user_id", data.getUserId() );
        dat.put( "serv_id", Core.SERVER_ID );
        return table.insert(dat);
    }

}
