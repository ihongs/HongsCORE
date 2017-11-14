package app.hongs.normal.serv;

import app.hongs.Cnst;
import app.hongs.action.serv.ApisAction;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * REST 接口适配(前缀模式)
 * 将 /api/abc/def 转换为 /abc/def.api 的形式
 * 同 API 的规则 , 未来有  URL rewrite 的功能
 * @author Hongs
 */
public class RestAction extends ApisAction {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse rsp)
            throws IOException, ServletException {
        reset(req);
        super.doGet(req, rsp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse rsp)
            throws IOException, ServletException {
        reset(req);
        super.doPut(req, rsp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse rsp)
            throws IOException, ServletException {
        reset(req);
        super.doPost(req, rsp);
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse rsp)
            throws IOException, ServletException {
        reset(req);
        super.doPatch(req, rsp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse rsp)
            throws IOException, ServletException {
        reset(req);
        super.doDelete(req, rsp);
    }

    private static void reset(HttpServletRequest req) {
        /**
         * RestAction 中 ServletPath 为 /api/, PathInfo 为 /abc/def
         * ApisAction 中 ServletPath 为 /abc/def.api, 并无 PathInfo
         * 故需将前者转换为后者的形式, 然后交由 ApisAction 继续处理
         */

        String uri = req.getPathInfo() + Cnst.API_EXT;

        req.setAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH , uri);
        req.removeAttribute(RequestDispatcher.INCLUDE_PATH_INFO);

        req.setAttribute(RequestDispatcher.FORWARD_SERVLET_PATH , uri);
        req.removeAttribute(RequestDispatcher.FORWARD_PATH_INFO);
    }

}
