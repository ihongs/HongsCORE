package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CoreRoster;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                __required__ : "true|false",
                __repeated__ : "true|false",
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

    CoreLogger.debug("Serialized form conf {}", name);
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
    return Math.max(serFile.lastModified(),xmlFile.lastModified());
  }

  static protected boolean expired(String namz , long timz)
    throws HongsException
  {
    File serFile = new File(Core.DATA_PATH
                 + File.separator + "serial"
                 + File.separator + namz + Cnst.FORM_EXT + ".ser");
    if ( serFile.exists() && serFile.lastModified() > timz)
    {
      return true;
    }

    File xmlFile = new File(Core.CONF_PATH
                 + File.separator + namz + Cnst.FORM_EXT + ".xml");
    if ( xmlFile.exists() )
    {
      return timz < xmlFile.lastModified();
    }

    long resTime = CoreRoster.getResourceModified(
               namz.contains("/") ? namz + Cnst.FORM_EXT + ".xml" :
               Cnst.CONF_PACK +"/"+ namz + Cnst.FORM_EXT + ".xml");
    if ( resTime > 0 ) {
      return timz < resTime;
    }

    throw new HongsException(910, "Can not find the config file '" + namz + Cnst.FORM_EXT + ".xml'");
  }

  public boolean expired()
    throws HongsException
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
        fn = name.contains("/") ? name + Cnst.FORM_EXT + ".xml"
           : Cnst.CONF_PACK +"/"+ name + Cnst.FORM_EXT + ".xml";
        is = CoreRoster.getResourceAsStream(fn);
        if ( null == is )
        {
            throw new HongsException(910, "Can not find the config file '" + name + Cnst.FORM_EXT + ".xml'");
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
      throw new HongsException(ex, 911, "Read '" +name+Cnst.FORM_EXT+".xml' error");
    }
    catch (SAXException ex)
    {
      throw new HongsException(ex, 911, "Parse '"+name+Cnst.FORM_EXT+".xml' error");
    }
    catch (ParserConfigurationException ex)
    {
      throw new HongsException(ex, 911, "Parse '"+name+Cnst.FORM_EXT+".xml' error");
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
        items.put("__text__", gotLanguage(attr));

        attr = element2.getAttribute("hint");
        items.put("__hint__", gotLanguage(attr));

        if (element2.hasAttribute("required")) {
            attr = element2.getAttribute("required");
            items.put("__required__", attr );
        } else {
            items.put("__required__", "no" );
        }

        if (element2.hasAttribute("repeated")) {
            attr = element2.getAttribute("repeated");
            items.put("__repeated__", attr );
        } else {
            items.put("__repeated__", "no" );
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
            if ("form".equals(typz)) {
                if (! items.containsKey("form")) {
                    namz = namz.replaceFirst("_id$","");
                    items.put("form", namz);
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
            if (items.containsKey("form")
            ||  items.containsKey("enum") ) {
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

      switch (type) {
        case "conf":
          return getProperty  (text);
        case "lang":
          return getLanguage  (text);
        case "map" :
          return Synt.toMap   (text);
        case "set" :
          return Synt.toSet   (text);
        case "list":
          return Synt.toList  (text);
        case "json":
          return Dawn.toObject(text);
        case "bool":
          return Synt.defoult(Synt.asBool(text), false);
        case "int" :
          return Synt.defoult(Synt.asInt (text), (int ) 0 );
        case "long":
          return Synt.defoult(Synt.asLong(text), (long) 0 );
        case "byte":
          return Synt.defoult(Synt.asByte(text), (byte) 0 );
        case "short" :
          return Synt.defoult(Synt.asShort (text), (short ) 0);
        case "float" :
          return Synt.defoult(Synt.asFloat (text), (float ) 0);
        case "double":
          return Synt.defoult(Synt.asDouble(text), (double) 0);
        case "number":
          return Synt.defoult(Synt.asDouble(text), (double) 0);
      }

      throw new HongsException(914, "Unrecognized type '$0'", type);
  }

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

  /**
   * 获取当前语言类
   * @deprecated 输出时会翻译, 不必预先翻译
   * @return
   */
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

  /**
   * 获取翻译的枚举
   * @deprecated 输出时会翻译, 不必预先翻译, 只需 getEnum
   * @param namc 枚举名称
   * @return
   * @throws HongsException
   */
  public Map getEnumTranslated(String namc)
    throws HongsException
  {
    return getEnum(namc);
    /* 2022/04/15 输出时会翻译
    Map items = getEnum(namc);
    Map itemz = new LinkedHashMap();
    CoreLocale lang = getCurrTranslator();
    itemz.putAll(items);
    for(Object o : itemz.entrySet()) {
      Map.Entry e = (Map.Entry) o;
      Object    n = e.getValue( );
      if (n instanceof String ) {
        e.setValue(lang.translate((String) n));
      }
    }
    return itemz;
    */
  }

  /**
   * 获取翻译的表单
   * @deprecated 输出时会翻译, 不必预先翻译, 只需 getForm
   * @param namc 表单名称
   * @return
   * @throws HongsException
   */
  public Map getFormTranslated(String namc)
    throws HongsException
  {
    return getForm(namc);
    /* 2022/04/15 输出时会翻译
    Map items = getForm(namc);
    Map itemz = new LinkedHashMap();
    CoreLocale lang = getCurrTranslator();
    for(Object o : items.entrySet()) {
      Map.Entry e = (Map.Entry) o;
      Map       m = (Map ) e.getValue();
      String    k = (String) e.getKey();
      String    n = (String) m.get("__text__");
      String    h = (String) m.get("__hint__");
      String    d = (String) m.get("__hail__");
      Map       u = new LinkedHashMap();
      u.putAll( m );
      if (n != null ||!"".equals(n)) {
          u.put("__text__", lang.translate(n));
      }
      if (h != null &&!"".equals(n)) {
          u.put("__hint__", lang.translate(h));
      }
      if (d != null &&!"".equals(d)) {
          u.put("__hail__", lang.translate(h));
      }
      itemz.put(k, u);
    }
    return itemz;
    */
  }

  private Object getProperty(String text) {
      if (null == text || text.isEmpty()) {
          return  text;
      }

      int p;
      String   conf;
      String   defs;

          p  = text. indexOf (';');
      if (p >= 0) {
        defs = text.substring(1+p);
        text = text.substring(0,p);
      } else {
        defs = null;
      }

          p  = text. indexOf (':');
      if (p >= 0) {
        conf = text.substring(0,p);
        text = text.substring(1+p);
      } else {
        conf = name;
      }

      return new CoreConfig.Property(conf, text, defs);
  }

  private Object getLanguage(String text) {
      if (null == text || text.isEmpty()) {
          return  text;
      }

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
      if (null == text || text.isEmpty()) {
          return  text;
      }

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

    fn = Core.DATA_PATH +fn + name + Cnst.FORM_EXT + ".ser";
    if (new File(fn).exists()) {
        return true;
    }

    fn = Core.CONF_PATH +"/"+ name + Cnst.FORM_EXT + ".xml";
    if (new File(fn).exists()) {
        return true;
    }

    fn = name.contains("/") ? name + Cnst.FORM_EXT + ".xml"
       : Cnst.CONF_PACK +"/"+ name + Cnst.FORM_EXT + ".xml";
    return CoreRoster.getResourceModified(fn) > 0;
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
