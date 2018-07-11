package foo.hongs.dh;

import foo.hongs.HongsException;
import foo.hongs.action.ActionHelper;

/**
 * CRUD 动作模型
 * @author Hongs
 */
public interface IAction {

    public void search(ActionHelper helper) throws HongsException;

    public void create(ActionHelper helper) throws HongsException;

    public void update(ActionHelper helper) throws HongsException;

    public void delete(ActionHelper helper) throws HongsException;

}
