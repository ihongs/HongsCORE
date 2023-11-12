package io.github.ihongs.dh;

import io.github.ihongs.CruxException;
import java.util.Map;

/**
 * CRUD 记录模型
 * @author Hongs
 */
public interface IEntity {

    public Map search(Map rd) throws CruxException;

    public Map recite(Map rd) throws CruxException;

    public String create(Map rd) throws CruxException;

    public int update(Map rd) throws CruxException;

    public int delete(Map rd) throws CruxException;

}
