package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CoreSerial;
import io.github.ihongs.HongsError;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.thread.Block;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
 * 区间: 0x10e0~0x10e7
 * 0x10e0 配置文件不存在
 * 0x10e1 解析文件失败
 * 0x10e2 角色无法获取
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
{

  private final String name;

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
    this. name = name ;
    this.initial(name);
  }

  private void initial(String name)
    throws HongsException
  {
    File serFile = new File(Core.DATA_PATH
                 + File.separator + "serial"
                 + File.separator + name + Cnst.NAVI_EXT + ".ser");

    //* 加锁读写 */

    Block.Larder lock = Block.getLarder(CoreSerial.class.getName() + ":" + name + Cnst.NAVI_EXT);

    lock.lockr();
    try {
      if (serFile.exists()
      && !expired( name )) {
          load( serFile );
          // 子菜单有过期则重新导入全部数据
          R : {
              for( String namz : imports ) {
              if (expired(namz)) {
                  break R;
              }}
              return;
          }
      }
    } finally {
      lock.unlockr();
    }

    lock.lockw();
    try {
      imports( );
      save(serFile );
    } finally {
      lock.unlockw();
    }
  }

  protected boolean expired(String namz)
  {
    File serFile = new File(Core.DATA_PATH
                 + File.separator + "serial"
                 + File.separator + name + Cnst.NAVI_EXT + ".ser");
    File xmlFile = new File(Core.CONF_PATH
                 + File.separator + namz + Cnst.NAVI_EXT + ".xml");
    if ( xmlFile.exists() )
    {
      return xmlFile.lastModified() > serFile.lastModified();
    }

    // 为减少判断逻辑对 jar 文件不做变更对比, 只要资源存在即可
    return null == getClass().getClassLoader().getResource(
             namz.contains(".")
          || namz.contains("/") ? namz + Cnst.NAVI_EXT + ".xml"
           : Cnst.CONF_PACK +"/"+ namz + Cnst.NAVI_EXT + ".xml"
    );
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
        is = this.getClass().getClassLoader().getResourceAsStream(fn);
        if (  is  ==  null )
        {
            throw new HongsException(0x10e0,
                "Can not find the config file '" + name + Cnst.NAVI_EXT + ".xml'.");
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
      throw new HongsException(0x10e1, "Read '" +name+Cnst.NAVI_EXT+".xml error'", ex);
    }
    catch (SAXException ex)
    {
      throw new HongsException(0x10e1, "Parse '"+name+Cnst.NAVI_EXT+".xml error'", ex);
    }
    catch (ParserConfigurationException ex)
    {
      throw new HongsException(0x10e1, "Parse '"+name+Cnst.NAVI_EXT+".xml error'", ex);
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
        throw new HongsException.Common(ex);
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
        if (text != null) menu2.put( "text", text );

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
            roles.put( namz , role2);
        }
        if (element2.hasAttribute("text")) {
            role2.put("text", element2.getAttribute("text"));
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
          if (0x10e0 == ex.getErrno( ))
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
   * 获取页面角色
   * @param names
   * @return 角色字典
   */
  public Map<String, Map> getMenuRoles(String... names)
  {
    Map<String, Map> rolez = new HashMap();

    for (String namz : names) {
        Map menu = getMenu(namz);
        if (menu == null) {
            throw new NullPointerException("Menu for href '"+name+"' is not in "+this.name);
        }

        // 此方法主要用在判断菜单能否可以进入
        // 通常的是判断其拥有任一个角色即有效
        // 如果取了依赖的角色
        // 会导致因为依赖通用角色总是误认有效
        // 例如所有管理后台角色均依赖基础管理
        Set set = (Set) menu.get("roles");
        if (set != null && !set.isEmpty()) {
            for (Object name : set) {
                rolez.put( (String) name, roles.get(name));
            }
//          rolez.putAll(getMoreRoles((String[]) set.toArray(new String[0])));
        }

        Map map = (Map) menu.get("menus");
        if (map != null && !map.isEmpty()) {
            rolez.putAll(getMenuRoles((String[]) map.keySet().toArray(new String[0])));
        }
    }

    return  rolez;
  }

  /**
   * 获取更多单元
   * @param names
   * @return 单元字典
   */
  public Map<String, Map> getMoreRoles(String... names)
  {
    Map<String, Map>  ds = new HashMap();
    this.getRoleAuths(ds , new HashSet(), names);
    return ds;
  }

  /**
   * 获取单元动作
   * @param names
   * @return 全部动作名
   */
  public Set<String> getRoleAuths(String... names)
  {
    Set<String>  as = new HashSet();
    this.getRoleAuths(new HashMap(), as , names);
    return as;
  }

  protected void getRoleAuths(Map roles, Set auths, String... names)
  {
    for  (String n : names)
    {
      Map role = getRole(n);
      if (role == null || roles.containsKey(n))
      {
        continue;
      }

      roles.put(n, role);

      if (role.containsKey("actions"))
      {
        Set<String> actionsSet = (Set<String>) role.get("actions");
        auths.addAll(actionsSet);
      }

      if (role.containsKey("depends"))
      {
        Set<String> dependsSet = (Set<String>) role.get("depends");
        String[ ]   dependsArr = dependsSet.toArray(new String[0]);
        this.getRoleAuths(roles, auths, dependsArr);
      }
    }
  }

  /**
   * 获取角色集合(与当前请求相关)
   * @return session 为空则返回 null
   * @throws io.github.ihongs.HongsException
   */
  public Set<String> getRoleSet() throws HongsException {
      if (session == null || session.length() == 0) {
          CoreLogger.error("Can not get roles for menu "+ name);
          return null;
      }
      if (session.startsWith("@")) {
          return getInstance(session.substring(1)).getRoleSet();
      } else
      if (session.startsWith("!")) {
          return ( Set ) Core.getInstance(session.substring(1));
      } else
      {
          return ( Set ) Core.getInstance( ActionHelper.class ).getSessibute(session);
      }
  }

  /**
   * 获取权限集合(与当前请求相关)
   * @return session 为空则返回 null
   * @throws io.github.ihongs.HongsException
   */
  public Set<String> getAuthSet() throws HongsException {
      Set<String> roleset = getRoleSet();
      if (null == roleset)  return null ;
      return getRoleAuths(roleset.toArray(new String[0]));
  }

  /**
   * 检查角色权限(与当前请求相关)
   * @param role
   * @return 可访问则为true
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
   * @param auth
   * @return 可访问则为true
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
   * @return 有一个动作可访问即返回true
   * @throws io.github.ihongs.HongsException
   */
  public Boolean chkMenu(String name) throws HongsException {
      Set<String> rolez = getMenuRoles(name).keySet();
      if (null == rolez || rolez.isEmpty( )) {
          return  true ;
      }

      Set<String> rolex = getRoleSet( /**/ );
      if (null == rolex || rolex.isEmpty( )) {
          return  false;
      }

      for(String  role : rolez) {
      if (rolex.contains(role)) {
          return  true ;
      }
      }

      /**/return  false;
  }

  /**
   * 获取翻译对象(与当前请求相关)
   * @return
   */
  public CoreLocale getCurrTranslator() {
    CoreLocale lang;
    try {
      lang = CoreLocale.getInstance(name).clone();
    }
    catch (HongsError e) {
      if  (e.getErrno( ) != 0x2a) {
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
   * 获取全部角色
   * @return
   */
  public List<Map> getRoleTranslates() {
      return getRoleTranslated(0, null);
  }

  public List<Map> getRoleTranslates(int d) {
      return getRoleTranslated(d, null);
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
      CoreLocale lang = getCurrTranslator();
      return getRoleTranslated(menus, rolez, lang, d, 0);
  }

  public List<Map> getRoleTranslates(String name) {
      return getRoleTranslated(name, 0, null);
  }

  public List<Map> getRoleTranslates(String name, int d) {
      return getRoleTranslated(name, d, null);
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
      CoreLocale lang= getCurrTranslator();
      Map menu = getMenu(name);
      if (menu == null) {
          throw new NullPointerException("Menu for href '"+name+"' is not in "+this.name);
      }
      return getRoleTranslated((Map) menu.get("menus"), rolez, lang, d, 0 );
  }

  protected List<Map> getRoleTranslated(Map<String, Map> menus, Set<String> rolez, CoreLocale lang, int j, int i) {
      List<Map> list = new ArrayList( );

      if (null == menus||(j != 0 && j <= i)) {
          return  list;
      }

      for(Map.Entry item : menus.entrySet()) {
          Map v = (Map) item.getValue();
          Map m = (Map) v.get("menus" );
          Set r = (Set) v.get("roles" );

          List<Map> rolz = new ArrayList();
          List<Map> subz = getRoleTranslated(m, rolez, lang, j, i + 1);

          if (r != null) for (String n : (Set<String>) r) {
              if (rolez != null && ! rolez.contains(n)  ) {
                  continue;
              }

              Map    o = getRole(n);
              Set    x = (Set) o.get("depends");
              String s = (String) o.get("text");

              // 没有指定 text 则用 name 获取
              if (s == null || s.length() == 0) {
                  s = "core.role."+name2Prop(n);
              }

              Map role = new HashMap();
              role.put("name", n);
              role.put("text", s);
              role.put("rels", x);
              rolz.add(role);
          }

          if (! rolz.isEmpty()) {
              String h = (String) item.getKey();
              String p = (String) v.get("hrel");
              String d = (String) v.get("icon");
              String s = (String) v.get("text");

              // 没有指定 text 则用 href 获取
              if (s == null || s.length() == 0) {
                  s = "core.role."+name2Prop(h);
              }
              s = lang.translate(s);

              Map menu = new HashMap();
              menu.put("href", h);
              menu.put("hrel", p);
              menu.put("icon", d);
              menu.put("text", s);
              menu.put("roles", rolz );
              list.add(menu);
          }

          // 拉平下级
          if (! subz.isEmpty()) {
              list.addAll(subz);
          }
      }

      return list;
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
      CoreLocale lang = getCurrTranslator();
      return getMenuTranslated(menus, rolez, lang, d, 0);
  }

  public List<Map> getMenuTranslates(String name) {
      return getMenuTranslated(name, 1, null);
  }

  public List<Map> getMenuTranslates(String name, int d) {
      return getMenuTranslated(name, d, null);
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
      CoreLocale lang= getCurrTranslator();
      Map menu = getMenu(name);
      if (menu == null) {
          throw new NullPointerException("Menu for href '"+name+"' is not in "+this.name);
      }
      return getMenuTranslated((Map) menu.get("menus"), rolez, lang, d, 0 );
  }

  protected List<Map> getMenuTranslated(Map<String, Map> menus, Set<String> rolez, CoreLocale lang, int j, int i) {
      List<Map> list = new ArrayList( );

      if (null == menus||(j != 0 && j <= i)) {
          return  list;
      }

      for(Map.Entry item : menus.entrySet()) {
          String h = (String) item.getKey();
          Map    v = (Map) item.getValue( );
          Map    m = (Map) v.get( "menus" );
          List<Map> subz = getMenuTranslated(m, rolez, lang, j, i + 1);

          // 页面下的任意一个动作有权限即认为是可访问的
          if (null != rolez && subz.isEmpty()) {
              Set<String> z  = getMenuRoles(h).keySet();
              if ( ! z.isEmpty( ) ) {
                  boolean e  = true ;
                  for(String x : z) {
                      if (rolez.contains( x )) {
                          e  = false;
                          /**/ break;
                      }
                  }
                  if (e) {
                      continue;
                  }
              }
          }

          String p = (String) v.get("hrel");
          String d = (String) v.get("icon");
          String s = (String) v.get("text");

          // 没有指定 text 则用 href 获取
          if (s == null || s.length() == 0) {
              s = "core.menu."+name2Prop(h);
          }
          s = lang.translate(s);

          Map menu = new HashMap();
          menu.put("href", h);
          menu.put("hrel", p);
          menu.put("icon", d);
          menu.put("text", s);
          menu.put("menus", subz );
          list.add( menu);
      }

      return list;
  }

  /**
   * 链接和名称转换为属性键
   * <pre>
   * 将一些特殊符号转换为点, 例如:
   * "abc/def#xyz" 转为 "abc.def.xyz"
   * "abc.def?x=z" 转为 "abc.def.x.z"
   * </pre>
   * @param n
   * @return
   */
  protected static final String name2Prop(String n) {
      n = CONV_DOT.matcher(n).replaceAll("."); // URL 符号换成点
      n = TRIM_DOT.matcher(n).replaceAll("" ); // 去前后多余的点
      return n;
  }
  private static final java.util.regex.Pattern CONV_DOT = java.util.regex.Pattern.compile("[/#?&=!]+");
  private static final java.util.regex.Pattern TRIM_DOT = java.util.regex.Pattern.compile("^\\.|\\.$");

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
    return null != NaviMap.class.getClassLoader().getResourceAsStream(fn);
  }

  public static NaviMap getInstance(String name) throws HongsException {
      String cn = NaviMap.class.getName() + ":" + name;
      Core core = Core.getInstance();
      NaviMap inst;
      if (core.containsKey(cn)) {
          inst = (NaviMap) core.get( cn );
      } else {
          inst = new NaviMap( name );
          core.put( cn , inst );
      }
      return inst;
  }

  public static NaviMap getInstance() throws HongsException {
      return getInstance("default");
  }

}
