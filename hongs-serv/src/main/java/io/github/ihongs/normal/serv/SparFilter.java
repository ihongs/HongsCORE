package io.github.ihongs.normal.serv;

import io.github.ihongs.Core;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionDriver.URLPatterns;
import io.github.ihongs.util.Synt;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.LinkedHashSet;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 单页应用路由
 * 
 * 设置参数:
 * index-file 默认使用 welcome-file-list
 * 支持 url-include 和 url-exclude
 * 
 * @author Hongs
 */
public class SparFilter implements Filter {

    private URLPatterns patter;
    private Set<String> access;

    @Override
    public void init(FilterConfig cnf) throws ServletException {
        patter = new ActionDriver.URLPatterns(
            cnf.getInitParameter("url-include"),
            cnf.getInitParameter("url-exclude")
        );

        // 索引文件列表
        access = Synt.toSet(cnf.getInitParameter("index-file"));
        if (access == null) {
            access  = getWelcomeFileList(cnf.getServletContext().getContextPath());
        }
    }

    @Override
    public void destroy() {
        this.patter = null;
        this.access = null;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse rsp, FilterChain fc)
    throws IOException, ServletException {
        HttpServletRequest raq = (HttpServletRequest) req;
        String url = ActionDriver.getRecentPath (raq);

        if (! patter.matches(url)) {
            fc.doFilter(req, rsp);
            return;
        }

        String uri;
        File   src;

        // URL 总是指向索引文件
        if (! url.endsWith("/") ) {
            src = new File(Core.BASE_PATH + url);
            if (src.isFile()) {
                fc.doFilter(req, rsp);
                return;
            }

            for (String idx : access) {
                uri = url +"/"+ idx;
                src = new File(Core.BASE_PATH + uri);
                if (src.isFile()) {
                    fc.doFilter(req, rsp);
                    return;
                }
            }
        } else {
            for (String idx : access) {
                uri = url   +   idx;
                src = new File(Core.BASE_PATH + uri);
                if (src.isFile()) {
                    fc.doFilter(req, rsp);
                    return;
                }
            }
        }

        // 逐级向上查找索引文件
        int pos ;
        while ( 0 < ( pos = url.lastIndexOf("/") ) ) {
            url = url.substring( 0 , pos);

            for (String idx : access) {
                uri = url +"/"+ idx;
                src = new File(Core.BASE_PATH + uri);
                if (src.isFile()) {
                    raq.getRequestDispatcher(uri).forward(req, rsp);
                    return;
                }
            }
        }

        fc.doFilter(req, rsp);
    }

    private static Set<String> getWelcomeFileList(String WEBDIR) throws ServletException {
        Set<String> set = new LinkedHashSet();

        try {
            File xml;
            if (Core.CORE_PATH == null) {
                 xml = new File(WEBDIR + "/WEB-INF/web.xml");
            } else {
                 xml = new File(Core.CONF_PATH + "/web.xml");
            if (!xml.exists())
                 xml = new File(Core.CORE_PATH + "/web.xml");
            }

            DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
            DocumentBuilder        bui = fac.newDocumentBuilder();
            Document               doc = bui.parse(xml);

            NodeList lst = doc.getElementsByTagName("welcome-file-list");
            for (int i = 0; i < lst.getLength(); i ++ ) {
                Element ele = (Element) lst.item(i);
                if (ele.getTagName().equals("welcome-file")) {
                    set.add( ele.getTextContent() );
                }
            }
        } catch (ParserConfigurationException ex) {
            throw new ServletException(ex);
        } catch (SAXException ex) {
            throw new ServletException(ex);
        } catch ( IOException ex) {
            throw new ServletException(ex);
        }

        if (set.isEmpty()) {
            set.add("index.jsp" );
            set.add("index.html");
        }

        return set;
    }

}
