package app.hongs.action.serv;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsError;
import app.hongs.action.ActionDriver;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.anno.Action;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Set;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 自动处理过滤器
 *
 * <h3>初始化参数(init-param):</h3>
 * <pre>
 * action-path  默认动作地址
 * layout-path  默认页面地址
 * ignore-urls  忽略的URL, 可用","分割多个, 可用"*"为前后缀
 * </pre>
 * <p>
 * 注意:
 * action-path, layout-path 与 filter-mapping 的 url-pattern 不能有从属关系,
 * 否则会造成死循环
 * </p>
 *
 * @author Hongs
 */
public class AutoFilter extends ActionDriver {

    private String action;
    private String layout;
    private Set<String> actset = null;
    private Set<String> layset = null;
    private FilterCheck ignore = null;

    @Override
    public void init(FilterConfig cnf) throws ServletException {
        super.init(cnf);
        
        action = cnf.getInitParameter("action-path");
        layout = cnf.getInitParameter("layout-path");
        if (action == null) {
            action ="/common/pages";
        }
        if (layout == null) {
            layout =  action;
        }

        // 获取不包含的URL
        this.ignore = new FilterCheck(
            cnf.getInitParameter("ignore-urls"),
            cnf.getInitParameter("attend-urls")
        );
    }

    @Override
    public void destroy() {
        actset = null;
        layset = null;
    }

    @Override
    public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  req = hlpr.getRequest( );
        HttpServletResponse rsp = hlpr.getResponse();
        String url = ActionDriver.getCurrPath( req );

        // 检查是否需要跳过
        if (ignore != null && ignore.ignore(url)) {
            chain.doFilter(req , rsp);
            return;
        }

        if (url.endsWith(".api")) {
            // 接口无需处理
        } else
        if (url.endsWith(".act")) {
            String act;
            String ext;
            int    pos;

            try {
                pos = url.lastIndexOf('.');
                ext = url.substring(  pos);
                act = url.substring(1,pos);
            } catch (IndexOutOfBoundsException ex) {
                // 如果无法拆分则直接跳过
                chain.doFilter ( req, rsp);
                return;
            }

            if (!ActionRunner.getActions().containsKey(act)) {
                for(String axt : getacts()) {
                    if (act.endsWith(axt )) {
                        doAction( req, rsp, url, axt + ext);
                        return;
                    }
                }
            }
        } else {
            // 默认引导页总叫 default.html
            if ( url.endsWith("/") ) {
                 url = url + "default.html";
            }

            File urf = new File(Core.BASE_PATH+ url);
            if (!urf.exists( )) {
                boolean jsp = url.endsWith (".jsp" );
                boolean htm = url.endsWith (".htm" )
                           || url.endsWith (".html");
                int     pos = url.lastIndexOf( "." );
                String  uxl = pos ==  -1 ? url: url.substring(0, pos);

                for(String uri : getlays()) {
                    if (url.endsWith(uri )) {
                        doLayout(req, rsp, url, uri);
                        return;
                    }
                    if (jsp) {
                        continue;
                    }
                    if (htm) {
                        // xxx.htm => xxx.jsp
                        if ((uxl + ".jsp").endsWith(uri)) {
                            doLayout(req, rsp, url, uri);
                            return;
                        }
                    } else {
                        // xxx.xxx => xxx.xxx.jsp
                        if ((url + ".jsp").endsWith(uri)) {
                            doLayout(req, rsp, url, uri);
                            return;
                        }
                    }
                }
            }
        }

        chain.doFilter(req, rsp);
    }

    private void doAction(ServletRequest req, ServletResponse rsp, String url, String uri)
            throws ServletException, IOException {
        // 虚拟路径
        req.setAttribute(Cnst.PATH_ATTR, url);
        // 转发请求
        req.getRequestDispatcher(action+ uri).include(req, rsp);
    }

    private void doLayout(ServletRequest req, ServletResponse rsp, String url, String uri)
            throws ServletException, IOException {
        // 虚拟路径
        req.setAttribute(Cnst.PATH_ATTR, url);
        // 转发请求
        req.getRequestDispatcher(layout+ uri).forward(req, rsp);
    }

    private Set<String> getacts() {
        if (null != actset) {
            return  actset;
        }

        // 总是通过 retrieve 获取动作 class
        // 即使无需 retrieve 也要存在 retrieve 方法才行
        Class cls;
        try {
            cls = ActionRunner.getActions()
                              .get(action.substring(1) + "/retrieve")
                              .getMclass( );
        } catch ( NullPointerException ex ) {
            throw new HongsError(0x3e,
                 "Auto action '" + action.substring(1) + "/retrieve' is not exists", ex);
        }

        actset = new TreeSet(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2 ) {
                return o1.length( ) < o2.length( ) ? 1 : -1;
            }
        });

        for(Method mtd : cls.getMethods(  )) {
            Action ann = mtd.getAnnotation(Action.class);
            if (ann != null) {
                if (!"".equals(ann.value())) {
                    actset.add("/"+ann.value(  ));
                } else {
                    actset.add("/"+mtd.getName());
                }
            }
        }

        return actset;
    }

    private Set<String> getlays() {
        if (null != layset) {
            return  layset;
        }

        File dir = new File(Core.BASE_PATH + layout);
        if (!dir.exists( )) {
            throw new HongsError(0x3f,
                 "Auto layout '" + layout.substring(1) + "' is not exists");
        }
        if (!dir.isDirectory()) {
            throw new HongsError(0x3f,
                 "Auto layout '" + layout.substring(1) + "' is not a directory");
        }

        layset = new TreeSet(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2 ) {
                return o1.length( ) < o2.length( ) ? 1 : -1;
            }
        });

        // 递归获取目录下所有文件
        getlays(layset, dir, "/");

        return  layset;
    }

    private void getlays(Set layset, File dx, String dn) {
        File[] fs = dx.listFiles();
        if (fs == null) {
            return;
        }
        for ( File fx : fs ) {
            String fn = fx.getName();
            if (fn.startsWith (".")) {
                continue;
            }
            if (fx.isDirectory(   )) {
                getlays(layset, fx, dn + fn + "/");
            }
            layset.add (dn + fn);
        }
    }

}
