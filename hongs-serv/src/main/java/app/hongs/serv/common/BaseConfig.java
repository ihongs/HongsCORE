package app.hongs.serv.common;

import app.hongs.action.ActionDriver;
import app.hongs.action.serv.AuthAction;
import app.hongs.action.serv.ConfAction;
import app.hongs.action.serv.LangAction;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 通用基础外部配置接口
 * 将 ConfAction, LangAction, AuthAction 集合到一起
 * @author Hongs
 */
public class BaseConfig extends ActionDriver {

    ConfAction conf = new ConfAction();
    LangAction lang = new LangAction();
    AuthAction auth = new AuthAction();

    @Override
    public void service(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException
    {
        String name = req.getServletPath();
        name = name.substring(name.lastIndexOf("/"));
        if ("conf".equals(name)) {
            conf.service(req, rsp);
        } else
        if ("lang".equals(name)) {
            lang.service(req, rsp);
        } else
        if ("auth".equals(name)) {
            auth.service(req, rsp);
        } else
        {
            rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            rsp.getWriter().print("Unsupported name "+name);
        }
    }

    @Override
    public void destroy()
    {
        conf.destroy();
        lang.destroy();
    }
}
