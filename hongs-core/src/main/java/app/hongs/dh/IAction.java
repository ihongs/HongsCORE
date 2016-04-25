package app.hongs.dh;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;

/**
 * CRUD 动作模型
 * @author Hongs
 */
public interface IAction {

    public void retrieve(ActionHelper helper) throws HongsException;

    public void create(ActionHelper helper) throws HongsException;

    public void update(ActionHelper helper) throws HongsException;

    public void delete(ActionHelper helper) throws HongsException;

}
