package foo.hongs.dh;

import foo.hongs.HongsException;
import foo.hongs.action.ActionHelper;
import foo.hongs.action.ActionRunner;

/**
 * CURD 初始动作
 * @author Hongs
 */
public interface IActing {

    public void acting(ActionHelper helper, ActionRunner runner) throws HongsException;

}
