package io.github.ihongs.server.init;

import org.eclipse.jetty.server.SameFileAliasChecker;
import org.eclipse.jetty.servlet.ServletContextHandler;

/**
 * Win 的软链
 * @author Hongs
 */
public class Linker implements Initer {

    @Override
    public void init(ServletContextHandler sc) {
        sc.addAliasCheck(new SameFileAliasChecker());
    }

}
