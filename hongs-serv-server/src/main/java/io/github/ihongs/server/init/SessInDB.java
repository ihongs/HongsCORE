package io.github.ihongs.server.init;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.db.DBConfig;
import java.util.Map;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.DatabaseAdaptor;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.JDBCSessionDataStore;
import org.eclipse.jetty.server.session.JDBCSessionDataStore.SessionTableSchema;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * 会话存到数据库
 * @author Hongs
 */
public class SessInDB  implements Initer {

    @Override
    public void init(ServletContextHandler sc) {
        CoreConfig  cc = CoreConfig.getInstance("defines");
        String dh = cc.getProperty("jetty.session.manager.db", "default");
        String th = cc.getProperty("jetty.session.manager.tb", "JettySessions");

        Server                   sv = sc . getServer             (  );
        DefaultSessionIdManager  im = new DefaultSessionIdManager(sv);
        im.setWorkerName        (Core.SERVER_ID);
        sv.setSessionIdManager  (im);

        SessionHandler           sh = sc . getSessionHandler  (  );
        DefaultSessionCache      ch = new DefaultSessionCache (sh);
        JDBCSessionDataStore     sd = new JDBCSessionDataStore(  );
        SessionTableSchema       st = new SessionTableSchema  (  );
        st.setTableName         (th);
        sd.setSessionTableSchema(st);
        sd.setDatabaseAdaptor   (getAdaptor(dh));
        ch.setSessionDataStore  (sd);
        sh.setSessionCache      (ch);
        sc.setSessionHandler    (sh);
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
