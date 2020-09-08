package io.github.ihongs.dh;

import io.github.ihongs.HongsException;
import java.util.Map;

/**
 * CRUD 记录模型
 * @author Hongs
 */
public interface IEntity {

    public Map search(Map rd) throws HongsException;

    public String create(Map rd) throws HongsException;

    public int update(Map rd) throws HongsException;

    public int delete(Map rd) throws HongsException;

}
