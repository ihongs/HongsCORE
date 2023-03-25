package io.github.ihongs.serv;

import io.github.ihongs.CoreLogger;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * 会话请求包装
 * @author Hongs
 * @deprecated 改用 Jetty 的 SessionManager
 */
public class SessCasing extends HttpServletRequestWrapper {

    private final HttpServletResponse rsp;
    private final SessFilter flt;

    private Sesion  ses;
    private boolean gotSes;
    private String  sid;
    private boolean gotSid;
    private String  xid;
    private boolean vldSid;
    private byte    frm= 0;

    public SessCasing(HttpServletRequest req, HttpServletResponse rsp, SessFilter flt) {
        super(  req  );
        this.rsp = rsp;
        this.flt = flt;
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        getRequestedSessionId();
        return (frm == 1);
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        getRequestedSessionId();
        return (frm == 2);
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        getSession(false);
        return vldSid ;
    }

    @Override
    public String getRequestedSessionId() {
        if ( ! gotSid ) {
            gotSid = true;

            do {
                String id;

                id = (String) this.getAttribute(flt.SSRA);
                if (id != null && id.length() != 0) {
                    sid = id;
                    frm = 8 ;
                    break ;
                }

                id = this.getParameter(flt.SSRN);
                if (id != null && id.length() != 0) {
                    sid = id;
                    frm = 4 ;
                    break ;
                }

                id = this.getCookibute(flt.SSCN);
                if (id != null && id.length() != 0) {
                    sid = id;
                    frm = 1 ;
                    break ;
                }

//              id = this.getPathibute(flt.SSCN);
//              if (id != null && id.length() != 0) {
//                  sid = id;
//                  src = 2 ;
//                  break ;
//              }
            } while(false);
        }

        return sid;
    }

    @Override
    public String changeSessionId() {
        if (ses != null) {
            ses.revalidate();
            xid  = ses.getId();
            setCookie (/**/);
        } else {
            getSession(true);
        }
        return ses.getId();
    }

    @Override
    public HttpSession getSession(boolean add) {
        if ( ! gotSes || (add && ses == null)) {
            gotSes = true;
            Sesion xes = null;
            getRequestedSessionId();

            if (sid != null) {
                xes = Sesion.getInstance(sid);
                if (xes != null) {
                    vldSid =  true ;
                } else
                if (add == true) {
                // 特定的会话 ID 会继续保留
                if (frm ==  8  ) {
                    vldSid =  true ;
                    xes = new Sesion(sid);
                } else {
                    xes = new Sesion(   );
                }}
            } else {
                if (add == true) {
                    xes = new Sesion(   );
                }
            }

            if (xes != null) {
                ServletContext cont = getServletContext();
                xes.setMaxInactiveInterval(flt.SSRX);
                xes.setServletContext(cont);
                xes.setServletRequest(this);

                ses  = xes;
                xid  = xes.getId();

                // 会话期的 Cookie 无需更新
                if (ses.isNew() || flt.SSCX > 0) {
                    setCookie();
                }
            }
        } else {
            if (ses != null && !ses.getId().equals(xid)) {
                xid  = ses.getId();

                // 会话变更 Cookie 需要更新
//              if (ses.isNew() || flt.SSCX > 0) {
                    setCookie();
//              }
            }
        }

        return  ses;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    protected final void setCookie() {
        if (rsp.isCommitted()) {
            CoreLogger.error("Can not SET session id for Cookie {}={}; Path={}, the response is committed.",
                      flt.SSCN, xid, flt.SSCP);
            return;
        }

        Cookie cok = new Cookie(flt.SSCN, xid);
        cok.setHttpOnly  (true);
        cok.setPath  (flt.SSCP);
        if (flt.SSCX > 0 ) {
        cok.setMaxAge(flt.SSCX);
        }
        rsp.addCookie(cok);
    }

    protected final void delCookie() {
        if (rsp.isCommitted()) {
            CoreLogger.error("Can not DEL session id for Cookie {}={}; Path={}, the response is committed.",
                      flt.SSCN, xid, flt.SSCP);
            return;
        }

        Cookie cok = new Cookie(flt.SSCN, xid);
        cok.setHttpOnly  (true);
        cok.setPath  (flt.SSCP);
        cok.setMaxAge( 0 );
        rsp.addCookie(cok);
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

//  private String getPathibute(String key) {
//      String uri = this.getRequestURI(  );
//      int pos  = uri.indexOf(";"+key+"=");
//      if (pos != -1 ) {
//          return uri.substring( pos + 1 );
//      }
//      return  null;
//  }

}
