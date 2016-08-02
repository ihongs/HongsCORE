package app.hongs.action;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLogger;
import app.hongs.HongsError;
import app.hongs.HongsUnchecked;
import app.hongs.util.Data;
import app.hongs.util.Dict;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * 动作助手类
 *
 * <p>
 * 通过 getRequestData,getParameter,getAttribute,getSessValue
 * 来获取请求/容器/会话; 通过 reply 来通知前端动作的成功或失败
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
   * HttpServletResponse
   */
  private HttpServletResponse response;

  /**
   * 响应数据
   */
  private Map<String, Object> responseData = null;

  /**
   * 响应输出
   */
  private OutputStream        outputStream = null;
  private Writer              outputWriter = null;

  /**
   * 初始化助手(用于cmdlet)
   *
   * @param req
   * @param att
   * @param ses
   * @param cok
   * @param out
   */
  public ActionHelper(Map req, Map att, Map ses, Map cok)
  {
    this.request      = null;
    this.requestData  = req != null ? req : new HashMap();
    this.contextData  = att != null ? att : new HashMap();
    this.sessionData  = ses != null ? ses : new HashMap();
    this.cookiesData  = cok != null ? cok : new HashMap();
    this.response     = null;
  }

  /**
   * 初始化助手(用于action)
   *
   * @param req
   * @param rsp
   */
  public ActionHelper(HttpServletRequest req, HttpServletResponse rsp)
  {
    this.request  = req;
    this.response = rsp;

    try
    {
      if (null != this.request )
      {
        this.request .setCharacterEncoding("UTF-8");
      }
      if (null != this.response)
      {
        this.response.setCharacterEncoding("UTF-8");
      }
    }
    catch (UnsupportedEncodingException ex)
    {
      throw new HongsError(0x31, "Can not set encoding.", ex);
    }
  }

  public void reinitHelper(HttpServletRequest req, HttpServletResponse rsp)
  {
    this.request  = req;
    this.response = rsp;

    try
    {
      if (null != this.request )
      {
        this.request .setCharacterEncoding("UTF-8");
      }
      if (null != this.response)
      {
        this.response.setCharacterEncoding("UTF-8");
      }
    }
    catch (UnsupportedEncodingException ex)
    {
      throw new HongsError(0x31, "Can not set encoding.", ex);
    }
  }

  public void setRequestData(Map<String, Object> data) {
    this.requestData  = data;
  }
  public void setContextData(Map<String, Object> data) {
    this.contextData  = data;
  }
  public void setSessionData(Map<String, Object> data) {
    this.sessionData  = data;
  }
  public void setCookiesData(Map<String, String> data) {
    this.cookiesData  = data;
  }
  public void setOutputStream(OutputStream pipe) {
    this.outputStream = pipe;
  }
  public void setOutputWriter(Writer pipe) {
    this.outputWriter = pipe;
  }

  public final HttpServletRequest getRequest()
  {
    return this.request;
  }

  /**
   * 获取请求文本
   * @return 请求文本
   */
  public String getRequestText()
  {
    if (null != this.request) try
    {
      BufferedReader reader = this.request.getReader();
      String text = "";
      char[] buf = new char[1024];
      int i = 0, j = 0;

      while (-1 != ( i = reader.read(buf, j, 1024) ) )
      {
        text += new String ( buf);
        j += i * 1024 ;
      }

      return text;
    }
    catch (IOException ex)
    {
      CoreLogger.error(ex);
    }

    return "";
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
      if  (  ct != null)
      {
        ct = ct.split(";", 2)[0];
      }
      else
      {
        ct = "application/x-www-form-urlencode" ;
      }

      if ("application/json".equals(ct) || "text/json".equals(ct))
      {
        this.requestData = (Map<String, Object>) Data.toObject(getRequestText());
      }
      else
      {
        this.requestData = parseParam(request.getParameterMap(  ));

        // 处理上传文件
        if ("multipart/form-data".equals(ct))
        {
          chkUploadsData( this.requestData );
        }
      }
    }
    return this.requestData;
  }

  /**
   * 解析 multipart/form-data 数据, 并将上传的文件放入临时目录
   * @param rd
   */
  protected final void chkUploadsData(Map rd ) {
    CoreConfig conf = CoreConfig.getInstance();
    String     path = Core.DATA_PATH+"/upload";

    // 临时目录不存在则创建
    File df = new File (path);
    if (!df.isDirectory()) {
         df.mkdirs();
    }

    Set<String> allowTypes = null;
    Set<String>  denyTypes = null;
    Set<String> allowExtns = null;
    Set<String>  denyExtns = null;
    String x;
    x = conf.getProperty("fore.upload.allow.types", null);
    if (x != null) {
        allowTypes = new HashSet(Arrays.asList(x.split(",")));
    }
    x = conf.getProperty("fore.upload.deny.types" , null);
    if (x != null) {
         denyTypes = new HashSet(Arrays.asList(x.split(",")));
    }
    x = conf.getProperty("fore.upload.allow.extns", null);
    if (x != null) {
        allowExtns = new HashSet(Arrays.asList(x.split(",")));
    }
    x = conf.getProperty("fore.upload.deny.extns" , null);
    if (x != null) {
         denyExtns = new HashSet(Arrays.asList(x.split(",")));
    }

    long fileSizeMax = 0;
    long fullSizeMax = 0;
    long i;
    i = conf.getProperty("fore.upload.file.size.max", 0);
    if ( i != 0 ) {
         fileSizeMax = i;
    }
    i = conf.getProperty("fore.upload.full.size.max", 0);
    if ( i != 0 ) {
         fullSizeMax = i;
    }

    Set<String> uploadKeys = new HashSet();
    setAttribute(Cnst.UPLOAD_ATTR, uploadKeys);

    //** 解析数据 **/

    try {
        ServletFileUpload sfu = new ServletFileUpload();
        if (fileSizeMax > 0) {
            sfu.setFileSizeMax(fullSizeMax);
        }
        if (fullSizeMax > 0) {
            sfu.setSizeMax/**/(fullSizeMax);
        }
        FileItemIterator fit = sfu.getItemIterator(request);

        while (fit.hasNext()) {
            FileItemStream fis = fit.next();
            String n  =  fis.getFieldName();
            String v;

            if (fis.isFormField()) {
                v = Streams.asString(fis.openStream());
            } else {
                String type = fis.getContentType();
                if (type != null) {
                    type  = type.split(";" , 2)[0];
                } else {
                    type  = "";
                }

                // 检查类型
                if (allowTypes != null && !allowTypes.contains(type)) {
                    continue;
                }
                if ( denyTypes != null &&   denyTypes.contains(type)) {
                    continue;
                }

                // 提取扩展
                String extn;
                String name = fis.getName().replaceAll("[\r\n]", "");
                int pos  = name.lastIndexOf('.');
                if (pos  > 1) {
                    extn = name.substring(1+pos);
                }
                else {
                    extn = "";
                }

                // 检查扩展
                if (allowExtns != null && !allowExtns.contains(extn)) {
                    continue;
                }
                if ( denyExtns != null &&   denyExtns.contains(extn)) {
                    continue;
                }

                v = Core.getUniqueId();
                String file = path + File.separator + v + ".tmp";
                String info = path + File.separator + v + ".tnp";

                // 存储到临时文件
                /**/FileOutputStream fos = new /**/FileOutputStream(new File( file ));
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                BufferedInputStream  bis = new BufferedInputStream (fis.openStream());
                long size = Streams.copy(bis, bos, true );

                // 如果没有则跳过
                if (size == 0) {
                    continue;
                }

                // 记录类型名称等
                try(FileWriter fw = new FileWriter(info)) {
                    fw.write(name+ "\r\n" + type + "\r\n" +size);
                }
            }

            Dict.setParam (rd, v, n);
            uploadKeys.add(/****/ n);
        }
    } catch (FileUploadException ex) {
        throw new HongsUnchecked(0x1110, ex);
    } catch (IOException ex) {
        throw new HongsUnchecked(0x1110, ex);
    }
  }

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
        throw new HongsError(0x32, "Can not get output stream.", ex);
      }
    } else
    {
        throw new HongsError(0x32, "Can not get output stream."/**/);
    }
  }

  /**
   * 获取响应输出
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
        throw new HongsError(0x32, "Can not get output writer.", ex);
      }
    } else
    {
        throw new HongsError(0x32, "Can not get output writer."/**/);
    }
  }

  /**
   * 获取请求参数
   * @param name
   * @return 当前请求参数
   */
  public String getParameter(String name)
  {
    Object o = Dict.getParam(getRequestData(), name);
    if (o == null)
    {
      return null;
    }
    if (o instanceof Map )
    {
      o = new ArrayList(((Map) o).values());
    }
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
   * 注意; 为防止歧义, 请不要在 name 中使用 "[","]"和"."
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
   * 注意; 为防止歧义, 请不要在 name 中使用 "[","]"和"."
   *       当 value 为 null 时 name 对应的会话属性将删除
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
    }
  }

  /**
   * 获取会话取值
   * 注意; 为防止歧义, 请不要在 name 中使用 "[","]"和"."
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
   * 注意; 为防止歧义, 请不要在 name 中使用 "[","]"和"."
   *       当 value 为 null 时 name 对应的会话属性将删除
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
    }
  }

  /**
   * 获取跟踪参数
   * @param name
   * @return
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
              throw  new  HongsUnchecked.Common ( e);
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
    if (this.response    != null) {
      if (value == null) {
        setCookibute(name, value, 0/*Remove*/, Core.BASE_HREF + "/", null, false, false );
      } else {
        setCookibute(name, value, Cnst.CL_DEF, Core.BASE_HREF + "/", null, false, false );
      }
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
      if (value != null) {
        try {
          value = URLEncoder.encode(value,"UTF-8");
        } catch ( UnsupportedEncodingException e ) {
          throw   new HongsError.Common(e);
        }
      }
      Cookie ce = new Cookie(name , value);
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
  }

  /**
   * ActionHelper 必须在 ActionDriver,CmdletRunner 初始化后才能用
   * @return
   * @deprecated
   */
  public static ActionHelper getInstance()
  {
    throw new HongsError.Common("Please use the ActionHelper in the coverage of the ActionDriver or CmdletRunner inside");
  }

  /**
   * 新建实例
   * 用于使用 ActionRunner 时快速构建请求对象,
   * 可用以上 setXxxxxData 在构建之后设置参数.
   * @return
   */
  public static ActionHelper newInstance() {
    Core   core = Core.getInstance();
    String inst = ActionHelper.class.getName();
    if (core.containsKey(inst)) {
        return ((ActionHelper) core.got(inst)).clone( );
    } else {
        return new ActionHelper(null, null, null, null);
    }
  }

  /**
   * 克隆方法
   * 用于使用 ActionRunner 时快速构建请求对象,
   * 可用以上 setXxxxxData 在克隆之后设置参数.
   * @return
   */
  @Override
  public ActionHelper clone() {
    ActionHelper helper;
    try {
      helper = (ActionHelper) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new HongsError.Common( /**/ ex);
    }
    helper.outputStream = this.getOutputStream();
    helper.outputWriter = this.getOutputWriter();
    helper.responseData = null;
    return helper;
  }

  //** 返回数据 **/

  /**
   * 返回响应数据
   * 针对 retrieve 等
   * @param map
   */
  public void reply(Map map)
  {
    if(!map.containsKey("ok" )) {
        map.put("ok", true);
    }
    if(!map.containsKey("ern")) {
        map.put("ern", "" );
    }
    if(!map.containsKey("err")) {
        map.put("err", "" );
    }
    if(!map.containsKey("msg")) {
        map.put("msg", "" );
    }
    this.responseData = map;
  }

  /**
   * 返回添加结果
   * 针对 create 等
   * @param msg
   * @param info
   */
  public void reply(String msg, Map info)
  {
    Map map = new HashMap();
    if (null !=  msg) {
        map.put("msg", msg);
    }
    map.put("info", info);
    reply(map);
  }

  /**
   * 返回操作行数
   * 针对 update,delete 等
   * @param msg
   * @param rows
   */
  public void reply(String msg, int rows)
  {
    Map map = new HashMap();
    if (null !=  msg) {
        map.put("msg", msg);
    }
    map.put("rows", rows);
    reply(map);
  }

  /**
   * 返回审核状态
   * 针对 unique,exists 等
   * @param msg
   * @param ok 对应 rows 的值 1,0
   */
  public void reply(String msg, boolean ok)
  {
    Map map = new HashMap();
    if (null !=  msg) {
        map.put("msg", msg);
    }
    map.put("rows", ok ? 1 : 0);
    reply(map);
  }

  /**
   * 返回操作提示
   * @param msg
   */
  public void reply(String msg)
  {
    Map map = new HashMap();
    if (null !=  msg) {
        map.put("msg", msg);
    }
    reply(map);
  }

  /**
   * 返回错误消息
   * @param msg
   */
  public void fault(String msg)
  {
    Map map = new HashMap();
    if (null !=  msg) {
        map.put("msg", msg);
    }
    map.put("ok", false);
    reply(map);
  }

  /**
   * 返回错误信息
   * @param msg
   * @param ern
   */
  public void fault(String msg, String ern)
  {
    Map map = new HashMap();
    if (null !=  msg) {
        map.put("msg", msg);
    }
    if (null !=  ern) {
        map.put("ern", ern);
    }
    map.put("ok", false);
    reply(map);
  }

  /**
   * 返回错误信息
   * @param msg
   * @param ern
   * @param err
   */
  public void fault(String msg, String ern, String err)
  {
    Map map = new HashMap();
    if (null !=  msg) {
        map.put("msg", msg);
    }
    if (null !=  ern) {
        map.put("ern", ern);
    }
    if (null !=  err) {
        map.put("err", err);
    }
    map.put("ok", false);
    reply(map);
  }

  //** 输出内容 **/

  /**
   * 输出内容
   * @param txt
   * @param ctt Content-Type 定义, 如 text/html
   * @param cst Content-Type 编码, 如 utf-8
   */
  public void print(String txt, String ctt, String cst)
  {
    if (this.response != null && !this.response.isCommitted()) {
      if (cst != null) {
        this.response.setCharacterEncoding(cst);
      }
      if (ctt != null) {
        this.response.setContentType(ctt);
      }
    }
    try {
      this.getOutputWriter(  ).write(txt);
    } catch (IOException e)  {
      throw new HongsError(0x32, "Can not send to client.", e);
    }
  }

  /**
   * 输出内容
   * @param txt
   * @param ctt
   */
  public void print(String txt, String ctt)
  {
    this.print(txt,ctt,"UTF-8");
  }

  /**
   * 输出内容
   * @param htm
   */
  public void print(String htm)
  {
    this.print(htm,"text/html");
  }

  /**
   * 输出数据
   *
   * @param dat
   */
  public void print(Object dat)
  {
    String  str = Data.toString( dat );
    this.print(str,"application/json");
  }

  //** 跳转及错误 **/

  /**
   * 返回数据
   * 将以 JSON/JSONP 格式输出, 编码 UTF-8
   */
  public void responed()
  {
    Writer pw = this.getOutputWriter(  );
    String pb = CoreConfig.getInstance().getProperty ("core.powered.by");
    String cb = ( String )  this.request.getAttribute( Cnst.BACK_ATTR  );

    if (this.response.isCommitted() != true) {
        this.response.setContentType(cb != null ? "text/javascript" : "application/json");
        this.response.setCharacterEncoding("UTF-8");
    }

    if (pb != null) {
        this.response.setHeader("X-Powered-By", pb);
    }

    try {
    if (cb != null) {
        pw.write("function " + cb + "() {return " );
        pw.write( Data.toString(this.responseData));
        pw.write(";}");
    } else {
        pw.write( Data.toString(this.responseData));
    }
    } catch (IOException ex ) {
        throw new HongsError(0x32, "Can not send to client.", ex);
    }

    this.responseData = null;
  }

  /**
   * 302重定向
   * @param url
   */
  public void redirect(String url)
  {
    this.response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    this.response.setHeader("Location", url);
    this.responseData = null;
  }

  /**
   * 400错误请求
   * @param msg
   */
  public void error400(String msg)
  {
    this.response.setStatus(HttpServletResponse.SC_BAD_REQUEST );
    this.responseData = null;
    this.print(msg);
  }

  /**
   * 401尚未登录
   * @param msg
   */
  public void error401(String msg)
  {
    this.response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    this.responseData = null;
    this.print(msg);
  }

  /**
   * 403禁止访问
   * @param msg
   */
  public void error403(String msg)
  {
    this.response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    this.responseData = null;
    this.print(msg);
  }

  /**
   * 404缺少页面
   * @param msg
   */
  public void error404(String msg)
  {
    this.response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    this.responseData = null;
    this.print(msg);
  }

  /**
   * 405方法错误
   * @param msg
   */
  public void error405(String msg)
  {
    this.response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    this.responseData = null;
    this.print(msg);
  }

  /**
   * 500内部错误
   * @param msg
   */
  public void error500(String msg)
  {
    this.response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    this.responseData = null;
    this.print(msg);
  }

  /**
   * 500 系统异常
   * @param ex
   */
  public void error500(Throwable ex)
  {
    this.error500(ex.getLocalizedMessage());
  }

  //** 工具方法 **/

  /**
   * 解析参数
   * 用于处理查询串
   * @param s
   * @return
   */
  public static Map parseQuery(String s) {
      HashMap<String, List<String>> a = new HashMap<>();
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
              throw new HongsError.Common(ex);
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
              throw new HongsError.Common(ex);
          }
          if (j < s.length() && s.charAt(j) == '&') {
              j++;
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
  public static Map parseParam(Map<String, String[]> params)
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
  public static Map parseQuest(Map<String, List<String>> params)
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
