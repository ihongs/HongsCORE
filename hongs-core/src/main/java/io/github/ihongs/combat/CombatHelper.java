package io.github.ihongs.combat;

import io.github.ihongs.Core;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Inst;
import io.github.ihongs.util.Synt;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 外壳程序助手类
 *
 * 此处的静态变量 IN,OUT,ERR 等可以单独设置,
 * 任务结束时执行 Core.close 会将其全部重置.
 *
 * @author Hongs
 */
public class CombatHelper
{

  /**
   * 输入接口
   * 默认为 System.in
   */
  public static final Core.Valuable<InputStream> IN  = new Core.Valuable("!SYSTEM_IN" ) {
    @Override
    protected InputStream initialValue() {
      return System.in ;
    }
  };

  /**
   * 输出接口
   * 默认为 System.out
   */
  public static final Core.Valuable<PrintStream> OUT = new Core.Valuable("!SYSTEM_OUT") {
    @Override
    protected PrintStream initialValue() {
      return System.out;
    }
  };

  /**
   * 错误接口
   * 默认为 System.err
   */
  public static final Core.Valuable<PrintStream> ERR = new Core.Valuable("!SYSTEM_ERR") {
    @Override
    protected PrintStream initialValue() {
      return System.err;
    }
  };

  /**
   * 环境变量
   * 默认同 System.getenv
   */
  public static final Core.Variable<Map<String, String>> ENV = new Core.Variable("!SYSTEM_ENV") {
    @Override
    protected Map<String, String> initialValue() {
      return new Env();
    }
  };

  /**
   * 环境变量容器
   * 缺省从系统取
   */
  private static final class Env extends HashMap<String, String> {
    @Override
    public String get(Object key) {
      String kay = Synt.asString(key);
      if (super.containsKey(kay)) {
          return super.get (key);
      }
      return System.getenv (kay);
    }
    @Override
    public boolean containsKey(Object key) {
      String kay = Synt.asString(key);
      if (super.containsKey(kay)) {
          return true ;
      }
      return System.getenv (kay) != null;
    }
  }

  /**
   * 命令行宽度
   * ENV 重设后务必清除
   */
  public static final Core.Variable<Integer> LW = new Core.Variable("!PRINTLW") {
     @Override
    protected Integer initialValue() {
      return Synt.declare(ENV.get().get("COLUMNS"), 80);
    }
  };

  /**
   * 单行输出中
   */
  public static final Core.Valuable<Boolean> LR = new Core.Valuable("!PRINTLR") {
    @Override
    protected Boolean initialValue() {
      return false;
    }
  };

  /**
   * 参数处理正则
   */
  private static final Pattern RP = Pattern.compile("^(.+?)(?:([=:+*])([sifb])?)?$");
  private static final Pattern BP = Pattern.compile("^(false|true|yes|no|y|n|1|0)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern FP = Pattern.compile("^\\d+(\\.\\d+)?$");
  private static final Pattern IP = Pattern.compile("^\\d+$");

  /**
   * 错误消息集合
   */
  private static final String[]
    GETERRS = {
      "Can not parse rule '{}'",
      "Option '{}' is required",
      "Option '{}' must be specified value",
      "Option '{}' can only have one value",
      "Value for option '{}' must be int",
      "Value for option '{}' must be float",
      "Value for option '{}' must be boolean",
      "Unrecognized option '{}'",
      "Unupport surplus options"
    };

  /**
   * 解析参数
   *
   * <p>本函数遵循 Perl 的 Getopt::Long 解析规则, 同时也遵循 Configure 类似的参数的解析规则.</p>
   * <pre>
   * 规则描述:
   *    OPT[=:+*][sifb]
   *    后两部分省略等同 :b
   *    最后部分省略等同  s
   * 符号意义:
   *    = 必要参数
   *    : 可选参数
   *    + 一个或多个
   *    * 零个或多个
   *    s 字符串
   *    i 整数
   *    f 小数
   *    b 是或否
   * 举例说明:
   *    yes     等同 yes:b
   *    str=    等同 str=s
   *    yes:b   匹配 --yes 或 --yes=true
   *    str:s   匹配 --str abc 或 --str=abc, 可选
   *    num=i   匹配 --num 123 或 --num=123, 必要
   *    abc+s   匹配 --abc xxx yyy 或 --abc xxx --abc yyy
   * 特殊标识:
   *    !Anonymous  允许匿名多余参数, 如 cmd --opt xyz 12 34 567, 后面的 12,34,567 将作为数组放入空选项中
   *    !Undefined  允许未定义的参数, 如 cmd --opt xyz --abc 123, 后面的 abc 参数没有定义也将放入空选项中
   *    ?HELP TEXT  未给出任何参数时, 将会抛出带此帮助消息的异常, 需注意全为可选参数的请不要设置这个选项
   * 匿名及多余参数获取方法:
   *    String args2 = (String[]) opts.get(""); // 总是字符串数组的形式
   * </pre>
   *
   * @param args
   * @param chks
   * @return
   */
  public static Map<String, Object> getOpts(String[] args, String... chks)
  {
    Map<String, Object[]>  chkz = new HashMap();
    Map<String, Object  >  opts = new HashMap();
    List<String>        newArgs = new ArrayList();
    Set<String>         reqOpts = new LinkedHashSet();
    Set<String>         errMsgs = new LinkedHashSet();
    String hlp = null; // 命令使用帮助
    boolean vb = true; // 禁止匿名参数
    boolean ub = true; // 禁止未知参数

    for (String chk : chks) {
      if (chk.equals("!A") || chk.equals("!Anonymous")) {
          vb = false;
          continue;
      } else
      if (chk.equals("!U") || chk.equals("!Undefined")) {
          ub = false;
          continue;
      } else
      if (chk.startsWith("?")) {
          hlp = chk.substring(1);
          continue;
      }

      Matcher m = RP.matcher(chk);
      if (!m.find()) {
          errMsgs.add(GETERRS[0].replace("{}" , chk));
          continue;
      }

      String name = m.group(1);
      String sign = m.group(2);
      String type = m.group(3);

      if (sign == null) {
          sign = ":";
          type = "b";
      } else
      if (type == null) {
          type = "s";
      }

      if ("=".equals(sign) || "+".equals(sign)) {
        reqOpts.add(name);
      }

      chkz.put(name, new Object[] {sign.charAt(0), type.charAt(0)});
    }

    F:for (int i = 0; i < args.length; i++) {
        String arg = args[i];
        String key ;
        String val ;

        if (arg.startsWith( "--" )) {
            key = arg.substring (2);
            if (key.length( ) == 0) {
                continue;
            }

            int q = key.indexOf('=');
            if (q > 0) {
                val = key.substring (1+q);
                key = key.substring (0,q);

                if (chkz.containsKey(key)) {
                    Object[] chk = chkz.get(key);
                    char sign = (Character) chk[0];
                    char type = (Character) chk[1];

                    switch (type) {
                        case 'b':
                            if (!BP.matcher(val).matches()) {
                                errMsgs.add(GETERRS[6].replace("{}", key));
                                continue;
                            }
                            break;
                        case 'i':
                            if (!IP.matcher(val).matches()) {
                                errMsgs.add(GETERRS[4].replace("{}", key));
                                continue;
                            }
                            break;
                        case 'f':
                            if (!FP.matcher(val).matches()) {
                                errMsgs.add(GETERRS[5].replace("{}", key));
                                continue;
                            }
                            break;
                    }

                    if ('+' == sign || '*' == sign) {
                        List vals;
                        vals = (List) opts.get(key);
                        if (vals == null) {
                            vals  = new ArrayList();
                            opts.put(key, vals);
                        }
                        vals.add(val);
                    } else {
                        if ('=' == sign && val.isEmpty()) {
                            errMsgs.add(GETERRS[2].replace("{}", key));
                        } else
                        if (opts.containsKey(key ) ) {
                            errMsgs.add(GETERRS[3].replace("{}", key));
                        } else {
                            opts.put(key, val);
                        }
                    }
                } else if (ub) {
                    // 未知参数
                    errMsgs.add(GETERRS[7].replace("{}", key));
                } else {
                    newArgs.add(args[i]);
                }
            } else {
                if (chkz.containsKey(key)) {
                    Object[] chk = chkz.get (key);
                    char sign = (Character) chk[0];
                    char type = (Character) chk[1];
                    List vals = null;

                    if ('b' == type) {
                        opts.put(key, true);
                        continue;
                    }

                    if ('+' == sign || '*' == sign) {
                        vals = (List) opts.get(key);
                        if (vals == null) {
                            vals  = new ArrayList();
                            opts.put(key, vals);
                        }
                    }

                    while (i < args.length - 1) {
                        val = args [i + 1];
                        if (val.startsWith("--")) {
                            if (vals == null || vals.isEmpty()) { // 缺少取值
                                errMsgs.add(GETERRS[2].replace("{}", key));
                            }
                            break;
                        } else {
                            i ++ ;
                        }

                        switch (type) {
                            case 'i':
                                if (!IP.matcher(val).matches()) {
                                    errMsgs.add(GETERRS[4].replace("{}", key));
                                    continue;
                                }
                                break;
                            case 'f':
                                if (!FP.matcher(val).matches()) {
                                    errMsgs.add(GETERRS[5].replace("{}", key));
                                    continue;
                                }
                                break;
                        }

                        if ('+' == sign || '*' == sign) {
                            vals.add(val);
                        } else {
                            if ('=' == sign && val.isEmpty()) {
                                errMsgs.add(GETERRS[2].replace("{}", key));
                            } else
                            if (opts.containsKey(key ) ) {
                                errMsgs.add(GETERRS[3].replace("{}", key));
                            } else {
                                opts.put(key, val);
                            }
                            break;
                        }
                    }
                } else if (ub) {
                    // 未知参数
                    errMsgs.add(GETERRS[7].replace("{}", key));
                } else {
                    newArgs.add(args[i]);
                }
            }
        } else if (vb) {
            // 匿名参数
            errMsgs.add(GETERRS[8]);
        } else {
            newArgs.add(args[i]);
        }
    }

    for (String name : reqOpts) {
      if (! opts.containsKey(name)) {
        Set<String> err = new LinkedHashSet();
        err.add(GETERRS[1].replace("{}", name));
        err.addAll( errMsgs );
        errMsgs  =  err ;
      }
    }

    if (!errMsgs.isEmpty()) {
      StringBuilder err = new StringBuilder();
      for ( String  msg : errMsgs ) {
        err.append("\r\n\t").append(msg);
      }
      if  ( null != hlp ) {
        err.append("\r\n\t").append(hlp);
      }
      hlp = err.toString();

      throw new CruxExemption(256, hlp, hlp);
    } else if (hlp != null && args.length == 0) {
      throw new CruxExemption(257, hlp, hlp);
    }

    // 把剩余的参数放进去
    opts.put("", ! newArgs.isEmpty() ? newArgs.toArray(new String[0]) : new String[0]);

    return opts;
  }

  /**
   * 是否中止执行
   * @return 线程或任务被中止为 true
   */
  public static boolean aborted()
  {
    return Thread.interrupted() || Core.INTERRUPTED.get();
  }

  /**
   * 终止单行输出
   */
  public static void printed()
  {
    if ( LR.get() )
    {
      PrintStream out = OUT.get();
      out.println();
      out.flush(  );
      LR .set(null);
    }
  }

  /**
   * 单行输出
   *
   * 行宽为环境变量 COLUMNS,
   * 超过这个宽度的会被截取.
   * 别用制表符等非定长字符,
   * 别用中日韩等非半角字符,
   * 除非你确信长度不会超出.
   *
   * @param text
   */
  public static void printlr(CharSequence text)
  {
    int k  = LW.get(/***/);
    int l  = text.length();
    if (l >= k ) {
        l  = k - 1 ;
        text = text.subSequence(0, l);
    }

    StringBuilder sb = new StringBuilder(k + l + 4);

    // 覆写本行
    sb.append('\r');
    for(int i = 0; i < k; i ++)
    sb.append(' ' );

    // 加入内容
    sb.append('\r');
    sb.append(text);
    sb.append(' ' );

    PrintStream out = OUT.get();
    out.print(sb);
    out.flush(  );
    LR .set(true);
  }

  /**
   * 输出内容
   * @param text
   */
  public static void println(CharSequence text)
  {
    printed  (  );

    PrintStream out = OUT.get();
    out.println(text);
    out.flush(  );
  }

  /**
   * 输出内容
   * @param text
   */
  public static void println(String text)
  {
    printed  (  );

    PrintStream out = OUT.get();
    out.println(text);
    out.flush(  );
  }

  /**
   * 输出数据
   * @param data
   */
  public static void println(Object data)
  {
    printed  (  );

    PrintStream out = OUT.get();
    Dist.append(out,data,false);
    out.println();
    out.flush(  );
  }

  /**
   * 输出执行进度
   * 由于大部分的终端(命令行)默认宽度普遍为 80 个字符,
   * 进度长 10, 建议将 text 控制在 70, 一个中文占两位.
   * @param rate 进度比例, 0~1的浮点数
   * @param text 说明文本
   */
  public static void progres(float rate, String text)
  {
    StringBuilder sb = new StringBuilder();

    rate = Math.max(0f,Math.min(100f,100f * rate));
    new Formatter(sb).format("[%6.2f%%] " , rate );

    if (null != text)
    {
      sb.append(text);
    }

    printlr(sb);
  }

  /**
   * 输出执行进度
   * @param done 完成数量
   * @param tote 总条目数
   */
  public static void progres(int done, int tote)
  {
    if (tote > 0)
    {
      CombatHelper.progres( (float) done / tote, done+"/"+tote );
    } else
    if (done > 0)
    {
      CombatHelper.progres( -1f, done+"" );
    } else
    {
      CombatHelper.progres( -1f,  "..."  );
    }
  }

  /**
   * 输出执行用时
   * @param done 完成数量
   * @param tote 总条目数
   * @param time 已用时间
   */
  public static void progres(int done, int tote, long time)
  {
    if (tote > 0)
    {
      CombatHelper.progres( (float) done / tote, done+"/"+tote+" "+Inst.phrase(time) );
    } else
    if (done > 0)
    {
      CombatHelper.progres( -1f, done+" "+Inst.phrase(time) );
    } else
    {
      CombatHelper.progres( -1f,  "... " +Inst.phrase(time) );
    }
  }

  /**
   * @deprecated 改用 println(text)
   */
  public static void paintln(String text)
  {
    println(text);
  }

  /**
   * @deprecated 改用 println(data)
   */
  public static void preview(Object data)
  {
    println(data);
  }

  /**
   * @deprecated 改用 printed()
   */
  public static void progres()
  {
    printed();
  }

}
