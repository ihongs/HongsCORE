package io.github.ihongs.server.init;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import java.io.File;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * 会话存到文件里
 * @author Hongs
 */
public class SessInFile implements Initer {

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
