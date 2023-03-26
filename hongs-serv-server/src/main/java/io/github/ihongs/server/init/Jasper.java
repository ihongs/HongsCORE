/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package io.github.ihongs.server.init;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import java.io.File;
import java.util.Arrays;
import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;

/**
 * JSP 初始器
 * @author Hongs
 */
public class Jasper implements Initer {

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
