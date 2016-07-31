package app.hongs.action;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLocale;
import app.hongs.HongsError;
import app.hongs.cmdlet.serv.ServerCmdlet;
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
 * @author Hongs
 */
public class SocketHelper extends ActionHelper {

    private SocketHelper(Map prop, Map data) {
        super( data, prop, null, null, null);
    }

    @Override
    public Object getSessibute(String  name) {
        HttpSession hsess = (HttpSession) getAttribute(HttpSession.class.getName());
        if (null != hsess) {
            return  hsess.getAttribute(name);
        }
        return null;
    }

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
     * 构建助手对象
     * @param data
     * @param prop
     * @param uri
     * @return
     */
    static public SocketHelper getInstance(Map prop, Map data, String uri) {
        SocketHelper hepr;
        hepr = (SocketHelper) prop.get(SocketHelper.class.getName());
        if ( null != hepr) {
            return   hepr;
        }

        Core core;
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
        Core.ACTION_NAME.set(  uri.substring ( 1 ) );

        CoreConfig conf = core.get(CoreConfig.class);

        Core.ACTION_ZONE.set(conf.getProperty("core.timezone.default","GMT-8"));
        if (conf.getProperty("core.timezone.probing", false)) {
            /**
             * 时区可以记录到 Session 里
             */
            String name = conf.getProperty("core.timezone.session", "zone");
            String zone = (String) hepr.getSessibute(name);
            if (zone == null || zone.length() == 0) {
                   zone = (String) hepr.getAttribute( "Accept-Timezone" );
            }

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
            if (lang == null || lang.length() == 0) {
                   lang = (String) hepr.getAttribute( "Accept-Language" );
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

        return  hepr;
    }

    /**
     * 获取助手对象
     * @param sess
     * @return
     */
    public static SocketHelper getInstance(Session sess) {
        SocketHelper hepr;
        hepr = (SocketHelper) sess.getUserProperties().get(SocketHelper.class.getName());
        if ( null != hepr) {
            return   hepr;
        }

        String u = sess.getRequestURI().toString();
        Map data = sess.getRequestParameterMap();
        Map prop = sess.getUserProperties ( );
        data = ActionHelper.parseQuest(data );
        data.putAll(sess.getPathParameters());

        return getInstance( prop , data , u );
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
             * 准备 SocketHelper
             */
            String u = request.getRequestURI().toString();
            Map data = request.getParameterMap ();
            Map prop = config.getUserProperties();
            data = ActionHelper.parseQuest(data );

            /**
             * 注入 HttpSession, Accept-Language
             */
            prop.put( HttpSession.class.getName(), request.getHttpSession() );
            List<String> lang = request.getHeaders().get( "Accept-Langauge" );
            if (null  != lang && ! lang.isEmpty()) {
                prop.put("Accept-Language" , lang.get(0));
            }

            /**
             * 构建 SocketHelper
             */
            getInstance(prop, data, u);
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
