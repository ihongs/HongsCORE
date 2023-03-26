package io.github.ihongs.server.init;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreRoster;
import io.github.ihongs.HongsExemption;
import java.io.IOException;
import java.util.EventListener;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.DispatcherType;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.WebListener;
import javax.servlet.annotation.WebInitParam;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * 应用加载器
 * @author Hongs
 */
public class Loader implements Initer {

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
