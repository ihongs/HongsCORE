package io.github.ihongs.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.NumberFormat;

/**
 * 常用文本工具
 *
 * <p>
 * 用于引用/编码或清理文本, 转换数值进制等
 * 2016/02/16 才发现其实 Java 早已提供 2~36 进制的转换方法,
 * 如: Long.valueOf(123, 36); Long.parseLong("123xyz", 36);
 * </p>
 *
 * @author Hongs
 */
public final class Syno
{

  private static final NumberFormat NS_FMT = NumberFormat.getInstance();
  static {
      NS_FMT.setGroupingUsed(false);
  }

  /**
   * 默认情况大数字转字符串会用科学计数法
   * 故用 NumberFormat 格式化成普通数字串
   * @param num
   * @return
   */
  public static String toNumStr(Number num) {
      return  NS_FMT.format ( num );
  }

  //** 进制 **/

  /**
   * 符号转换表
   * <pre>
   * 所用字符集: 0-9A-Za-z
   * 随想:
   * 这拉丁字符实在太乱了, 字符高矮不齐, 单词长短不一;
   * 对于一个中国人, 早已习惯中文的整齐, 看着有些别扭;
   * 不过发现了16进制的一个特点, 从a到f, 偶数矮奇数高;
   * 正所谓: 阳中有阴, 阴中有阳, 呵呵^________________^
   * </pre>
   */
  private static final char[] HEX_36 = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
    'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
    'U', 'V', 'W', 'X', 'Y', 'Z'
  };
  private static final char[] HEX_26 = {
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
    'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
    'U', 'V', 'W', 'X', 'Y', 'Z'
  };

  /**
   * 十进制转其他进制
   * 进制为arr的长度, 左边不能是arr首位, 如36进制(数字加字母), 35是Z, 36是10
   * @param num 待转数字 0~MAX
   * @param arr 转换序列
   * @return 指定进制的数字字符串
   */
  public static String toXHex(long num, char[] arr)
  {
    StringBuilder str = new StringBuilder();
    int x = arr.length;
    int i;

    if   (num == 0)
          str.insert(0, arr[0]);
    while(num >= 1)
    {
          i   = (int) ( num%x );
          num = (long)( num/x );
          str.insert(0, arr[i]);
    }

    return str.toString();
  }

  /**
   * 十进制转其他进制
   * 进制为arr的长度, 左边可以是arr首位, 如26进制(字母), 1是A, 26是Z, 27是AA
   * @param num 待转数字 1~MAX
   * @param arr 转换序列
   * @return 指定进制的数字字符串
   */
  public static String toYHex(long num, char[] arr)
  {
    StringBuilder str = new StringBuilder();
    int x = arr.length;
    int i;

    while(num >= 1)
    {     num -= 1;
          i   = (int) ( num%x );
          num = (long)( num/x );
          str.insert(0, arr[i]);
    }

    return str.toString();
  }

  /**
   * 十进制转36进制(0~Z)
   * @param num 0~MAX
   * @return 36进制串
   */
  public static String to36Hex(long num)
  {
    return Syno.toXHex(num, HEX_36);
  }

  /**
   * 十进制转26进制(A-Z)
   * @param num 1~MAX
   * @return 26进制串
   */
  public static String to26Hex(long num)
  {
    return Syno.toYHex(num, HEX_26);
  }

  public static long asXHex(String str, char[] arr) {
      Map<Character, Integer> map = new HashMap();
      char[] chs = str.toCharArray();
      long   num = 0;
      int x = arr.length;
      int y = chs.length;
      int i;
      for (i = 0; i < x; i ++) {
          map.put( arr[i], i );
      }
      for (i = 0; i < y; i ++) {
          num += Math.pow(x, y - i - 1) * map.get(chs[i]);
      }
      return num;
  }

  public static long asYHex(String str, char[] arr) {
      Map<Character, Integer> map = new HashMap();
      char[] chs = str.toCharArray();
      long   num = 0;
      int x = arr.length;
      int y = chs.length;
      int i;
      for (i = 0; i < x; i ++) {
          map.put( arr[i], i );
      }
      for (i = 0; i < y; i ++) {
          num += Math.pow(y - i + 1, x) * (map.get(chs[i]) + 1);
      }
      return num;
  }

  public static long as36Hex(String str) {
      return asXHex(str, HEX_36);
  }

  public static long as26Hex(String str) {
      return asYHex(str, HEX_26);
  }

  //** 转义 **/

  /**
   * 转义正则符
   * jdk1.5 中 Pattern.quote( s ) 替换有问题
   * 故对正则符等转换成Unicode形式来使之正常
   * 2015/9/6: 因现已不考虑 jdk1.5 及以下版本, 恢复使用 Pattern.quote
   * @param str 原字符串
   * @return 转换了的字符串
   */
  private static String escapeRegular(String str)
  {
    /*
    StringBuilder sb = new StringBuilder();
         char[]   cs = str.toCharArray(  );
    for (char c : cs)
    {
      sb.append(String.format("\\u%04x", (int)c));
    }
    return sb.toString();
    */
    return Pattern.quote(str);
  }

  /**
   * 转义转义符
   * jdk1.5 中缺少 Matcher.quoteReplacement
   * 故对转义符等前面加斜杠进行转义使之正常
   * 2015/9/6: 因现已不考虑 jdk1.5 及以下版本, 恢复使用 Matcher.quoteReplacement
   * @param str 原字符串
   * @return 转换了的字符串
   */
  private static String escapeReplace(String str)
  {
    /*
    return str.replaceAll("(\\\\|\\$)", "\\\\$1");
    */
    return Matcher.quoteReplacement(str);
  }

  /**
   * 引用(转义指定字符)
   * @param str 待引用源字符串
   * @param esc 被转义字符
   * @param sym 转义字符
   * @return 已转义特定字符
   */
  public static String escape(String str, String esc, String sym)
  {
    if (sym.length() == 1 && !esc.contains(sym))
    {
        esc += sym;
    }

    /**
     * 转换转义符里的特殊字符,
     * 偶数个转义符后加转义符.
     */

    String esc2 = Syno.escapeRegular(esc);
    String sym2 = Syno.escapeReplace(sym);

    return str.replaceAll("(["+esc2+"])", sym2+"$1");
  }
  public static String escape(String str, String esc)
  {
    return Syno.escape(str, esc, "\\");
  }
  public static String escape(String str)
  {
    return Syno.escape(str, "'\"", "\\");
  }

  /**
   * 反引用(反转义指定字符)
   * @param str 待反引用源字符串
   * @param esc 被转义字符
   * @param sym 转义字符
   * @return 已反转义特定字符
   */
  public static String resume(String str, String esc, String sym)
  {
    if (sym.length() == 1 && !esc.contains(sym))
    {
        esc += sym;
    }

    /**
     * 把两个转义符换成一个,
     * 把单个转义符全删除掉.
     */

    String esc2 = Syno.escapeRegular(esc);
    String sym2 = Syno.escapeRegular(sym);

    return str.replaceAll(sym2+"(["+esc2+"])", "$1");
  }
  public static String resume(String str, String esc)
  {
    return Syno.resume(str, esc, "\\");
  }
  public static String resume(String str)
  {
    return Syno.resume(str, "'\"", "\\");
  }

  //** 缩进 **/

  /**
   * 缩进
   * @param str
   * @param ind
   * @return 缩进后的文本
   */
  public static String indent(String str, String ind) {
      ind = /**/escapeReplace(ind);
      return  Pattern.compile("^", Pattern.MULTILINE)
                     .matcher(str).replaceAll( ind  );
  }

  /**
   * 缩进
   * @param str
   * @return 缩进后的文本
   */
  public static String indent(String str) {
      return  indent(str, "\t");
  }

  /**
   * 反缩进
   * @param str
   * @param ind
   * @return 反缩进后的文本
   */
  public static String undent(String str, String ind) {
      ind = "^"+escapeRegular(ind);
      return  Pattern.compile(ind, Pattern.MULTILINE)
                     .matcher(str).replaceAll(  ""  );
  }

  /**
   * 反缩进
   * @param str
   * @return 反缩进后的文本
   */
  public static String undent(String str) {
      return  undent(str, "\t");
  }

  //** 注入 **/

  // 偶数个转义符$单词或{文本}
  private static final Pattern IJ_PAT = Pattern.compile("((?:[\\\\][\\\\])*)\\$(?:(\\w+)|\\{(.*?)\\})");

  /**
   * 注入参数
   * @param str
   * @param vars
   * @return 注入后的文本
   */
  public static String inject(String str, Map vars) {
      Matcher matcher = IJ_PAT.matcher(str);
      StringBuffer sb = new StringBuffer( );
      Object       ob;
      String       st;

      while  ( matcher.find() ) {
              st  = matcher.group(2);
          if (st == null) {
              st  = matcher.group(3);
          }

              ob  = vars.get(  st  );
          if (ob != null) {
              st  = ob.toString (  );
          } else {
              st  = "";
          }

          st = matcher.group(1) + st;
          st = Matcher.quoteReplacement(st);
          matcher.appendReplacement(sb, st);
      }
      matcher.appendTail(sb);

      return sb.toString(  );
  }

  /**
   * 注入参数
   * @param str
   * @param vars
   * @return 注入后的文本
   */
  public static String inject(String str, List vars)
  {
    /**
     * 将语句中替换$n或${n}为指定的文字, n从0开始
     */
      Map rep2 = new HashMap();
      for (int i = 0; i < vars.size(); i ++) {
          rep2.put(String.valueOf(i) , vars.get(i) );
      }
      return inject(str, rep2);
  }

  /**
   * 注入参数
   * @param str
   * @param vars
   * @return  注入后的文本
   */
  public static String inject(String str, String... vars)
  {
    /**
     * 将语句中替换$n或${n}为指定的文字, n从0开始
     */
      Map rep2 = new HashMap();
      for (int i = 0; i < vars.length; i ++) {
          rep2.put(String.valueOf(i) , vars[i] );
      }
      return inject(str, rep2);
  }

  //** 清理 **/

  /**
   * 清理首尾空格
   * @param str
   * @return
   */
  public static String stripEnds(String str)
  {
    Pattern pat;
    pat = Pattern.compile("^[\\h\\v]+" );
    str = pat.matcher(str).replaceAll("" );
    pat = Pattern.compile( "[\\h\\v]+$");
    str = pat.matcher(str).replaceAll("" );
    return  str;
  }

  /**
   * 清理标签代码
   * 该实现并不严谨, 不建议用于处理含 CDATA 等的 XML 文档
   * @param str
   * @return 新串
   */
  public static String stripTags(String str)
  {
    Pattern pat;
    pat = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    str = pat.matcher(str).replaceAll("" );
    pat = Pattern.compile("<[^>]*?>");
    str = pat.matcher(str).replaceAll(" ");
    pat = Pattern.compile("&[^&;]*;");
    str = pat.matcher(str).replaceAll(" ");
    return  str;
  }

  /**
   * 清理脚本代码
   * 该实现并不严谨, 仅清理可被利用的 iframe/script/style
   * @param str
   * @return 新串
   */
  public static String stripCros(String str)
  {
    Pattern pat;
    pat = Pattern.compile("(<iframe.*?/>|<iframe.*?>.*?</iframe>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    str = pat.matcher(str).replaceAll("" );
    pat = Pattern.compile("(<script.*?/>|<script.*?>.*?</script>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    str = pat.matcher(str).replaceAll("" );
    pat = Pattern.compile("(<style.*?/>|<style.*?>.*?</style>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    str = pat.matcher(str).replaceAll("" );
    pat = Pattern.compile("(\\son\\w+=('.*?'|\".*?\"))", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    str = pat.matcher(str).replaceAll("" );
    return  str;
  }

  //** 格式 **/

  /**
   * 友好的时间格式
   * @param time 毫秒数
   * @return 最大到w 注意: 已带单位, 毫秒作为小数部分
   */
  public static String humanTime(long time)
  {
    StringBuilder sb = new StringBuilder( );
    int item;

    item = (int) Math.floor(time / 604800000);
    if (item > 0) {  time = time % 604800000;
        sb.append(item).append("w");
    }

    item = (int) Math.floor(time / 86400000);
    if (item > 0) {  time = time % 86400000;
        sb.append(item).append("d");
    } else
    if (time > 0 && 0 < sb.length()) {
        sb.append("0d");
    }

    item = (int) Math.floor(time / 3600000);
    if (item > 0) {  time = time % 3600000;
        sb.append(item).append("h");
    } else
    if (time > 0 && 0 < sb.length()) {
        sb.append("0h");
    }

    item = (int) Math.floor(time / 60000);
    if (item > 0) {  time = time % 60000;
        sb.append(item).append("m");
    } else
    if (time > 0 && 0 < sb.length()) {
        sb.append("0m");
    }

    float last = (float) time/1000 ;
    if (last > 0 || 0== sb.length()) {
        sb.append(last).append("s");
    }

    return sb.toString();
  }

  /**
   * 友好的容量格式
   * @param size 容量数
   * @return 最大到T 注意: 不带单位, B或b等请自行补充
   */
  public static String humanSize(long size)
  {
    StringBuilder sb = new StringBuilder( );
    int item;

    item = (int) Math.floor(size / 0x10000000000L);
    if (item > 0) {  size = size % 0x10000000000L;
        sb.append(item).append("T");
    }

    item = (int) Math.floor(size / 0x40000000);
    if (item > 0) {  size = size % 0x40000000;
        sb.append(item).append("G");
    } else
    if (size > 0 && 0 < sb.length()) {
        sb.append("0G");
    }

    item = (int) Math.floor(size / 0x100000);
    if (item > 0) {  size = size % 0x100000;
        sb.append(item).append("M");
    } else
    if (size > 0 && 0 < sb.length()) {
        sb.append("0M");
    }

    item = (int) Math.floor(size / 0x400);
    if (item > 0) {  size = size % 0x400;
        sb.append(item).append("K");
    } else
    if (size > 0 && 0 < sb.length()) {
        sb.append("0K");
    }

    if (size > 0 || 0== sb.length()) {
        sb.append(size);
    }

    return sb.toString();
  }

  //** 路径 **/

  /**
   * 防止单个目录下文件过多而无法存放
   * 将名称拆解成路径, 按名称长度调用 splitPn36,splitPn16
   * @param name
   * @return
   */
  public static String splitPath(String name) {
      int len  =  name.length( );
      if (len >= 16 && len < 32) {
          return splitPn36(name);
      } else {
          return splitPn16(name);
      }
  }

  /**
   * 防止单个目录下文件过多而无法存放
   * 将36进制名称(如uid)拆解成路径
   * @param name
   * @return
   */
  public static String splitPn36(String name) {
      int i  = 0 ;
      int j  = name.length();
      if (j >= 8 ) {
          j  = 8 ;
      } else {
          j  = j - ( j % 2 );
      }
      StringBuilder path = new StringBuilder();
      for (; i < j; i += 2 ) {
          path.append(name.substring(i, i+ 2))
              .append( "/" ); // 不用 File.separator, 规避 Windows 下造成困扰
      }   path.append(name );
      return path.toString();
  }

  /**
   * 防止单个目录下文件过多而无法存放
   * 将16进制名称(如md5)拆解成路径
   * @param name
   * @return
   */
  public static String splitPn16(String name) {
      int i  = 0 ;
      int j  = name.length();
      if (j >= 32) {
          j  = 32;
      } else {
          j  = j - ( j % 4 );
      }
      StringBuilder path = new StringBuilder();
      for (; i < j; i += 4 ) {
          path.append(name.substring(i, i+ 4))
              .append( "/" ); // 不用 File.separator, 规避 Windows 下造成困扰
      }   path.append(name );
      return path.toString();
  }

}
