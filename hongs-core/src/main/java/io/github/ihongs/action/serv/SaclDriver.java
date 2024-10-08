package io.github.ihongs.action.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CoreSerial;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.NaviMap;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;

/**
 * 综合配置接口
 * 将 AuthAction,ConfAction,LangAction 集合到一起
 * @author Hongs
 */
public class SaclDriver
  extends  ActionDriver
{

  @Override
  public void service(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    String name = req.getServletPath();
           name = name.substring(name.lastIndexOf("/"));
    if (null != name)
        switch (name) {
    case "/auth":
        authService(req, rsp);
        break;
    case "/conf":
        confService(req, rsp);
        break;
    case "/lang":
        langService(req, rsp);
        break;
    default:
        rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        rsp.getWriter().print("Unsupported name "+name);
        break;
    } else {
        rsp.setStatus(HttpServletResponse.SC_NOT_FOUND);
        rsp.getWriter().print("Unsupported name "+name);
    }
  }

  public void authService(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    Core core = ActionDriver.getActualCore(req);
    ActionHelper helper = core.got(ActionHelper.class);

    String name = req.getPathInfo();
    if (name == null || name.length() == 0) {
      helper.error(400, "Path info required");
      return;
    }
    int p = name.lastIndexOf( '.' );
    if (p < 0) {
      helper.error(400, "File type required");
      return;
    }
    String type = name.substring(1 + p);
           name = name.substring(1 , p);
    if (!"js".equals(type) && !"json".equals(type)) {
      helper.error(400, "Wrong file type: "+ type);
      return;
    }

    String s;
    try {
      NaviMap  sitemap = NaviMap.getInstance(name);
      Set<String> roleset = sitemap.getUserRoles();
      Set<String> authset ;

      // 没有设置 rsname 的不公开
      if (null == sitemap.session) {
        helper.error(403, "Auth data for '"+name+"' is not open to the public");
        return;
      }

      // HTTP 304 缓存策略
      if (roleset instanceof CoreSerial.Mtimes) {
        CoreSerial.Mtimes rolemod = (CoreSerial.Mtimes) roleset;
        long m = Math.max(sitemap.dataModified(), rolemod.dataModified());
        if ( m > 0 ) {
          String u = Synt.declare(helper.getSessibute(Cnst.UID_SES), "" ); // 用户ID
          String t = etag(name +":"+ u +":"+ m );
          String f = helper.getRequest().getHeader("If-None-Match");
          if (t.equals(f)) {
            helper.getResponse().setStatus(SC_NOT_MODIFIED);
            return;
          } else {
            helper.getResponse().setHeader("ETag", t /**/ );
          }
        } else {
            helper.getResponse().setHeader("Cache-Control" , "no-cache" );
        }
      } else {
            helper.getResponse().setHeader("Cache-Control" , "no-cache" );
      }

      Map<String, Boolean> datamap = new HashMap();
      if (null == roleset) authset = new HashSet();
      else authset = sitemap.getRoleAuths(roleset);
      for(  String  act: sitemap.actions ) {
        datamap.put(act, authset.contains( act ) );
      }

      s = Dist.toString(datamap);
    }
    catch (CruxException|CruxExemption ex) {
      helper.error(404, ex.getMessage());
      return;
    }
    catch (IllegalArgumentException ex) {
      helper.error(500, ex.getMessage());
      return;
    }

    // 输出权限信息
    if ("json".equals(type)) {
      helper.write("application/json", s);
    } else {
      String c = req.getParameter("callback");
      if (c != null && !c.isEmpty()) {
        if (!c.matches("^[a-zA-Z_\\$][a-zA-Z0-9_]*$")) {
          helper.error(400, "Illegal callback function name!");
          return;
        }
        helper.write("text/javascript", c +"("+ s +");");
      } else {
        c = "self.HsAUTH=Object.assign(self.HsAUTH||{}" ;
        helper.write("text/javascript", c +","+ s +");");
      }
    }
  }

  public void confService(HttpServletRequest req, HttpServletResponse rsp)
    throws ServletException, IOException
  {
    Core core = ActionDriver.getActualCore(req);
    ActionHelper helper = core.got(ActionHelper.class);

    String name = req.getPathInfo();
    if (name == null || name.length() == 0) {
      helper.error(400, "Path info required");
      return;
    }
    int p = name.lastIndexOf( '.' );
    if (p < 0) {
      helper.error(400, "File type required");
      return;
    }
    String type = name.substring(1 + p);
           name = name.substring(1 , p);
    if (!"js".equals(type) && !"json".equals(type)) {
      helper.error(400, "Wrong file type: "+ type);
      return;
    }

    Conf mk;
    try {
      mk = new Conf( name );
    }
    catch (CruxExemption e) {
      helper.error(404, e.getMessage());
      return;
    }

    long m = mk.modified( );
    if ( m < 1 ) {
      m  = Core.STARTS_TIME;
    }

    /**
     * 如果指定配置的数据并没有改变
     * 则直接返回 304 Not modified
     */
    String t = etag(name +":"+ m );
    String f = helper.getRequest().getHeader("If-None-Match");
    if (t.equals(f)) {
      helper.getResponse().setStatus(SC_NOT_MODIFIED);
      return;
    } else {
      helper.getResponse().setHeader("ETag", t /**/ );
    }

    // 输出配置信息
    String s = mk.toString();
    if ("json".equals(type))
    {
      helper.write("application/json", s);
    }
    else
    {
      String c = req.getParameter("callback");
      if (c != null && !c.isEmpty())
      {
        if (!c.matches("^[a-zA-Z_\\$][a-zA-Z0-9_]*$"))
        {
          helper.error(400, "Illegal callback function name!");
          return;
        }
        helper.write("text/javascript", c +"("+ s +");");
      }
      else
      {
        c = "self.HsCONF=Object.assign(self.HsCONF||{}" ;
        helper.write("text/javascript", c +","+ s +");");
      }
    }
  }

  public void langService(HttpServletRequest req, HttpServletResponse rsp)
    throws IOException, ServletException
  {
    Core core = ActionDriver.getActualCore(req);
    ActionHelper helper = core.got(ActionHelper.class);

    String name = req.getPathInfo();
    if (name == null || name.length() == 0) {
      helper.error(400, "Path info required");
      return;
    }
    int p = name.lastIndexOf( '.' );
    if (p < 0) {
      helper.error(400, "File type required");
      return;
    }
    String type = name.substring(1 + p);
           name = name.substring(1 , p);
    if (!"js".equals(type) && !"json".equals(type)) {
      helper.error(400, "Wrong file type: "+ type);
      return;
    }

    Lang mk;
    try {
      mk = new Lang( name );
    }
    catch (CruxExemption e) {
      helper.error(404 , e.getMessage());
      return;
    }

    long m = mk.modified( );
    if ( m < 1 ) {
      m  = Core.STARTS_TIME;
    }

    /**
     * 如果指定语言的数据并没有改变
     * 则直接返回 304 Not modified
     */
    String l = Core.ACTION_LANG.get();
    String t = etag(name+":"+l+":"+m);
    String f = helper.getRequest().getHeader("If-None-Match");
    if (t.equals(f)) {
      helper.getResponse().setStatus(SC_NOT_MODIFIED);
      return;
    } else {
      helper.getResponse().setHeader("ETag", t /**/ );
    }

    // 输出语言信息
    String s = mk.toString();
    if ("json".equals(type))
    {
      helper.write("application/json", s);
    }
    else
    {
      String c = req.getParameter("callback");
      if (c != null && !c.isEmpty())
      {
        if (!c.matches("^[a-zA-Z_\\$][a-zA-Z0-9_]*$"))
        {
          helper.error(400, "Illegal callback function name!");
          return;
        }
        helper.write("text/javascript", c +"("+ s +");");
      }
      else
      {
        c = "self.HsLANG=Object.assign(self.HsLANG||{}" ;
        helper.write("text/javascript", c +","+ s +");");
      }
    }
  }

  private String etag(String n) {
    try {
      MessageDigest m = MessageDigest.getInstance("MD5");
      byte[] a;
      a = n.getBytes( );
      a = m.digest( a );
      return new BigInteger(1, a).toString(16);
    } catch (NoSuchAlgorithmException ex) {
      throw new CruxExemption(ex);
    }
  }

  //** 辅助工具类 **/

  /**
   * 配置信息辅助构造类
   */
  private static class Conf
  {
    private final     String name;
    private final CoreConfig conf;

    public Conf(String name)
    {
      this.name = name;
      this.conf = CoreConfig.getInstance(name);

      // 未设置 core.fore.keys 不公开
      if ( this.conf.getProperty("core.fore.keys", null) == null )
      {
        throw new CruxExemption (404, "Conf for "+name+" is non-public");
      }
    }

    public long modified()
    {
      return CoreConfig.getInstance().getProperty("core.load.config.once", false)
           ? conf.dataModified()
           : conf.fileModified();
    }

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder();

      /** 配置代码 **/

      sb.append("{\r\n");

      // 公共配置
      if ("default".equals(name))
      {
        sb.append("\t\"DEBUG\":")
          .append(String.valueOf(Core.DEBUG))
          .append(",\r\n")
          .append("\t\"SERVER_ID\":\"")
          .append(Core.SERVER_ID)
          .append("\",\r\n")
          .append("\t\"BASE_HREF\":\"")
          .append(Core.SERV_PATH)
          .append("\",\r\n");
      }

      // 查找共享配置信息
      boolean m = false;
      String  x = conf.getProperty("core.fore.keys");
      if (null !=  x ) for (String k : x.split(";")) {
          k = k.trim();
          if (k.length()==0) {
              continue;
          }
          if (k.equals("+")) {
              m = true;
              continue;
          }
          String[] a = k.split("=", 2);
          String   n ;
          if (1  < a.length) {
              n  = a[0];
              k  = a[1];
          } else {
              n  = a[0];
              k  = a[0];
          }
          sb.append ( make(n , k) );
      }

      // 查找扩展配置信息
      if (m)
      {
        for (String nk : conf.stringPropertyNames())
        {
          if (nk.startsWith("fore."))
          {
              sb.append( make(nk.substring(5), nk) );
          }
        }
      }

      sb.append("\t\"\":\"\"\r\n}");
      return sb.toString();
    }

    public String make(String nam, String key)
    {
      String  fmt;
      int p = nam.lastIndexOf (".");
      if (p > 0) {
          fmt = nam.substring (1+p);
      } else {
          fmt = "" ;
      }
      switch (fmt) {
        case "json":
        case "bool":
        case "num" :
          nam = nam.substring (0,p);
          return this.makeCode(nam, key);
        case "str" :
          nam = nam.substring (0,p);
          return this.makeConf(nam, key);
        default:
          return this.makeConf(nam, key);
      }
    }

    private String makeCode(String nam, String key)
    {
      String val = this.getValue(key, "");
      if ( val.isEmpty( ) ) val = "null" ;
      return "\t\""+ nam +"\":"  + val +  ",\r\n";
    }

    private String makeConf(String nam, String key)
    {
      String val = this.getValue(key, "");
             val = Dist.doEscape(  val  );
      return "\t\""+ nam +"\":\""+ val +"\",\r\n";
    }

    private String getValue(String key, String def)
    {
      int pos = key.indexOf(":");
      if (pos == -1) {
          return this.conf.getProperty (key, def);
      } else {
          return CoreConfig
                .getInstance(key.substring(0,pos)/**/)
                .getProperty(key.substring(1+pos),def);
      }
    }
  }

  /**
   * 语言信息辅助构造类
   */
  private static class Lang
  {
    private final     String name;
    private final CoreLocale lang;

    public Lang(String name)
    {
      this.name = name;
      this.lang = CoreLocale.getInstance(name);

      // 未设置 core.fore.keys 不公开
      if ( this.lang.getProperty("core.fore.keys", null) == null )
      {
        throw new CruxExemption (404, "Lang for "+name+" is non-public");
      }
    }

    public long modified()
    {
      return CoreConfig.getInstance().getProperty("core.load.locale.once", false)
           ? lang.dataModified()
           : lang.fileModified();
    }

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder();

      /** 配置代码 **/

      sb.append("{\r\n");

      // 公共语言
      if ("default".equals(name))
      {
        sb.append("\t\"lang\":\"")
          .append(Core.ACTION_LANG.get())
          .append("\",\r\n")
          .append("\t\"zone\":\"")
          .append(Core.ACTION_ZONE.get())
          .append("\",\r\n");
      }

      // 查找共享语言信息
      boolean m = false;
      String  x = lang.getProperty("core.fore.keys");
      if (null !=  x ) for (String k : x.split(";")) {
          k = k.trim();
          if (k.length()==0) {
              continue;
          }
          if (k.equals("+")) {
              m = true;
              continue;
          }
          String[] a = k.split("=", 2);
          String   n ;
          if (1  < a.length) {
              n  = a[0];
              k  = a[1];
          } else {
              n  = a[0];
              k  = a[0];
          }
          sb.append ( make(n , k) );
      }

      // 查找扩展语言信息
      if (m)
      {
        for (String nk : lang.stringPropertyNames())
        {
          if (nk.startsWith("fore."))
          {
              sb.append( make(nk.substring(5), nk) );
          }
        }
      }

      sb.append("\t\"\":\"\"\r\n}");
      return sb.toString();
    }

    public String make(String nam, String key)
    {
      String  fmt;
      int p = nam.lastIndexOf (".");
      if (p > 0) {
          fmt = nam.substring (1+p);
      } else {
          fmt = "" ;
      }
      switch (fmt) {
        case "json":
        case "bool":
        case "num" :
          nam = nam.substring (0,p);
          return this.makeCode(nam, key);
        case "str" :
          nam = nam.substring (0,p);
          return this.makeConf(nam, key);
        default:
          return this.makeConf(nam, key);
      }
    }

    private String makeCode(String nam, String key)
    {
      String val = this.getValue(key, "");
      if ( val.isEmpty( ) ) val = "null" ;
      return "\t\""+ nam +"\":"  + val +  ",\r\n";
    }

    private String makeConf(String nam, String key)
    {
      String val = this.getValue(key, "");
             val = Dist.doEscape(  val  );
      return "\t\""+ nam +"\":\""+ val +"\",\r\n";
    }

    private String getValue(String key, String def)
    {
      int pos = key.indexOf(":");
      if (pos == -1) {
          return this.lang.getProperty (key, def);
      } else {
          return CoreLocale
                .getInstance(key.substring(0,pos)/**/)
                .getProperty(key.substring(1+pos),def);
      }
    }
  }

}
