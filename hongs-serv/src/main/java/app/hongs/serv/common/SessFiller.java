package app.hongs.serv.common;

import app.hongs.Core;
import app.hongs.CoreLogger;
import app.hongs.HongsUnchecked;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * 会话请求包装
 * @author Hongs
 */
public class SessFiller extends HttpServletRequestWrapper {

    private final static Pattern      SID = Pattern.compile("^[a-zA-Z0-9_\\-]{1,32}$");
    private final /****/ SessFilter   flt;
    private final HttpServletResponse rsp;

    public Sesion  ses;
    public boolean gotSes;
    public String  sid;
    public boolean gotSid;

    public SessFiller(HttpServletRequest req, HttpServletResponse rsp, SessFilter flt) {
        super(  req  );
        this.rsp = rsp;
        this.flt = flt;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public HttpSession getSession(boolean add) {
        // 从请求数据提取会话 ID
        if (! gotSid) {
            gotSid = true;

            String xid;
            do {
                xid = this.getParameter(flt.SSRN);
                if (xid != null && xid.length() != 0) {
                    setSid(xid);
                    break ;
                }

                xid = this.getCookibute(flt.SSCN);
                if (xid != null && xid.length() != 0) {
                    setSid(xid);
                    break ;
                }

                xid = this.getAuthibute(flt.SSCN);
                if (xid != null && xid.length() != 0) {
                    setSid(xid);
                    break ;
                }
            } while(false);
        }

        // 获取或构建会话对象
        if (! gotSes || (add && ses == null)) {
            gotSes = true;

            Sesion xes;
            if (sid != null) {
                xes  = Sesion.getSesion(sid);
                if (xes != null) {
                    setSes(xes);
                } else
                if (add == true) {
                    xes  =  new  Sesion(sid);
                    setSes(xes);
                }
            } else {
                if (add == true) {
                    xes  =  new  Sesion(   );
                    setSes(xes);
                }
            }

            // 延期会话 Cookie
            setCookies();
        }

        return ses;
    }

    public void setCookies() {
        if (ses == null) {
            return;
        }

        if (rsp.isCommitted ( )) {
            CoreLogger.error("Can not set SessionID to Cookie {}:{} = {}, Response is committed",
                      flt.SSCN, flt.SSCP, ses.getId());
            return;
        }

        Cookie cok = new Cookie(flt.SSCN, ses.getId());
        cok.setPath(Core.BASE_HREF + flt.SSCP);
        cok.setHttpOnly ( true);
        cok.setMaxAge(flt.CEXP);
        rsp.addCookie(cok /**/);
    }

    private void setSes(Sesion xes) {
        ServletContext cont = getServletContext();
        xes.setMaxInactiveInterval(flt.SEXP);
        xes.setServletContext(cont);
        xes.setRequestContext(this);
        ses = xes;
    }

    private void setSid(String xid) {
        if (SID.matcher(xid).matches() == false) {
            throw new HongsUnchecked(0x1100, "Session ID must be 1 to 32 alphanumeric, '-' and '_'");
        }
        sid = xid;
    }

    private String getCookibute(String key) {
        Cookie[ ] cks = this.getCookies();
        if (cks != null) for (Cookie cok : cks) {
            if (key.equals(cok.getName ())) {
                return     cok.getValue();
            }
        }
        return  null;
    }

    private String getAuthibute(String key) {
        String xid = this.getHeader("Authorization");
        if (xid != null && xid.startsWith(key + "=")) {
            return xid.substring( key.length() + 1 );
        }
        return  null;
    }

}
