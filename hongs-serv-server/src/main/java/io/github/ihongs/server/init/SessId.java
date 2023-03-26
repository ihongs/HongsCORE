package io.github.ihongs.server.init;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.util.Synt;
import java.util.Set;
import javax.servlet.DispatcherType;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * 会话 ID 设置
 * 注意: 必须放在 SessInDB 或 SessInFile 之前
 * @author Hongs
 */
public class SessId implements Initer {

    @Override
    public void init(ServletContextHandler sc) {
        Hand   hd = new Hand(  );
        sc.setSessionHandler(hd);

        // 追踪模式
        CoreConfig cc = CoreConfig.getInstance("defines");
        Set tm  = Synt.toSet(cc.getProperty("jetty.session.tracking.mode"));
        if (tm != null && !tm.isEmpty()) {
            hd.setUsingHeaders(tm.contains("HEADER"));
            hd.setUsingRequest(tm.contains("PARAMS"));
        }
    }

    private static class Hand extends SessionHandler {
        protected boolean _usingHeaders = false;
        protected String _sessionHeaderName = null;
        protected boolean _usingRequest = false;
        protected String _sessionParamsName = null;

        protected Hand () {
            super();
        }

        public void setUsingURLs(boolean used) {
            _usingURLs = used;
        }

        public boolean isUsingHeaders() {
            return _usingHeaders;
        }

        public void setUsingHeaders(boolean used) {
            _usingHeaders = used;
        }

        public void setSessionHeaderName(String param) {
            _sessionHeaderName = param;
        }

        public String getSessionHeaderName() {
            if (_sessionHeaderName == null ) {
                _sessionHeaderName  = CoreConfig.getInstance("defines").getProperty("jetty.session.header.name");
            if (_sessionHeaderName == null ) {
                _sessionHeaderName  = "x-" + getSessionCookieName(getSessionCookieConfig()).toLowerCase();
            }}
            return _sessionHeaderName;
        }

        public boolean isUsingRequest() {
            return _usingRequest;
        }

        public void setUsingRequest(boolean used) {
            _usingRequest = used;
        }

        public void setSessionParamsName(String param) {
            _sessionParamsName = param;
        }

        public String getSessionParamsName() {
            if (_sessionParamsName == null ) {
                _sessionParamsName  = CoreConfig.getInstance("defines").getProperty("jetty.session.params.name");
            if (_sessionParamsName == null ) {
                _sessionParamsName  =  "." + getSessionCookieName(getSessionCookieConfig()).toLowerCase();
            }}
            return _sessionParamsName;
        }

        @Override
        protected void checkRequestedSessionId(Request baseRequest, HttpServletRequest request)
        {
            String ssid = request.getRequestedSessionId();

            if (ssid != null) {
                HttpSession sess = getHttpSession( ssid );
                if (sess != null && isValid (sess)) {
                    baseRequest.enterSession(sess);
                    baseRequest.  setSession(sess);
                }
                return;
            } else
            if (! DispatcherType.REQUEST.equals(baseRequest.getDispatcherType())) {
                return; // 仅在最外层读取会话
            }

            if (isUsingCookies()) {
                Cookie[] cooks = request.getCookies( );
                if (cooks != null && cooks.length > 0) {
                    HttpSession sess;
                    String  sessName;
                    String  cookName;
                        sessName = getSessionCookieName(getSessionCookieConfig());
                    for(Cookie cook: cooks) {
                        cookName = cook.getName ();
                        if (sessName.equalsIgnoreCase(cookName)) {
                            ssid = cook.getValue();
                            //if (sess == null) {
                                sess  = getHttpSession(ssid);
                                if (sess != null && isValid (sess)) {
                                    baseRequest.enterSession(sess);
                                    baseRequest.  setSession(sess);
                                    CoreLogger.debug("Got session id from cookies, {}={}", sessName, ssid);
                                    break; // 取到就不再继续
                                } else {
                                    CoreLogger.warn ("Wrong session id in cookies, {}={}", sessName, ssid);
                                }
                            /*} else {
                                CoreLogger.warn ("Duplicate session id in cookies, {}:{},{}", sessName, sess.getId(), ssid);
                            }*/
                        }
                    }
                }
            }

            boolean fromCook = ssid != null;

            if (ssid == null && isUsingHeaders()) {
                HttpSession sess;
                String sessName = getSessionHeaderName();
                ssid = request.getHeader    ( sessName );
                if (ssid != null) {
                sess = getHttpSession(ssid);
                if (sess != null && isValid (sess)) {
                    baseRequest.enterSession(sess);
                    baseRequest.  setSession(sess);
                    CoreLogger.debug("Got session id from headers, {}={}", sessName, ssid);
                }}
            }

            if (ssid == null && isUsingRequest()) {
                HttpSession sess;
                String sessName = getSessionParamsName();
                ssid = request.getParameter ( sessName );
                if (ssid != null) {
                sess = getHttpSession(ssid);
                if (sess != null && isValid (sess)) {
                    baseRequest.enterSession(sess);
                    baseRequest.  setSession(sess);
                    CoreLogger.debug("Got session id from request, {}={}", sessName, ssid);
                }}
            }

            if (ssid == null && isUsingURLs()) {
                String uri = request.getRequestURI();
                String pre = getSessionIdPathParameterNamePrefix();
                if (pre != null)
                {
                    int s  = uri.indexOf(pre);
                    if (s >= 0)
                    {
                        s += pre.length();
                        int i = s;
                        while (i < uri.length())
                        {
                            char c = uri.charAt(i);
                            if (c == ';' || c == '#' || c == '?' || c == '/')
                                break ;
                            i++;
                        }

                        HttpSession sess;
                        ssid = uri.substring (s, i);
                        sess = getHttpSession(ssid);
                        if (sess != null && isValid (sess)) {
                            baseRequest.enterSession(sess);
                            baseRequest.  setSession(sess);
                            CoreLogger.debug("Got session id from url, {}={}", pre, ssid);
                        }
                    }
                }
            }

            baseRequest.setRequestedSessionId(ssid);
            baseRequest.setRequestedSessionIdFromCookie(ssid != null && fromCook);
        }
    }

}
