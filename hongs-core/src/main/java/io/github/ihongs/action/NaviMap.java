package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CoreRoster;
import io.github.ihongs.CoreSerial;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.daemon.Gate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 导航配置.
 *
 * <p>
 * 该工具会将配置数据自动缓存, 会在构建对象时核对配置的修改时间;
 * 但无法确保其对象在反复使用中会自动重载,
 * 最好在修改配置后删除临时文件并重启应用.
 * </p>
 *
 * <h3>数据结构:</h3>
 * <pre>
    menus = {
      "href" : {
        hrel: 页面,
        icon: 图标,
        text: 名称,
        hint: 说明,
        menus : {
          子级菜单...
        },
        roles : [
          "role.name1",
          "role.name2",
          ...
        ]
      }
      ...
    }
    roles = {
      "name" : {
        text: 名称,
        hint: 说明,
        depends : [
          "fole.name1",
          "role.name2",
          ...
        ],
        actions : [
          "auth.name1",
          "auth.name2",
          ...
        ]
      }
      ...
    }
 * </pre>
 *
 * <h3>异常代码:</h3>
 * <pre>
 * 区间: 920~929
 * 920 配置文件不存在
 * 921 解析文件失败
 * 922 角色无法获取
 * </pre>
 *
 * <h3>特别注意:</h3>
 * <p>
 * 由于 rsname 拥有引用作用,
 * 配置不当会导致 getRoleSet 无限循环,
 * 规定 rsname 只可引用上级.
 * </p>
 * @author Hongs
 */
public class NaviMap
     extends CoreSerial
  implements CoreSerial.Mtimes
{

  protected transient String name;
  protected transient  long  time;

  /**
   * 菜单层级信息
   */
  public Map<String, Map> menus;

  /**
   * 菜单检索信息
   */
  public Map<String, Map> manus;

  /**
   * 全部分组信息
   */
  public Map<String, Map> roles;

  /**
   * 全部动作
   */
  public Set<String> actions = null;

  /**
   * 全部导入
   */
  public Set<String> imports = null;

  /**
   * 会话名称
   */
  public /**/String  session = null;

  public NaviMap(String name)
    throws HongsException
  {
    this.name = name ;
    this.init ( /**/);
  }

  public final void init()
    throws HongsException
  {
    File serFile = new File(Core.DATA_PATH
                 + File.separator + "serial"
                 + File.separator + name + Cnst.NAVI_EXT + ".ser");
    time = serFile.lastModified();

    //* 加锁读写 */

    Gate.Leader lock = Gate.getLeader(NaviMap.class.getName() + ":" + name);

    lock.lockr();
    try {
    if (! expired()) {
      load(serFile );
    if (! expires()) {
      return;
    }}
    } finally {
      lock.unlockr();
    }

    lock.lockw();
    try {
      imports ();
      save(serFile );
    } finally {
      lock.unlockw();
    }

    time = serFile.lastModified();

    CoreLogger.debug("Serialized navi conf {}", name);
  }

  @Override
  public long dataModified()
  {
    return time;
  }

  @Override
  public long fileModified()
  {
    File serFile = new File(Core.DATA_PATH
                 + File.separator + "serial"
                 + File.separator + name + Cnst.NAVI_EXT + ".ser");
    File xmlFile = new File(Core.CONF_PATH
                 + File.separator + name + Cnst.NAVI_EXT + ".xml");
    return Math.max(serFile.lastModified(),xmlFile.lastModified());
  }

  static protected boolean expired(String namz , long timz)
    throws HongsException
  {
    File serFile = new File(Core.DATA_PATH
                 + File.separator + "serial"
                 + File.separator + namz + Cnst.NAVI_EXT + ".ser");
    if ( serFile.exists() && serFile.lastModified() > timz)
    {
      return true;
    }

    File xmlFile = new File(Core.CONF_PATH
                 + File.separator + namz + Cnst.NAVI_EXT + ".xml");
    if ( xmlFile.exists() )
    {
      return timz < xmlFile.lastModified();
    }

    long resTime = CoreRoster.getResourceModified(
               namz.contains("/") ? namz + Cnst.NAVI_EXT + ".xml" :
               Cnst.CONF_PACK +"/"+ namz + Cnst.NAVI_EXT + ".xml");
    if ( resTime > 0 ) {
      return timz < resTime;
    }

    throw new HongsException(920, "Can not find the config file '" + namz + Cnst.NAVI_EXT + ".xml'");
  }

  public boolean expired()
    throws HongsException
  {
    return expired (name, time);
  }

  public boolean expires()
    throws HongsException
  {
    /**
     * 逐一检查导入的配置
     * 任一过期则重新导入
     */
    for (String namz : imports ) {
        if (expired(namz, time)) {
            return  true;
        }
    }

    return  false;
  }

  @Override
  protected void imports()
    throws HongsException
  {
    InputStream is;
    String      fn;

    try
    {
        fn = Core.CONF_PATH +"/"+ name + Cnst.NAVI_EXT + ".xml";
        is = new FileInputStream(fn);
    }
    catch (FileNotFoundException ex)
    {
        fn = name.contains(".")
          || name.contains("/") ? name + Cnst.NAVI_EXT + ".xml"
           : Cnst.CONF_PACK +"/"+ name + Cnst.NAVI_EXT + ".xml";
        is = CoreRoster.getResourceAsStream(fn);
        if (  is  ==  null )
        {
            throw new HongsException(920, "Can not find the config file '" + name + Cnst.NAVI_EXT + ".xml'");
        }
    }

    try
    {

    Element root;
    try
    {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder dbn = dbf.newDocumentBuilder();
      Document  doc = dbn.parse( is );
      root = doc.getDocumentElement();

      // 角色会话名称
      NodeList list = root.getElementsByTagName("rsname");
      if (list.getLength() != 0)
      {
          session = list.item(0).getTextContent();
      }
    }
    catch ( IOException ex)
    {
      throw new HongsException(ex, 921, "Read '" +name+Cnst.NAVI_EXT+".xml' error");
    }
    catch (SAXException ex)
    {
      throw new HongsException(ex, 921, "Parse '"+name+Cnst.NAVI_EXT+".xml' error");
    }
    catch (ParserConfigurationException ex)
    {
      throw new HongsException(ex, 921, "Parse '"+name+Cnst.NAVI_EXT+".xml' error");
    }

    this.menus = new LinkedHashMap();
    this.manus = new HashMap();
    this.roles = new HashMap();
    this.actions = new HashSet();
    this.imports = new HashSet();

    this.parse(root, this.menus, this.manus, this.roles, this.imports, this.actions, new HashSet());

    }
    finally
    {
      try {
        is.close();
      } catch (IOException ex) {
        throw new HongsException(ex);
      }
    }
  }

  private void parse(Element element, Map menus, Map manus, Map roles, Set imports, Set actions, Set depends)
    throws HongsException
  {
    if (!element.hasChildNodes())
    {
      return;
    }

    NodeList nodes = element.getChildNodes();

    for (int i = 0; i < nodes.getLength(); i ++)
    {
      Node node = nodes.item(i);
      if (node.getNodeType() != Node.ELEMENT_NODE)
      {
        continue;
      }

      Element element2 = (Element)node;
      String  tagName2 = element2.getTagName();

      if (imports == null
      && !"action".equals(tagName2)
      && !"depend".equals(tagName2)
      )
      {
        continue;
      }

      if ("menu".equals(tagName2))
      {
        Map menu2 = new HashMap();

        String href = element2.getAttribute("href");
        if (href == null) href = "" ;

        menus.put( href , menu2 );
        manus.put( href , menu2 );

        String hrel = element2.getAttribute("hrel");
        if (hrel != null) menu2.put( "hrel", hrel );

        String icon = element2.getAttribute("icon");
        if (icon != null) menu2.put( "icon", icon );

        String text = element2.getAttribute("text");
        if (text != null) menu2.put( "text", gotLanguage(text));

        String hint = element2.getAttribute("hint");
        if (hint != null) menu2.put( "hint", gotLanguage(hint));

        Map menus2 = new LinkedHashMap();
        Set roles2 = new LinkedHashSet();

        // 获取下级页面和权限
        this.parse(element2, menus2, manus, roles, imports, actions, roles2);

        if (!menus2.isEmpty())
        {
          menu2.put("menus", menus2);
        }
        if (!roles2.isEmpty())
        {
          menu2.put("roles", roles2);
        }
      }
      else
      if ("role".equals(tagName2))
      {
        String namz = element2.getAttribute("name");
        if (namz == null) namz = "" ;

        /**
         * 角色可以服用和补充
         * 角色标签文本为可选
         */
        Map role2 ;
        if (roles.containsKey(namz)) {
            role2 = (Map) roles.get(namz);
        } else {
            role2 =  new  HashMap( );
            role2.put("text",  ""  );
            role2.put("hint",  ""  );
            roles.put( namz , role2);
        }
        if (element2.hasAttribute("text")) {
            role2.put("text", element2.getAttribute("text"));
        }
        if (element2.hasAttribute("hint")) {
            role2.put("hint", element2.getAttribute("hint"));
        }

        Set actions2 = new HashSet();
        Set depends2 = new HashSet();

        // 获取下级动作和依赖
        this.parse(element2, null, null, null, null, actions2, depends2);

        if (!actions2.isEmpty())
        {
          role2.put("actions", actions2);
          actions.addAll(actions2);
        }
        if (!depends2.isEmpty())
        {
          role2.put("depends", depends2);
//        depends.addAll(depends2);
        }

        // 上级页面依赖的权限
        depends.add( namz );
      }
      else
      if ("action".equals(tagName2))
      {
        String action = element2.getTextContent();
        actions.add(action);
      }
      else
      if ("depend".equals(tagName2))
      {
        String depend = element2.getTextContent();
        depends.add(depend);
      }
      else
      if ("import".equals(tagName2))
      {
        String impart = element2.getTextContent().trim();
        try
        {
          NaviMap  conf = new NaviMap(impart);
          imports.add   (   impart   );
          imports.addAll(conf.imports);
          actions.addAll(conf.actions);
            menus.putAll(conf.menus  );
            manus.putAll(conf.manus  );

          /**
           * 深度合并
           * 角色同名视为追加权限设置
           * 故需要将下级权限合并过来
           * 不采用 Dict.putAll 为避免 name 被覆盖
           */
          for(Map.Entry<String, Map> et : conf.roles.entrySet()) {
              String  namz = et.getKey(  );
              Map     srol = et.getValue();
              Map     crol = (Map) roles.get(namz);

              if (crol != null) {
                  Set sact, cact;

                  sact = (Set) srol.get("actions");
                  cact = (Set) crol.get("actions");
                  if (sact != null) {
                  if (cact != null) {
                      cact.addAll(sact);
                  } else {
                      crol.put("actions", sact);
                  }}

                  sact = (Set) srol.get("depends");
                  cact = (Set) crol.get("depends");
                  if (sact != null) {
                  if (cact != null) {
                      cact.addAll(sact);
                  } else {
                      crol.put("depends", sact);
                  }}
              } else {
                  roles.put(namz, srol);
              }
          }
        }
        catch (HongsException ex )
        {
          // 找不到文件仅将错误写到日志
          if (920 == ex.getErrno())
          {
            CoreLogger.error( ex );
          }
          else
          {
            throw ex;
          }
        }
      }
    }
  }

  public String getName() {
      return  this.name;
  }

  /**
   * 获取单元信息
   * @param name
   * @return 找不到则返回null
   */
  public Map getRole(String name)
  {
    return this.roles.get ( name);
  }

  /**
   * 获取页面信息
   * @param name
   * @return 找不到则返回null
   */
  public Map getMenu(String name)
  {
    return this.manus.get ( name);
  }

  /**
   * 获取更多单元
   * @param names
   * @return 全部角色名
   */
  public Set<String> getMoreRoles(Collection<String> names)
  {
    Set <String> rolez = new HashSet();
    Set <String> authz = new HashSet();
    this.getRoleAuths(names, rolez, authz);
    return  rolez;
  }
  public Set<String> getMoreRoles(String... names)
  {
    return  this.getMoreRoles(Arrays.asList(names));
  }

  /**
   * 获取单元动作
   * @param names
   * @return 全部动作名
   */
  public Set<String> getRoleAuths(Collection<String> names)
  {
    Set <String> rolez = new HashSet();
    Set <String> authz = new HashSet();
    this.getRoleAuths(names, rolez, authz);
    return  authz;
  }
  public Set<String> getRoleAuths(String... names)
  {
    return  this.getRoleAuths(Arrays.asList(names));
  }

  protected void getRoleAuths(Collection<String> names, Set roles, Set auths)
  {
    for(String n : names)
    {
      // 规避循环依赖
      if (roles.contains(n))
      {
        continue;
      }

      Map role = getRole(n);
      if (role == null)
      {
        continue;
      }

      roles.add(n);

      if (role.containsKey("actions"))
      {
        Set <String> actionsSet = (Set<String>) role.get("actions");
        auths.addAll(actionsSet);
      }

      if (role.containsKey("depends"))
      {
        Set <String> dependsSet = (Set<String>) role.get("depends");
        getRoleAuths(dependsSet , roles, auths);
      }
    }
  }

  //** 用户权限 **/

  /**
   * 获取角色集合(与当前请求相关)
   * 注意: 并不包含其依赖的角色
   * @return session 为空则返回 null
   * @throws io.github.ihongs.HongsException
   */
  public Set<String> getRoleSet() throws HongsException {
      if (session == null || session.length() == 0) {
          CoreLogger.warn("Can not get roles for menu " + name);
          return null;
      }
      if (session.startsWith("#")) {
          return getInstance(session.substring(1)).getRoleSet();
      } else
      if (session.startsWith("@")) {
          return ( Set ) Core.getInstance(session.substring(1));
      } else
      {
          return ( Set ) Core.getInstance( ActionHelper.class ).getSessibute(session);
      }
  }

  /**
   * 获取权限集合(与当前请求相关)
   * 注意: 包含依赖的角色的权限
   * @return session 为空则返回 null
   * @throws io.github.ihongs.HongsException
   */
  public Set<String> getAuthSet() throws HongsException {
      Set<String> roleset = getRoleSet();
      if (null == roleset)  return null ;
      return getRoleAuths(roleset);
  }

  /**
   * 检查角色权限(与当前请求相关)
   * 注意: 并不包含其依赖的角色, 配置里没登记角色也返 true
   * @param role
   * @return 可访问则为 true
   * @throws io.github.ihongs.HongsException
   */
  public boolean chkRole(String role) throws HongsException {
      Set<String> roleset = getRoleSet( );
      if (null == roleset) {
          return !roles.containsKey(role);
      }
      return roleset.contains(role) || !roles.containsKey(role);
  }

  /**
   * 检查动作权限(与当前请求相关)
   * 注意: 包含依赖的角色的权限, 配置里没登记动作也返 true
   * @param auth
   * @return 可访问则为 true
   * @throws io.github.ihongs.HongsException
   */
  public boolean chkAuth(String auth) throws HongsException {
      Set<String> authset = getAuthSet( );
      if (null == authset) {
          return !actions.contains (auth);
      }
      return authset.contains(auth) || !actions.contains (auth);
  }

  /**
   * 检查页面权限(与当前请求相关)
   * @param name
   * @return 有一个配置角色即为 true
   * @throws io.github.ihongs.HongsException
   */
  public boolean chkMenu(String name) throws HongsException {
      Map menu = getMenu(name);
      if (menu == null) {
          return false;
      }

      return chkMenu(menu,getRoleSet());
  }
  private boolean chkMenu(Map menu, Set rolez) {
      Set r  = (Set) menu.get("roles");
      Map m  = (Map) menu.get("menus");

      boolean h = true;

      if (r != null && ! r.isEmpty( )) {
          for(Object x : r ) {
              if (rolez.contains((String) x)) {
                  return true;
              }
          }
          h  = false;
      }

      if (m != null && ! m.isEmpty( )) {
          for(Object o : m.entrySet()) {
              Map.Entry e = (Map.Entry) o;
              Map n = (Map) e.getValue( );
              if (chkMenu(n, rolez)) {
                  return true;
              }
          }
          h  = false;
      }

      return h;
  }

  //** 导航菜单及权限表单 **/

  /**
   * 获取当前语言类
   * @deprecated 输出时会翻译, 不必预先翻译
   * @return
   */
  public CoreLocale getCurrTranslator() {
    CoreLocale lang;
    try {
      lang = CoreLocale.getInstance(name).clone();
    }
    catch (HongsExemption e) {
      if  (e.getErrno( ) != 826 ) {
        throw e;
      }
      lang = new CoreLocale(null);
    }
    // 需加载所有下级菜单语言资源
    for ( String namz : imports ) {
      lang.fill (namz);
    }
    return lang;
  }

  /**
   * 获取全部菜单
   * @return
   */
  public List<Map> getMenuTranslates() {
      return getMenuTranslated(1, null );
  }

  public List<Map> getMenuTranslates(int d) {
      return getMenuTranslated(d, null );
  }

  public List<Map> getMenuTranslates(String name) {
      return getMenuTranslated(name, 1, null);
  }

  public List<Map> getMenuTranslates(String name, int d) {
      return getMenuTranslated(name, d, null);
  }

  /**
   * 获取当前用户有权的菜单
   * @return
   * @throws io.github.ihongs.HongsException
   */
  public List<Map> getMenuTranslated()
  throws HongsException {
      Set rolez =   getRoleSet();
      if (rolez == null) {
          rolez =  new HashSet();
      }
      return getMenuTranslated(1, rolez);
  }

  public List<Map> getMenuTranslated(int d)
  throws HongsException {
      Set rolez =   getRoleSet();
      if (rolez == null) {
          rolez =  new HashSet();
      }
      return getMenuTranslated(d, rolez);
  }

  public List<Map> getMenuTranslated(int d, Set<String> rolez) {
      return getMenuTranslated(menus, rolez, d, 0);
  }

  public List<Map> getMenuTranslated(String name)
  throws HongsException {
      Set rolez =   getRoleSet();
      if (rolez == null) {
          rolez =  new HashSet();
      }
      return getMenuTranslated(name, 1, rolez);
  }

  public List<Map> getMenuTranslated(String name, int d)
  throws HongsException {
      Set rolez =   getRoleSet();
      if (rolez == null) {
          rolez =  new HashSet();
      }
      return getMenuTranslated(name, d, rolez);
  }

  public List<Map> getMenuTranslated(String name, int d, Set<String> rolez) {
      Map menu = getMenu(name);
      if (menu == null) {
          throw new NullPointerException("Menu for href '"+name+"' is not in "+this.name);
      }
      return getMenuTranslated((Map) menu.get("menus"), rolez, d, 0);
  }

  protected List<Map> getMenuTranslated(Map<String, Map> menus, Set<String> rolez, int j, int i) {
      List<Map> list = new LinkedList();

      if (null == menus||(j != 0 && j <= i)) {
          return  list;
      }

      for(Map.Entry item : menus.entrySet()) {
          String h = (String) item.getKey();
          Map    v = (Map) item.getValue( );
          Map    m = (Map) v.get( "menus" );
          Set    r = (Set) v.get( "roles" );

          List<Map> subz = getMenuTranslated(m, rolez, j, i + 1);

          /**
           * 当前菜单没有权限则跳过
           * 下级菜单均无权限则跳过
           */
          if (null != rolez) {
              if (null != r && !r.isEmpty()) {
                  boolean y = true ;
                  for (Object x : r) {
                      if (rolez.contains((String) x)) {
                          y = false;
                              break;
                      }
                  }
                  if (y) {
                      continue;
                  }
              } else
              if (null != m && !m.isEmpty()) {
                  if (subz.isEmpty()) {
                      continue;
                  }
              }
          }

          String p = (String) v.get("hrel");
          String d = (String) v.get("icon");
          String s = Synt.declare(v.get("text"), "");
          String z = Synt.declare(v.get("hint"), "");

          Map menu = new HashMap();
          menu.put("href", h);
          menu.put("hrel", p);
          menu.put("icon", d);
          menu.put("text", s);
          menu.put("hint", z);
          menu.put("menus", subz );
          list.add( menu);
      }

      return list;
  }

  /**
   * 获取全部角色
   * @return
   */
  public List<Map> getRoleTranslates() {
      return getRoleTranslated(0, null);
  }

  public List<Map> getRoleTranslates(int d) {
      return getRoleTranslated(d, null);
  }

  public List<Map> getRoleTranslates(String name) {
      return getRoleTranslated(name, 0, null);
  }

  public List<Map> getRoleTranslates(String name, int d) {
      return getRoleTranslated(name, d, null);
  }

  /**
   * 获取当前用户有权的角色
   * @return
   * @throws io.github.ihongs.HongsException
   */
  public List<Map> getRoleTranslated()
  throws HongsException {
      Set rolez =   getRoleSet();
      if (rolez == null) {
          rolez =  new HashSet();
      }
      return getRoleTranslated(0, rolez);
  }

  public List<Map> getRoleTranslated(int d)
  throws HongsException {
      Set rolez =   getRoleSet();
      if (rolez == null) {
          rolez =  new HashSet();
      }
      return getRoleTranslated(d, rolez);
  }

  public List<Map> getRoleTranslated(int d, Set<String> rolez) {
      return getRoleTranslated(menus, rolez, d, 0);
  }

  public List<Map> getRoleTranslated(String name)
  throws HongsException {
      Set rolez =   getRoleSet();
      if (rolez == null) {
          rolez =  new HashSet();
      }
      return getRoleTranslated(name, 0, rolez);
  }

  public List<Map> getRoleTranslated(String name, int d)
  throws HongsException {
      Set rolez =   getRoleSet();
      if (rolez == null) {
          rolez =  new HashSet();
      }
      return getRoleTranslated(name, d, rolez);
  }

  public List<Map> getRoleTranslated(String name, int d, Set<String> rolez) {
      Map menu = getMenu(name);
      if (menu == null) {
          throw new NullPointerException("Menu for href '"+name+"' is not in "+this.name);
      }
      return getRoleTranslated((Map) menu.get("menus"), rolez, d, 0);
  }

  protected List<Map> getRoleTranslated(Map<String, Map> menus, Set<String> rolez, int j, int i) {
      return getRoleTranslated(menus, rolez, j, i, new HashSet(), new ArrayList(0));
  }
  protected List<Map> getRoleTranslated(Map<String, Map> menus, Set<String> rolez, int j, int i, Set q, List p) {
      List<Map> list = new LinkedList();

      if (null == menus||(j != 0 && j <= i)) {
          return  list;
      }

      for(Map.Entry item : menus.entrySet()) {
          Map v = (Map) item.getValue();
          Map m = (Map) v.get ("menus");
          Set r = (Set) v.get ("roles");

          String t = Synt.declare(v.get("text"), "");
          String d = Synt.declare(v.get("hint"), "");

          if (r != null) {
          List<Map> rolz = new LinkedList();
          for(String n : ( Set<String> ) r) {
              if (rolez != null
              && !rolez.contains(n)) {
                  continue; // 无权
              }
              if (/**/q.contains(n)) {
                  continue; // 重复
              }

              Map o = getRole(n);
              Set x = (Set) o.get ("depends");

              String l = Synt.declare(o.get("text"), "");
              String b = Synt.declare(o.get("hint"), "");

              if (l == null
              ||  l.length( ) == 0 ) {
                  continue; // 无名
              }

              Map role = new HashMap();
              role.put("name", n);
              role.put("text", l);
              role.put("hint", b);
              role.put("rels", x);
              rolz.add(role);

              q.add(n);
          }

          if (! rolz.isEmpty()) {
              String h = (String) item.getKey();
              String l = (String) v.get("hrel");
              String b = (String) v.get("icon");

              Map menu = new HashMap();
              menu.put("href", h);
              menu.put("hrel", l);
              menu.put("icon", b);
              menu.put("text", t);
              menu.put("hint", d);
              menu.put("tabs", p);
              menu.put("roles", rolz );
              list.add(menu);
          }
          }

          // 拉平下级
          List<String> g = new ArrayList(1 + p.size());
                       g.addAll(p);
                       g.add   (t);
          List<Map> subz = getRoleTranslated(m, rolez, j, i + 1, q, g);
          if (! subz.isEmpty()) {
              list.addAll(subz);
          }
      }

      return list;
  }

  private Object getLanguage(String text) {
      if (text == null || text.isEmpty())
          return  text;

      int p;
      String   conf;
      String   repo;
      String[] reps;

          p  = text. indexOf (';');
      if (p >= 0) {
        repo = text.substring(1+p);
        text = text.substring(0,p);
        reps = ((List<String>)Synt.toList(repo)).toArray(new String[0]);
      } else {
        reps = null;
      }

          p  = text. indexOf (':');
      if (p >= 0) {
        conf = text.substring(0,p);
        text = text.substring(1+p);
      } else {
        conf = name;
      }

      return new CoreLocale.Property(conf, text, reps);
  }

  private Object gotLanguage(String text) {
      if (text == null || text.isEmpty())
          return  text;

      if (text.length() > 0 && text.charAt(0) == '@') {
          text = text.substring(1);
      if (text.length() > 0 && text.charAt(0) != '@') {
          return getLanguage(text);
      }}

      return text;
  }

  //** 工厂方法 **/

  public static boolean hasConfFile(String name) {
    String fn = "/serial/";

    fn = Core.DATA_PATH +fn + name + Cnst.NAVI_EXT + ".ser";
    if (new File(fn).exists()) {
        return true;
    }

    fn = Core.CONF_PATH +"/"+ name + Cnst.NAVI_EXT + ".xml";
    if (new File(fn).exists()) {
        return true;
    }

    fn = name.contains(".")
      || name.contains("/") ? name + Cnst.NAVI_EXT + ".xml"
       : Cnst.CONF_PACK +"/"+ name + Cnst.NAVI_EXT + ".xml";
    return CoreRoster.getResourceModified(fn) > 0;
  }

  public static NaviMap getInstance(String name) throws HongsException {
      Core    core =  Core.getInstance ();
      String  code =  NaviMap.class.getName() + ":" + name;
      NaviMap inst = (NaviMap) core.get(code);
      if (inst == null) {
          inst  = new NaviMap( name );
          core.set(code, inst);
      }
      return inst;
  }

  public static NaviMap getInstance() throws HongsException {
      return getInstance("default");
  }

}
