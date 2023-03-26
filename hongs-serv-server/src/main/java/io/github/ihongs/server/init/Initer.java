package io.github.ihongs.server.init;

import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * 容器初始器
 * @author Hongs
 */
public interface Initer {
    
    public void init(ServletContextHandler context);

}
