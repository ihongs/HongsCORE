package app.hongs.action;

import app.hongs.Core;
import java.io.IOException;
import javax.servlet.ServletException;

/**
 * 执行包裹
 * @author Hongs
 */
public interface ActionDrive {
    
    public void doDriver(Core core, ActionHelper hlpr) throws ServletException, IOException;

}
