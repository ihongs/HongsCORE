package io.github.ihongs.combat.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.server.init.Initer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SameFileAliasChecker;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * 服务启动命令
 * @author Hongs
 */
@Combat("server")
public class ServerCombat {

    @Combat("__main__")
    public static void exec (String[] args) throws CruxException {
        if (args.length > 0) {
            String [ ] opts = new String [ -1+ args.length];
            System . arraycopy(args,1, opts,0, opts.length);
            if ("share".equals(args[0])) {
                 share(opts);
                return ;
            } else
            if ("start".equals(args[0])) {
                 start(opts);
                return ;
            }
        }
        System.err.println("Usage: server {start|share} [PORT]");
    }

    @Combat("share")
    public static void share(String[] args) throws CruxException {
        int    port = args.length >0 ? Integer.parseInt(args[0]) : 8080;
        String conf = Core.CORE_PATH + File.separator + "web.xml";
        if ( ! (new File(conf)).exists( ) ) {
               conf = Core.CONF_PATH + File.separator + "web.xml";
        }

        /**
         * 取消名称
         * 日志中将记录各独立的线程名
         * 以便于区分不同的非动作任务
         */
        Core.ACTION_NAME.remove();
        Core.BASE_PATH = System.getProperty("user.dir");

        Server          server;
        WebAppContext   webapp;
        ServerConnector conner;
        Connector[]     connes;

        server = new Server (new QueuedThreadPool());
        webapp = new WebAppContext( );
        conner = new ServerConnector(server);
        connes = new Connector[/**/]{conner};

        conner.setPort(port);
        conner.setHost(null);
        server.setConnectors(connes);

        webapp.setDescriptor( conf );
        webapp.setContextPath  (Core.SERV_PATH);
        webapp.setResourceBase (Core.BASE_PATH);
        webapp.setTempDirectory( new File(Core.DATA_PATH + "/server/temp"));
        webapp.setPersistTempDirectory ( true );
        webapp.setParentLoaderPriority ( true );
        webapp.setThrowUnavailableOnStartupException(true);
        webapp.addAliasCheck( new SameFileAliasChecker() ); // 开启软链支持
        server.setHandler   (webapp);

        String x;

        // 默认微调
        x = org.eclipse.jetty.servlet.DefaultServlet.CONTEXT_INIT;
        webapp.setInitParameter(x+"useFileMappedBuffer", "false");
        webapp.setInitParameter(x+"dirAllowed"         , "true" );

        // 启用软链、JSP
        new io.github.ihongs.server.init.Linker( ).init( webapp );
        new io.github.ihongs.server.init.Jasper( ).init( webapp );

        // 中止机制
        Runtime.getRuntime().addShutdownHook(new Stoper( server , null ));

        // 启动服务
        try {
            server.start();

            if (Core.DEBUG == 0 ) {
                System.err.println("HTTP server is started.");
            }

            /**
             * URL 会话参数等同于对应的 Cookie, 总是小写
             * server.start_ 执行之前 web.xml 还没解析
             */
                x = webapp.getInitParameter (SessionHandler.__SessionIdPathParameterNameProperty);
            if (x == null || x.isEmpty()) {
                x = webapp.getServletContext().getSessionCookieConfig().getName();
                x = x.toLowerCase( );
                    webapp.getSessionHandler().setSessionIdPathParameterName( x );
            }

            server.join( );
        } catch (Exception e) {
            throw new CruxException(e);
        } catch (Error     e) {
            throw new CruxExemption(e);
        }
    }

    @Combat("start")
    public static void start(String[] args) throws CruxException {
        int    port = args.length >0 ? Integer.parseInt(args[0]) : 8080;
        String proc = ManagementFactory.getRuntimeMXBean().getName().split("@", 2)[0]; // 进程ID
        String conf = Core.CORE_PATH + File.separator + "web.xml";
        if ( ! (new File(conf)).exists( ) ) {
               conf = Core.CONF_PATH + File.separator + "web.xml";
        }
        String serd = Core.DATA_PATH + File.separator + "server" ;
        File   ppid = new  File(serd + File.separator + "ppid" ) ;
        File   ppcd = new  File(serd);

        // 检查进程
        if (ppid.exists() != false) {
            System.err.println("ERROR: The server has not exit, or did not exit normally.");
            System.exit(126);
            return;
        }
        if (ppcd.exists() == false) {
            ppcd.mkdirs();
        }
        try (
            FileWriter fw  = new FileWriter(ppid, true);
        ) {
            fw.write(proc + " " + port);
            fw.close();
        } catch (IOException e) {
            throw new CruxException(e);
        }

        CoreConfig cc = CoreConfig.getInstance("defines");

        /**
         * 取消名称
         * 日志中将记录各独立的线程名
         * 以便于区分不同的非动作任务
         */
        Core.ACTION_NAME.remove();

        Server          server;
        WebAppContext   webapp;
        ServerConnector conner;
        Connector[]     connes;

        server = new Server (new QueuedThreadPool(
            cc.getProperty("jetty.pool.max.threads" ,  254 ),
            cc.getProperty("jetty.pool.min.threads" ,  010 ),
            cc.getProperty("jetty.pool.idle.timeout", 30000)
        ));
        webapp = new WebAppContext( );
        conner = new ServerConnector(server);
        connes = new Connector[/**/]{conner};

        conner.setPort(port);
        conner.setHost(null);
        conner.setIdleTimeout(cc.getProperty("jetty.conn.idle.timeout", 30000L));
        conner.setAcceptQueueSize(cc.getProperty("jetty.conn.accept.queue.size" , 254));
        conner.setAcceptedSendBufferSize(cc.getProperty("jetty.conn.accept.sndbuf.size", -1));
        conner.setAcceptedReceiveBufferSize(cc.getProperty("jetty.conn.accept.rcvbuf.size", -1));
        server.setConnectors(connes);

        webapp.setDescriptor( conf );
        webapp.setContextPath  (Core.SERV_PATH);
        webapp.setResourceBase (Core.BASE_PATH);
        webapp.setTempDirectory( new File(Core.DATA_PATH + "/server/temp"));
        webapp.setPersistTempDirectory ( true );
        webapp.setParentLoaderPriority ( true );
        webapp.setThrowUnavailableOnStartupException(true);
    //  webapp.setMaxFormKeys(cc.getProperty("jetty.serv.max.form.keys", 10000));
    //  webapp.setMaxFormContentSize(cc.getProperty("jetty.serv.max.form.size", 200000));
        server.setHandler   (webapp);

        String x;

        // 默认微调
        x = org.eclipse.jetty.servlet.DefaultServlet.CONTEXT_INIT;
        webapp.setInitParameter(x+"useFileMappedBuffer", "false");
        webapp.setInitParameter(x+"dirAllowed"         , "false");

        /**
         * 初始设置
         * 光能外部配置参数还不够方便
         * 可能需要替换 JSP 解析器或 Session 容器
         * 可以设置 jetty.init 来注入 Initer 对象
         */
        x = cc.getProperty("jetty.init");
        if (null !=  x) {
            String[] a = x.split(";");
            for ( String n  : a ) {
                     n = n.trim (   );
                if ( n.isEmpty()) {
                    continue;
                }

                try {
                    ((Initer) Class.forName(n).getDeclaredConstructor().newInstance())
                        .init(webapp);
                } catch (ClassNotFoundException e) {
                    throw new CruxExemption(e);
                } catch ( NoSuchMethodException e) {
                    throw new CruxExemption(e);
                } catch (InstantiationException e) {
                    throw new CruxExemption(e);
                } catch (IllegalAccessException e) {
                    throw new CruxExemption(e);
                } catch (InvocationTargetException e) {
                    throw new CruxExemption(e);
                }
            }
        }

        // 中止机制
        Runtime.getRuntime().addShutdownHook(new Stoper(server, ppid));

        // 启动服务
        try {
            server.start();

            if (Core.DEBUG == 0 ) {
                System.err.println("HTTP server is started.");
            }

            /**
             * URL 会话参数等同于对应的 Cookie, 总是小写
             * server.start_ 执行之前 web.xml 还没解析
             */
                x = webapp.getInitParameter (SessionHandler.__SessionIdPathParameterNameProperty);
            if (x == null || x.isEmpty()) {
                x = webapp.getServletContext().getSessionCookieConfig().getName();
                x = x.toLowerCase( );
                    webapp.getSessionHandler().setSessionIdPathParameterName( x );
            }

            server.join( );
        } catch (Exception e) {
            throw new CruxException(e);
        } catch (Error     e) {
            throw new CruxExemption(e);
        }
    }

    private static class Stoper extends Thread {

        private final Server server;
        private final File   ppid  ;

        public Stoper(Server server, File ppid) {
            this.server = server;
            this.ppid   = ppid  ;
        }

        @Override
        public void run() {
            System.out.println( "" );
            if (server.isStopping()) {
                if (Core.DEBUG == 0) {
                    System.err.println("HTTP server is stopping");
                }
                return;
            }
            if (server.isStopped ()) {
                if (Core.DEBUG == 0) {
                    System.err.println("HTTP server is stopped!");
                }
                return;
            }
            try {
                server.stop();
                if (Core.DEBUG == 0) {
                    System.err.println("HTTP server is stopped.");
                }

                // 核心重置, 释放资源
                Core.GLOBAL_CORE.reset();
            } catch ( Exception  e ) {
                throw new Error( e );
            } finally {
                if (ppid != null) {
                    ppid.delete();
                }
            }
        }

    }

}
