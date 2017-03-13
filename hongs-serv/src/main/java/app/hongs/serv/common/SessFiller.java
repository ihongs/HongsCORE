package app.hongs.serv.common;

import app.hongs.Core;
import app.hongs.CoreLogger;
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

    private final        SessFilter   flt;
    private final HttpServletResponse rsp;

    private Sesion  ses;
    private boolean gotSes;
    private String  sid;
    private boolean gotSid;
    private boolean frmCok;
    private boolean frmUrl;

    public SessFiller(HttpServletRequest req, HttpServletResponse rsp, SessFilter flt) {
        super(  req  );
        this.rsp = rsp;
        this.flt = flt;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        getRequestedSessionId();
        return frmCok;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        getRequestedSessionId();
        return frmUrl;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        return getSession(false) != null;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true );
    }

    @Override
    public HttpSession getSession(boolean add) {
        // 获取或构建会话对象
        if ( ! gotSes || (add && ses == null)) {
            gotSes = true;

            // 获取会话 ID
            getRequestedSessionId();

            if (sid != null) {
                Sesion xes = Sesion.getSesion(sid);
                if (xes != null) {
                    setSession(xes);
                } else
                if (add == true) {
                    setSession(new Sesion());
                }
            } else {
                if (add == true) {
                    setSession(new Sesion());
                }
            }

            // 延期会话 Cookie
            fitRequestedSessionId();
        }

        return ses;
    }

    @Override
    public String getRequestedSessionId() {
        // 从请求数据提取会话 ID
        if ( ! gotSid ) {
            gotSid = true;

            String xid;
            do {
//              xid = this.getPathibute(flt.SSCN);
//              if (xid != null && xid.length() != 0) {
//                  frmUrl = true;
//                  sid = xid;
//                  break ;
//              }

                xid = this.getCookibute(flt.SSCN);
                if (xid != null && xid.length() != 0) {
                    frmCok = true;
                    sid = xid;
                    break ;
                }

                xid = this.getAuthibute(flt.SSCN);
                if (xid != null && xid.length() != 0) {
                    sid = xid;
                    break ;
                }

                xid = this.getParameter(flt.SSRN);
                if (xid != null && xid.length() != 0) {
                    sid = xid;
                    break ;
                }
            } while(false);
        }

        return sid;
    }

    public void fitRequestedSessionId() {
        if (ses == null) {
            return;
        }

        if (rsp.isCommitted ( )) {
            CoreLogger.error("Can not set SessionID to Cookie {}:{} = {}, Response is committed",
                      flt.SSCN, flt.SSCP, ses.getId());
            return;
        }

        Cookie cok = new Cookie(flt.SSCN, ses.getId());
        cok.setPath(Core.BASE_HREF+flt.SSCP);
        cok.setHttpOnly ( true);
        cok.setMaxAge(flt.CEXP);
        rsp.addCookie(cok /**/);
    }

    private void setSession(Sesion xes) {
        ServletContext cont = getServletContext();
        xes.setMaxInactiveInterval(flt.SEXP);
        xes.setServletContext(cont);
        xes.setRequestContext(this);
        ses = xes;
    }

//  private String getPathibute(String key) {
//      String uri = this.getRequestURI(  );
//      int pos  = uri.indexOf(";"+key+"=");
//      if (pos != -1 ) {
//          return uri.substring( pos + 1 );
//      }
//      return  null;
//  }

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
