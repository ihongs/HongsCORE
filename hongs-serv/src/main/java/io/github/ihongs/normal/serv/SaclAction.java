package io.github.ihongs.normal.serv;

import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.serv.AuthAction;
import io.github.ihongs.action.serv.ConfAction;
import io.github.ihongs.action.serv.LangAction;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 综合配置接口
 * 将 AuthAction,ConfAction,LangAction 集合到一起
 * @author Hongs
 */
public class SaclAction extends ActionDriver {

    AuthAction auth = new AuthAction();
    ConfAction conf = new ConfAction();
    LangAction lang = new LangAction();

    @Override
    public void service(HttpServletRequest req, HttpServletResponse rsp)
      throws ServletException, IOException
    {
        String name = req.getServletPath();
               name = name.substring(name.lastIndexOf("/"));
        if (null != name)
            switch (name) {
        case "/auth":
            auth.service(req, rsp);
            break;
        case "/conf":
            conf.service(req, rsp);
            break;
        case "/lang":
            lang.service(req, rsp);
            break;
        default:
            rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            rsp.getWriter().print("Unsupported name "+name);
            break;
        } else {
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
