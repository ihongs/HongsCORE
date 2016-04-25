package app.hongs.dh;

import app.hongs.HongsException;
import java.util.Map;

/**
 * CRUD 记录模型
 * @author Hongs
 */
public interface IEntity {

    public Map retrieve(Map rd) throws HongsException;

    public Map create(Map rd) throws HongsException;

    public int update(Map rd) throws HongsException;

    public int delete(Map rd) throws HongsException;

}
