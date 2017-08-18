package app.hongs.serv.common;

import app.hongs.Core;
import app.hongs.action.ActionDriver;
import app.hongs.action.ActionDriver.FilterCheck;
import java.io.File;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
/*
import java.util.Set;
import java.util.HashSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
*/

/**
 * 单页应用路由
 * @author Hongs
 */
public class SparFilter implements Filter {

    private int         offset;
    private FilterCheck ignore;
//  private Set<String> indexs;

    @Override
    public void init(FilterConfig cnf) throws ServletException {
        this.offset = Core.BASE_PATH.length(  );
        this.ignore = new FilterCheck(
            cnf.getInitParameter("ignore-urls"),
            cnf.getInitParameter("attend-urls")
        );
//      this.indexs = getUrlPatterns(cnf.getFilterName());
    }


    @Override
    public void destroy() {
        this.ignore = null;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse rsp, FilterChain fc)
    throws IOException, ServletException {
        HttpServletRequest raq = (HttpServletRequest) req;
        String url = ActionDriver.getCurrPath(raq);

        if (null != raq.getHeader("X-Requested-With")
        || (null != ignore  &&  ignore.ignore(url)) ) {
            fc.doFilter(req, rsp);
            return;
        }

        // 查找当前的 URL 前缀
        // 没必要了, 万一找不到最后就到首页了, 也就知道哪里缺少了文件了
        /*
        String  top = null;
        for  ( String pre : indexs ) {
            if (url.startsWith(pre)) {
                top = pre ;
            }
        }
        if (null == top) {
            fc.doFilter(req, rsp);
            return;
        }
        */

        String uri;
        File   src;
        int    pos;

        // URL 总是指向索引文件
        if (!url.endsWith("/")) {
            src = new File (Core.BASE_PATH + url);
        if (src.isDirectory( )) {
            url += "/index.html";
            src = new File (Core.BASE_PATH + url);
        }} else {
            url +=  "index.html";
            src = new File (Core.BASE_PATH + url);
        }

        // 逐级向上查找索引文件
        if (!src.exists () ) {
            while ( 0 < (pos = url.lastIndexOf( "/" )) ) {
                url = url.substring(0, pos);
                uri = url  +  "/index.html";
                src = new File(Core.BASE_PATH + uri );
                if (src.exists( )) {
                    req.getRequestDispatcher(uri).forward(req, rsp);
                    return;
                }
            }
        }

        fc.doFilter(req, rsp);
    }

    /*
    private static Set<String> getUrlPatterns(String name) throws ServletException {
        Set<String> urls = new HashSet();

        try {
            File xml = new File(Core.CONF_PATH + "/web.xml");
            if (!xml.exists()) {
                 xml = new File(Core.CORE_PATH + "/web.xml");
            }

            DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
            DocumentBuilder        bui = fac.newDocumentBuilder( );
            Document               doc = bui.parse(xml);

            NodeList lst = doc.getElementsByTagName("filter-name");
            for (int i = 0; i < lst.getLength(); i ++ ) {
                Element nod = (Element) lst.item(i);
                if (!nod.getTextContent().equals(name)) {
                    continue;
                }

                NodeList lzt = nod.getParentNode().getChildNodes();
                for (int j = 0; j < lzt.getLength(); j ++ ) {
                    Element ele = (Element) lzt.item(i);
                    if (!nod.getTagName(  ).equals("url-pattern")) {
                        continue;
                    }

                    // 去掉后缀 *
                    urls.add(ele.getTextContent().replace("*",""));
                }
            }
        } catch (ParserConfigurationException ex) {
            throw new ServletException(ex);
        } catch (SAXException ex) {
            throw new ServletException(ex);
        } catch (IOException ex) {
            throw new ServletException(ex);
        }

        return urls;
    }
    */

}
