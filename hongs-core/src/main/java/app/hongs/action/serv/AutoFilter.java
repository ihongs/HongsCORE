package app.hongs.action.serv;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsError;
import app.hongs.action.ActionDriver;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CustomReplies;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashSet;
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
    private FilterCheck ignore = null;
    private Set<String> layset = null;
    private Set<String> actset = null;
    private Set<String> cstset = null;
//  private Map<String, String> cstmap = null; // 可 inlucde 的动作脚本
//  private Map<String, String> cxtmap = null; // 可 forward 的动作脚本

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
        HttpServletResponse rsp = hlpr.getResponse();
        HttpServletRequest  req = hlpr.getRequest( );
        String url = ActionDriver.getCurrPath( req );

        // 检查是否需要跳过
        if (ignore != null && ignore.ignore(url)) {
            chain.doFilter(req , rsp);
            return;
        }

        if (url.endsWith(".api")) {
                             // 接口不需要处理
        } else
        if (url.endsWith(".act")) {
            String act, ext; // 动作路径和扩展
            String src, met; // 资源路径和方法
            String uri;
            int    pos;

            try {
                pos = url.lastIndexOf('.');
                ext = url.substring(  pos);
                act = url.substring(1,pos);

                pos = act.lastIndexOf('/');
                met = act.substring(1+pos);
                src = act.substring(0,pos);
            } catch (IndexOutOfBoundsException ex) {
                // 如果无法拆分则直接跳过
                chain.doFilter ( req, rsp);
                return;
            }

            // 检查是否有特定动作脚本
            uri = "/"+ src +"/@"+ met +".jsp";
            if (new File(Core.BASE_PATH+ uri).exists()) {
                include ( req, rsp, url, uri);
                return;
            }
            uri = "/"+ src +"/#"+ met +".jsp";
            if (new File(Core.BASE_PATH+ uri).exists()) {
                forward ( req, rsp, url, uri);
                return;
            }

            if (!ActionRunner.getActions().containsKey(act)) {
                // 检查是否有重写动作脚本 (废弃, 动作归动作)
                /*
                getlays();
                for(Map.Entry<String, String> et : cstmap.entrySet()) {
                    met = et.getKey  (  );
                    uri = et.getValue(  );
                    if (act.endsWith(met)) {
                        include(req,rsp, url, layout + uri);
                        return;
                    }
                }
                for(Map.Entry<String, String> et : cxtmap.entrySet()) {
                    met = et.getKey  (  );
                    uri = et.getValue(  );
                    if (act.endsWith(met)) {
                        forward(req,rsp, url, layout + uri);
                        return;
                    }
                }
                */

                for(String axt: getacts()) {
                    if (act.endsWith(axt)) {
                        if (cstset.contains(axt)) {
                            forward(req, rsp, url, action + axt + ext);
                        } else {
                            include(req, rsp, url, action + axt + ext);
                        }
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

                for(String uri: getlays()) {
                    if (url.endsWith(uri)) {
                        forward(req, rsp, url, layout + uri);
                        return;
                    }
                    if (jsp) {
                        continue;
                    }
                    if (htm) {
                        // xxx.htm => xxx.jsp
                        String uxl;
                        int  pos = url.lastIndexOf (".");
                             uxl = url.substring(0, pos);
                        if ((uxl + ".jsp").endsWith(uri)) {
                            forward(req, rsp, url, layout + uri);
                            return;
                        }
                    } else {
                        // xxx.xxx => xxx.xxx.jsp
                        if ((url + ".jsp").endsWith(uri)) {
                            forward(req, rsp, url, layout + uri);
                            return;
                        }
                    }
                }
            }
        }

        chain.doFilter(req, rsp);
    }

    private void include(ServletRequest req, ServletResponse rsp, String url, String uri)
            throws ServletException, IOException {
        // 虚拟路径
        req.setAttribute(Cnst.PATH_ATTR, url);
        // 转发请求
        req.getRequestDispatcher(uri).include(req, rsp);
    }

    private void forward(ServletRequest req, ServletResponse rsp, String url, String uri)
            throws ServletException, IOException {
        // 虚拟路径
        req.setAttribute(Cnst.PATH_ATTR, url);
        // 转发请求
        req.getRequestDispatcher(uri).forward(req, rsp);
    }

    private Set<String> getacts() {
        if (null != actset) {
            return  actset;
        }

        // 总是通过 search 获取动作 class
        // 即使无需 search 也要存在 search 方法才行
        Class cls;
        try {
            cls = ActionRunner.getActions()
                              .get(action.substring(1) + "/search")
                              .getMclass( );
        } catch ( NullPointerException ex ) {
            throw new HongsError(0x3e,
                 "Auto action '" + action.substring(1) + "/search' is not exists", ex);
        }

        cstset = new HashSet();
        actset = new TreeSet(new Comparator<String>( ) {
            @Override
            public int compare( String o1, String o2 ) {
                // 对比两个动作路径层级数
                // 优先匹配层级更深的动作
                int i, c1 = 0, c2 = 0;
                i = 0;
                while ((i = o1.indexOf('/', i)) != -1) {
                    i  ++;
                    c1 ++;
                }
                i = 0;
                while ((i = o2.indexOf('/', i)) != -1) {
                    i  ++;
                    c2 ++;
                }
                return Integer.compare(c2, c1);
            }
        });

        for (Method mtd : cls.getMethods( )) {
             Action ann = mtd.getAnnotation(Action.class);
            if (null != ann) {
                String  uri;
                if (!"".equals(ann.value())) {
                    uri = "/"+ ann.value(  );
                } else {
                    uri = "/"+ mtd.getName();
                }
                if (mtd.isAnnotationPresent(CustomReplies.class)) {
                    cstset.add(uri);
                }
                    actset.add(uri);
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

        /**
         * 这里不需要管层级的深度
         * 下面是按越深越先加入的
         */
        layset = new LinkedHashSet();
//      cstmap = new LinkedHashMap();
//      cxtmap = new LinkedHashMap();

        // 递归获取目录下所有文件
        getlays(layset, dir, "/");

        return  layset;
    }

    private void getlays(Set layset, File dx, String dn) {
        File[] fs = dx.listFiles(  );
        if (null == fs) {
            return;
        }

        for ( File fx : fs ) {
            String fn = fx.getName();
            if (fn.startsWith (".")
            ||  fn.startsWith ("_")) {
                continue;
            }

            if (fx.isDirectory(   )) {
                getlays(layset, fx, dn + fn + "/");
            }

            /**
             * @,# 都表示这是一个动作脚步
             * @ 为 include, # 为 forward
             */
//          if (fn.startsWith ("@")) {
//              int    l  = fn.lastIndexOf(".");
//              String ln = fn.substring(1 , l);
//              cstmap.put( dn + ln , dn + fn );
//          } else
//          if (fn.startsWith ("#")) {
//              int    l  = fn.lastIndexOf(".");
//              String ln = fn.substring(1 , l);
//              cxtmap.put( dn + ln , dn + fn );
//          } else
//          {
                layset.add( dn + fn);
//          }
        }
    }

}
