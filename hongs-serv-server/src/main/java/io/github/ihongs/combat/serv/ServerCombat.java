package io.github.ihongs.combat.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreRoster;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.db.DBConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.EventListener;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebInitParam;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.servlet.ServletContextHandler;

// Session 初始化依赖的类
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.server.session.DatabaseAdaptor;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.JDBCSessionDataStore;

// JSP 初始化依赖的类
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.SameFileAliasChecker;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * 服务启动命令
 * @author Hongs
 */
@Combat("server")
public class ServerCombat {

    @Combat("start")
    public static void start(String[] args) throws HongsException {
        int    port = args.length >0 ? Integer.parseInt(args[0]) : 8080;
        String conf = Core.CORE_PATH + File.separator + "web.xml";
        if ( ! (new File(conf)).exists( ) ) {
               conf = Core.CONF_PATH + File.separator + "web.xml";
        }
        String serd = Core.DATA_PATH + File.separator + "server" ;
        File   ppid = new  File(serd + File.separator +  port + ".pid");
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
            FileWriter fw  = new FileWriter( ppid, true );
        ) {
            fw.write(ManagementFactory.getRuntimeMXBean()
                        .getName( ).split( "@", 2 )[ 0 ]);
            fw.close(  );
        } catch (IOException e) {
            throw new HongsException(e);
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
                    ((Initer) Class.forName (n).getDeclaredConstructor().newInstance())
                        .init(webapp);
                } catch (ClassNotFoundException e) {
                    throw new HongsExemption(e);
                } catch ( NoSuchMethodException e) {
                    throw new HongsExemption(e);
                } catch (InstantiationException e) {
                    throw new HongsExemption(e);
                } catch (IllegalAccessException e) {
                    throw new HongsExemption(e);
                } catch (InvocationTargetException e) {
                    throw new HongsExemption(e);
                }
            }
        }

        // 中止机制
        Runtime.getRuntime( ).addShutdownHook( new Stoper(server, ppid) );

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
            throw new HongsException(e);
        } catch (Error     e) {
            throw new HongsExemption(e);
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
     * Win 的软链
     */
    public static class Linker implements Initer {

        @Override
        public void init(ServletContextHandler sc) {
            sc.addAliasCheck(new SameFileAliasChecker());
        }

    }

    /**
     * JSP 初始器
     */
    public static class Jasper implements Initer {

        @Override
        public void init(ServletContextHandler sc) {
            CoreConfig cc = CoreConfig.getInstance("defines");
            String dn = cc.getProperty("jetty.servlet.context.path", "server" + File.separator + "temp");
            File   dh = new File(dn);
            if ( ! dh.isAbsolute() ) {
                   dn = Core.DATA_PATH + File.separator + dn ;
                   dh = new File(dn);
            }
            if ( ! dh.exists() /**/) {
                   dh.mkdirs();
            }

            sc.setAttribute("javax.servlet.context.tempdir", dh);
            sc.setAttribute("org.eclipse.jetty.containerInitializers",
              Arrays.asList(new ContainerInitializer(new JettyJasperInitializer(),null)));
            sc.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());
        }

    }

    /**
     * 会话初始器(存文件)
     */
    public static class Snaper implements Initer {

        @Override
        public void init(ServletContextHandler sc) {
            CoreConfig  cc = CoreConfig.getInstance("defines");
            String dn = cc.getProperty("jetty.session.manager.path", "server" + File.separator + "sess");
            File   dh = new File(dn);
            if ( ! dh.isAbsolute() ) {
                   dn = Core.DATA_PATH + File.separator + dn;
                   dh = new File(dn);
            }
            if ( ! dh.exists() /**/) {
                   dh.mkdirs();
            }

            Server                  sv = sc . getServer             (  );
            DefaultSessionIdManager im = new DefaultSessionIdManager(sv);
            im.setWorkerName       (Core.SERVER_ID);
            sv.setSessionIdManager (im);

            SessionHandler          sh = sc . getSessionHandler  (  );
            DefaultSessionCache     ch = new DefaultSessionCache (sh);
            FileSessionDataStore    sd = new FileSessionDataStore(  );
            sd.setStoreDir         (dh);
            ch.setSessionDataStore (sd);
            sh.setSessionCache     (ch);
            sc.setSessionHandler   (sh);
        }

    }

    /**
     * 会话初始器(数据库)
     */
    public static class Swaper implements Initer {

        @Override
        public void init(ServletContextHandler sc) {
            CoreConfig  cc = CoreConfig.getInstance("defines");
            String dh = cc.getProperty("jetty.session.manager.db", "default");

            Server                  sv = sc . getServer             (  );
            DefaultSessionIdManager im = new DefaultSessionIdManager(sv);
            im.setWorkerName       (Core.SERVER_ID);
            sv.setSessionIdManager (im);

            SessionHandler          sh = sc . getSessionHandler  (  );
            DefaultSessionCache     ch = new DefaultSessionCache (sh);
            JDBCSessionDataStore    sd = new JDBCSessionDataStore(  );
            sd.setDatabaseAdaptor  (getAdaptor(dh));
            ch.setSessionDataStore (sd);
            sh.setSessionCache     (ch);
            sc.setSessionHandler   (sh);
        }

        private DatabaseAdaptor getAdaptor(String dh) {
            DBConfig conf;
            try {
                conf= new DBConfig(dh);
            } catch (HongsException e) {
                throw new HongsExemption (e);
            }

            if (conf.link != null && conf.link.length() != 0 ) {
                return getAdaptor(conf.link);
            } else
            if (conf.origin != null && !conf.origin.isEmpty()) {
                dh = (String) conf.origin.get("name");
                dh = ( dh + getOptions (conf.origin));
                DatabaseAdaptor da = new DatabaseAdaptor();
                da.setDatasourceName(dh);
                return da;
            } else
            if (conf.source != null && !conf.source.isEmpty()) {
                String dt ;
                dt = (String) conf.source.get("jdbc");
                dh = (String) conf.source.get("name");
                dh = ( DBConfig.fixSourceName( dh  ));
                dh = ( dh + getOptions (conf.source));
                DatabaseAdaptor da = new DatabaseAdaptor();
                da.setDriverInfo(dt, dh);
                return da;
            } else {
                throw new HongsExemption("Wrong session manager jdbc!");
            }
        }

        private String getOptions( Map dc ) {
            Map dp = (Map) dc.get( "info" );
            if (dp == null || dp.isEmpty()) {
                return "";
            }

            // 拼接用户名密码
            StringBuilder sb = new StringBuilder();
            if (dp.containsKey("username")) {
                sb.append("&user="/**/).append(dp.get("username"));
            }
            if (dp.containsKey("password")) {
                sb.append("&password=").append(dp.get("password"));
            }
            // Jetty 采用序列化存储, 无特别数据类型,
            // 附加参数会反而被作为库名部分导致错误.
            /*
            if (dp.containsKey("connectionProperties")) {
                sb.append("&").append(((String)dp.get("connectionProperties")).replace(';','&'));
            }
            */
            if (sb.length(   ) != 0 ) {
                sb.setCharAt (0, '?');
                return sb.toString( );
            }

            return "";
        }

    }

    /**
     * 应用加载器
     */
    public static class Loader implements Initer {

        @Override
        public void init(ServletContextHandler context) {
            String pkgx  = CoreConfig.getInstance("defines"   )
                                     .getProperty("apply.serv");
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

                        WebFilter   wf = (WebFilter  ) clso.getAnnotation(WebFilter.class  );
                        if (null != wf) {
                            addFilter  (context, clso, wf);
                        }

                        WebServlet  wb = (WebServlet ) clso.getAnnotation(WebServlet.class );
                        if (null != wb) {
                            addServlet (context, clso, wb);
                        }

                        WebListener wl = (WebListener) clso.getAnnotation(WebListener.class);
                        if (null != wl) {
                            addListener(context, clso, wl);
                        }
                    }
                }
            }
        }

        private void addFilter(ServletContextHandler context, Class clso, WebFilter anno) {
            DispatcherType[]  ds = anno.dispatcherTypes(  );
            List   <DispatcherType> ls = Arrays .asList(ds);
            EnumSet<DispatcherType> es = EnumSet.copyOf(ls);

            FilterHolder  hd = new FilterHolder (clso );
            hd.setName          (anno.filterName(    ));
            hd.setAsyncSupported(anno.asyncSupported());

            for(WebInitParam nv : anno.initParams ()) {
                hd.setInitParameter(nv.name( ), nv.value());
            }

            for(String       ur : anno.urlPatterns()) {
                context.addFilter (hd, ur, es);
            }
            for(String       ur : anno.value()) {
                context.addFilter (hd, ur, es);
            }
        }

        private void addServlet(ServletContextHandler context, Class clso, WebServlet anno) {
            ServletHolder hd = new ServletHolder(clso );
            hd.setName          (anno./****/name(    ));
            hd.setAsyncSupported(anno.asyncSupported());

            for(WebInitParam nv : anno.initParams ()) {
                hd.setInitParameter(nv.name( ), nv.value());
            }

            for(String       ur : anno.urlPatterns()) {
                context.addServlet(hd, ur/**/);
            }
            for(String       ur : anno.value()) {
                context.addServlet(hd, ur/**/);
            }
        }

        private void addListener(ServletContextHandler context, Class clso, WebListener anno) {
            try {
                EventListener evto = (EventListener) clso.newInstance();
                context.addEventListener(evto);
            } catch (InstantiationException e) {
                throw new HongsExemption(e);
            } catch (IllegalAccessException e) {
                throw new HongsExemption(e);
            }
        }

        private Class getClso(String clsn) {
            Class  clso;
            try {
                clso = Class.forName(clsn);
            } catch (ClassNotFoundException ex ) {
                throw new HongsExemption(ex, "Can not find class '" + clsn + "'.");
            }
            return clso;
        }

        private Set<String> getClss(String pkgn) {
            Set<String> clss;

            if (pkgn.endsWith(".**")) {
                pkgn = pkgn.substring(0, pkgn.length() - 3);
                try {
                    clss = CoreRoster.getClassNames(pkgn, true );
                } catch (IOException ex) {
                    throw new HongsExemption(ex, "Can not load package '" + pkgn + "'.");
                }
                if (clss == null) {
                    throw new HongsExemption("Can not find package '" + pkgn + "'.");
                }
            } else
            if (pkgn.endsWith(".*" )) {
                pkgn = pkgn.substring(0, pkgn.length() - 2);
                try {
                    clss = CoreRoster.getClassNames(pkgn, false);
                } catch (IOException ex) {
                    throw new HongsExemption(ex, "Can not load package '" + pkgn + "'.");
                }
                if (clss == null) {
                    throw new HongsExemption("Can not find package '" + pkgn + "'.");
                }
            } else {
                clss = new HashSet();
                clss.add  (  pkgn  );
            }

            return clss;
        }

    }

    /**
     * @see Swaper
     * @deprecated 改用 Snaper
     */
    public static class Sesion extends Snaper {}

}
