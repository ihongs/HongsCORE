package io.github.ihongs.dh;

import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;

/**
 * CURD 初始动作
 * @author Hongs
 */
public interface IActing {

    public void acting(ActionHelper helper, ActionRunner runner) throws CruxException;

}
