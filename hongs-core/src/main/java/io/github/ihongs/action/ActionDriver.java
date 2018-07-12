package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.util.Data;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.Tool;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TimeZone;
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
 * server.id              服务ID
 * core.language.probing  探测语言
 * core.language.default  默认语言
 * core.timezone.probing  探测时区
 * core.timezone.default  默认时区
 * </pre>
 *
 * @author Hong
 */
public class ActionDriver extends HttpServlet implements Servlet, Filter {

    /**
     * 首位标识, 为 true 表示最先执行，负责初始化和清理
     */
    private boolean INIT = false;

    /**
     * 关闭表示, 为 true 表示有初始化, 需要承担全局清理
     */
    private boolean SHUT = false;

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

        INIT= true;
        if (Core.BASE_HREF == null) {
        SHUT= true;

            System.setProperty("file.encoding", "UTF-8");

            /** 核心属性配置 **/

            Core.DEBUG = Synt.declare(cont.getInitParameter("debug"), (byte) 0);

            Core.BASE_HREF = cont.getContextPath();
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

            CoreConfig cnf = CoreConfig.getInstance("defines");
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
                    v = Tool.inject(v,m);
                    System.setProperty(k,v);
                }
            }

            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
            // 调试系统属性
            for(Map.Entry et : cnf.entrySet()) {
                String k = (String) et.getKey  ();
                String v = (String) et.getValue();
                if (k.startsWith("debug.")) {
                    k = k.substring(6  );
                    v = Tool.inject(v,m);
                    System.setProperty(k,v);
                }
            }
            }

            // 设置默认语言
            cnf = CoreConfig.getInstance("default");
            Core.ACTION_LANG.set(cnf.getProperty("core.language.default", "zh_CN"));
            Core.ACTION_ZONE.set(cnf.getProperty("core.timezone.default", "GMT-8"));
        }

        // 调一下 ActionRunner 来加载动作
        ActionRunner.getActions();

        if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
            CoreLogger.debug(new StringBuilder("...")
                .append("\r\n\tDEBUG       : ").append(Core.DEBUG)
                .append("\r\n\tSERVER_ID   : ").append(Core.SERVER_ID)
                .append("\r\n\tBASE_HREF   : ").append(Core.BASE_HREF)
                .append("\r\n\tBASE_PATH   : ").append(Core.BASE_PATH)
                .append("\r\n\tCORE_PATH   : ").append(Core.CORE_PATH)
                .append("\r\n\tCONF_PATH   : ").append(Core.CONF_PATH)
                .append("\r\n\tDATA_PATH   : ").append(Core.DATA_PATH)
                .toString());
        }
    }

    /**
     * 公共销毁
     */
    @Override
    public void destroy () {
        if (! INIT) {
            return;
        }

        if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
            Core core = Core.GLOBAL_CORE;
            long time = System.currentTimeMillis() - Core.STARTS_TIME;
            CoreLogger.debug(new StringBuilder("...")
                .append("\r\n\tSERVER_ID   : ").append(Core.SERVER_ID)
                .append("\r\n\tObjects     : ").append(core.toString())
                .append("\r\n\tRuntime     : ").append(Tool.humanTime(time))
                .toString());
        }

        if (! SHUT) {
            return;
        }

        try {
            Core.GLOBAL_CORE.close();
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
            core.put ( ActionHelper.class.getName(), hlpr );

            try {
                doLaunch(core, hlpr, req, rsq );
                agt.doDriver ( core, hlpr);
                doCommit(core, hlpr, req, rsq );
            } catch (IOException ex) {
                CoreLogger.error(ex);
            } catch (ServletException ex ) {
                CoreLogger.error(ex);
            } catch (RuntimeException ex ) {
                CoreLogger.error(ex);
            } catch (Error er) {
                CoreLogger.error(er);
            } finally {
                doFinish(core, hlpr, req );
            }
        } else {
            /**
             * 内层调用
             */
            Core.THREAD_CORE.set(core);
            hlpr = core.get(ActionHelper.class);
            hlpr.updateHelper( req, rsq );
            /**/ agt.doDriver(core, hlpr);
        }
    }

    private void doCommit(Core core, ActionHelper hlpr, HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException {
        Map dat  = hlpr.getResponseData();
        if (dat != null) {
            req .setAttribute(Cnst.RESP_ATTR, dat);
            hlpr.updateHelper( req, rsp );
            hlpr.responed();
        }
    }

    private void doLaunch(Core core, ActionHelper hlpr, HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException {
        Core.ACTION_TIME.set(System.currentTimeMillis/**/( ));
        Core.CLIENT_ADDR.set(getClientAddr(req)/*Remote IP*/);
        Core.ACTION_NAME.set(getOriginPath(req).substring(1));

        CoreConfig conf = core.get(CoreConfig.class);

        Core.ACTION_ZONE.set(conf.getProperty("core.timezone.default","GMT+8"));
        if (conf.getProperty("core.timezone.probing", false)) {
            /**
             * 时区可以记录到Session/Cookies里
             */
            String sess = conf.getProperty("core.timezone.session", "zone");
            String zone = (String) hlpr.getSessibute(sess);
            if (zone == null || zone.length() == 0) {
                   zone = (String) hlpr.getCookibute(sess);
            if (zone == null || zone.length() == 0) {
                   zone = req.getHeader(/*Cur*/"Timezone");
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

        Core.ACTION_LANG.set(conf.getProperty("core.language.default","zh_CN"));
        if (conf.getProperty("core.language.probing", false)) {
            /**
             * 语言可以记录到Session/Cookies里
             */
            String sess = conf.getProperty("core.language.session", "lang");
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

        if (! hlpr.getResponse().isCommitted()) {
            /**
             * 输出特定的服务器信息
             */
            String pb;
            pb = conf.getProperty("core.powered.by");
            if (pb != null && pb.length() != 0) {
                rsp.setHeader("X-Powered-By", pb);
            }
            pb = conf.getProperty("core.service.by");
            if (pb != null && pb.length() != 0) {
                rsp.setHeader(  "Server"    , pb);
            }
        }
    }

    private void doFinish(Core core, ActionHelper hlpr, HttpServletRequest req) {
        try {
            if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
                /**
                 * 提取必要的客户相关标识
                 * 以便判断用户和模拟登录
                 */
                            req = hlpr.getRequest(/***/);
                HttpSession ses = req .getSession(false);
                Object      uid = hlpr.getSessibute(Cnst.UID_SES);
                String      mem ;
                if (ses == null ) {
                    mem  =  "-" ;
                } else {
                    mem  =  ses.getId();
                }
                if (uid != null ) {
                    mem +=  " " + uid  ;
                }

                long time = System.currentTimeMillis(  ) - Core.ACTION_TIME.get();
                StringBuilder sb = new StringBuilder("...");
                  sb.append("\r\n\tACTION_NAME : ").append(Core.ACTION_NAME.get())
                    .append("\r\n\tACTION_TIME : ").append(Core.ACTION_TIME.get())
                    .append("\r\n\tACTION_LANG : ").append(Core.ACTION_LANG.get())
                    .append("\r\n\tACTION_ZONE : ").append(Core.ACTION_ZONE.get())
                    .append("\r\n\tMethod      : ").append(req.getMethod( ))
                    .append("\r\n\tMember      : ").append(mem)
                    .append("\r\n\tObjects     : ").append(core.toString( ))
                    .append("\r\n\tRuntime     : ").append(Tool.humanTime( time ));

                /**
                 * 显示请求报头及输入输出
                 * 这对调试程序非常有帮助
                 */

                CoreConfig cf = CoreConfig.getInstance();

                if (cf.getProperty("core.debug.action.request", false)) {
                    Map rd  = null;
                    try {
                        rd  = hlpr.getRequestData();
                    } catch ( HongsExemption  ex  ) {
                        CoreLogger.debug(ex.getMessage());
                    }
                    if (rd != null && !rd.isEmpty()) {
                        sb.append("\r\n\tRequest     : ")
                          .append(Tool.indent(Data.toString(rd)).substring(1));
                    }
                }

                if (cf.getProperty("core.debug.action.results", false)) {
                    Map xd  = hlpr.getResponseData();
                    if (xd == null) {
                        xd  = (Map) req.getAttribute(Cnst.RESP_ATTR);
                    }
                    if (xd != null && !xd.isEmpty()) {
                        sb.append("\r\n\tResults     : ")
                          .append(Tool.indent(Data.toString(xd)).substring(1));
                    }
                }

                if (cf.getProperty("core.debug.action.session", false) && ses != null) {
                  Map         map = new HashMap();
                  Enumeration<String> nms = ses.getAttributeNames();
                  while (nms.hasMoreElements()) {
                      String  nme = nms.nextElement();
                      map.put(nme , ses.getAttribute(nme));
                  }
                  if (!map.isEmpty()) {
                      sb.append("\r\n\tSession     : ")
                        .append(Tool.indent(Data.toString(map)).substring(1));
                  }
                }

                if (cf.getProperty("core.debug.action.context", false)) {
                  Map         map = new HashMap();
                  Enumeration<String> nms = req.getAttributeNames();
                  while (nms.hasMoreElements()) {
                      String  nme = nms.nextElement();
                      map.put(nme , req.getAttribute(nme));
                  }
                  if (!map.isEmpty()) {
                      sb.append("\r\n\tContext     : ")
                        .append(Tool.indent(Data.toString(map)).substring(1));
                  }
                }

                if (cf.getProperty("core.debug.action.headers", false)) {
                  Map         map = new HashMap();
                  Enumeration<String> nms = req.getHeaderNames();
                  while (nms.hasMoreElements()) {
                      String  nme = nms.nextElement();
                      map.put(nme , req.getHeader(nme));
                  }
                  if (!map.isEmpty()) {
                      sb.append("\r\n\tHeaders     : ")
                        .append(Tool.indent(Data.toString(map)).substring(1));
                  }
                }

                if (cf.getProperty("core.debug.action.cookies", false)) {
                  Map         map = new HashMap();
                  Cookie[]    cks = req.getCookies();
                  for (Cookie cke : cks) {
                      map.put(cke.getName( ), cke.getValue( ));
                  }
                  if (!map.isEmpty()) {
                      sb.append("\r\n\tCookies     : ")
                        .append(Tool.indent(Data.toString(map)).substring(1));
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
                core.close( );
            } catch (Error e) {
                CoreLogger.error( e );
            } catch (Exception e) {
                CoreLogger.error( e );
            }
            req.removeAttribute(Core.class.getName());
            Core.THREAD_CORE.remove();
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
        service(/**/ hlpr.getRequest(), hlpr.getResponse());
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
     * 获得发起的客户端IP
     * @param req
     * @return
     */
    public static final String getClientAddr(HttpServletRequest req) {
        String ip = (String)req.getAttribute(Cnst.CLIENT_ATTR);
        if (null != ip) {
            return  ip;
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
     * 执行动作代理
     */
    public static  interface  DriverProxy {

        public void doDriver(Core core, ActionHelper hlpr) throws ServletException, IOException;

    }

}
