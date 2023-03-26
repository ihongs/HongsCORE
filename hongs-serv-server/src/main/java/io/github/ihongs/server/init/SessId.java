package io.github.ihongs.server.init;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.combat.serv.ServerCombat;
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
 * @author Hongs
 */
public class SessId implements ServerCombat.Initer {

    @Override
    public void init(ServletContextHandler sc) {
        Hand   hd = new Hand(  );
        sc.setSessionHandler(hd);

        CoreConfig cc = CoreConfig.getInstance("defines");
        // 参数名称
        String cn = hd.getSessionCookieName(hd.getSessionCookieConfig()).toLowerCase();
        hd.setSessionHeaderName(cc.getProperty("jetty.session.header.name", "x-"+ cn));
        hd.setSessionParamsName(cc.getProperty("jetty.session.params.name", "." + cn));
        // 追踪模式
        Set tm  = Synt.toSet(cc.getProperty("jetty.session.tracking.mode").toUpperCase());
        if (tm != null && !tm.isEmpty()) {
            if (tm.contains("COOKIE")) {
                hd.setUsingCookies(true);
            }
            if (tm.contains("HEADER")) {
                hd.setUsingHeaders(true);
            }
            if (tm.contains("PARAMS")) {
                hd.setUsingRequest(true);
            }
            if (tm.contains("URL")) {
                hd.setUsingURLs(true);
            }
        }
    }

    private static class Hand extends SessionHandler {
        protected boolean _usingHeaders = false;
        protected String _sessionHeaderName = "x-ssid";
        protected boolean _usingRequest = false;
        protected String _sessionParamsName =  ".ssid";

        protected Hand () {
            super();
        }

        public boolean isUsingHeaders() {
            return _usingHeaders;
        }

        public void setUsingHeaders(boolean usingHeaders) {
            _usingHeaders = usingHeaders;
        }

        public String getSessionHeaderName() {
            return _sessionHeaderName ;
        }

        public void setSessionHeaderName(String param) {
            _sessionHeaderName = param;
        }

        public boolean isUsingRequest() {
            return _usingRequest;
        }

        public void setUsingRequest(boolean usingRequest) {
            _usingRequest = usingRequest;
        }

        public String getSessionParamsName() {
            return _sessionParamsName ;
        }

        public void setSessionParamsName(String param) {
            _sessionParamsName = param;
        }

        public void setUsingURLs(boolean usingURLs) {
            _usingURLs = usingURLs;
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
                ssid = request.getHeader    (sessName);
                sess = getHttpSession(ssid);
                if (sess != null && isValid (sess)) {
                    baseRequest.enterSession(sess);
                    baseRequest.  setSession(sess);
                    CoreLogger.debug("Got session id from headers, {}={}", sessName, ssid);
                }
            }

            if (ssid == null && isUsingRequest()) {
                HttpSession sess;
                String sessName = getSessionParamsName();
                ssid = request.getParameter (sessName);
                sess = getHttpSession(ssid);
                if (sess != null && isValid (sess)) {
                    baseRequest.enterSession(sess);
                    baseRequest.  setSession(sess);
                    CoreLogger.debug("Got session id from request, {}={}", sessName, ssid);
                }
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
                                break;
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
