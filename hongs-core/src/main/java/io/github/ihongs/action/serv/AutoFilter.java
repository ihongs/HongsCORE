package io.github.ihongs.action.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.PasserHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CustomReplies;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Set;
import java.util.regex.Pattern;
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
    private PasserHelper ignore = null;
    private Set<String>  layset = null;
    private Set<String>  actset = null;
    private Set<String>  cstset = null;
//  private Map<String, String> cstmap = null; // 可 inlucde 的动作脚本
//  private Map<String, String> cxtmap = null; // 可 forward 的动作脚本

    private static final Pattern DENY_JSPS = Pattern.compile("(/_|\\.)[^/]*\\.jsp$"); // [_#$]

    @Override
    public void init(FilterConfig cnf) throws ServletException {
        super.init(cnf);

        action = cnf.getInitParameter("action-path");
        layout = cnf.getInitParameter("layout-path");
        if (action == null) {
            action ="/common/auto";
        }
        if (layout == null) {
            layout =  action;
        }

        // 获取不包含的URL
        this.ignore = new PasserHelper(
            cnf.getInitParameter("ignore-urls"),
            cnf.getInitParameter("attend-urls")
        );
    }

    @Override
    public void destroy() {
        super.destroy();
        actset = null;
        layset = null;
    }

    @Override
    public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse rsp = hlpr.getResponse();
        HttpServletRequest  req = hlpr.getRequest( );
        String url = ActionDriver.getRecentPath(req);
        String ref = ActionDriver.getOriginPath(req);

        // 检查是否需要跳过
        if (ignore != null && ignore.ignore(url)) {
            chain.doFilter( req , rsp );
            return;
        }

        // 禁止访问动作脚本, 避免绕过权限过滤
        if (DENY_JSPS.matcher(ref).find()) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "What's your problem?");
            return;
        }

        if (url.endsWith(Cnst.API_EXT)) {
            /**
             * 为避免一个动作有多种路径,
             * 这很可能导致权限校验歧义,
             * 故这里不用做类似下方处理;
             * 上方的 DENY_JSPS 同此理.
             */
        } else
        if (url.endsWith(Cnst.ACT_EXT)) {
            String act; // 动作路径
            String src; // 资源路径
//          String met; // 动作方法
            String uri;
            int    pos;

            try {
                pos = url.lastIndexOf('.');
                act = url.substring(1,pos);

                pos = act.lastIndexOf('/');
                src = act.substring(0,pos);
//              met = act.substring(1+pos);
            } catch (IndexOutOfBoundsException ex) {
                // 如果无法拆分则直接跳过
                chain.doFilter ( req, rsp);
                return;
            }

            // 检查是否有特定动作脚本
            uri = "/" + src + "/__main__.jsp";
            if (new File(Core.BASE_PATH+ uri).exists()) {
                include ( req, rsp, url, uri);
                return;
            }
            // 废弃, 仅用以上方式处理
            /*
            uri = "/"+ src +"/#"+ met +".jsp";
            if (new File(Core.BASE_PATH+ uri).exists()) {
                include ( req, rsp, url, uri);
                return;
            }
            uri = "/"+ src +"/$"+ met +".jsp";
            if (new File(Core.BASE_PATH+ uri).exists()) {
                forward ( req, rsp, url, uri);
                return;
            }
            */

            if (!ActionRunner.getActions().containsKey(act)) {
                // 检查是否有重写动作脚本 (废弃, 动作的归动作)
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
                            forward(req, rsp, url, action + axt + Cnst.ACT_EXT);
                        } else {
                            include(req, rsp, url, action + axt + Cnst.ACT_EXT);
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
                String  uxl = null;
                if (htm) {
                    int pos = url.lastIndexOf( "." );
                        uxl = url.substring(0 , pos);
                }

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
        req.setAttribute(Cnst.ORIGIN_ATTR, Core.ACTION_NAME.get());
        req.setAttribute(Cnst.ACTION_ATTR, url.substring(1));
        // 转发请求
        req.getRequestDispatcher( uri ).include( req , rsp );
    }

    private void forward(ServletRequest req, ServletResponse rsp, String url, String uri)
            throws ServletException, IOException {
        // 虚拟路径
        req.setAttribute(Cnst.ORIGIN_ATTR, Core.ACTION_NAME.get());
        req.setAttribute(Cnst.ACTION_ATTR, url.substring(1));
        // 转发请求
        req.getRequestDispatcher( uri ).forward( req , rsp );
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
            throw new HongsExemption(0x1130,
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
                i = Integer.compare(c2, c1);
                return i != 0 ? i : 1;
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
            throw new HongsExemption(0x1131,
                 "Auto layout '" + layout.substring(1) + "' is not exists");
        }
        if (!dir.isDirectory()) {
            throw new HongsExemption(0x1131,
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

        Set tmpset;
        tmpset = new LinkedHashSet();

        for ( File fx : fs ) {
            String fn = fx.getName();
            if (fn.startsWith (".")
            ||  fn.startsWith ("_")) {
                continue;
            }

            if (fx.isFile  (   )) {
                tmpset.add (dn + fn);
            } else
            if (fx.isDirectory(   )) {
                getlays(layset , fx , dn + fn + "/");
            }

            /**
             * #,$ 都表示这是一个动作脚步
             * # 为 include, $ 为 forward
             * 废弃, 动作的归动作
             */
            /*
            if (fn.startsWith ("#")) {
                int    l  = fn.lastIndexOf(".");
                String ln = fn.substring(1 , l);
                cstmap.put( dn + ln , dn + fn );
            } else
            if (fn.startsWith ("$")) {
                int    l  = fn.lastIndexOf(".");
                String ln = fn.substring(1 , l);
                cxtmap.put( dn + ln , dn + fn );
            } else
            {
                layset.add( dn + fn);
            }
            */
        }

        layset.addAll(tmpset);
    }

}
