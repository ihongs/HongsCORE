package app.hongs.action;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLocale;
import app.hongs.CoreLogger;
import app.hongs.HongsError;
import app.hongs.cmdlet.serv.ServerCmdlet;
import app.hongs.util.Clses;
import app.hongs.util.Data;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.websocket.DeploymentException;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

/**
 * WebSocket 助手类
 *
 * <code>
 *  // 事件处理器示例:
 *  @ServerEndpoint(value="/sock/path/{xxx}", configurator=SocketHelper.Config.class)
 *  public class Xxxx {
 *      @OnXxxx
 *      public void onXxxx(Session ss) {
 *          SocketHelper sh = SocketHelper.getInstance(ss);
 *          try {
 *              // TODO: Some thing...
 *          }
 *          catch (Error|Exception ex) {
 *              CoreLogger.error ( ex);
 *          }
 *          finally {
 *              sh.destroy(); // 销毁环境, 务必执行
 *          }
 *      }
 *  }
 *  // 并将此类名(含包名)加入 _init_.properties 中 jetty.sock 值
 * </code>
 *
 * @author Hongs
 */
public class SocketHelper extends ActionHelper {

    protected SocketHelper(Map data, Map prop) {
        super(data, prop, null, null);

        /**
         * 放入 UserProperties 中以便随时取用
         */
        prop.put(SocketHelper.class.getName(), this);
        prop.put(ActionHelper.class.getName(), this);
    }

    /**
     * 更新环境
     * @param core
     * @param sess 
     */
    protected void updateHelper(Core core, Session sess) {
        Map prop = sess.getUserProperties( );
        prop.put(Core.class.getName(), core);
        prop.put(Session.class.getName(), sess);

        /**
         * 放入 Core 中以便在动作线程内随时取用
         */
        core.put(SocketHelper.class.getName(), this);
        core.put(ActionHelper.class.getName(), this);

        /**
         * 在 Jetty 中(其他的容器还没试)
         * 每次事件都有可能在不同的线程中
         * Core 却是为常规 Servlet 设计的
         * 故需每次事件判断 Core 与会话匹配否
         * 不匹配则重新设置 Core 动作环境信息
         */
        Core.ACTION_TIME.set(System.currentTimeMillis());
        Core.ACTION_NAME.set(Synt.declare(prop.get("ACTION_NAME"), ""));
        Core.ACTION_LANG.set(Synt.declare(prop.get("ACTION_LANG"), ""));
        Core.ACTION_ZONE.set(Synt.declare(prop.get("ACTION_ZONE"), ""));
    }

    /**
     * 构建环境
     * @param core
     * @param sess
     */
    protected void createHelper(Core core, Session sess) {
        Map prop = sess.getUserProperties( );
        prop.put(Core.class.getName(), core);
        prop.put(Session.class.getName(), sess);

        /**
         * 放入 Core 中以便在动作线程内随时取用
         */
        core.put(SocketHelper.class.getName(), this);
        core.put(ActionHelper.class.getName(), this);

        /**
         * 按照 Servlet 过程一样初始化动作环境
         */

        Core.ACTION_TIME.set(System.currentTimeMillis());

        String name  =  sess.getRequestURI( ).getPath( );
        if (Core.BASE_HREF.length() +1 <= name.length()) {
            Core.ACTION_NAME.set(name.substring(Core.BASE_HREF.length( ) +1));
        } else {
            throw new HongsError.Common( "Wrong web socket uri: "+ name);
        }

        CoreConfig conf = core.get(CoreConfig.class);

        Core.ACTION_ZONE.set(conf.getProperty("core.timezone.default", "GMT-8"));
        if (conf.getProperty("core.timezone.probing", false)) {
            /**
             * 时区可以记录到 Session 里
             */
            name = conf.getProperty("core.timezone.session", "zone");
            name = (String) this.getSessibute(name);

            if (name != null) {
                Core.ACTION_ZONE.set(name);
            }
        }

        Core.ACTION_LANG.set(conf.getProperty("core.language.default", "zh_CN"));
        if (conf.getProperty("core.language.probing", false)) {
            /**
             * 语言可以记录到 Session 里
             */
            name = conf.getProperty("core.language.session", "lang");
            name = (String) this.getSessibute(name);

            /**
             * 通过 WebSocket Headers 提取语言选项
             */
            if (name == null || name.length() == 0) {
                do {
                    Map <String, List<String>> headers;
                    headers  = ( Map <String, List<String>> )
                        this.getAttribute(SocketHelper.class.getName()+".httpHeaders");
                    if (headers == null) {
                        break ;
                    }
                    List<String> headerz;
                    headerz  = headers.get("Accept-Language");
                    if (headerz == null) {
                        break ;
                    }
                    name = headerz.isEmpty() ? headerz.get(0): null;
                } while(false);
            }

            /**
             * 检查是否是支持的语言
             */
            if (name != null) {
                name = CoreLocale.getAcceptLanguage(name);
            if (name != null) {
                Core.ACTION_LANG.set(name);
            }
            }
        }

        /**
         * 写入当前会话备用
         */
        setAttribute("ACTION_TIME", Core.ACTION_TIME.get());
        setAttribute("ACTION_NAME", Core.ACTION_NAME.get());
        setAttribute("ACTION_LANG", Core.ACTION_LANG.get());
        setAttribute("ACTION_ZONE", Core.ACTION_ZONE.get());

        /**
         * 输出一些调试信息
         */
        if (Core.DEBUG > 0) {
            StringBuilder sb = new StringBuilder("WebSocket start");
              sb.append("\r\n\tACTION_NAME : ").append(Core.ACTION_NAME.get())
                .append("\r\n\tACTION_TIME : ").append(Core.ACTION_TIME.get())
                .append("\r\n\tACTION_LANG : ").append(Core.ACTION_LANG.get())
                .append("\r\n\tACTION_ZONE : ").append(Core.ACTION_ZONE.get());
            CoreLogger.debug(sb.toString());
        }
    }

    /**
     * 销毁环境
     */
    public void destroy() {
        Core core = getCore();

        if ( Core.DEBUG > 0 ) {
            long time = System.currentTimeMillis(  ) - Core.ACTION_TIME.get();
            StringBuilder sb = new StringBuilder("...");
              sb.append("\r\n\tACTION_NAME : ").append(Core.ACTION_NAME.get())
                .append("\r\n\tACTION_TIME : ").append(Core.ACTION_TIME.get())
                .append("\r\n\tACTION_LANG : ").append(Core.ACTION_LANG.get())
                .append("\r\n\tACTION_ZONE : ").append(Core.ACTION_ZONE.get())
                .append("\r\n\tObjects     : ").append(core.keySet().toString())
                .append("\r\n\tRuntime     : ").append(Tool.humanTime(  time  ));
            CoreLogger.debug(sb.toString());
        }

        core.close();
    }

    public Core getCore() {
        return (Core) getAttribute(Core.class.getName());
    }

    public Session getSockSession() {
        return (Session) getAttribute(Session.class.getName());
    }

    public HttpSession getHttpSession() {
        return (HttpSession) getAttribute(HttpSession.class.getName());
    }

    /**
     * 获取实例
     * @param sess
     * @return
     */
    public static SocketHelper getInstance(Session sess) {
        Core         core = Core.getInstance();
        Map          prop = sess.getUserProperties ();
        SocketHelper hepr = (SocketHelper) prop.get(SocketHelper.class.getName());
        SocketHelper hepc = (SocketHelper) core.got(SocketHelper.class.getName());

        if (hepr == null) {
            /**
             * 提取和整理请求数据
             * 且合并路径上的参数
             */
            Map data  = (Map) prop.get(SocketHelper.class.getName()+".httpRequest");
            if (data == null) {
                data  = sess.getRequestParameterMap();
                data  = ActionHelper.parseParan(data);
            }   data.putAll(sess.getPathParameters());

            hepr = new SocketHelper(data, prop);
            hepr.createHelper(core, sess);
        } else
        if (hepc == null || ! hepc.equals(hepr)) {
            hepr.updateHelper(core, sess);
        }

        return hepr;
    }

    /**
     * 获取 HttpSession
     * 获取 WebSocket Session 请使用 getAttribute
     * @param name
     * @return
     */
    @Override
    public Object getSessibute(String  name) {
        HttpSession hsess = getHttpSession();
        if (null != hsess) {
            return  hsess.getAttribute(name);
        }
        return null;
    }

    /**
     * 设置 HttpSession
     * 设置 WebSocket Session 请使用 setAttribute
     * @param name
     * @param value
     */
    @Override
    public void setSessibute(String name, Object value) {
        HttpSession hsess = getHttpSession();
        if (null != hsess) {
        if (null != value) {
            hsess.   setAttribute(name, value);
        } else {
            hsess.removeAttribute(name);
        }
        }
    }

    /**
     * 获取输出流
     * @return
     */
    @Override
    public OutputStream getOutputStream() {
        Session sess = getSockSession();
        if (null != sess) {
            try {
                return sess.getBasicRemote().getSendStream();
            } catch (IOException ex) {
                throw new HongsError(0x32, "Can not get socket stream.", ex);
            }
        }
        return super.getOutputStream();
    }

    /**
     * 获取输出器
     * @return
     */
    @Override
    public Writer getOutputWriter() {
        Session sess = getSockSession();
        if (null != sess) {
            try {
                return sess.getBasicRemote().getSendWriter();
            } catch (IOException ex) {
                throw new HongsError(0x32, "Can not get socket writer.", ex);
            }
        }
        return super.getOutputWriter();
    }

    @Override
    public String getClientName() {
        InetSocketAddress rip  = (InetSocketAddress) getAttribute("javax.we‌​bsocket.endpoint.rem‌​oteAddress");
        if (rip != null) {
            return rip.getAddress().getHostAddress();
        }
        return null;
    }

    /**
     * 响应输出
     * 与 ActionHelper 不同, 此处会立即输出
     * @param dat
     */
    @Override
    public void reply(Map dat) {
        super.reply(dat);
         this.responed();
    }

    /**
     * 响应输出
     * 将响应数据立即发送到客户端
     */
    @Override
    public void responed() {
        Session sess = getSockSession();
        if (null != sess) {
            try {
                Map    map = getResponseData( );
                String str = Data.toString(map);
                sess.getBasicRemote().sendText(str);
            } catch (IOException ex ) {
                throw new HongsError(0x32, "Can not send to remote.", ex );
            }
        } else {
                throw new HongsError(0x32, "Session does not exist." /**/);
        }
    }

    /**
     * @deprecated WebSocket 中不支持
     * @param url 
     */
    @Override
    public void redirect(String url) {
        throw new UnsupportedOperationException("Can not redirect to "+url+" in web socket");
    }

    /**
     * @deprecated WebSocket 中不支持
     * @param msg 
     */
    @Override
    public void error400(String msg) {
        throw new UnsupportedOperationException("Can not send http stat 400 in web socket, msg: "+msg);
    }

    /**
     * @deprecated WebSocket 中不支持
     * @param msg 
     */
    @Override
    public void error401(String msg) {
        throw new UnsupportedOperationException("Can not send http stat 401 in web socket, msg: "+msg);
    }

    /**
     * @deprecated WebSocket 中不支持
     * @param msg 
     */
    @Override
    public void error403(String msg) {
        throw new UnsupportedOperationException("Can not send http stat 403 in web socket, msg: "+msg);
    }

    /**
     * @deprecated WebSocket 中不支持
     * @param msg 
     */
    @Override
    public void error404(String msg) {
        throw new UnsupportedOperationException("Can not send http stat 404 in web socket, msg: "+msg);
    }

    /**
     * @deprecated WebSocket 中不支持
     * @param msg 
     */
    @Override
    public void error405(String msg) {
        throw new UnsupportedOperationException("Can not send http stat 405 in web socket, msg: "+msg);
    }

    /**
     * @deprecated WebSocket 中不支持
     * @param msg 
     */
    @Override
    public void error500(String msg) {
        throw new UnsupportedOperationException("Can not send http stat 500 in web socket, msg: "+msg);
    }

    /**
     * WebSocket 配置器
     * 用于初始化请求环境和记录 HttpSession 等
     * 用于 \@ServerEndpoint(value="/xxx" configurator=SocketHelper.config)
     */
    static public class Config extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(
           ServerEndpointConfig   config,
                HandshakeRequest  request,
                HandshakeResponse response)
        {
            Map prop = config.getUserProperties();
            Map head = request.getHeaders (    );
            Map data = request.getParameterMap();
            data = ActionHelper.parseParan(data);

            prop.put(SocketHelper.class.getName()+".httpHeaders", head);
            prop.put(SocketHelper.class.getName()+".httpRequest", data);
            prop.put( HttpSession.class.getName(), request.getHttpSession());
        }
    }

    /**
     * WebSocket 加载器
     * 使用 _init_.properties 设置 mouse.serv 来告知 ServletContext 要加载哪些 WebSocket 类
     * 多个类名使用分号";"分隔
     */
    static public class Loader implements ServerCmdlet.Initer {

        @Override
        public void init(ServletContextHandler context) {
            ServerContainer contain;
            try {
                contain = WebSocketServerContainerInitializer.configureContext(context);
            } catch (ServletException ex) {
                throw new HongsError.Common(ex);
            }

            String pkgx  = CoreConfig.getInstance( "_init_" ).getProperty("jetty.sock");
            if  (  pkgx != null ) {
                String[]   pkgs = pkgx.split(";");
                for(String pkgn : pkgs) {
                    pkgn = pkgn.trim  ( );
                    if  (  pkgn.length( ) == 0  ) {
                        continue;
                    }

                    Set<String> clss = getClss(pkgn);
                    for(String  clsn : clss) {
                        Class   clso = getClso(clsn);

                        ServerEndpoint anno = (ServerEndpoint) clso.getAnnotation(ServerEndpoint.class);
                        if (anno != null) {
                            try {
                                contain.addEndpoint(clso);
                            } catch  (  DeploymentException ex) {
                                throw new HongsError.Common(ex);
                            }
                        }
                    }
                }
            }
        }

        private Class getClso(String clsn) {
            Class  clso;
            try {
                clso = Class.forName(clsn);
            } catch (ClassNotFoundException ex ) {
                throw new HongsError.Common("Can not find class '" + clsn + "'.", ex);
            }
            return clso;
        }

        private Set<String> getClss(String pkgn) {
            Set<String> clss;

            if (pkgn.endsWith(".**")) {
                pkgn = pkgn.substring(0, pkgn.length() - 3);
                try {
                    clss = Clses.getClassNames(pkgn, true );
                } catch (IOException ex) {
                    throw new HongsError.Common("Can not load package '" + pkgn + "'.", ex);
                }
                if (clss == null) {
                    throw new HongsError.Common("Can not find package '" + pkgn + "'.");
                }
            } else
            if (pkgn.endsWith(".*" )) {
                pkgn = pkgn.substring(0, pkgn.length() - 2);
                try {
                    clss = Clses.getClassNames(pkgn, false);
                } catch (IOException ex) {
                    throw new HongsError.Common("Can not load package '" + pkgn + "'.", ex);
                }
                if (clss == null) {
                    throw new HongsError.Common("Can not find package '" + pkgn + "'.");
                }
            } else {
                clss = new HashSet();
                clss.add  (  pkgn  );
            }

            return clss;
        }

    }

}
