package app.hongs.action;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLocale;
import app.hongs.CoreLogger;
import app.hongs.HongsError;
import app.hongs.cmdlet.serv.ServerCmdlet;
import app.hongs.util.Synt;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.websocket.DeploymentException;
import javax.websocket.HandshakeResponse;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

/**
 * WebSocket 助手类
 *
 * <code>
 *  // 事件处理器示例:
 *  @ServerEndpoint(value="/sock/path/{xxx}", configurator=SocketHelper.Config.class)
 *  public class Xxx {
 *      @OnXxx
 *      public void onXxx(Session sess) {
 *          SocketHelper helper = SocketHelper.getInstance(sess);
 *          try {
 *              // TODO: Some thing...
 *          }
 *          catch (Error|Exception ex) {
 *              CoreLogger.error ( ex);
 *          }
 *          finally {
 *              helper.destroy(); // 销毁环境
 *          }
 *      }
 *  }
 *  // 并将此类名(含包名)加入 _init_.properties 中 jetty.webs 值
 * </code>
 *
 * @author Hongs
 */
public class SocketHelper extends ActionHelper {

    private SocketHelper(Map prop, Map data) {
        super(data, prop, null, null);
    }

    /**
     * 销毁环境
     */
    public void destroy() {
        getCore().destroy();
    }

    /**
     * 获取核心
     * @return
     */
    public Core getCore() {
        return (Core) getAttribute(Core.class.getName());
    }

    /**
     * 获取 HttpSession
     * 获取 WebSocket Session 请使用 getAttribute
     * @param name
     * @return
     */
    @Override
    public Object getSessibute(String  name) {
        HttpSession hsess = (HttpSession) getAttribute(HttpSession.class.getName());
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
        HttpSession hsess = (HttpSession) getAttribute(HttpSession.class.getName());
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
        Session sess = (Session) getAttribute(Session.class.getName());
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
        Session sess = (Session) getAttribute(Session.class.getName());
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
    public void responed() {
        print(getResponseData());
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
     * 构建助手对象
     * @param data
     * @param prop
     * @param uri
     * @return
     */
    static public SocketHelper newInstance(Map prop, Map data, String uri) {
        Core         core;
        SocketHelper hepr;
        core = Core.getInstance();
        hepr = new SocketHelper(prop, data);

        /**
         * 放入 Core,UserProperties 中随时取用
         */

        prop.put(Core.class.getName(), core );
        prop.put(SocketHelper.class.getName(), hepr);
        core.put(SocketHelper.class.getName(), hepr);
        if(!prop.containsKey(ActionHelper.class.getName())) {
            prop.put(ActionHelper.class.getName(), hepr);
        }
        if(!core.containsKey(ActionHelper.class.getName())) {
            core.put(ActionHelper.class.getName(), hepr);
        }

        /**
         * 按照 Servlet 过程一样初始化动作环境
         */

        Core.ACTION_TIME.set(System.currentTimeMillis());
        if (Core.BASE_HREF.length() + 1 <= uri.length()) {
            Core.ACTION_NAME.set(uri.substring(Core.BASE_HREF.length() + 1));
        } else {
            throw  new  HongsError.Common ( "Wrong web socket uri: " + uri );
        }

        CoreConfig conf = core.get(CoreConfig.class);

        Core.ACTION_ZONE.set(conf.getProperty("core.timezone.default","GMT-8"));
        if (conf.getProperty("core.timezone.probing", false)) {
            /**
             * 时区可以记录到 Session 里
             */
            String name = conf.getProperty("core.timezone.session", "zone");
            String zone = (String) hepr.getSessibute(name);

            if (zone != null) {
                Core.ACTION_ZONE.set(zone);
            }
        }

        Core.ACTION_LANG.set(conf.getProperty("core.language.default","zh_CN"));
        if (conf.getProperty("core.language.probing", false)) {
            /**
             * 语言可以记录到 Session 里
             */
            String name = conf.getProperty("core.language.session", "lang");
            String lang = (String) hepr.getSessibute(name);

            /**
             * 通过 WebSocket Headers 提取语言选项
             */
            if (lang == null || lang.length() == 0) {
                do {
                    Map <String, List<String>> headers;
                    headers  = ( Map <String, List<String>> ) hepr.getAttribute("HttpHeaders");
                    if (headers == null) {
                        break ;
                    }
                    List<String> headerz;
                    headerz  = headers.get("Accept-Language");
                    if (headerz == null) {
                        break ;
                    }
                    lang = headerz.isEmpty() ? headerz.get(0): null;
                } while(false);
            }

            /**
             * 检查是否是支持的语言
             */
            if (lang != null) {
                lang = CoreLocale.getAcceptLanguage(lang);
            if (lang != null) {
                Core.ACTION_LANG.set(lang);
            }
            }
        }

        /**
         * 写入当前会话备用
         */
        prop.put("ACTION_NAME", Core.ACTION_NAME.get());
        prop.put("ACTION_TIME", Core.ACTION_TIME.get());
        prop.put("ACTION_LANG", Core.ACTION_LANG.get());
        prop.put("ACTION_ZONE", Core.ACTION_ZONE.get());

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

        return  hepr;
    }

    /**
     * 获取助手对象
     * @param sess
     * @return
     */
    public static SocketHelper getInstance(Session sess) {
        Core  core = Core.getInstance();
        Map   prop = sess.getUserProperties();
        Session sexx = (Session) core.got(Session.class.getName());
        SocketHelper hepr = (SocketHelper) prop.get(SocketHelper.class.getName());

        if ( null == hepr ) {
            /**
             * 提取数据和路径等
             * 用于构造助手对象
             */
            String u = sess.getRequestURI().getPath();
            Map data = sess.getRequestParameterMap( );
            data = ActionHelper.parseQuest(data );
            data.putAll(sess.getPathParameters());
            hepr = newInstance( prop , data , u );

            core.put(Session.class.getName(), sess);
        } else
        if ( null != sexx && !sess.equals(sexx) ) {
            core.put(Session.class.getName(), sess);

            /**
             * 在 Jetty 中(别的应用容器还没试)
             * 每次事件都有可能在不同的线程中
             * Core 却是为常规 Servlet 设计的
             * 故需每次事件判断 Core 与会话匹配否
             * 不匹配则重新设置 Core 动作环境信息
             */
            Core.ACTION_TIME.set(System.currentTimeMillis());
            Core.ACTION_NAME.set(Synt.asserts(prop.get("ACTION_NAME"), ""));
            Core.ACTION_LANG.set(Synt.asserts(prop.get("ACTION_LANG"), ""));
            Core.ACTION_ZONE.set(Synt.asserts(prop.get("ACTION_ZONE"), ""));
        }

        return hepr;
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
            /**
             * 注入 HttpSession, Headers
             */
            Map prop = config.getUserProperties();
            prop.put("HttpHeaders", request.getHeaders( ));
            prop.put( HttpSession.class.getName(), request.getHttpSession());
        }
    }

    /**
     * WebSocket 加载器
     * 使用 _init_.properties 设置 jetty.webs 来告知 ServletContext 要加载哪些 WebSocket 类
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

            CoreConfig   c = CoreConfig.getInstance("_init_");
            String ws =  c.getProperty("jetty.webs");
            if (null !=  ws) {
                String[] wa = ws.split(";");
                for ( String  wn: wa) {
                         wn = wn.trim (   );
                    if ("".equals(wn)) {
                        continue;
                    }

                    try {
                        contain.addEndpoint(Class.forName(wn));
                    } catch (ClassNotFoundException ex) {
                        throw new HongsError.Common(ex);
                    } catch (   DeploymentException ex) {
                        throw new HongsError.Common(ex);
                    }
                }
            }
        }

    }

}
