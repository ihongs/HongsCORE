package app.hongs.cmdlet.serv;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.cmdlet.CmdletHelper;
import app.hongs.cmdlet.anno.Cmdlet;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.lang.management.ManagementFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.servlet.ServletContextHandler;

// JSP,Session 初始器依赖的类
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.server.session.HashSessionManager;

/**
 * 服务启动命令
 * @author Hongs
 */
@Cmdlet("server")
public class ServerCmdlet {

    @Cmdlet("start")
    public static void start(String[] args) throws HongsException {
        int    port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        String conf = Core.CORE_PATH + File.separator + "web.xml";
        if ( ! (new File(conf)).exists( ) ) {
               conf = Core.CONF_PATH + File.separator + "web.xml";
        }
        String serd = Core.DATA_PATH + File.separator + "server" ;
        File   ppid = new  File(serd + File.separator +  port + ".pid" );

        // 检查进程
        if ( ppid.exists() ) {
            CmdletHelper.println("Process already exists!");
            return;
        }
        try {
            String     pid = ManagementFactory.getRuntimeMXBean()
                            .getName().split("@" , 2)[0];
            FileWriter dip = new  FileWriter(ppid, true);
            dip.write( pid );
            dip.close(     );
        }
        catch (IOException e) {
            throw new HongsException.Common (e);
        }

        // 构建应用
        WebAppContext webapp  = new WebAppContext();
        webapp.setDescriptor   (conf);
        webapp.setContextPath  (Core.BASE_HREF);
        webapp.setResourceBase (Core.BASE_PATH);
        webapp.setParentLoaderPriority ( true );

        // 外部配置
        CoreConfig c = CoreConfig.getInstance("_init_");
        for(String k : c.stringPropertyNames()) {
            String v = c.getProperty(k);
            if (k.startsWith("jetty.attr.")) {
                webapp.setAttribute(k.substring(11), v);
            } else
            if (k.startsWith("jetty.init.")) {
                webapp.setInitParameter(k.substring(11), v);
            }
        }

        /**
         * 初始设置
         * 光能外部配置参数还不够方便
         * 可能需要替换 JSP 解析器或 Session 容器
         * 可以设置 jetty.init 来注入 Initer 对象
         */
        String xs = c.getProperty("jetty.init");
        if (null !=  xs) {
            String[] xa = xs.split(";");
            for ( String  xn: xa ) {
                     xn = xn.trim (   );
                if ("".equals(xn)) {
                    continue;
                }

                try {
                    ((Initer) Class.forName(xn).newInstance()).init(webapp);
                } catch (ClassNotFoundException ex) {
                    throw new HongsError.Common(ex);
                } catch (InstantiationException ex) {
                    throw new HongsError.Common(ex);
                } catch (IllegalAccessException ex) {
                    throw new HongsError.Common(ex);
                }
            }
        }

        Server server;
        server = new Server(port);
        server.setHandler(webapp);

        // 停止机制
        Runtime.getRuntime( ).addShutdownHook(new Stoper(server, ppid));

        try {
            server.start();
            server.join( );
        }
        catch (  Exception e) {
            throw new HongsException.Common(e);
        }
    }

    private static class Stoper extends Thread {

        private final org.eclipse.jetty.server.Server server;
        private final File ppid  ;

        public Stoper(org.eclipse.jetty.server.Server server, File ppid) {
            this.server  = server;
            this.ppid    = ppid  ;
        }

        @Override
        public void run() {
            System.out.println("");
            if (server.isStopped( )) {
                System.err.println("Server is stopped !!!");
                return;
            }
            if (server.isStopping()) {
                System.err.println("Server is stopping...");
                return;
            }
            try {
                server.stop();
            } catch ( Exception ex) {
                throw new Error(ex);
            } finally {
                ppid.delete();
            }
        }

    }

    /**
     * Servlet 容器初始器
     */
    public static interface Initer {

        public void init(ServletContextHandler context);

    }

    /**
     * JSP 初始器
     */
    public static class Jasper implements Initer {

        @Override
        public void init(ServletContextHandler sc) {
            File dh = new File(Core.DATA_PATH + File.separator + "server"+ File.separator + "temp");
            if (!dh.exists()) {
                 dh.mkdirs();
            }
            sc.setAttribute("javax.servlet.context.tempdir", dh);
            sc.setAttribute("org.eclipse.jetty.containerInitializers",
              Arrays.asList(new ContainerInitializer(new JettyJasperInitializer(),null)));
            sc.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        }

    }

    /**
     * 会话初始器
     */
    public static class Sesion implements Initer {

        @Override
        public void init(ServletContextHandler sc) {
            File dh = new File(Core.DATA_PATH + File.separator + "server"+ File.separator + "sess");
            if (!dh.exists()) {
                 dh.mkdirs();
            }
            try {
                HashSessionManager sm = new HashSessionManager();
    //          sm.setHttpOnly( true  ); sm.setLazyLoad( true  );
    //          sm.setSessionCookie/*rameterNa*/(Cnst.CSID_KEY );
    //          sm.setSessionIdPathParameterName(Cnst.PSID_KEY );
                sm.setStoreDirectory( dh );
                /**/SessionHandler sh = new SessionHandler( sm );
                sc.setSessionHandler( sh );
            }
            catch (IOException e) {
                throw new HongsError.Common(e);
            }
        }

    }

}
