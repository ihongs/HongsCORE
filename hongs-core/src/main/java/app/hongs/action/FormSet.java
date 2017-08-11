package app.hongs.action;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreLocale;
import app.hongs.CoreSerial;
import app.hongs.HongsException;
import app.hongs.util.Data;
import app.hongs.util.Synt;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
 * <h3>异常代码:</h3>
 * <pre>
 * 区间: 0x10e8~0x10ef
 * 0x10e8 配置文件不存在
 * 0x10e9 解析文件失败
 * </pre>
 *
 * @author Hongs
 */
public class FormSet
  extends CoreSerial
{

  private final String name;

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
    this.name = name;
    this.init(name + Cnst.FORM_EXT);
  }

  @Override
  protected boolean expired(long time)
  {
    File xmlFile = new File(Core.CONF_PATH
                 + File.separator + name + Cnst.FORM_EXT + ".xml");
    File serFile = new File(Core.DATA_PATH
                 + File.separator + "serial"
                 + File.separator + name + Cnst.FORM_EXT + ".ser");
    return xmlFile.exists() && xmlFile.lastModified() > serFile.lastModified();
  }

  @Override
  protected void imports()
    throws HongsException
  {
    InputStream is;
    String      fn;

    try
    {
        fn = Core.CONF_PATH + File.separator + name + Cnst.FORM_EXT + ".xml";
        is = new FileInputStream(fn);
    }
    catch (FileNotFoundException ex)
    {
        fn = name.contains(".")
          || name.contains("/") ? name + Cnst.FORM_EXT + ".xml"
           : "app/hongs/conf/"  + name + Cnst.FORM_EXT + ".xml";
        is = this.getClass().getClassLoader().getResourceAsStream(fn);
        if ( null == is )
        {
            throw new app.hongs.HongsException(0x10e8,
                "Can not find the config file '" + name + Cnst.FORM_EXT + ".xml'.");
        }
    }

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
      throw new HongsException(0x10e9, "Read '" +name+Cnst.FORM_EXT+".xml error'", ex);
    }
    catch (SAXException ex)
    {
      throw new HongsException(0x10e9, "Parse '"+name+Cnst.FORM_EXT+".xml error'", ex);
    }
    catch (ParserConfigurationException ex)
    {
      throw new HongsException(0x10e9, "Parse '"+name+Cnst.FORM_EXT+".xml error'", ex);
    }

    this.forms = new HashMap();
    this.enums = new HashMap();
    this.parse(root, this.forms, this.enums);
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

      if ("form".equals(tagName2))
      {
        String namz = element2.getAttribute("name");
        if (namz == null) namz = "";
        Map items = new LinkedHashMap();
        this.parse(element2, items, null);
        forms.put(namz, items);
      }
      if ("enum".equals(tagName2))
      {
        String namz = element2.getAttribute("name");
        if (namz == null) namz = "";
        Map items = new LinkedHashMap();
        this.parse(element2, null, items);
        enums.put(namz, items);
      }
      if ("field".equals(tagName2))
      {
        String namz = element2.getAttribute("name");
        if (namz == null) namz = "";
        Map items = new LinkedHashMap();
        this.parse(element2, items, null);
        forms.put(namz, items);

        items.put("__name__", namz);

        namz = element2.getAttribute("text");
        items.put("__text__", namz);

        namz = element2.getAttribute("hint");
        items.put("__hint__", namz);

        namz = element2.getAttribute("type");
        items.put("__type__", namz);

        namz = element2.getAttribute("rule");
        items.put("__rule__", namz);

        if (element2.hasAttribute("required")) {
            namz = element2.getAttribute("required");
            items.put("__required__",  namz  );
        } else {
            items.put("__required__", "false");
        }

        if (element2.hasAttribute("repeated")) {
            namz = element2.getAttribute("repeated");
            items.put("__repeated__",  namz  );
        } else {
            items.put("__repeated__", "false");
        }
      }
      else
      if ("param".equals(tagName2))
      {
        String namz = element2.getAttribute("name");
        String typz = element2.getAttribute("type");
        String text = element2.getTextContent();
        forms.put(namz, parse(typz, text));
      }
      else
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
          return  text  ;
      }

      text = text.trim();

      if ("bool".equals(type)) {
          return Synt.defoult(Synt.asBool(text), false);
      }

      if ("json".equals(type)) {
        if (text.startsWith("(") && text.endsWith(")")) {
          return Data.toObject(text.substring(1, text.length() - 1));
        } else {
          return Data.toObject(text);
        }
      }

      if ("list".equals(type)) {
        if (text.startsWith("[") && text.endsWith("]")) {
          return ( List) Data.toObject(text);
        } else {
          return  new  ArrayList   (
              Arrays.asList(SEXP.split(text))
          );
        }
      }

      if ( "set".equals(type)) {
        if (text.startsWith("[") && text.endsWith("]")) {
          return  new LinkedHashSet(
                 ( List) Data.toObject(text)
          );
        } else {
          return  new LinkedHashSet(
              Arrays.asList(SEXP.split(text))
          );
        }
      }

      if ( "map".equals(type)) {
        if (text.startsWith("{") && text.endsWith("}")) {
          return ( Map ) Data.toObject(text);
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

      throw new HongsException.Common("Unrecognized type '"+type+"'");
  }
  private static final Pattern SEXP = Pattern.compile ( "\\s*,\\s*" );
  private static final Pattern MEXP = Pattern.compile ( "\\s*:\\s*" );

  public String getName() {
      return  this.name;
  }

  public Map getEnum(String name) throws HongsException {
    if (!enums.containsKey(name)) {
        throw  new HongsException(0x10eb, "Enum "+name+" in "+this.name+" is not exists");
    }
    return enums.get(name);
  }

  public Map getForm(String name) throws HongsException {
    if (!forms.containsKey(name)) {
        throw  new HongsException(0x10ea, "Form "+name+" in "+this.name+" is not exists");
    }
    return forms.get(name);
  }

  public CoreLocale getCurrTranslator() {
    try {
      return CoreLocale.getInstance(name);
    }
    catch (app.hongs.HongsError e) {
      if  (  e.getErrno() != 0x2a) {
        throw e;
      }
      return CoreLocale.getInstance("default");
    }
  }

  public Map getEnumTranslated(String namc) {
    Map items = enums.get(namc);
    Map itemz = new LinkedHashMap();
    if (items == null) return itemz;
    CoreLocale lang = getCurrTranslator();
    itemz.putAll(items);
    for(Object o : itemz.entrySet()) {
      Map.Entry e = (Map.Entry) o ;
      String    k = (String) e.getKey(  );
      String    n = (String) e.getValue();
      if (n == null || "".equals(n)) {
          n = "fore.enum."+name+"."+namc+"."+k;
      }
      e.setValue( lang.translate(n));
    }
    return itemz;
  }

  public Map getFormTranslated(String namc)
    throws HongsException
  {
    Map items = getForm(namc);
    Map itemz = new LinkedHashMap();
    if (items == null) return itemz;
    CoreLocale lang = getCurrTranslator();
    for(Object o : items.entrySet()) {
      Map.Entry e = (Map.Entry) o;
      Map       m = (Map ) e.getValue();
      String    k = (String) e.getKey();
      String    n = (String) m.get("__text__");
      String    h = (String) m.get("__hint__");
      Map       u = new LinkedHashMap();
      u.putAll( m );
      if (n == null || "".equals(n)) {
          n = "fore.form."+name+"."+namc+"."+k;
      }   u.put("__text__", lang.translate(n));
      if (h != null &&!"".equals(n)) {
          u.put("__hint__", lang.translate(h));
      }
      itemz.put(k, u);
    }
    return itemz;
  }

  //** 工厂方法 **/

  public static boolean hasConfFile(String name) {
    String fn;

    fn = Core.DATA_PATH
       + File.separator + "serial"
       + File.separator + name + Cnst.FORM_EXT + ".ser";
    if (new File(fn).exists()) {
        return true;
    }

    fn = Core.CONF_PATH
       + File.separator + name + Cnst.FORM_EXT + ".xml";
    if (new File(fn).exists()) {
        return true;
    }

    fn = name.contains(".")
      || name.contains("/") ? name + Cnst.FORM_EXT + ".xml"
       : "app/hongs/conf/"  + name + Cnst.FORM_EXT + ".xml";
    return null != FormSet.class.getClassLoader().getResourceAsStream(fn);
  }

  public static FormSet getInstance(String name) throws HongsException {
      String key = FormSet.class.getName() + ":" + name;
      Core core = Core.getInstance();
      FormSet inst;
      if (core.containsKey(key)) {
          inst = (FormSet)core.get(key);
      }
      else {
          inst = new FormSet(name);
          core.put( key, inst );
      }
      return inst;
  }

  public static FormSet getInstance() throws HongsException {
      return getInstance("default");
  }

}
