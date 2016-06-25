package app.hongs.cmdlet.serv;

import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.cmdlet.CmdletHelper;
import app.hongs.cmdlet.anno.Cmdlet;
import app.hongs.util.Synt;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
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
        int port = args.length > 0 ? Synt.declare(args[0], 8080) : 8080;
        String conf = Core.CORE_PATH + File.separator + "web.xml";
        if ( ! (new File(conf)).exists( ) ) {
               conf = Core.CONF_PATH + File.separator + "web.xml";
        }
        String serd = Core.DATA_PATH + File.separator + "server" ;
        String sesd = serd + File.separator + "sess";
        String temd = serd + File.separator + "work";
        String pidf = serd + File.separator +  port + ".pid";
        File sess = new File(sesd);
        File temp = new File(temd);
        File ppid = new File(pidf);

        if (!sess.exists() ) {
             sess.mkdirs();
        }
        if (!temp.exists() ) {
             temp.mkdirs();
        }

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
            throw new HongsException.Common(e);
        }

        // 构建应用
        WebAppContext webapp  = new WebAppContext();
        webapp.setDescriptor   (conf);
        webapp.setTempDirectory(temp);
        webapp.setDisplayName  ("CORE");
        webapp.setContextPath  (Core.BASE_HREF);
        webapp.setResourceBase (Core.BASE_PATH);
        webapp.setParentLoaderPriority ( true );

        // 设置会话
        try {
            HashSessionManager sm = new HashSessionManager();
//          sm.setHttpOnly( true  ); sm.setLazyLoad( true  );
//          sm.setSessionCookie/*rameterNa*/(Cnst.CSID_KEY );
//          sm.setSessionIdPathParameterName(Cnst.PSID_KEY );
            sm.setStoreDirectory( sess );
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

}
