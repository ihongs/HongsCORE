package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.daemon.Chore;
import io.github.ihongs.util.daemon.Latch;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;
import javax.servlet.http.Part;

/**
 * 动作驱动器
 *
 * <p>
 * 其他的 Servlet,Filter 继承此类即可安全的使用 Core 请求对象;
 * 也可以将其作为 Filter 加入到 web.xml,
 * 其后的 Servlet,Filter 实例对象均可安全的使用 Core 请求对象.
 * </p>
 *
 * <h3>配置选项:</h3>
 * <pre>
 * server.id             服务ID
 * core.language.probing 探测语言
 * core.language.default 默认语言
 * core.timezone.probing 探测时区
 * core.timezone.default 默认时区
 * </pre>
 *
 * @author Hong
 */
public class ActionDriver extends HttpServlet implements Servlet, Filter {

    /**
     * 首位标识, 为 true 表示最先执行，负责初始化和清理
     */
    private boolean FIRST = false;

    /**
     * 关闭标识, 为 true 表示有初始化, 需要承担全局清理
     */
    private boolean SETUP = false;

    private static final Pattern URL_REG = Pattern.compile("^\\w+://[^/]+");
    private static final Pattern URI_REG = Pattern.compile("^\\w+://|^/"  );

    /**
     * 初始化 Filter
     * @param conf
     * @throws ServletException
     */
    @Override
    public void init( FilterConfig conf) throws ServletException {
        this.init(conf.getServletContext());
    }

    /**
     * 初始化 Servlet
     * @param conf
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig conf) throws ServletException {
       super.init(conf /*call super init*/);
        this.init(conf.getServletContext());
    }

    /**
     * 公共初始化
     * @param cont
     * @throws ServletException
     */
    synchronized final void init(ServletContext cont) throws ServletException {
        if (Core.ENVIR != 1) {
            Core.ENVIR  = 1;
        } else {
            return;
        }

        Properties def = CoreConfig.getInstance("default");
        Properties cnf = CoreConfig.getInstance("defines");

        FIRST=true;
        if (Core.SERV_PATH == null) {
        SETUP=true;

            System.setProperty("file.encoding", "UTF-8");

            /** 核心属性配置 **/

            Core.DEBUG = Synt.declare(cont.getInitParameter("debug"), (byte) 0);

            Core.SERV_PATH = cont.getContextPath();
            Core.BASE_PATH = cont.getRealPath("" );

            Core.BASE_PATH = Core.BASE_PATH.replaceFirst("[/\\\\]$", "");
            Core.CORE_PATH = Core.BASE_PATH + File.separator + "WEB-INF";

            File cp = new File(Core.CORE_PATH );
            if (!cp.exists()) {
                Core.CORE_PATH = cp.getParent();
            }

            Core.CONF_PATH = Core.CORE_PATH + File.separator + "etc";
            Core.DATA_PATH = Core.CORE_PATH + File.separator + "var";

            //** 系统属性配置 **/

            Core.SERVER_ID = cnf.getProperty("server.id", "0");

            // 用于替换下面系统属性中的变量
            Map m = new HashMap();
            m.put("SERVER_ID", Core.SERVER_ID);
            m.put("BASE_PATH", Core.BASE_PATH);
            m.put("CORE_PATH", Core.CORE_PATH);
            m.put("CONF_PATH", Core.CONF_PATH);
            m.put("DATA_PATH", Core.DATA_PATH);

            // 启动系统属性
            for(Map.Entry et : cnf.entrySet()) {
                String k = (String) et.getKey  ();
                String v = (String) et.getValue();
                if (k.startsWith("envir.")) {
                    k = k.substring(6  );
                    v = Syno.inject(v,m);
                    System.setProperty(k,v);
                }
            }

            if (4 == (4 & Core.DEBUG)) {
            // 调试系统属性
            for(Map.Entry et : cnf.entrySet()) {
                String k = (String) et.getKey  ();
                String v = (String) et.getValue();
                if (k.startsWith("debug.")) {
                    k = k.substring(6  );
                    v = Syno.inject(v,m);
                    System.setProperty(k,v);
                }
            }
            }

            // 默认域名前缀
            String su = System.getProperty ("serv.url");
            if (null != su) {
                Matcher matcher = URL_REG.matcher( su );
                if (matcher.find()) {
                    Core.SERV_PATH = su.substring(0 + matcher.end());
                    Core.SERV_HREF = su.substring(0 , matcher.end());
                } else {
                    Core.SERV_PATH = su;
                    Core.SERV_HREF = "";
                }
            }

            // 设置默认语言
            Core.ACTION_LANG.set(def.getProperty("core.language.default", Cnst.LANG_DEF));
            Core.ACTION_ZONE.set(def.getProperty("core.timezone.default", Cnst.ZONE_DEF));
            Locale  .setDefault(Core.getLocality());
            TimeZone.setDefault(Core.getTimezone());
        }

        // 调用一下可预加载动作类
        ActionRunner.getActions();

        // 清空全局好准备重新开始
        Core.GLOBAL_CORE.reset( );

        // 设置全局对象维护的任务
        Chore ch = Chore.getInstance ( );
        ch.runTimed(() -> Latch.clean());
        ch.runTimed(() -> Core.GLOBAL_CORE.unuse());
        ch.runDaily(() -> Core.GLOBAL_CORE.reuse());

        // 启动后需立即执行的任务
        String ss = cnf.getProperty( "serve.init" );
        if (ss != null) for (String sn : ss.split(";")) {
            sn  = sn.trim();
            if (! sn.isEmpty( ) ) try {
               ( (Runnable) Class.forName(sn)
                    .getDeclaredConstructor()
                    .newInstance( ) ).run(  );
            } catch (ClassNotFoundException ex) {
                throw new  ServletException(ex);
            } catch ( NoSuchMethodException ex) {
                throw new  ServletException(ex);
            } catch (IllegalAccessException ex) {
                throw new  ServletException(ex);
            } catch (InstantiationException ex) {
                throw new  ServletException(ex);
            } catch (InvocationTargetException ex) {
                throw new  ServletException(ex);
            }
        }

        CoreLogger.info("HTTP server is started."
            + "\r\n\tDEBUG       : {}"
            + "\r\n\tSERVER_ID   : {}"
            + "\r\n\tCORE_PATH   : {}"
            + "\r\n\tCONF_PATH   : {}"
            + "\r\n\tDATA_PATH   : {}"
            + "\r\n\tBASE_PATH   : {}"
            + "\r\n\tSERV_HREF   : {}{}",
            Core.DEBUG    , Core.SERVER_ID,
            Core.CORE_PATH, Core.CONF_PATH,
            Core.DATA_PATH, Core.BASE_PATH,
            Core.SERV_HREF, Core.SERV_PATH
        );
    }

    /**
     * 公共销毁
     */
    @Override
    public void destroy () {
        if (!FIRST) {
            return;
        }

        long time = System.currentTimeMillis()
                  - Core.STARTS_TIME;
        Core core = Core.GLOBAL_CORE;
        CoreLogger.info(
            "HTTP server is stopped."
            + "\r\n\tSERVER_ID   : {}"
            + "\r\n\tRuntime     : {}"
            + "\r\n\tObjects     : {}",
            Core.SERVER_ID,
            Syno.humanTime(time),
            core.toString (/**/)
        );

        if (!SETUP) {
            return;
        }

        try {
            Core.GLOBAL_CORE.reset( );
        } catch ( Throwable  e) {
            CoreLogger.error(e);
        }
    }

    @Override
    public void service (ServletRequest rep, ServletResponse rsp)
    throws ServletException, IOException {
        doDriver(rep, rsp, new DriverProxy() {
            @Override
            public void doDriver(Core core, ActionHelper hlpr)
            throws ServletException, IOException {
                doAction(core, hlpr);
            }
        });
    }

    @Override
    public void doFilter(ServletRequest rep, ServletResponse rsp, final FilterChain chn)
    throws ServletException, IOException {
        doDriver(rep, rsp, new DriverProxy() {
            @Override
            public void doDriver(Core core, ActionHelper hlpr)
            throws ServletException, IOException {
                doFilter(core, hlpr, chn);
            }
        });
    }

    final  void doDriver(ServletRequest rep, ServletResponse rsp, final DriverProxy agt)
    throws ServletException, IOException {
        HttpServletRequest  req = (HttpServletRequest ) rep;
        HttpServletResponse rsq = (HttpServletResponse) rsp;

        ActionHelper hlpr;
        Core core = (Core) req.getAttribute(Core.class.getName());
        if ( core == null) {
            /**
             * 外层调用
             */
            core = Core.getInstance( );
            req.setAttribute ( Core.class.getName(), core );
            hlpr = new ActionHelper( req, rsq );
            core.set ( ActionHelper.class.getName(), hlpr );

            try {
                doLaunch(core, hlpr, req );
                agt.doDriver ( core, hlpr);
                doCommit(core, hlpr, req, rsq );
            } catch (IOException ex) {
                // 非调试模式忽略客户端中途断开
                Throwable cx = ex.getCause();
                if (cx != null && cx.getClass().getSimpleName().equalsIgnoreCase("EofException")
                &&  4  != (4 & Core.DEBUG) ) {
                    return;
                }

                /**
                 * 异常抛到这层了就必须记入日志
                 * 继续抛出将被登记的错误页处理
                 * 下同
                 */
                CoreLogger.error(ex);
                throw ex;
            } catch (ServletException ex ) {
                CoreLogger.error(ex);
                throw ex;
            } catch (RuntimeException ex ) {
                CoreLogger.error(ex);
                throw ex;
            } catch (Error er) {
                CoreLogger.error(er);
                throw er;
            } finally {
                doFinish(core, hlpr, req );
            }
        } else {
            /**
             * 内层调用
             */
            Core.THREAD_CORE.set(core);
            hlpr = core.got(ActionHelper.class);
            hlpr.updateHelper( req, rsq );
            /**/ agt.doDriver(core, hlpr);
        }
    }

    private void doCommit(Core core, ActionHelper hlpr, HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException {
        /**
         * 输出特定服务信息
         */
        if (rsp.isCommitted( ) == false) {
            String pb;
            CoreConfig cc = core.got(CoreConfig.class);
            pb = cc.getProperty("core.service.by");
            if ( pb != null && pb.length( ) != 0 ) {
                rsp.setHeader(  "Server"    , pb );
            }
            pb = cc.getProperty("core.powered.by");
            if ( pb != null && pb.length( ) != 0 ) {
                rsp.setHeader("X-Powered-By", pb );
            }
        }

        Map dat  = hlpr.getResponseData();
        if (dat != null) {
            req .setAttribute( Cnst.RESPON_ATTR, dat );
            hlpr.updateHelper( req, rsp );
            hlpr.flush();
        }
    }

    private void doLaunch(Core core, ActionHelper hlpr, HttpServletRequest req)
    throws ServletException {
        Core.ACTION_TIME.set(System.currentTimeMillis(/***/));
        Core.ACTION_NAME.set(getOriginPath(req).substring(1));

        /*
        // 无需指定, 在需要时提取
        Core.CLIENT_ADDR.set(getClientAddr(req));
        Core.SERVER_HREF.set(getServerHref(req));
        */

        // 外部没有指定网站域名则在首次请求时进行设置(非线程安全)
        if (Core.SERV_HREF == null
        ||  Core.SERV_HREF.isEmpty()) {
            Core.SERV_HREF  = Core.SERVER_HREF.get();
        }

        CoreConfig conf = core.got(CoreConfig.class);

        Core.ACTION_LANG.set(conf.getProperty("core.language.default", Cnst.LANG_DEF));
        if (conf.getProperty("core.language.probing", false)) {
            /**
             * 语言可以记录到Session/Cookies里
             */
            String sess = conf.getProperty("core.language.session", Cnst.LANG_KEY);
            String lang = (String) hlpr.getSessibute(sess);
            if (lang == null || lang.length() == 0) {
                   lang = (String) hlpr.getCookibute(sess);
            if (lang == null || lang.length() == 0) {
                   lang = req.getHeader("Accept-Language");
            }
            }

            /**
             * 检查是否是支持的语言
             */
            if (lang != null) {
                lang  = CoreLocale.getAcceptLanguage(lang);
            if (lang != null) {
                Core.ACTION_LANG.set(lang);
            }
            }
        }

        Core.ACTION_ZONE.set(conf.getProperty("core.timezone.default", Cnst.ZONE_DEF));
        if (conf.getProperty("core.timezone.probing", false)) {
            /**
             * 时区可以记录到Session/Cookies里
             */
            String sess = conf.getProperty("core.timezone.session", Cnst.ZONE_KEY);
            String zone = (String) hlpr.getSessibute(sess);
            if (zone == null || zone.length() == 0) {
                   zone = (String) hlpr.getCookibute(sess);
            if (zone == null || zone.length() == 0) {
                   zone = req.getHeader(/**/ "X-Timezone");
            }
            }

            /**
             * 过滤一下避免错误时区
             */
            if (zone != null) {
                zone  = TimeZone.getTimeZone(zone).getID();
//          if (zone != null) {
                Core.ACTION_ZONE.set(zone);
//          }
            }
        }
    }

    private void doFinish(Core core, ActionHelper hlpr, HttpServletRequest req) {
        try {
            if (4 == (4 & Core.DEBUG)) {
                /**
                 * 提取必要的客户相关标识
                 * 以便判断用户和模拟登录
                 */
                HttpSession ses;
                Object      uid;
                String      mem;
                String      tim;
                req = hlpr.getRequest(/***/);
                ses = req .getSession(false);
                tim = Syno.humanTime (System.currentTimeMillis() - Core.ACTION_TIME.get());
                uid = hlpr.getSessibute(Cnst.UID_SES);
                if (uid != null) {
                    mem  =  uid.toString ( );
                } else
                if (ses != null) {
                    mem  =  "$"+ses.getId( );
                } else {
                    mem  =  "-";
                }

                StringBuilder sb = new StringBuilder("...");
                sb.append("\r\n\tACTION_NAME : ").append(Core.ACTION_NAME.get())
                  .append("\r\n\tACTION_TIME : ").append(Core.ACTION_TIME.get())
                  .append("\r\n\tACTION_LANG : ").append(Core.ACTION_LANG.get())
                  .append("\r\n\tACTION_ZONE : ").append(Core.ACTION_ZONE.get())
                  .append("\r\n\tThread      : ").append(Thread.currentThread().getName())
                  .append("\r\n\tMethod      : ").append(req.getMethod())
                  .append("\r\n\tMember      : ").append(mem)
                  .append("\r\n\tRuntime     : ").append(tim)
                  .append("\r\n\tObjects     : ").append(core.toString());

                /**
                 * 显示请求报头及输入输出
                 * 这对调试程序非常有帮助
                 */

                if (Synt.declare(System.getProperty("show.request"), false)) {
                    Map rd  = null;
                    try {
                        rd  = hlpr.getRequestData( );
                    } catch ( RuntimeException ex  ) {
                        CoreLogger.debug(ex.getMessage());
                    }
                    if (rd != null && !rd.isEmpty()) {
                        sb.append("\r\n\tRequest     : ")
                          .append(Syno.indent(Dawn.toString(rd)).substring(1));
                    }
                }

                if (Synt.declare(System.getProperty("show.results"), false)) {
                    Map xd  = hlpr.getResponseData();
                    if (xd == null) {
                        xd  = (Map) req.getAttribute(Cnst.RESPON_ATTR);
                    }
                    if (xd != null && !xd.isEmpty()) {
                        sb.append("\r\n\tResults     : ")
                          .append(Syno.indent(Dawn.toString(xd)).substring(1));
                    }
                }

                if (Synt.declare(System.getProperty("show.session"), false) && ses != null) {
                  Map         map = new HashMap();
                  Enumeration<String> nms = ses.getAttributeNames();
                  while (nms.hasMoreElements()) {
                      String  nme = nms.nextElement();
                      map.put(nme , ses.getAttribute(nme));
                  }
                  if (!map.isEmpty()) {
                      sb.append("\r\n\tSession     : ")
                        .append(Syno.indent(Dawn.toString(map)).substring(1));
                  }
                }

                if (Synt.declare(System.getProperty("show.context"), false)) {
                  Map         map = new HashMap();
                  Enumeration<String> nms = req.getAttributeNames();
                  while (nms.hasMoreElements()) {
                      String  nme = nms.nextElement();
                      map.put(nme , req.getAttribute(nme));
                  }
                  if (!map.isEmpty()) {
                      sb.append("\r\n\tContext     : ")
                        .append(Syno.indent(Dawn.toString(map)).substring(1));
                  }
                }

                if (Synt.declare(System.getProperty("show.headers"), false)) {
                  Map         map = new HashMap();
                  Enumeration<String> nms = req.getHeaderNames();
                  while (nms.hasMoreElements()) {
                      String  nme = nms.nextElement();
                      map.put(nme , req.getHeader(nme));
                  }
                  if (!map.isEmpty()) {
                      sb.append("\r\n\tHeaders     : ")
                        .append(Syno.indent(Dawn.toString(map)).substring(1));
                  }
                }

                if (Synt.declare(System.getProperty("show.cookies"), false)) {
                  Map         map = new HashMap();
                  Cookie[]    cks = req.getCookies();
                  for (Cookie cke : cks) {
                      map.put(cke.getName( ), cke.getValue( ));
                  }
                  if (!map.isEmpty()) {
                      sb.append("\r\n\tCookies     : ")
                        .append(Syno.indent(Dawn.toString(map)).substring(1));
                  }
                }

                CoreLogger.debug(sb.toString());
            }

            // 删除上传的临时文件
            Map<String, List<Part>> ud = Synt.asMap(hlpr.getAttribute(Cnst.UPLOAD_ATTR));
            if (ud != null) {
                for(List<Part> pa : ud.values()) {
                    for (Part  pr : pa) {
                        try {
                            pr.delete();
                        } catch (IOException ex) {
                            CoreLogger.error(ex);
                        }
                    }
                }
            }
        } finally {
            // 销毁此周期内的对象
            try {
                core.reset( );
            } catch (Error e) {
                CoreLogger.error( e );
            } catch (Exception e) {
                CoreLogger.error( e );
            }
            req.removeAttribute(Core.class.getName());
            Core.THREAD_CORE.remove();
            Core.CLIENT_ADDR.remove();
            Core.ACTION_TIME.remove();
            Core.ACTION_ZONE.remove();
            Core.ACTION_LANG.remove();
            Core.ACTION_NAME.remove();
        }
    }

    /**
     * 执行过滤
     * 如需其他操作请覆盖此方法
     * @param core
     * @param hlpr
     * @param chn
     * @throws ServletException
     * @throws IOException
     */
    protected void doFilter(Core core, ActionHelper hlpr, FilterChain chn)
    throws ServletException, IOException {
        chn.doFilter(hlpr.getRequest(), hlpr.getResponse());
    }

    /**
     * 执行动作
     * 如需其他操作请覆盖此方法
     * @param core
     * @param hlpr
     * @throws ServletException
     * @throws IOException
     */
    protected void doAction(Core core, ActionHelper hlpr)
    throws ServletException, IOException {
        /**/ service(hlpr.getRequest(), hlpr.getResponse());
    }

    //** 静态工具函数 **/

    /**
     * 获得当前工作的Core
     * @param req
     * @return
     */
    public static final Core getActualCore(HttpServletRequest req) {
       Core core  = (Core) req.getAttribute(Core.class.getName());
        if (core ==  null) {
            core  =  Core.GLOBAL_CORE ;
        } else {
            Core.THREAD_CORE.set(core);
        }
        return core;
    }

    /**
     * 获得当前的ServletPath
     * @param req
     * @return
     */
    public static final String getRecentPath(HttpServletRequest req) {
        String uri = (String) req.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
        String suf = (String) req.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
        if (uri == null) {
            uri  = req.getServletPath();
            suf  = req.getPathInfo();
        }
        if (suf != null) {
            uri += suf;
        }
        return uri;
    }

    /**
     * 获得起源的ServletPath
     * @param req
     * @return
     */
    public static final String getOriginPath(HttpServletRequest req) {
        String uri = (String) req.getAttribute(RequestDispatcher.FORWARD_SERVLET_PATH);
        String suf = (String) req.getAttribute(RequestDispatcher.FORWARD_PATH_INFO);
        if (uri == null) {
            uri  = req.getServletPath();
            suf  = req.getPathInfo();
        }
        if (suf != null) {
            uri += suf;
        }
        return uri;
    }

    /**
     * 获取当前的服务网址
     * @param req
     * @return
     */
    public static final String getServerHref(HttpServletRequest req) {
        String item = (String) req.getAttribute(Cnst.SERVER_ATTR);
        if (null != item) {
            return  item;
        }

        String prot;
        String host;
        int    port;

        prot = req.getScheme();
        host = req.getServerName();
        port = req.getServerPort();

        // RFC 7239, 标准代理格式
        item = req.getHeader("Forwarded");
        if (null != item) {
            String  line;
            int beg = 0 , end, sep;
                end = item.indexOf(',', beg);
                if (end != -1) {
                    item = item.substring(beg, end);
                }
            while  (end != -1) {
                end = item.indexOf(';', beg);
                if (end != -1) {
                    line = item.substring(beg, end);
                } else {
                    line = item.substring(beg /**/);
                }
                sep = line.indexOf("=" /**/);
                if (sep != -1) {
                    String key = line.substring(0, sep).trim();
                    String val = line.substring(1+ sep).trim();
                    if ("proto".equals(key)) {
                        prot = val;
                    } else
                    if ("host" .equals(key)) {
                        host = val;
                    } else
                    if ("port" .equals(key)) {
                        port = Synt.asInt(val);
                    }
                }
                beg = end + 1;
            }
        } else {
            // 非标准的格式
            item = req.getHeader("X-Forwarded-Proto");
            if (item != null) {
                prot  = item;
            }
            item = req.getHeader("X-Forwarded-Host" );
            if (item != null) {
                host  = item;
            }
            item = req.getHeader("X-Forwarded-Port" );
            if (item != null) {
                port  = Synt.asInt(item);
            }
        }

        if (port != 80 && port != 443) {
            host += ":" + port;
        }
        return prot+"://"+host;
    }

    /**
     * 获取当前的远程地址
     * @param req
     * @return
     */
    public static final String getClientAddr(HttpServletRequest req) {
        String addr = (String) req.getAttribute(Cnst.CLIENT_ATTR);
        if (null != addr) {
            return  addr;
        }

        // RFC 7239, 标准代理格式
        String h_0 = req.getHeader("Forwarded");
        if ( h_0 != null && h_0.length() != 0 ) {
            // 按逗号拆分代理节点
            int e_0,b_0 = 0;
            String  h_1;
            while (true) {
                e_0 = h_0.indexOf(',' , b_0);
                if (e_0 != -1) {
                    h_1 = h_0.substring(b_0, e_0);
                    b_0 = e_0 + 1;
                } else
                if (b_0 !=  0) {
                    h_1 = h_0.substring(b_0);
                } else
                {
                    h_1 = h_0;
                }

                // 按分号拆分条目
                int e_1,b_1 = 0;
                String  h_2;
                while (true) {
                    e_1 = h_1.indexOf(';' , b_1);
                    if (e_1 != -1) {
                        h_2 = h_1.substring(b_1, e_1);
                        b_1 = e_1 + 1;
                    } else
                    if (b_1 !=  0) {
                        h_2 = h_1.substring(b_1);
                    } else
                    {
                        h_2 = h_1;
                    }

                    // 拆分键值对
                    int e_2  = h_2.indexOf ('=');
                    if (e_2 != -1) {
                        String key = h_2.substring(0 , e_2).trim();
                        String val = h_2.substring(1 + e_2).trim();
                        key =  key.toLowerCase(    );

                        // 源地址
                        if (    "for"  .equals(key )
                        &&  ! "unknown".equals(val )) {
                            /**
                             * 按照官方文档的格式描述
                             * IPv4 的形式为 X.X.X.X:PORT
                             * IPv6 的形式为 "[X:X:X:X:X]:PORT"
                             * 需去掉端口引号和方括号
                             */
                            if (val.startsWith("\"")
                            &&  val.  endsWith("\"")) {
                                val = val.substring(1 , val.length() - 1);
                            }
                            if (val.startsWith("[" )) {
                                e_2 = val.indexOf("]" );
                                if (e_2 != -1) {
                                    val = val.substring(1 , e_2);
                                }
                            } else {
                                e_2 = val.indexOf(":" );
                                if (e_2 != -1) {
                                    val = val.substring(0 , e_2);
                                }
                            }
                            return  val;
                        }
                    }

                    if (e_1 == -1) {
                        break;
                    }
                }

                if (e_0 == -1) {
                    break;
                }
            }
        }

        // 其他非标准代理地址报头
        for (String key : new String[] {
                  "X-Forwarded-For",
                  "Proxy-Client-IP",
               "WL-Proxy-Client-IP"} ) {
             String val = req.getHeader(key);
            if (val != null && val.length( )  !=  0 ) {
                int pos = val.indexOf  (',');
                if (pos > 0) {
                    val = val.substring(0, pos);
                }   val = val.trim();
                if (!"unknown".equalsIgnoreCase(val)) {
                    return val;
                }
            }
        }

        // 上级客户端真实网络地址
        return  req.getRemoteAddr( );
    }

    /**
     * 补全URL, 增加域名前缀
     * @param url
     * @return
     */
    public static String fixUrl(String url) {
        if (URL_REG.matcher(url).find( ) == false) {
        if (url.startsWith ("/")) {
            url = Core.SERVER_HREF.get()
                + url;
        } else {
            url = Core.SERVER_HREF.get()
                + Core.SERV_PATH
                + "/"
                + url;
        }}
        return url;
    }

    /**
     * 补全URI, 增加应用前缀
     * @param uri
     * @return
     */
    public static String fixUri(String uri) {
        if (URI_REG.matcher(uri).find( ) == false) {
            uri = Core.SERV_PATH
                + "/"
                + uri;
        }
        return uri;
    }

    /**
     * 执行动作代理
     */
    public static interface DriverProxy {

        public void doDriver(Core core, ActionHelper hlpr) throws ServletException, IOException;

    }

    /**
     * URL 匹配助手
     *
     * 构建通配符简单正则,
     * 忽略换行及首尾空白.
     *
     * <ul>
     * <li><b>*</b> 表零个或多个字符</li>
     * <li><b>?</b> 表单独的一个字符</li>
     * <li><b>;</b> 分隔多组路径模式</li>
     * </ul>
     *
     * <pre>
     * 如通配项
     * <code>*.api;*.js;*.json;*.css;*.gif;*.jpg;*.png;</code>
     * <code>/centre/sign/create.act;/centre/login.html</code>
     * 转为正则
     * <code>(.*\.api)|(.*\.js)|(.*\.json)|(.*\.css)|(.*\.gif)|(.*\.jpg)|(.*\.png)|(/centre/sign/create\.act)|(/centre/login\.html)</code>
     * </pre>
     *
     * @author Hongs
     */
    public static final class URLPatterns {

        private final Pattern include;
        private final Pattern exclude;

        public URLPatterns(String urlInclude, String urlExclude) {
            if (urlInclude != null && ! urlInclude.isEmpty()) {
                include = compile(urlInclude);
            } else {
                include = null;
            }
            if (urlExclude != null && ! urlExclude.isEmpty()) {
                exclude = compile(urlExclude);
            } else {
                exclude = null;
            }
        }

        public URLPatterns(String urlInclude) {
            this(urlInclude, null);
        }

        @Override
        public  String toString( ) {
            if (include == null && exclude == null) {
                return "";
            } else
            if (exclude == null) {
                return "Include: "+include.toString();
            } else
            if (include == null) {
                return "Exclude: "+include.toString();
            } else
            {
                return "Include: "+include.toString()
                       +  "\r\n"  +
                       "Exclude: "+exclude.toString();
            }
        }

        public  boolean matches(String uri) {
            if (include == null && exclude == null) {
                return  false  ;
            } else
            if (exclude == null) {
                return  include.matcher(uri).matches();
            } else
            if (include == null) {
                return !exclude.matcher(uri).matches();
            } else
            {
                return  include.matcher(uri).matches()
                    && !exclude.matcher(uri).matches();
            }
        }

        private Pattern compile(String pat) {
            pat = pat.trim();
            pat = pat.replaceAll("[\\^\\$\\(\\)\\[\\]\\{\\}\\+\\.\\\\]", "\\\\$0");
            pat = pat.replaceAll("\\s*;\\s*", ")|(");
            pat = pat.replace("*", ".*");
            pat = pat.replace("?", ".?");
            pat = "(" + pat + ")";
            return Pattern.compile (pat);
        }

    }

}
