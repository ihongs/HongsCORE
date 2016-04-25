package app.hongs.cmdlet.serv;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.cmdlet.CmdletHelper;
import app.hongs.cmdlet.anno.Cmdlet;
import app.hongs.util.Synt;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;

/**
 * 服务启动命令
 * @author Hongs
 */
@Cmdlet("server")
public class ServerCmdlet {

    private static File PPID;

    @Cmdlet("start")
    public static void start(String[] args) throws HongsException {
        int port = args.length > 0 ? Synt.declare(args[0], 8080) : 8080;

        // 进程检查
        if (port != 8080 ) {
            PPID  = new File(Core.DATA_PATH +"/server."+ port +".pid" );
        } else {
            PPID  = new File(Core.DATA_PATH +"/server.pid");
        }
        if (PPID.exists()) {
            CmdletHelper.println("Process already exists!");
            return;
        }
        try {
            String     pid = ManagementFactory.getRuntimeMXBean()
                            .getName().split("@" , 2)[0];
            FileWriter dip = new  FileWriter(PPID, true);
            dip.write( pid );
            dip.close(     );
        }
        catch (IOException e) {
            throw new HongsException.Common(e);
        }

        String conf = Core.CORE_PATH + File.separator + "web.xml";
        if ( ! ( new  File(conf) ).exists( ) ) {
               conf = Core.CONF_PATH + File.separator + "web.xml";
        }
        String temp = Core.DATA_PATH + File.separator + "server" ;

        WebAppContext webapp  = new WebAppContext();
        webapp.setDescriptor   (conf);
        webapp.setDisplayName  ("CORE");
        webapp.setContextPath  (Core.BASE_HREF);
        webapp.setResourceBase (Core.BASE_PATH);
        webapp.setTempDirectory(new File(temp));
        webapp.setParentLoaderPriority ( true );

        // 设置会话
        try {
            File sd = new File( Core.DATA_PATH + "/sesion" );
            if (!sd.exists()) {
                 sd.mkdirs();
            }
            HashSessionManager sm = new HashSessionManager();
//          sm.setHttpOnly( true  ); sm.setLazyLoad( true  );
//          sm.setSessionCookie/*rameterNa*/(Cnst.CSID_KEY );
//          sm.setSessionIdPathParameterName(Cnst.PSID_KEY );
            sm.setStoreDirectory(/**/sd);
            SessionHandler/**/ sh = new SessionHandler( sm );
            webapp.setSessionHandler(sh);
        }
        catch (IOException e) {
            throw new HongsException.Common(e);
        }

        Server server;
        server = new Server(port);
        server.setHandler(webapp);

        // 停止机制
        Runtime.getRuntime( ).addShutdownHook(new Stoper(server));
        try {
            server.start();
            server.join( );
        }
        catch (  Exception e) {
            throw new HongsException.Common(e);
        }
    }

    private static class Stoper extends Thread {

        private final/**/ org.eclipse.jetty.server.Server server;

        public Stoper(org.eclipse.jetty.server.Server server) {
            this.server = server;
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
            } catch (Exception ex) {
                throw new HongsError.Common(ex);
            } finally {
                PPID.delete();
            }
        }

    }

}
