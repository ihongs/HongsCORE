package app.hongs.dh;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;

/**
 * CURD 初始动作
 * @author Hongs
 */
public interface IActing {
    
    public void initiate(ActionHelper helper, ActionRunner runner) throws HongsException;

}
