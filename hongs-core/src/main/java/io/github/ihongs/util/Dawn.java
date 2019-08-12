package io.github.ihongs.util;

import io.github.ihongs.Core;
import io.github.ihongs.HongsExemption;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.parser.ContainerFactory;

import java.io.Reader;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.List;
import java.util.Date;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.LinkedHashMap;

/**
 * JSON格式工具
 *
 * <p>
 * 支持将 <b>数组,集合框架,基础类型</b> 的数据转换为 JSON 字符串,
 * 反向解析 JSON 字符串到 Java 集合框架对象; 采用 org.json.simple 完成
 * </p>
 *
 * <p>
 顺便说说为什么不采用第3方的 JSON 库:
 最开始用 org.json, 还不错, 可惜在解析 JSON 时会解析成他自身的对象而不是 Java 集合框架对象;
 后来采用 org.json.simple , 也很好, 但是不支持 Set , 需要修改其源码将 List 改成 Collection;
 考虑到我有一个 Dump 类, 用于调试输出基础类型和集合对象, 其实现与 JSON 大同小异,
 故将其修改成该 Dawn 类; 但是 JSON 的解析太麻烦, 就还是调 org.json.simple 好了 ;
 </p>
 *
 * <h3>异常代码</h3>
 * <pre>
 * 0x1150 解析JSON数据失败
 * 0x1151 写入JSON数据失败
 * </pre>
 *
 * @author Hongs
 */
public final class Dawn
{

  /**
   * 将JSON字符流解析成Java对象
   * @param str JSON字符流
   * @return 基础类型,集合框架
   */
  public static Object toObject(Reader str)
  {
    try
    {
      return new JSONParser().parse(str,CF);
    }
    catch (ParseException | IOException ex)
    {
      throw new HongsExemption(0x1150, "Can not parse data by json", ex);
    }
  }

  /**
   * 将JSON字符串解析成Java对象
   * @param str JSON字符串
   * @return 基础类型,集合框架
   */
  public static Object toObject(String str)
  {
    try
    {
      return new JSONParser().parse(str,CF);
    }
    catch (ParseException ex)
    {
      throw new HongsExemption(0x1150, "Can not parse data by json", ex);
    }
  }

  /**
   * 将Java对象转换为JSON字符串
   * @param obj 基础类型,集合框架,数组
   * @param compact 紧凑模式
   * @return JSON字符串
   */
  public static String toString(Object obj, boolean compact)
  {
    StringBuilder out = new StringBuilder();
    Dawn.append(out, obj, compact);
    return out.toString();
  }

  /**
   * 将Java对象转换为JSON字符串
   * @param obj 基础类型,集合框架,数组
   * @return JSON字符串
   */
  public static String toString(Object obj)
  {
    StringBuilder out = new StringBuilder();
    Dawn.append(out, obj);
    return out.toString();
  }

  /**
   * 将Java文本转义为JSON字符串
   * @param str 待JSON转义的Java字符串
   * @return 不含双引号
   */
  public static String doEscape(String str)
  {
    StringBuilder out = new StringBuilder();
    Dawn.excape(out, str);
    return out.toString();
  }

  /**
   * 将Java对象输出到指定输出流
   * 非开发环境将会启用紧凑模式
   * @param obj
   * @param out
   */
  public static void append(Appendable out, Object obj)
  {
    Dawn.append(out, obj, 0 == Core.DEBUG);
  }

  /**
   * 将Java对象输出到指定输出流
   * @param out
   * @param obj
   * @param compact 紧凑模式
   */
  public static void append(Appendable out, Object obj, boolean compact)
  {
    try
    {
      Dawn.append(out, compact ? null : "" , null, obj, false);
    }
    catch (IOException ex)
    {
      throw new HongsExemption(0x1151, "Can not write data for json", ex);
    }
  }

  //** 操作方法 **/

  private static void append(Appendable sb, String pre, Object key, Object val, boolean hasNext) throws IOException
  {
    if (pre != null)
    {
      sb.append(pre);
    }

    /** 键 **/

    if (key != null)
    {
      sb.append('"');
            Dawn.escape(sb, String.valueOf(key));
      sb.append('"');
      sb.append(':');
    }

    /** 值 **/

    if (val == null)
    {
      sb.append("null");
    }
    else if (val instanceof Object[])
    {
      append(sb, pre, (Object[]) val);
    }
    else if (val instanceof Iterator)
    {
      append(sb, pre, (Iterator) val);
    }
    else if (val instanceof Enumeration)
    {
      append(sb, pre, (Enumeration) val);
    }
    else if (val instanceof Collection)
    {
      append(sb, pre, (Collection) val);
    }
    else if (val instanceof Dictionary)
    {
      append(sb, pre, (Dictionary) val);
    }
    else if (val instanceof Map)
    {
      append(sb, pre, (Map) val);
    }
    else if (val instanceof Boolean)
    {
      sb.append(val.toString( ));
    }
    else if (val instanceof Number )
    {
      sb.append(Syno.toNumStr((Number) val));
    }
    else if (val instanceof  Date  )
    {
      sb.append(Syno.toNumStr(((Date ) val).getTime()));
    }
    else if (val instanceof Serializable
      &&  ! (val instanceof CharSequence)
      &&  ! (val instanceof Character)
      &&  ! (val instanceof Throwable)  )
    {
      append(sb, pre, val); // 反射钻取之
    }
    else
    {
      sb.append('"');
            Dawn.escape(sb, val.toString());
      sb.append('"');
    }

    if (hasNext)
    {
      sb.append(',');
    }

    if (pre != null
    && !pre.isEmpty( ))
    {
      sb.append("\r\n");
    }
  }

  private static void append(Appendable sb, String pre, Object[] arr) throws IOException
  {
    String pra;

    sb.append("[");
        pra  = pre;
    if (pre != null)
    {
      sb.append("\r\n");
      pra = pre + "\t" ;
    }

    for (int i = 0, j = arr.length; i < j ; i ++)
    {
      append(sb, pra, null, arr[i], i < j - 1   );
    }

    if (pre != null)
    {
      sb.append(pre);
    }
    sb.append("]");
  }

  private static void append(Appendable sb, String pre, Iterator itr) throws IOException
  {
    String pra;

    sb.append("[");
        pra  = pre;
    if (pre != null)
    {
      sb.append("\r\n");
      pra = pre + "\t" ;
    }

    boolean hasNext = itr.hasNext();
    while ( hasNext )
    {
      Object obj = itr.next();
      hasNext = itr.hasNext();
      append(sb, pra, null, obj, hasNext);
    }

    if (pre != null)
    {
      sb.append(pre);
    }
    sb.append("]");
  }

  private static void append(Appendable sb, String pre, Enumeration enu) throws IOException
  {
    String pra;

    sb.append("[");
        pra  = pre;
    if (pre != null)
    {
      sb.append("\r\n");
      pra = pre + "\t" ;
    }

    boolean hasNext = enu.hasMoreElements();
    while ( hasNext )
    {
      Object obj = enu.nextElement( );
      hasNext = enu.hasMoreElements();
      append(sb, pra, null, obj, hasNext);
    }

    if (pre != null)
    {
      sb.append(pre);
    }
    sb.append("]");
  }

  private static void append(Appendable sb, String pre, Collection col) throws IOException
  {
    String pra;

    sb.append("[");
        pra  = pre;
    if (pre != null)
    {
      sb.append("\r\n");
      pra = pre + "\t" ;
    }

    Iterator itr = col.iterator(  );
    boolean hasNext = itr.hasNext();
    while ( hasNext )
    {
      Object obj = itr.next();
      hasNext = itr.hasNext();
      append(sb, pra, null, obj, hasNext);
    }

    if (pre != null)
    {
      sb.append(pre);
    }
    sb.append("]");
  }

  private static void append(Appendable sb, String pre, Dictionary dic) throws IOException
  {
    String pra;

    sb.append("{");
        pra  = pre;
    if (pre != null)
    {
      sb.append("\r\n");
      pra = pre + "\t" ;
    }

    Enumeration enu = dic.keys( );
    boolean hasNext = enu.hasMoreElements();
    while ( hasNext )
    {
      Object key = enu.nextElement( );
      hasNext = enu.hasMoreElements();
      Object val = dic.get(key);
      if (key == null) key = "";
      append(sb, pra, key, val, hasNext);
    }

    if (pre != null)
    {
      sb.append(pre);
    }
    sb.append("}");
  }

  private static void append(Appendable sb, String pre, Map map) throws IOException
  {
    String pra;

    sb.append("{");
        pra  = pre;
    if (pre != null)
    {
      sb.append("\r\n");
      pra = pre + "\t" ;
    }

    Iterator itr = map.entrySet().iterator();
    boolean hasNext = itr.hasNext();
    while ( hasNext )
    {
      Map.Entry obj = (Map.Entry) itr.next();
      Object val = obj.getValue();
      Object key = obj.getKey(  );
      hasNext = itr.hasNext(  );
      if (key == null) key = "";
      append(sb, pra, key, val, hasNext);
    }

    if (pre != null)
    {
      sb.append(pre);
    }
    sb.append("}");
  }

    private static void append(Appendable sb, String pre, Object obj) throws IOException {
        String  pra;
        Field[] fds;
        Class   cls = obj.getClass();

        sb.append("{");
            pra  = pre;
        if (pre != null) {
            sb.append("\r\n");
            pra = pre + "\t" ;
        }

        // 读取公共属性
        fds = cls.getFields();
        for (int i = 0, j = fds.length; i < j; i ++) {
            try {
                Field fld = fds[i];
                int   mod = fld.getModifiers();
                if (Modifier.isTransient(mod )
                ||  Modifier.isStatic(mod )
                ||  Modifier.isFinal (mod)) {
                    continue;
                }
                String key = fld.getName();
                Object val = fld.get (obj);
                append(sb, pra, key, val, i < j - 1);
            } catch (
              IllegalArgumentException
              | IllegalAccessException ex) {
                throw  new IOException(ex);
            }
        }

        // 读取非公属性
        fds = cls.getDeclaredFields();
        for (int i = 0, j = fds.length; i < j; i ++) {
            try {
                Field fld = fds[i];
                int   mod = fld.getModifiers();
                if (Modifier.isTransient(mod )
                ||  Modifier.isPublic(mod )
                ||  Modifier.isStatic(mod )
                ||  Modifier.isFinal (mod)) {
                    continue;
                }
                fld.setAccessible ( true );
                String key = fld.getName();
                Object val = fld.get (obj);
                append(sb, pra, key, val, i < j - 1);
            } catch (
              IllegalArgumentException
              | IllegalAccessException ex) {
                throw  new IOException(ex);
            }
        }

        if (pre != null) {
            sb.append(pre);
        }
        sb.append("}");
    }

    private static void excape(Appendable sb, String s) {
        try {
            if (s == null) {
                sb.append("");
            } else {
                escape(sb, s);
            }
        }
        catch (IOException e)  {
            throw new HongsExemption(0x1151, "Can not write data for json", e);
        }
    }

    private static void escape(Appendable sb, String s) throws IOException {
        for (int i = 0, j = s.length(); i < j; i++ ) {
            char c = s.charAt(i);
            switch (c) {
                case '"' :
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '/' :
                    sb.append("\\/");
                    break;
                default:
                    //Reference: http://www.unicode.org/versions/Unicode5.1.0/
                    if ((c >= '\u0000' && c <= '\u001F')
                    ||  (c >= '\u007F' && c <= '\u009F')
                    ||  (c >= '\u2000' && c <= '\u20FF')) {
                        String  n = Integer.toHexString(c).toUpperCase();
                            sb.append("\\u");
                        for(int k = 0, l = 4 - n.length( ); k < l; k ++) {
                            sb.append( '0' );
                        }
                        sb.append(n);
                    } else {
                        sb.append(c);
                    }
            }
        }
    }

    private static final ContainerFactory CF = new ContainerFactory() {
        @Override
        public Map createObjectContainer() {
            return new LinkedHashMap();
        }
        @Override
        public List creatArrayContainer() {
            return new ArrayList();
        }
    };

  //** 编码 **/
  /*
  private final static String[] hex = {
      "00","01","02","03","04","05","06","07","08","09","0A","0B","0C","0D","0E","0F",
      "10","11","12","13","14","15","16","17","18","19","1A","1B","1C","1D","1E","1F",
      "20","21","22","23","24","25","26","27","28","29","2A","2B","2C","2D","2E","2F",
      "30","31","32","33","34","35","36","37","38","39","3A","3B","3C","3D","3E","3F",
      "40","41","42","43","44","45","46","47","48","49","4A","4B","4C","4D","4E","4F",
      "50","51","52","53","54","55","56","57","58","59","5A","5B","5C","5D","5E","5F",
      "60","61","62","63","64","65","66","67","68","69","6A","6B","6C","6D","6E","6F",
      "70","71","72","73","74","75","76","77","78","79","7A","7B","7C","7D","7E","7F",
      "80","81","82","83","84","85","86","87","88","89","8A","8B","8C","8D","8E","8F",
      "90","91","92","93","94","95","96","97","98","99","9A","9B","9C","9D","9E","9F",
      "A0","A1","A2","A3","A4","A5","A6","A7","A8","A9","AA","AB","AC","AD","AE","AF",
      "B0","B1","B2","B3","B4","B5","B6","B7","B8","B9","BA","BB","BC","BD","BE","BF",
      "C0","C1","C2","C3","C4","C5","C6","C7","C8","C9","CA","CB","CC","CD","CE","CF",
      "D0","D1","D2","D3","D4","D5","D6","D7","D8","D9","DA","DB","DC","DD","DE","DF",
      "E0","E1","E2","E3","E4","E5","E6","E7","E8","E9","EA","EB","EC","ED","EE","EF",
      "F0","F1","F2","F3","F4","F5","F6","F7","F8","F9","FA","FB","FC","FD","FE","FF"
  };

  private final static byte[] val = {
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,0x08,0x09,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x0A,0x0B,0x0C,0x0D,0x0E,0x0F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,
      0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F,0x3F
  };

  public static String encode(String str) {
    StringBuilder sb = new StringBuilder();
    int len = str.length();
    int i = 0;
    while (i < len) {
      int ch = str.charAt(i);
      if (ch == ' ') {                        // space : map to '+'
        sb.append('+');
      } else if ('0' <= ch && ch <= '9') {    // '0'..'9' : as it was
        sb.append((char)ch);
      } else if ('A' <= ch && ch <= 'Z') {    // 'A'..'Z' : as it was
        sb.append((char)ch);
      } else if ('a' <= ch && ch <= 'z') {    // 'a'..'z' : as it was
        sb.append((char)ch);
      } else if (ch == '-' || ch == '_'       // unreserved : as it was
        || ch == '.' || ch == '!'
        || ch == '~' || ch == '*'
        || ch == '(' || ch == ')'
        || ch == '\'') {
        sb.append((char)ch);
      } else if (ch <= 0x007F) {              // ASCII : map to %XX
        sb.append('%');
        sb.append(hex[ch]);
      } else {                                // Unicode : map to %uXXXX
        sb.append('%');
        sb.append('u');
        sb.append(hex[(ch >>> 8)]);
        sb.append(hex[(0x00FF & ch)]);
      }
      i++;
    }
    return sb.toString();
  }

  public static String decode(String str) {
    StringBuilder sb = new StringBuilder();
    int len = str.length();
    int i = 0;
    while (i < len) {
      int ch = str.charAt(i);
      if (ch == '+') {                        // + : map to ' '
        sb.append(' ');
      } else if ('0' <= ch && ch <= '9') {    // '0'..'9' : as it was
        sb.append((char)ch);
      } else if ('A' <= ch && ch <= 'Z') {    // 'A'..'Z' : as it was
        sb.append((char)ch);
      } else if ('a' <= ch && ch <= 'z') {    // 'a'..'z' : as it was
        sb.append((char)ch);
      } else if (ch == '-' || ch == '_'       // unreserved : as it was
        || ch == '.' || ch == '!'
        || ch == '~' || ch == '*'
        || ch == '(' || ch == ')'
        || ch == '\'') {
        sb.append((char)ch);
      } else if (ch == '%') {
        int cint = 0;
        if ('u' != str.charAt(i+1)) {         // %XX : map to ASCII(XX)
          cint = (cint << 4) | val[str.charAt(i+1)];
          cint = (cint << 4) | val[str.charAt(i+2)];
          i+=2;
        } else {                              // %uXXXX : map to Unicode(XXXX)
          cint = (cint << 4) | val[str.charAt(i+2)];
          cint = (cint << 4) | val[str.charAt(i+3)];
          cint = (cint << 4) | val[str.charAt(i+4)];
          cint = (cint << 4) | val[str.charAt(i+5)];
          i+=5;
        }
        sb.append((char)cint);
      }
      i++;
    }
    return sb.toString();
  }
  */

}
