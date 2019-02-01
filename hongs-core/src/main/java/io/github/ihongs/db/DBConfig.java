package io.github.ihongs.db;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreSerial;
import io.github.ihongs.HongsError;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.Tool;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 数据库配置信息解析类
 *
 * <h3>异常代码:</h3>
 * <pre>
 * 区间: 0x1060~0x106f
 * 0x1061  找不到数据库配置文件
 * 0x1063  无法解析数据库配置文档
 * 0x1065  在配置文档中找不到根节点
 * 0x1067  无法读取XML文件
 * 0x1069  无法读取XML流
 * </pre>
 *
 * @author Hongs
 */
public class DBConfig
     extends CoreSerial
  implements Serializable
{

  //** 缓存 **/

  protected transient String name;

  public DBConfig(String name)
    throws HongsException
  {
    this.name = name;
    this.init ( name+Cnst.DB_EXT);
  }

  @Override
  protected boolean expired(long time)
  {
    File serFile = new File(Core.DATA_PATH
                 + File.separator + "serial"
                 + File.separator + name + Cnst.DB_EXT + ".ser");
    File xmlFile = new File(Core.CONF_PATH
                 + File.separator + name + Cnst.DB_EXT + ".xml");
    if ( xmlFile.exists() )
    {
      return xmlFile.lastModified() > serFile.lastModified();
    }

    // 为减少判断逻辑对 jar 文件不做变更对比, 只要资源存在即可
    return null == getClass().getClassLoader().getResource(
             name.contains(".")
          || name.contains("/") ? name + Cnst.DB_EXT + ".xml"
           : Cnst.CONF_PACK +"/"+ name + Cnst.DB_EXT + ".xml"
    );
  }

  @Override
  protected void imports()
    throws HongsException
  {
    InputStream is;
    String      fn;
    DBConfig    cp;

    try
    {
        fn = Core.CONF_PATH +"/"+ name + Cnst.DB_EXT + ".xml";
        is = new FileInputStream(fn);
    }
    catch (FileNotFoundException ex)
    {
        fn = name.contains(".")
          || name.contains("/") ? name + Cnst.DB_EXT + ".xml"
           : Cnst.CONF_PACK +"/"+ name + Cnst.DB_EXT + ".xml";
        is = this.getClass().getClassLoader().getResourceAsStream(fn);
        if (  is  ==  null )
        {
            throw new HongsError(0x2a,
                "Can not find the config file '" + name + Cnst.DB_EXT + ".xml'.");
        }
    }

    try {
        cp = parseByStream(is);
    } finally {
      try {
        is.close();
      } catch (IOException ex) {
        throw new HongsException.Common(ex);
      }
    }

    this.link         = cp.link;
    this.source       = cp.source;
    this.origin       = cp.origin;
    this.dbClass      = cp.dbClass;
    this.tableClass   = cp.tableClass;
    this.modelClass   = cp.modelClass;
    this.tablePrefix  = cp.tablePrefix;
    this.tableSuffix  = cp.tableSuffix;
    this.tableConfigs = cp.tableConfigs;
  }

  //** 数据 **/

  public String link;

  public String dbClass;

  public String tableClass;

  public String modelClass;

  public String tablePrefix;

  public String tableSuffix;

  public Map<String, Map> tableConfigs;

  public Map source;

  public Map origin;

  private static Set<String> tableAttrs = new HashSet(
  Arrays.asList( new String[] {
    "name","tableName","primaryKey","class","model"
  }));
  private static Set<String> assocAttrs = new HashSet(
  Arrays.asList( new String[] {
    "name","tableName","primaryKey","foreignKey","type","join"
  }));

  public DBConfig(Document doc)
    throws HongsException
  {
    /**
     * 仅当type为BLS_TO或HAS_ONE时join可用;
     * 当type为BLS_TO时, foreignKey为基本表的外键,
     * 当type为HAS_ONE或HAS_MANY时, foreignKey为关联表的外键;
     * 关联表其他可选配置select|filter|groupBy|having|orderBy|limit见FetchCase说明.
     */

    Element root = doc.getDocumentElement();
    if (!root.hasChildNodes())
    {
      throw new HongsException(0x1065, "Can not find root element in config document.");
    }

    String attr;
    this.link = null;
    this.dbClass = "";
    this.tableClass = "";
    this.modelClass = "";
    this.tablePrefix = "";
    this.tableSuffix = "";
    this.source = new HashMap();
    this.origin = new HashMap();
    this.tableConfigs = new HashMap();

    NodeList childs = root.getChildNodes();
    for (int i = 0; i < childs.getLength(); i ++)
    {
      Node node = childs.item(i);
      if (node.getNodeType() != Node.ELEMENT_NODE)
      {
        continue;
      }

      Element element = (Element)node;
      String  tagName = element.getTagName();

      if (tagName.equals("config"))
      {
        attr = getAttribute(element, "link", null);
        if (attr != null)
        {
          DBConfig conf = new DBConfig(attr);
          link  =  attr ;
          dbClass = conf.dbClass;
          tableClass = conf.tableClass;
          modelClass = conf.modelClass;
          tablePrefix = conf.tablePrefix;
          tableSuffix = conf.tableSuffix;
        }
//      else
//      {
          dbClass = getAttribute(element, "dbClass", dbClass);
          tableClass = getAttribute(element, "tableClass", tableClass);
          modelClass = getAttribute(element, "modelClass", modelClass);
          tablePrefix = getAttribute(element, "tablePrefix", tablePrefix);
          tableSuffix = getAttribute(element, "tableSuffix", tablePrefix);
//      }
      }
      else
      if (tagName.equals("source"))
      {
        attr = getAttribute(element, "link", null);
        if (attr != null)
        {
          DBConfig conf = new DBConfig(attr);
          this.source = conf.source;
        }
        else
        {
          this.source = DBConfig.getSource(element);
        }
      }
      else
      if (tagName.equals("origin"))
      {
        attr = getAttribute(element, "link", null);
        if (attr != null)
        {
          DBConfig conf = new DBConfig(attr);
          this.origin = conf.origin;
        }
        else
        {
          this.origin = DBConfig.getOrigin(element);
        }
      }
      else
      if (tagName.equals("tables"))
      {
        this.tableConfigs = DBConfig.getTables(element);
      }
    }
  }

  /**
   * 根据文件解析配置
   *
   * @param df
   * @return 配置对象
   * @throws io.github.ihongs.HongsException
   */
  public static DBConfig parseByFile(File df)
    throws HongsException
  {
    Document doc;
    try
    {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder dbn = dbf.newDocumentBuilder();
      doc = dbn.parse(df);
    }
    catch (ParserConfigurationException ex)
    {
      throw new  HongsException(0x1063, ex);
    }
    catch (SAXException ex)
    {
      throw new  HongsException(0x1063, ex);
    }
    catch ( IOException ex)
    {
      throw new  HongsException(0x1067, ex);
    }

    return new DBConfig(doc);
  }

  /**
   * 根据输入流解析配置
   *
   * @param ds
   * @return 配置对象
   * @throws io.github.ihongs.HongsException
   */
  public static DBConfig parseByStream(InputStream ds)
    throws HongsException
  {
    Document doc;
    try
    {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder dbn = dbf.newDocumentBuilder();
      doc = dbn.parse(ds);
    }
    catch (ParserConfigurationException ex)
    {
      throw new  HongsException(0x1063, ex);
    }
    catch (SAXException ex)
    {
      throw new  HongsException(0x1063, ex);
    }
    catch ( IOException ex)
    {
      throw new  HongsException(0x1069, ex);
    }

    return new DBConfig(doc);
  }

  /**
   * 根据输入流解析配置
   *
   * @param ds
   * @return 配置对象
   * @throws io.github.ihongs.HongsException
   */
  public static DBConfig parseBySource(InputSource ds)
    throws HongsException
  {
    Document doc;
    try
    {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder dbn = dbf.newDocumentBuilder();
      doc = dbn.parse(ds);
    }
    catch (ParserConfigurationException ex)
    {
      throw new  HongsException(0x1063, ex);
    }
    catch (SAXException ex)
    {
      throw new  HongsException(0x1063, ex);
    }
    catch ( IOException ex)
    {
      throw new  HongsException(0x1069, ex);
    }

    return new DBConfig(doc);
  }

  private static Map getOrigin(Element element)
  {
    String mode = "";
    String namc = "";
    Properties info = new Properties();

    NamedNodeMap atts = element.getAttributes();
    for (int i = 0; i < atts.getLength(); i ++)
    {
      Node attr = atts.item(i);
      String name = attr.getNodeName();
      String value = attr.getNodeValue();
      if ("jndi".equals(name))
      {
        mode = value;
      }
      if ("name".equals(name))
      {
        namc = value;
      }
      else
      {
        info.setProperty(name, value);
      }
    }

    // 2016/9/4 增加 source,origin 的 param 节点, 附加设置可使用 param
    getProperties(element, info);

    Map origin = new HashMap();
    origin.put("jndi", mode);
    origin.put("name", namc);
    origin.put("info", info);

    return origin;
  }

  private static Map getSource(Element element)
  {
    String mode = "";
    String namc = "";
    Properties info = new Properties();

    NamedNodeMap atts = element.getAttributes();
    for (int i = 0; i < atts.getLength(); i ++)
    {
      Node attr = atts.item(i);
      String name = attr.getNodeName();
      String value = attr.getNodeValue();
      if ("jdbc".equals(name))
      {
        mode = value;
      }
      else
      if ("name".equals(name))
      {
        namc = value;
      }
      else
      {
        info.setProperty(name, value);
      }
    }

    // 2016/9/4 增加 source,origin 的 param 节点, 附加设置可使用 param
    getProperties(element, info);

    Map source = new HashMap();
    source.put("jdbc", mode);
    source.put("name", namc);
    source.put("info", info);

    return source;
  }

  private static Map getTables(Element element)
  {
    Map tables = new LinkedHashMap();

    NodeList childs2 = element.getChildNodes();
    for (int j = 0; j < childs2.getLength(); j ++ )
    {
      Node node2 = childs2.item(j);
      if (node2.getNodeType() != Node.ELEMENT_NODE)
      {
        continue;
      }

      Element element2 = (Element)node2;
      String  tagName2 = element2.getTagName();
      if (tagName2.equals("table"))
      {
        Map table = new HashMap();

        NamedNodeMap atts = element2.getAttributes();
        for (int i = 0; i < atts.getLength(); i ++)
        {
          Node attr = atts.item(i);
          String name = attr.getNodeName();
          String value = attr.getNodeValue();

          if (tableAttrs.contains(name))
          {
            table.put(name, value);
          }
        }

        // 放入基础表中
        tables.put(table.get("name"), table);

        // 放入关联配置
        Map params2 = new /***/ HashMap();
        Map relats2 = new LinkedHashMap();
        Map assocs2 = getAssocs(element2, params2, relats2, new ArrayList());
        if (assocs2.isEmpty( ) == false )
        {
            table.put("assocs" , assocs2);
            table.put("relats" , relats2);
        }
        if (params2.isEmpty( ) == false )
        {
            table.put("params" , params2);
        }
      }
    }

    return tables;
  }

  private static Map getAssocs(Element element, Map params, Map relats, List tns)
  {
    Map assocs = new LinkedHashMap();

    NodeList childs2 = element.getChildNodes();
    for (int j = 0; j < childs2.getLength(); j ++ )
    {
      Node node2 = childs2.item(j);
      if (node2.getNodeType() != Node.ELEMENT_NODE)
      {
        continue;
      }

      Element element2 = (Element)node2;
      String  tagName2 = element2.getTagName();

      if (tagName2.equals("param"))
      {
        String name  = element2.getAttribute("name");
        String value = element2.getTextContent(/**/);

        if (value != null)
        {
          params.put(name, value.trim());
        }
      } else
      if (tagName2.equals("assoc"))
      {
        Map assoc = new HashMap();

        NamedNodeMap atts = element2.getAttributes();
        for (int i = 0; i < atts.getLength(); i ++ )
        {
          Node   attr  = atts.item(i);
          String name  = attr.getNodeName( );
          String value = attr.getNodeValue();

          if (assocAttrs.contains(name))
          {
            assoc.put(name, value);
          }
        }

        // 放入关联表中
        String tn2 = (String)assoc.get("name");
        List tns2 = new ArrayList(tns);
             tns2.add(tn2);
        assocs.put(tn2, assoc);
        relats.put(tn2, assoc);
        if (! tns.isEmpty( ) )
        {
            assoc.put("path" , tns);
        }

        // 递归关联配置
        Map params2 = new /***/ HashMap();
        Map assocs2 = getAssocs(element2, params2, relats, tns2);
        if (assocs2.isEmpty( ) == false )
        {
            assoc.put("assocs" , assocs2);
        }
        if (params2.isEmpty( ) == false )
        {
            assoc.put("params" , params2);
        }
      }
    }

    return assocs;
  }

  private static String getAttribute(Element element, String name, String def)
  {
    String text = element.getAttribute(name);
    return text != null && text.length() > 0 ? text : def;
  }

  private static void getProperties(Element element, Properties info) {
    NodeList list = element.getElementsByTagName("param");
    for (int i = 0, j = list.getLength( ); i < j; i += 1) {
        Element item = (Element) list.item(i);
        String n = item.getAttribute("name" );
        String v = item.getTextContent( );
        if ( v  != null ) {
            info.setProperty(n, v.trim());
        }
    }
  }

  /**
   * 补全驱动路径
   * @param name
   * @return
   */
  public static String fixSourceName(String name) {
    // Sqlite 相对路径补全
    if (name.startsWith("jdbc:sqlite:")) {
        name = name.substring( 12 );

        // 使用变量
        Map  opts  =  new HashMap();
        opts.put("SERVER_ID", Core.SERVER_ID);
        opts.put("CORE_PATH", Core.CORE_PATH);
        opts.put("CONF_PATH", Core.CONF_PATH);
        opts.put("DATA_PATH", Core.DATA_PATH);
        name = Tool.inject( name, opts );

        if(!new File(name).isAbsolute()) {
            name = Core.DATA_PATH +"/sqlite/"+ name;
        }
        if(!new File(name).getParentFile().exists()) {
            new File(name).getParentFile().mkdirs();
        }
        name = "jdbc:sqlite:"+ name;
    }

    return name;
  }

  /** 源 **/
/*
  public static class DBSource {

  }

  public static class DBOrigin {

  }
*/
  /** 表 **/
/*
  public static class TableConfig {

  }

  public static class AssocConfig {

  }
*/
}
