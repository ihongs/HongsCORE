package app.hongs.serv.record;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/**
 * 会话状态包裹
 * @author Hongs
 */
public class StatusWraper extends HttpServletRequestWrapper {

    private String sid;
    private Status ses;

    public StatusWraper(HttpServletRequest request, String sid) {
        super(request);
        this.sid = sid;
    }

    /**
     * 返回自定义 HttpSession
     * 没有则新建
     * @return
     */
    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    /**
     * 返回自定义 HttpSession
     * @param add
     * @return
     */
    @Override
    public HttpSession getSession(boolean add) {
        if (ses != null) {
            return ses;
        }

        // 存在会话 ID 则尝试去获取
        if (sid != null) {
            ses  = Status.getStatus(sid);
            if (ses != null) {
                sid  = ses.getId( );
                ses.setServletContext(getServletContext());
                return ses;
            }
        }

        // 会话缺失则判断是否要新建
        if (ses == null) {
            if (add == true) {
                ses  = new Status();
                sid  = ses.getId( );
                ses.setServletContext(getServletContext());
                return ses;
            }
        }

        return  ses;
    }

}
