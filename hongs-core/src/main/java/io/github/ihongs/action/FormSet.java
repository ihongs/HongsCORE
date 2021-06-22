package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CoreSerial;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.daemon.Gate;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 表单配置.
 *
 * <p>
 * 该工具会将配置数据自动缓存, 会在构建对象时核对配置的修改时间;
 * 但无法确保其对象在反复使用中会自动重载,
 * 最好在修改配置后删除临时文件并重启应用.
 * </p>
 *
 * <h3>数据结构:</h3>
 * <pre>
    forms = {
        "form_name" : {
            "field_name" : {
                __text__ : "Label",
                __type__ : "string|number|date|file|enum|form",
                __rule__ : "rule.class.Name",
                __required__ : "yes|no",
                __repeated__ : "yes|no",
                "param_name" : "Value"
                ...
            }
            ...
        }
        ...
    }
    enums = {
        "enum_name" : {
            "value_code" : "Label"
            ...
        }
        ...
    }
 * </pre>
 *
 * <h3>错误代码:</h3>
 * <pre>
 * 区间: 910~919
 * 910 配置文件不存在
 * 911 解析文件失败
 * 912 表单不存在
 * 913 枚举不存在
 * </pre>
 *
 * @author Hongs
 */
public class FormSet
     extends CoreSerial
  implements CoreSerial.Mtimes
{

  protected transient String name;
  protected transient  long  time;

  /**
   * 表单集合
   */
  public Map<String, Map> forms;

  /**
   * 枚举集合
   */
  public Map<String, Map> enums;

  public FormSet(String name)
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
                 + File.separator + name + Cnst.FORM_EXT + ".ser");
    time = serFile.lastModified();

    //* 加锁读写 */

    Gate.Leader lock = Gate.getLeader(FormSet.class.getName() + ":" + name);

    lock.lockr();
    try {
    if (! expired()) {
      load(serFile );
      return;
    }
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
                 + File.separator + name + Cnst.FORM_EXT + ".ser");
    File xmlFile = new File(Core.CONF_PATH
                 + File.separator + name + Cnst.FORM_EXT + ".xml");
    return  Math.max(serFile.lastModified(), xmlFile.lastModified());
  }

  static protected boolean expired(String namz , long timz)
  {
    File serFile = new File(Core.DATA_PATH
                 + File.separator + "serial"
                 + File.separator + namz + Cnst.FORM_EXT + ".ser");
    if (!serFile.exists() || serFile.lastModified() > timz )
    {
      return true;
    }

    File xmlFile = new File(Core.CONF_PATH
                 + File.separator + namz + Cnst.FORM_EXT + ".xml");
    if ( xmlFile.exists() )
    {
      return xmlFile.lastModified() > timz;
    }

    // 为减少判断逻辑对 jar 文件不做变更对比, 只要资源存在即可
    return null == FormSet.class.getClassLoader().getResource(
             namz.contains(".")
          || namz.contains("/") ? namz + Cnst.FORM_EXT + ".xml"
           : Cnst.CONF_PACK +"/"+ namz + Cnst.FORM_EXT + ".xml"
    );
  }

  public boolean expired()
  {
    return expired (name, time);
  }

  @Override
  protected void imports()
    throws HongsException
  {
    InputStream is;
    String      fn;

    try
    {
        fn = Core.CONF_PATH +"/"+ name + Cnst.FORM_EXT + ".xml";
        is = new FileInputStream(fn);
    }
    catch (FileNotFoundException ex)
    {
        fn = name.contains(".")
          || name.contains("/") ? name + Cnst.FORM_EXT + ".xml"
           : Cnst.CONF_PACK +"/"+ name + Cnst.FORM_EXT + ".xml";
        is = this.getClass().getClassLoader().getResourceAsStream(fn);
        if ( null == is )
        {
            throw new HongsException(910,
                "Can not find the config file '" + name + Cnst.FORM_EXT + ".xml'.");
        }
    }

    try
    {

    Element root;
    try
    {
      DocumentBuilderFactory dbf  = DocumentBuilderFactory.newInstance();
      DocumentBuilder dbn  = dbf.newDocumentBuilder();
      Document  doc = dbn.parse( is );
      root = doc.getDocumentElement();
    }
    catch ( IOException ex)
    {
      throw new HongsException(911, "Read '" +name+Cnst.FORM_EXT+".xml error'", ex);
    }
    catch (SAXException ex)
    {
      throw new HongsException(911, "Parse '"+name+Cnst.FORM_EXT+".xml error'", ex);
    }
    catch (ParserConfigurationException ex)
    {
      throw new HongsException(911, "Parse '"+name+Cnst.FORM_EXT+".xml error'", ex);
    }

    this.forms = new HashMap();
    this.enums = new HashMap();
    this.parse(root, this.forms, this.enums);

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

  private void parse(Element element, Map forms, Map enums)
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

      if ("enum".equals(tagName2))
      {
        String namz = element2.hasAttribute("name")
                    ? element2.getAttribute("name")
                    : "" ;
        Map items = new LinkedHashMap();
        this.parse(element2, null, items);
        enums.put(namz, items);
      } else
      if ("form".equals(tagName2))
      {
        String namz = element2.hasAttribute("name")
                    ? element2.getAttribute("name")
                    : "" ;
        Map items = new LinkedHashMap();
        this.parse(element2, items, null);
        forms.put(namz, items);
      } else
      if ("field".equals(tagName2))
      {
        String namz = element2.hasAttribute("name")
                    ? element2.getAttribute("name")
                    : "@"; // 缺省为当前表单扩展配置
        Map items = new LinkedHashMap();
        this.parse(element2, items, null);
        forms.put(namz, items);

        items.put("__name__", namz);

        String typz, attr;

        typz = element2.getAttribute("type");
        items.put("__type__", typz);

        attr = element2.getAttribute("rule");
        items.put("__rule__", attr);

        attr = element2.getAttribute("text");
        items.put("__text__", attr);

        attr = element2.getAttribute("hint");
        items.put("__hint__", attr);

        if (element2.hasAttribute("required")) {
            attr = element2.getAttribute("required");
            items.put("__required__",  attr  );
        } else {
            items.put("__required__", "false");
        }

        if (element2.hasAttribute("repeated")) {
            attr = element2.getAttribute("repeated");
            items.put("__repeated__",  attr  );
        } else {
            items.put("__repeated__", "false");
        }

        /**
         * 预优化
         * 枚举类型和关联类型缺失配置路径时可自动补上
         * 注意规避解析默认表单配置时可能引起无限递归
         */
        if (!"default".equals(name)) {
            typz = (String) getInstance().getEnum("__types__").get(typz);
            if ( "@"  .equals(namz)) {
                if (! items.containsKey("form")) {
                    attr = element.getAttribute("name");
                    items.put("form", attr);
                }
                if (! items.containsKey("conf")) {
                    items.put("conf", name);
                }
            } else
            if ("enum".equals(typz)) {
                if (! items.containsKey("enum")) {
                //  namz = namz.replaceFirst("_id$","");
                    items.put("enum", namz);
                }
                if (! items.containsKey("conf")) {
                    items.put("conf", name);
                }
            } else
            if ("form".equals(typz)) {
                if (! items.containsKey("form")) {
                    namz = namz.replaceFirst("_id$","");
                    items.put("form", namz);
                }
                if (! items.containsKey("conf")) {
                    items.put("conf", name);
                }
            } else
            if ("fork".equals(typz)) {
                if (! items.containsKey("data-at" )
                &&  ! items.containsKey("data-al")) {
                if (! items.containsKey("form")) {
                    namz = namz.replaceFirst("_id$","");
                    items.put("form", namz);
                }
                if (! items.containsKey("conf")) {
                    items.put("conf", name);
                }
                }
            } else
            if (items.containsKey("enum")
            ||  items.containsKey("form") ) {
                if (! items.containsKey("conf")) {
                    items.put("conf", name);
                }
            }
        }
      } else
      if ("param".equals(tagName2))
      {
        String namz = element2.getAttribute("name");
        String typz = element2.getAttribute("type");
        String text = element2.getTextContent();
        forms.put(namz, parse(typz, text));
      } else
      if ("value".equals(tagName2))
      {
        String namz = element2.getAttribute("code");
        String typz = element2.getAttribute("type");
        String text = element2.getTextContent();
        enums.put(namz, parse(typz, text));
      }
    }
  }

  private Object parse(String type, String text) throws HongsException {
      if (null == type || "".equals(type)) {
          return  text.trim();
      } else {
          text =  text.trim();
      }

      if ("bool".equals(type)) {
          return Synt.defoult(Synt.asBool(text), false);
      }

      if ("json".equals(type)) {
        if (text.startsWith("(") && text.endsWith(")")) {
          text = text.substring( 1, text.length() - 1 );
          return Dawn.toObject(text);
        } else {
          return Dawn.toObject(text);
        }
      }

      if ("list".equals(type)) {
        if (text.startsWith("[") && text.endsWith("]")) {
          return ( List) Dawn.toObject(text);
        } else {
          return  new  ArrayList   (
              Arrays.asList(SEXP.split(text))
          );
        }
      }

      if ( "set".equals(type)) {
        if (text.startsWith("[") && text.endsWith("]")) {
          return  new LinkedHashSet(
                 ( List) Dawn.toObject(text)
          );
        } else {
          return  new LinkedHashSet(
              Arrays.asList(SEXP.split(text))
          );
        }
      }

      if ( "map".equals(type)) {
        if (text.startsWith("{") && text.endsWith("}")) {
          return ( Map ) Dawn.toObject(text);
        } else {
          Map m = new LinkedHashMap();
          for(String   s : SEXP.split (text)) {
              String[] a = MEXP.split (s, 2);
              if ( 2 > a.length ) {
                  m.put( a[0], a[0] );
              } else {
                  m.put( a[0], a[1] );
              }
          }
          return  m;
        }
      }

      throw new HongsException(914, "Unrecognized type '"+ type +"'")
          .setLocalizedOptions(type);
  }
  private static final Pattern SEXP = Pattern.compile ( "\\s*,\\s*" );
  private static final Pattern MEXP = Pattern.compile ( "\\s*:\\s*" );

  public String getName() {
      return  this.name;
  }

  public Map getEnum(String name) throws HongsException {
    if (null == name) {
        throw new NullPointerException( "Enum name can not be null" );
    }
    Map names = enums.get("__enum__");
    if (null != names
    &&  names.containsKey(name)) {
        name  = (String)  names.get(name);
    }
    if (enums.containsKey(name)) {
        return  ( Map  )  enums.get(name);
    }
    if (name . startsWith ("@")) {
        return  ( Map  )  Core.getInstance(name.substring(1));
    }
    throw new HongsException(913, "Enum "+name+" in "+this.name+" is not exists");
  }

  public Map getForm(String name) throws HongsException {
    if (null == name) {
        throw new NullPointerException( "Form name can not be null" );
    }
    Map names = enums.get("__form__");
    if (null != names
    &&  names.containsKey(name)) {
        name  = (String)  names.get(name);
    }
    if (forms.containsKey(name)) {
        return  ( Map  )  forms.get(name);
    }
    if (name . startsWith ("@")) {
        return  ( Map  )  Core.getInstance(name.substring(1));
    }
    throw new HongsException(912, "Form "+name+" in "+this.name+" is not exists");
  }

  public CoreLocale getCurrTranslator() {
    try {
      return CoreLocale.getInstance(name);
    }
    catch (HongsExemption e) {
      if  (e.getErrno( ) != 826 ) {
        throw e;
      }
      return new CoreLocale(null);
    }
  }

  public Map getEnumTranslated(String namc)
    throws HongsException
  {
    Map items = getEnum(namc);
    Map itemz = new LinkedHashMap();
    CoreLocale lang = getCurrTranslator();
    itemz.putAll(items);
    for(Object o : itemz.entrySet()) {
      Map.Entry e = (Map.Entry) o;
//    String    k = (String) e.getKey(  );
      String    n = (String) e.getValue();
      e.setValue(lang.translate(n));
    }
    return itemz;
  }

  public Map getFormTranslated(String namc)
    throws HongsException
  {
    Map items = getForm(namc);
    Map itemz = new LinkedHashMap();
    CoreLocale lang = getCurrTranslator();
    for(Object o : items.entrySet()) {
      Map.Entry e = (Map.Entry) o;
      Map       m = (Map ) e.getValue();
      String    k = (String) e.getKey();
      String    n = (String) m.get("__text__");
      String    h = (String) m.get("__hint__");
      Map       u = new LinkedHashMap();
      u.putAll( m );
      if (n != null ||!"".equals(n)) {
          u.put("__text__", lang.translate(n));
      }
      if (h != null &&!"".equals(n)) {
          u.put("__hint__", lang.translate(h));
      }
      itemz.put(k, u);
    }
    return itemz;
  }

  //** 工厂方法 **/

  public static boolean hasConfFile(String name) {
    String fn = "/serial/";

    fn = Core.DATA_PATH +fn + name + Cnst.FORM_EXT + ".ser";
    if (new File(fn).exists()) {
        return true;
    }

    fn = Core.CONF_PATH +"/"+ name + Cnst.FORM_EXT + ".xml";
    if (new File(fn).exists()) {
        return true;
    }

    fn = name.contains(".")
      || name.contains("/") ? name + Cnst.FORM_EXT + ".xml"
       : Cnst.CONF_PACK +"/"+ name + Cnst.FORM_EXT + ".xml";
    return null != FormSet.class.getClassLoader().getResourceAsStream(fn);
  }

  public static FormSet getInstance(String name) throws HongsException {
      Core    core =  Core.getInstance ();
      String  code =  FormSet.class.getName() + ":" + name;
      FormSet inst = (FormSet) core.get(code);
      if (inst == null) {
          inst  = new FormSet( name );
          core.set(code, inst);
      }
      return inst;
  }

  public static FormSet getInstance() throws HongsException {
      return getInstance("default");
  }

}
