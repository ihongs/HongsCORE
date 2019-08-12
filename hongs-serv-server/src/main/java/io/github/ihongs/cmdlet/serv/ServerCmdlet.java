package io.github.ihongs.cmdlet.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsError;
import io.github.ihongs.HongsException;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.db.DBConfig;
import io.github.ihongs.util.reflex.Classes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
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
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet. FilterHolder;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;

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
        File   ppcd = ppid.getParentFile( );

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
            throw new HongsException.Common(e);
        }

        /**
         * 取消名称
         * 日志中将记录各独立的线程名
         * 以便于区分不同的非动作任务
         */
        Core.ACTION_NAME.remove();

        // 构建应用
        Server        server;
        WebAppContext webapp;
        server = new  Server ( port );
        webapp = new  WebAppContext();
        webapp.setDescriptor ( conf );
        webapp.setContextPath (Core.BASE_HREF);
        webapp.setResourceBase(Core.BASE_PATH);
        webapp.setParentLoaderPriority( true );
        webapp.setThrowUnavailableOnStartupException ( true ); // 缺了这个启动时异常看不到
        server.setHandler    (webapp);

        // 外部配置
        CoreConfig c = CoreConfig.getInstance("defines");
        for(Map.Entry  t : c.entrySet( )) {
            String k = (String) t.getKey  ();
            String v = (String) t.getValue();
            if (k.startsWith("jetty.attr.")) {
                webapp.setAttribute    ( k.substring(11), v );
            } else
            if (k.startsWith("jetty.para.")) {
                webapp.setInitParameter( k.substring(11), v );
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

        // 中止机制
        Runtime.getRuntime().addShutdownHook(new Stoper(server, ppid));

        // 启动服务
        try {
            server.setHandler(webapp);
            server.start();
        //  server.dumpStdErr();
            server.join( );
        } catch (Exception e) {
            throw new HongsException.Common(e);
        } catch (Error     e) {
            throw new HongsError    .Common(e);
        }
    }

    private static class Stoper extends Thread {

        private final Server server;
        private final File   ppid  ;

        public Stoper(Server server, File ppid) {
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
                throw new HongsError.Common(e);
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
                throw new HongsError.Common("Wrong session manager jdbc!");
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
        }

        private void addListener(ServletContextHandler context, Class clso, WebListener anno) {
            try {
                EventListener evto = (EventListener) clso.newInstance();
                context.addEventListener(evto);
            } catch (InstantiationException e) {
                throw new HongsError.Common(e);
            } catch (IllegalAccessException e) {
                throw new HongsError.Common(e);
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
                    clss = Classes.getClassNames(pkgn, true );
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
                    clss = Classes.getClassNames(pkgn, false);
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

    /**
     * @see Swaper
     * @deprecated 改用 Snaper
     */
    public static class Sesion extends Snaper {}

}
