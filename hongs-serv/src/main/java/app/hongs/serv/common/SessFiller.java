package app.hongs.serv.common;

import app.hongs.HongsUnchecked;
import java.util.regex.Pattern;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * 会话状态包裹
 * @author Hongs
 */
public class SessFiller extends HttpServletRequestWrapper {

    private Sesion ses;
    private String sid;
    private int    exp;

    private static final Pattern SIDFMT = Pattern.compile("^[a-zA-Z0-9\\-]{1,32}$");

    public SessFiller(HttpServletRequest request, String sid, int exp) {
        super(request);

        // 判断会话 ID 是否正确, 不对则报 400 错误请求
        if (sid != null && sid.length() != 0
        && !SIDFMT.matcher(sid).matches()) {
            throw new HongsUnchecked(0x1100, "Session ID must be 1 to 32 alphanumeric or '-'.");
        }

        this.sid = sid;
        this.exp = exp;
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

            if (sid != null) {
                ses  = Sesion.getSesion(sid);
                if (ses != null) {
                    ses.setServletContext(getServletContext());
                } else
                if (add == true) {
                    ses  =  new  Sesion(sid);
                    ses.setMaxInactiveInterval(exp);
                    ses.setServletContext(getServletContext());
                }
            } else {
                if (add == true) {
                    ses  =  new  Sesion(   );
                    ses.setMaxInactiveInterval(exp);
                    ses.setServletContext(getServletContext());
                    sid  =  ses.getId  (   );
                }
            }

        return  ses  ;
    }

}
