package app.hongs.serv.medium;

import app.hongs.HongsException;
import app.hongs.db.FetchCase;
import app.hongs.db.Model;
import app.hongs.db.Table;
import java.util.Map;

/**
 * 基础表模型
 * 类似 article,section
 * @author Hongs
 */
public class ABaseModel extends Model {
    
    protected String type;

    public ABaseModel(Table table) throws HongsException {
        super(table);
    }
    
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String add(Map rd) throws HongsException {
        rd.put("type", type);
        return super.add(rd);
    }

    @Override
    public int put(String id, Map rd, FetchCase fc) throws HongsException {
        rd.put("type", type);
        return super.put(id, rd, fc);
    }

    @Override
    protected void filter(FetchCase fc, Map rd) throws HongsException {
        super.filter(fc, rd);
        if (type == null) {
            fc.where(".`type` IS NULL"   );
        } else {
            fc.where(".`type` = ? ", type);
        }
    }

}
