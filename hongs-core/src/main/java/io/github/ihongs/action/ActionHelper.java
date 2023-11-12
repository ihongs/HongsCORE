package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxCause;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Dist;

import java.io.File;
import java.io.Writer;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;
import javax.servlet.http.Part;

/**
 * 动作助手类
 *
 * <p>
 * 通过 getRequestData, getParameter, getAttribute, getSessibute, getCookibute
 * 来获取请求/容器/会话, 通过 reply 来告知此次动作是成功还是失败.
 * reply,fault 方法并不会将数据立即发往终端, flush 调用时才输出, 默认在一次动作结束后自动输出;
 * error,ensue 方法会立即发往终端然后关闭流, write 并不会关闭流, 可以在一次动作中反复调用输出.
 * </p>
 *
 * @author Hongs
 */
public class ActionHelper implements Cloneable
{

  /**
   * HttpServletRequest
   */
  private HttpServletRequest  request;

  /**
   * HttpServletResponse
   */
  private HttpServletResponse response;

  /**
   * 字节输出
   */
  private OutputStream        outputStream;

  /**
   * 字符输出
   */
  private       Writer        outputWriter;

  /**
   * 请求数据
   */
  private Map<String, Object> requestData = null;

  /**
   * 容器数据
   */
  private Map<String, Object> contextData = null;

  /**
   * 会话数据
   */
  private Map<String, Object> sessionData = null;

  /**
   * 跟踪数据
   */
  private Map<String, String> cookiesData = null;

  /**
   * 响应数据
   */
  private Map<String, Object> responseData = null;

  /**
   * 初始化助手(用于combat)
   *
   * @param req 请求数据
   * @param att 容器属性
   * @param ses Session 数据
   * @param cok Cookies 数据
   */
  public ActionHelper(Map req, Map att, Map ses, Map cok)
  {
    this.request      = null;
    this.response     = null;
    this.outputStream = null;
    this.outputWriter = null;

    this.requestData  = req ;
    this.contextData  = att ;
    this.sessionData  = ses ;
    this.cookiesData  = cok ;
  }

  /**
   * 初始化助手(用于action)
   *
   * @param req
   * @param rsp
   */
  public ActionHelper(HttpServletRequest req, HttpServletResponse rsp)
  {
    try
    {
      if (null != req)
      {
        req.setCharacterEncoding("UTF-8");
      }
      if (null != rsp)
      {
        rsp.setCharacterEncoding("UTF-8");
      }
    }
    catch (UnsupportedEncodingException ex)
    {
      throw new CruxExemption(ex, 1111, "Can not set encoding.");
    }

    this.request      = req ;
    this.response     = rsp ;
    this.outputStream = null;
    this.outputWriter = null;
  }

  /**
   * 供 ActionDriver 重设助手
   * @param req
   * @param rsp
   */
  public final void updateHelper(HttpServletRequest req, HttpServletResponse rsp)
  {
    try
    {
      if (null != req)
      {
        req.setCharacterEncoding("UTF-8");
      }
      if (null != rsp)
      {
        rsp.setCharacterEncoding("UTF-8");
      }
    }
    catch (UnsupportedEncodingException ex)
    {
      throw new CruxExemption(ex, 1111, "Can not set encoding.");
    }

    this.request      = req ;
    this.response     = rsp ;
    this.outputStream = null;
    this.outputWriter = null;
  }

  /**
   * 供 CombatRunner 重设输出
   * @param out
   * @param wrt
   */
  public final void updateOutput(OutputStream out, Writer wrt)
  {
    this.outputStream = out ;
    this.outputWriter = wrt ;
  }

  /**
   * 供 CombatRunner 重设输出
   * @param out
   */
  public final void updateOutput(OutputStream out)
  {
    this.updateOutput( out , new OutputStreamWriter( out ) );
  }

  public final void setRequestData(Map<String, Object> data) {
    this.requestData = data;
  }
  public final void setContextData(Map<String, Object> data) {
    this.contextData = data;
  }
  public final void setSessionData(Map<String, Object> data) {
    this.sessionData = data;
  }
  public final void setCookiesData(Map<String, String> data) {
    this.cookiesData = data;
  }

  /**
   * 获取 Servlet 请求对象
   * 注意: 当请求来自系统终端运维命令或虚构请求, 一般会返回 null
   * @return
   */
  public final HttpServletRequest getRequest()
  {
    return this.request;
  }

  /**
   * 获取请求数据
   *
   * 不同于 request.getParameterMap,
   * 该方法会将带"[]"的拆成子List, 将带"[xxx]"的拆成子Map, 并逐级递归.
   * 也可以解析用"." 连接的参数, 如 a.b.c 与上面的 a[b][c] 是同样效果.
   * 故请务必注意参数中的"."和"[]"符号.
   * 如果Content-Type为"multipart/form-data", 则使用 apache-common-fileupload 先将文件存入临时目录.
   *
   * @return 请求数据
   */
  public Map<String, Object> getRequestData()
  {
    if (this.request != null && this.requestData == null)
    {
      String ct  = this.request.getContentType();
      if  (  ct == null)
      {
        ct = "application/x-www-form-urlencode" ;
      }
      else
      {
        ct = ct.split(";", 2)[0];
      }

      try
      {
        Map ad, rd;
          ad = this.getRequestAttr(); // 可用属性传递
        if (ct.startsWith("multipart/"))
        {
          rd = this.getRequestPart(); // 处理上传文件
        } else if (ct.endsWith("/json"))
        {
          rd = this.getRequestJson(); // 处理JSON数据
        } else
        {
          rd = null;
        }

        requestData = parseParam(request.getParameterMap());

        // 深度整合数据
        if (rd != null)
        {
          Dict.putAll(requestData, rd);
        }
        if (ad != null)
        {
          Dict.putAll(requestData, ad);
        }
      }
      finally
      {
        // 防止解析故障后再调用又出错
        if (this.requestData == null )
        {
            this.requestData  = new HashMap();
        }
      }
    }
    return  this.requestData;
  }

  /**
   * 特殊 Content-Type 可通过过滤解析并传递
   * @return
   */
  final Map getRequestAttr() {
    // 特殊类型可用 Filter 预解析
    // 无法转换则报 HTTP 400 错误
    try {
        return (Map) request.getAttribute(Cnst.REQUES_ATTR);
    } catch (ClassCastException ex) {
        throw new CruxExemption(ex, 400);
    }
  }

  /**
   * 解析 application/json 或 text/json 数据
   * @return
   */
  final Map getRequestJson() {
    // 协议报文内容为空则不必理会
    // 解析失败则报 HTTP 400 错误
    if (request.getContentLength() < 1 ) {
        return null;
    }
    try {
        return (Map) Dist.toObject(request.getReader());
    } catch (RuntimeException ex) {
        throw new CruxExemption(ex, 400 );
    } catch (IOException ex) {
        throw new CruxExemption(ex, 1114);
    }
  }

  /**
   * 解析 multipart/form-map 数据, 处理上传
   * @return
   */
  final Map getRequestPart() {
    CoreConfig conf = CoreConfig.getInstance();

    long  sizeLimit = conf.getProperty("fore.upload.size.limit", 0L);
    Set<String> accept = null;
    String a = conf.getProperty("fore.upload.accept", null);
    if (a != null) {
        accept = new HashSet(Arrays.asList(a.trim().split("\\s*,\\s*")));
    }
    Set<String> reject = null;
    String r = conf.getProperty("fore.upload.reject", null);
    if (r != null) {
        reject = new HashSet(Arrays.asList(r.trim().split("\\s*,\\s*")));
    }

    //** 解析数据 **/

    try {
        Map rd = new HashMap();
        Map ud = new HashMap();

        for ( Part part : request.getParts( ) ) {
            long   size = part.getSize();
            String name = part.getName();
            String type = part.getContentType();
            String kind = part.getSubmittedFileName();

            // 无类型的普通参数已在外部处理
            if (name == null
            ||  type == null
            ||  kind == null) {
                continue;
            }

            // 空文件无伴随参数则将其设为空
            // 在修改的操作中表示将其置为空
            if (size ==  0  ) {
                if (null == request.getParameter(name)) {
                    Dict.setParam  ( rd , null , name );
                }
                continue;
            }

            // 检查大小
            if (sizeLimit > 0 && sizeLimit < size) {
                throw new CruxExemption(400, "File size must not exceed "+ sizeLimit);
            }

            if (accept != null || reject != null ) {

            // 从原始的文件名中提取出扩展名
            int pos  = kind.lastIndexOf('.');
            if (pos != -1) {
                kind = kind.substring(1+pos);
            } else {
                kind = null;
            }

            // 从文件类型中拆分得到通配类型
            String typa = null;
            pos  = type.indexOf(';');
            if (pos != -1) {
                type = type.substring(0,pos);
            }
            pos  = type.indexOf('/');
            if (pos != -1) {
                typa = type.substring(0,pos) + "/*";
            }

            // 检查类型
            if (accept != null
            && !accept.contains(kind)
            && !accept.contains(type)
            && !accept.contains(typa) ) {
                throw new CruxExemption(400, "File type is not allowed");
            }
            if (reject != null
            && (reject.contains(kind)
            ||  reject.contains(type)
            ||  reject.contains(typa))) {
                throw new CruxExemption(400, "File type is denied");
            }

            } // End if accept or reject

            Dict.setValue( ud, part, name  , null );
            Dict.setParam( rd, part, name );
        }

        // 记录在应用里以便有需要时候还可读取原始值
        setAttribute(Cnst.UPLOAD_ATTR , ud);

        return rd;
    } catch (IllegalStateException e) {
        throw new CruxExemption(e, 400 ); // 上传受限, 如大小超标
    } catch (ServletException e) {
        throw new CruxExemption(e, 1113);
    } catch (IOException e) {
        throw new CruxExemption(e, 1113);
    }
  }

  /**
   * 获取 Servlet 响应对象
   * 注意: 当请求来自系统终端运维命令或虚构请求, 一般会返回 null
   * @return
   */
  public final HttpServletResponse getResponse()
  {
    return this.response;
  }

  /**
   * 获取响应数据
   *
   * 注意:
   * 该函数为过滤器提供原始的返回数据,
   * 只有使用 reply 函数返回的数据才会被记录,
   * 其他方式返回的数据均不会记录在此,
   * 使用时务必判断是否为 null.
   *
   * @return 响应数据
   */
  public Map<String, Object> getResponseData()
  {
    return this.responseData;
  }

  /**
   * 获取响应输出
   * 注意: 当为虚拟请求时, 可能抛 CruxExemption, 错误码 1110
   * @return 响应输出
   */
  public OutputStream getOutputStream()
  {
    if (this.outputStream != null)
    {
        return this.outputStream;
    } else
    if (this.response/**/ != null)
    {
      try
      {
        return this.response.getOutputStream();
      }
      catch (IOException ex)
      {
        throw new CruxExemption(ex, 1110, "Can not get output stream.");
      }
    } else
    {
        throw new CruxExemption(/**/1110, "Can not get output stream.");
    }
  }

  /**
   * 获取响应输出
   * 注意: 当为虚拟请求时, 可能抛 CruxExemption, 错误码 1110
   * @return 响应输出
   */
  public Writer getOutputWriter()
  {
    if (this.outputWriter != null)
    {
        return this.outputWriter;
    } else
    if (this.response/**/ != null)
    {
      try
      {
        return this.response.getWriter();
      }
      catch (IOException ex)
      {
        throw new CruxExemption(ex, 1110, "Can not get output writer.");
      }
    } else
    {
        throw new CruxExemption(/**/1110, "Can not get output writer.");
    }
  }

  /**
   * 获取请求参数
   * @param name
   * @return 当前请求参数
   */
  public String getParameter(String name)
  {
    Map d = getRequestData();
    if (d == null)
    {
      return null;
    }
    Object o = Dict.getParam ( d ,  name  );
    if (o == null)
    {
      return null;
    }
    if (o instanceof Map )
    {
      o = new ArrayList(((Map) o).values());
    } else
    if (o instanceof Set )
    {
      o = new ArrayList(((Set) o));
    }
    if (o instanceof List)
    {
      List a = (List) o;
      int  i = a.size();
      o =  a.get(i - 1);
    }
    return o.toString();
  }

  /**
   * 获取容器属性
   * @param name
   * @return 当前属性, 没有则为 null
   */
  public Object getAttribute(String  name)
  {
    if (null != this.contextData) {
        return  this.contextData.get(name);
    } else
    if (null != this.request/**/) {
        return  this.request.getAttribute(name);
    } else {
        return  null;
    }
  }

  /**
   * 设置容器属性
   * 当 value 为 null 时 name 对应的属性将删除
   * @param name
   * @param value
   */
  public void setAttribute(String name, Object value)
  {
    if (this.contextData != null) {
      if (value == null) {
        this.contextData.remove(name);
      } else {
        this.contextData.put(name , value);
      }
        this.contextData.put(Cnst.UPDATE_ATTR, System.currentTimeMillis());
    } else
    if (this.request/**/ != null) {
      if (value == null) {
        this.request.removeAttribute(name);
      } else {
        this.request.setAttribute(name , value);
      }
    } else {
      throw new NullPointerException("Context is not ready");
    }
  }

  /**
   * 获取会话取值
   * @param name
   * @return 当前取值, 没有则为 null
   */
  public Object getSessibute(String  name)
  {
    if (null != this.sessionData) {
        return  this.sessionData.get(name);
    } else
    if (null != this.request/**/) {
      HttpSession ss = this.request.getSession(false);
      if (null != ss ) return ss.getAttribute ( name);
      return null;
    } else {
      return null;
    }
  }

  /**
   * 设置会话取值
   * 当 value 为 null 时 name 对应的数据将删除
   * @param name
   * @param value
   */
  public void setSessibute(String name, Object value)
  {
    if (this.sessionData != null) {
      if (value == null) {
        this.sessionData.remove(name);
      } else {
        this.sessionData.put(name , value);
      }
        this.sessionData.put(Cnst.UPDATE_ATTR, System.currentTimeMillis());
    } else
    if (this.request/**/ != null) {
      if (value == null) {
        HttpSession ss = this.request.getSession(false);
        if (null != ss ) ss.removeAttribute(name);
      } else {
        HttpSession ss = this.request.getSession(true );
        if (null != ss ) ss.setAttribute(name , value );
      }
    } else {
      throw new NullPointerException("Session is not ready");
    }
  }

  /**
   * 获取跟踪参数
   * @param name
   * @return 当前取值, 没有则为 null
   */
  public String getCookibute(String  name) {
    if (null != this.cookiesData) {
        return  this.cookiesData.get(name);
    } else
    if (null != this.request/**/) {
      Cookie [] cs = this.request.getCookies();
      if (cs != null) {
        for(Cookie ce: cs) {
          if (ce.getName().equals(name)) {
            try {
              return URLDecoder.decode(ce.getValue(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
              throw new CruxExemption(e, 1111);
            }
          }
        }
      }
      return null;
    } else {
      return null;
    }
  }

  /**
   * 设置跟踪参数
   * 当 value 为 null 时 name 对应的数据将删除
   * @param name
   * @param value
   */
  public void setCookibute(String name, String value) {
    if (this.cookiesData != null) {
      if (value == null) {
        this.cookiesData.remove(name);
      } else {
        this.cookiesData.put(name, value);
      }
        this.cookiesData.put(Cnst.UPDATE_ATTR, Long.toString(System.currentTimeMillis()));
    } else
    if (this.response != null) {
      if (value == null) {
        setCookibute(name, value,  0, Core.SERV_PATH + "/", null, false, false);
      } else {
        setCookibute(name, value, -1, Core.SERV_PATH + "/", null, false, false);
      }
    } else {
      throw new NullPointerException("Cookies is not ready");
    }
  }

  /**
   * 设置跟踪参数
   * 注意: 此方法总是操作真实 Cookie
   * @param name
   * @param value
   * @param life 生命周期(秒)
   * @param path 路径
   * @param host 域名
   * @param httpOnly 文档内禁读取
   * @param secuOnly 使用安全连接
   */
  public void setCookibute(String name, String value,
    int life, String path, String host, boolean httpOnly, boolean secuOnly) {
    if (response != null) {
      if ( value != null) {
        try {
          value = URLEncoder.encode(value,"UTF-8");
        } catch ( UnsupportedEncodingException e ) {
          throw new CruxExemption(e);
        }
      }
      Cookie ce = new Cookie(name, value);
      if (path != null) {
          ce.setPath  (path);
      }
      if (host != null) {
          ce.setDomain(host);
      }
      if (life >=  0  ) {
          ce.setMaxAge(life);
      }
      if (secuOnly) {
          ce.setSecure(true);
      }
      if (httpOnly) {
          ce.setHttpOnly(true);
      }
      response.addCookie( ce );
    } else {
      throw new NullPointerException("Response is not ready for set cookie");
    }
  }

  /**
   * 获取实例
   * 通常要在 ActionDriver 等容器初始化后使用,
   * 独立异步线程内将填充空的请求和容器空间等.
   * @return
   */
  public static ActionHelper getInstance()
  {
    Core   core = Core.getInstance();
    String name = ActionHelper.class.getName();
    ActionHelper  inst = (ActionHelper) core.get(name);
    if (inst == null) {
        inst = new ActionHelper (new HashMap(), new HashMap(0), new HashMap(0), new HashMap(0));
        core.put(name, inst);
    }
    return inst;
  }

  /**
   * 新建实例
   * 用于使用 ActionRunner 时快速构建请求对象,
   * 可用以上 setXxxData() 在构建之后设置参数.
   * @return
   */
  public static ActionHelper newInstance() {
    Core   core = Core.getInstance();
    String name = ActionHelper.class.getName();
    ActionHelper  inst = (ActionHelper) core.get(name);
    if (inst == null) {
        inst = new ActionHelper (new HashMap(), new HashMap(0), new HashMap(0), new HashMap(0));
    } else {
        inst = inst.clone( );
    }
    return inst;
  }

  @Override
  public ActionHelper clone() {
    ActionHelper helper;
    try {
      helper = (ActionHelper) super.clone( );
    } catch ( CloneNotSupportedException e ) {
      throw new CruxExemption(e);
    }
    helper.responseData = null;
    return helper;
  }

  //** 返回数据 **/

  /**
   * 返回响应数据
   * 针对 search 等
   * @param map
   */
  public void reply(Map map)
  {
    if (map != null
    && !map.containsKey("ok")) {
        map.put( "ok" , true );
    }
    this.responseData = map;
  }

  /**
   * 返回结果信息
   * 针对 create 等
   * @param msg
   * @param info
   * @deprecated 新的 create 返回 id
   */
  public void reply(String msg, Map info)
  {
    Map map = new HashMap(3);
    if (null !=  msg) {
        map.put("msg" , msg);
    }
    map.put("info", info);
    reply(map);
  }

  /**
   * 返回新的编号
   * 针对 create 等
   * @param msg
   * @param id 新的编号
   */
  public void reply(String msg, String id)
  {
    Map map = new HashMap(3);
    if (null !=  msg) {
        map.put("msg" , msg);
    }
    map.put(Cnst.ID_KEY, id);
    reply(map);
  }

  /**
   * 返回影响行数
   * 针对 delete 等
   * @param msg
   * @param rn 影响数量
   */
  public void reply(String msg, Number rn)
  {
    Map map = new HashMap(3);
    if (null !=  msg) {
        map.put("msg" , msg);
    }
    map.put(Cnst.RN_KEY, rn);
    reply(map);
  }

  /**
   * 返回操作提示
   * @param msg
   */
  public void reply(String msg)
  {
    Map map = new HashMap(2);
    if (null !=  msg) {
        map.put("msg" , msg);
    }
    map.put("ok", true );
    reply(map);
  }

  /**
   * 返回错误消息
   * @param msg
   */
  public void fault(String msg)
  {
    Map map = new HashMap(2);
    if (null !=  msg) {
        map.put("msg" , msg);
    }
    map.put("ok", false);
    reply(map);
  }

  /**
   * 返回异常信息
   * @param ex
   */
  public void fault(CruxCause ex)
  {
    HttpServletResponse rs = getResponse();
    Throwable ta = (Throwable) ex;
    Throwable te = ta.getCause( );
    int      ero = ex.getState( );
    String   ern = ex.getStage( );
    String   err ;
    String   msg ;

    // 错误消息
    DO: {
      err = ta.getMessage();
      msg = ta.getLocalizedMessage();
      if (null != msg && ! msg.isEmpty()) break DO;

      if (null != te) {
      msg = te.getLocalizedMessage();
      if (null != msg && ! msg.isEmpty()) break DO;
      }

      msg = CoreLocale.getInstance().translate("core.error.undef");
    }

    // 外部错误, 可不记录
    if (ero >= 400 && ero <= 499) {
        if (null != rs) {
            rs.setStatus(ero);
        }
        if (4 == (4 &Core.DEBUG)) {
            CoreLogger.debug(ta.toString());
        }
    } else
    // 内部错误
    if (ero >= 500 && ero <= 599) {
        if (null != rs) {
            rs.setStatus(ero);
        }
        CoreLogger.error(ta );
    } else
    // 其他异常
    if (ero >= 600 || ero <= 199) {
        if (null != rs) {
            rs.setStatus(500);
        }
        CoreLogger.error(ta );
    } else {
        CoreLogger.error(ta );
    }

    Map map = new HashMap (4);
    map.put( "ok" , false );
    map.put("ern" , ern );
    map.put("err" , err );
    map.put("msg" , msg );
    reply(map);
  }

  //** 输出内容 **/

  /**
   * 输出数据
   * 按情况以 JSON/JSONP 格式输出
   */
  public void flush()
  {
    if (null == this.responseData) {
        return;
    }

    // 动作调用了 forward 之后
    // 这时再调用 getWriter 会抛 STREAM 异常
    // 因容器调用 getOutputStream 来输出内容
    // 所以必须将 OutputStream 包装成 Writer
    Writer out;
    try {
        out = getOutputWriter();
    } catch ( IllegalStateException e ) {
        out = new OutputStreamWriter(getOutputStream());
    }

    // 检查是否有 JSONP 的回调函数
    String  fun  = null;
    if (request != null) {
        fun = request.getParameter (Cnst.CB_KEY);
        if (fun == null) {
            fun  = request.getParameter (
                CoreConfig.getInstance( )
                          .getProperty("core.callback", "callback")
            );
        }
    }

    // 默认的数据输出的格式为 JSON
    // 有指定回调函数名则使用 JSONP
    // 特殊前缀则返回嵌 JS 的 XHTML
    try {
        if (fun != null && !fun.isEmpty() && !fun.equals("~") ) {
            if (fun.startsWith(   "top.")
            ||  fun.startsWith("parent.")
            ||  fun.startsWith("opener.")
            ||  fun.startsWith("frames.")  ) {
                if (this.response != null
                && !this.response.isCommitted( )) {
                    this.response.setCharacterEncoding("UTF-8");
                    this.response.setContentType( "text/html" );
                }

                out.append("<script type=\"text/javascript\">");
                out.append( fun);
                out.append("(" );
                Dist.append(out, this.responseData);
                out.append(");");
                out.append("</script>");
            } else {
                if (this.response != null
                && !this.response.isCommitted( )) {
                    this.response.setCharacterEncoding("UTF-8");
                    this.response.setContentType( "text/javascript");
                }

                out.append( fun);
                out.append("(" );
                Dist.append(out, this.responseData);
                out.append(");");
            }
        } else {
                if (this.response != null
                && !this.response.isCommitted( )) {
                    this.response.setCharacterEncoding("UTF-8");
                    this.response.setContentType("application/json");
                }

                Dist.append(out, this.responseData);
        }

        out.flush( );
    } catch ( IOException e ) {
      throw new CruxExemption(e, 1110, "Can not send to client.");
    }

    this.responseData = null;
  }

  /**
   * 输出网页
   * @param htm
   */
  public void write(String htm)
  {
    this.write("text/html",htm);
  }

  /**
   * 输出内容
   * @param ct Content-Type
   * @param txt
   */
  public void write(String ct, String txt)
  {
    if (null != this.response) {
        if (null != ct ) {
            this.response.setContentType(ct);
        if (! ct.contains("charset=") ) {
            this.response.setCharacterEncoding("utf-8");
        } } else {
            this.response.setCharacterEncoding("utf-8");
        }
    }

    // 动作调用了 forward 之后
    // 这时再调用 getWriter 会抛 STREAM 异常
    // 因容器调用 getOutputStream 来输出内容
    // 所以必须将 OutputStream 包装成 Writer
    Writer out;
    try {
        out = getOutputWriter();
    } catch ( IllegalStateException e ) {
        out = new OutputStreamWriter(getOutputStream());
    }

    try {
        out.write(txt);
    //  out.flush(   ); // 不必立即输出, 可能反复调用
    } catch ( IOException e ) {
      throw new CruxExemption(e, 1110, "Can not send to client.");
    }
  }

  /**
   * 错误通知
   * @param msg
   */
  public void error(String msg)
  {
    this.error(500 , msg);
  }

  /**
   * 错误通知
   * @param sc 400,500 等
   * @param msg
   */
  public void error(int sc, String msg)
  {
    try {
      this.response.sendError(sc , msg);
      this.responseData = null;
    } catch ( IOException e ) {
      throw new CruxExemption(e, 1110, "Can not send to client.");
    }
  }

  /**
   * 转入目标
   * @param url
   */
  public void ensue(String url)
  {
    this.ensue(302 , url);
  }

  /**
   * 转入目标
   * @param sc 301,302 等
   * @param url
   */
  public void ensue(int sc, String url)
  {
    url = ActionDriver.fixUrl(url);
    try {
      this.response.setStatus(/** 30X **/ sc );
      this.response.setHeader("Location", url);
      this.response.flushBuffer( );
      this.responseData = null;
    } catch ( IOException e ) {
      throw new CruxExemption(e, 1110, "Can not send to client.");
    }
  }

  /**
   * 跳转页面
   * @param url
   * @param msg
   */
  public void ensue(String url, String msg)
  {
    this.ensue(302 , url , msg);
  }

  /**
   * 跳转页面
   * @param sc 302,404 等
   * @param url
   * @param msg
   */
  public void ensue(int sc, String url, String msg)
  {
    url = ActionDriver.fixUrl(url);
    String p = CoreConfig.getInstance().getProperty("core.redirect", "/300.jsp");
    if ( ! p.isEmpty() && new File(Core.BASE_PATH + p).exists())
    {
      this.request.setAttribute("javax.servlet.location" , url);
      this.request.setAttribute("javax.servlet.error.message", msg);
      this.request.setAttribute("javax.servlet.error.status_code", sc);
      try {
        this.request.getRequestDispatcher(p).forward(request, response);
      } catch ( IOException|ServletException e ) {
        throw new CruxExemption( e , 1110 , "Can not send to client." );
      }
    }
    else
    {
      try {
        this.response.setStatus(sc);
        this.response.getWriter().print(
            "<html><head>"
          + "<meta http-equiv=\"Expires\" content=\"0\">"
          + "<meta http-equiv=\"Refresh\" content=\"3; url="+url+"\">"
          + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">"
          + "<title>"+msg+"</title></head><body> <a href=\""+url+"\">"+msg+"</a> </body></html>"
        );
        this.responseData = null;
      } catch ( IOException e ) {
        throw new CruxExemption( e , 1110 , "Can not send to client." );
      }
    }
  }

  //** 工具方法 **/

  /**
   * 解析参数
   * 用于处理查询串
   * @param s
   * @return
   */
  public static final Map parseQuery(String s) {
      Map<String, List<String>> a = new HashMap();
      int j , i;
          j = 0;

      while (j < s.length()) {
          // 查找键
          i = j;
          while (j < s.length() && s.charAt(j) != '=' && s.charAt(j) != '&') {
              j ++;
          }
          String k;
          try {
              k = s.substring(i, j);
              k = URLDecoder.decode(k, "UTF-8");
          } catch (UnsupportedEncodingException ex) {
              throw new CruxExemption ( ex , 1111 );
          }
          if (j < s.length() && s.charAt(j) == '=') {
              j++;
          }

          // 查找值
          i = j;
          while (j < s.length() && s.charAt(j) != '&') {
              j++;
          }
          String v;
          try {
              v = s.substring(i, j);
              v = URLDecoder.decode(v, "UTF-8");
          } catch (UnsupportedEncodingException ex) {
              throw new CruxExemption ( ex , 1111 );
          }
          if (j < s.length() && s.charAt(j) == '&') {
              j++;
          }

          // 匿名参数, 规则同 CombatHelper.getOpts
          if (s.charAt(i - 1) != '=') {
              v =  k ;
              k = ".";
          }

          Dict.setParam(a, v, k);
      }

      return a;
  }

  /**
   * 解析参数
   * 用于处理 Servlet 请求数据
   * @param params
   * @return 解析后的Map
   */
  public static final Map parseParam(Map<String, String[]> params)
  {
    Map<String, Object> paramz = new HashMap();
    for (Map.Entry<String, String[]> et : params.entrySet())
    {
        String   key = et.getKey(  );
        String[] arr = et.getValue();
        for ( String value  :  arr )
        {
            Dict.setParam(paramz, value, key );
        }
    }
    return paramz;
  }

  /**
   * 解析参数
   * 用于处理 WebSocket 请求数据
   * @param params
   * @return 解析后的Map
   */
  final public static Map parseParan(Map<String, List<String>> params)
  {
    Map<String, Object> paramz = new HashMap();
    for (Map.Entry<String, List<String>> et : params.entrySet())
    {
        String   key = et.getKey(  );
        List     arr = et.getValue();
        for ( Object value  :  arr )
        {
            Dict.setParam(paramz, value, key );
        }
    }
    return paramz;
  }

}
