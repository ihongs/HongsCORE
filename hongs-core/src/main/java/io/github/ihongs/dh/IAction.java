package io.github.ihongs.dh;

import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;

/**
 * CRUD 动作模型
 * @author Hongs
 */
public interface IAction {

    public void search(ActionHelper helper) throws CruxException;

    public void recite(ActionHelper helper) throws CruxException;

    public void create(ActionHelper helper) throws CruxException;

    public void update(ActionHelper helper) throws CruxException;

    public void delete(ActionHelper helper) throws CruxException;

}
