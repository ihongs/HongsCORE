package app.hongs.action;

import app.hongs.Core;
import java.io.IOException;
import javax.servlet.ServletException;

/**
 * 动作驱动链
 * @author Hongs
 */
public interface DirverChain {

    public void doDriver(Core core, ActionHelper hlpr) throws ServletException, IOException;

}
