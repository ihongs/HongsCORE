package io.github.ihongs.combat;

import io.github.ihongs.Core;
import io.github.ihongs.HongsExemption;
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
  public static final Core.Variable<InputStream> IN  = new Core.Variable("!SYSTEM_IN" ) {
    @Override
    protected InputStream initialValue() {
      return System.in ;
    }
  };

  /**
   * 输出接口
   * 默认为 System.out
   */
  public static final Core.Variable<PrintStream> OUT = new Core.Variable("!SYSTEM_OUT") {
    @Override
    protected PrintStream initialValue() {
      return System.out;
    }
  };

  /**
   * 错误接口
   * 默认为 System.err
   */
  public static final Core.Variable<PrintStream> ERR = new Core.Variable("!SYSTEM_ERR") {
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
   * 参数处理正则
   */
  private static final Pattern RP = Pattern.compile("^([\\w\\.\\-\\|]*)(=|:|\\+|\\*)([sifb]|\\/(.+)\\/(i)?( .*)?)$");
  private static final Pattern BP = Pattern.compile("^(false|true|yes|no|y|n|1|0)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern FP = Pattern.compile("^\\d+(\\.\\d+)?$");
  private static final Pattern IP = Pattern.compile("^\\d+$");

  /**
   * 错误消息集合
   */
  private static final String[]
    GETERRS = {
      "Can not parse rule '%chk'",
      "Option '%opt' is required",
      "Option '%opt' must be specified value",
      "Option '%opt' can only have one value",
      "Value for option '%opt' must be int",
      "Value for option '%opt' must be float",
      "Value for option '%opt' must be boolean",
      "Value for option '%opt' not matches: %mat",
      "Unrecognized option '%opt'",
      "Unupport anonymous options"
    };

  //** 参数相关 **/

  /**
   * 解析参数
   * 本函数遵循 Perl 的 Getopt::Long 解析规则
   * 同时也遵循 Configure 类似的参数的解析规则
   * @param args
   * @param chks
   * @return
   */
  public static Map<String, Object> getOpts(String[] args, String... chks)
  {
    Map<String, Object[]>  chkz = new HashMap();
    Map<String, Object> newOpts = new HashMap();
    List<String>        newArgs = new ArrayList();
    Set<String>         reqOpts = new LinkedHashSet();
    Set<String>         errMsgs = new LinkedHashSet();
    String hlp = null; // 命令使用帮助
    boolean vb = true; // 禁止匿名参数
    boolean ub = true; // 禁止未知参数

    for (String chk : chks) {
      Matcher m = RP.matcher(chk);

      if (!m.find()) {
        if (chk.startsWith("?") ) {
            hlp= chk.substring(1);
        } else
        if (chk.equals("!A") || chk.equals("!Anonymous")) {
            vb = false;
        } else
        if (chk.equals("!U") || chk.equals("!Undefined")) {
            ub = false;
        } else
        {
            errMsgs.add(GETERRS[0].replace("%chk" , chk));
        }
        continue;
      }

      String name = m.group(1);
      String sign = m.group(2);
      String type = m.group(3);

      if ("=".equals(sign) || "+".equals(sign)) {
        reqOpts.add(name);
      }

      if (type.startsWith("/")) {
        String  reg = m.group(4);
        String  mod = m.group(5);
        String  err = m.group(6);
        Pattern pat;
        if (mod != null) {
          pat = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);
        } else {
          pat = Pattern.compile(reg);
        }
        if (err != null) {
          err = err.trim();
        } else {
          err = GETERRS[7];
        }
        reg = "/"+reg+"/"+mod;
        chkz.put(name, new Object[] {sign.charAt(0),'r',pat,reg,err});
      } else {
        chkz.put(name, new Object[] {sign.charAt(0), type.charAt(0)});
      }
    }

    F:for (int i = 0; i < args.length; i++) {
        String name = args[i];

        if (name.startsWith( "--" )) {
            name = name.substring(2);
            if (name.length( ) == 0) {
                continue;
            }

            int q = name.indexOf('=');
            if (q > 0) {
                String arg ;
                Object val ;
                arg  = name.substring(1+q);
                name = name.substring(0,q);

                if (chkz.containsKey(name)) {
                    Object[] chk = chkz.get (name);
                    char sign = (Character) chk[0];
                    char type = (Character) chk[1];

                    switch (type) {
                        case 'b':
                            if (!BP.matcher(arg).matches()) {
                                errMsgs.add(GETERRS[6].replace("%opt", name));
                                continue;
                            }
                            val = Synt.asBool(arg);
                            break;
                        case 'i':
                            if (!IP.matcher(arg).matches()) {
                                errMsgs.add(GETERRS[4].replace("%opt", name));
                                continue;
                            }
                            val = Long.parseLong(arg);
                            break;
                        case 'f':
                            if (!FP.matcher(arg).matches()) {
                                errMsgs.add(GETERRS[5].replace("%opt", name));
                                continue;
                            }
                            val = Double.parseDouble(arg);
                            break;
                        case 'r':
                            Pattern rp  = (Pattern) chk[2];
                            String  reg = (String ) chk[3];
                            String  err = (String ) chk[4];
                            if (!rp.matcher(arg).matches()) {
                                errMsgs.add(err.replace("%mat", reg).replace("%opt", name));
                                continue;
                            }
                        default:
                            val = arg;
                    }

                    if ('+' == sign || '*' == sign) {
                        List vals;
                        vals = (List) newOpts.get(name);
                        if (vals == null) {
                            vals  = new ArrayList();
                            newOpts.put(name, vals);
                        }
                        vals.add(val);
                    } else {
                        if ('=' == sign && arg.isEmpty()) {
                            errMsgs.add(GETERRS[2].replace("%opt", name));
                        } else
                        if (newOpts.containsKey( name ) ) {
                            errMsgs.add(GETERRS[3].replace("%opt", name));
                        } else {
                            newOpts.put(name, val);
                        }
                    }
                } else if (ub) {
                    // 未知参数
                    errMsgs.add(GETERRS[8].replace("%opt", name));
                } else {
                    newArgs.add(args[i]);
                }
            } else {
                if (chkz.containsKey(name)) {
                    Object[] chk = chkz.get (name);
                    char sign = (Character) chk[0];
                    char type = (Character) chk[1];

                    Pattern rp = null;
                    String reg = null;
                    String err = null;
                    List  vals = null;

                    if ('b' == type) {
                        newOpts.put(name,true);
                        continue;
                    }

                    if ('r' == type) {
                        rp  = (Pattern) chk[2];
                        reg = (String ) chk[3];
                        err = (String ) chk[4];
                    }

                    if ('+' == sign || '*' == sign) {
                        vals = (List) newOpts.get(name);
                        if (vals == null) {
                            vals  = new ArrayList();
                            newOpts.put(name, vals);
                        }
                    }

                    W:while (i < args.length - 1) {
                        Object val ;
                        String arg = args [i + 1];
                        if (arg.startsWith("--")) {
                            if (vals == null || vals.isEmpty()) { // 缺少取值
                                errMsgs.add(GETERRS[2].replace("%opt", name));
                            }
                            break;
                        } else {
                            i ++ ;
                        }

                        switch (type) {
                            case 'i':
                                if (!IP.matcher(arg).matches()) {
                                    errMsgs.add(GETERRS[4].replace("%opt", name));
                                    continue;
                                }
                                val = Long.parseLong(arg);
                                break;
                            case 'f':
                                if (!FP.matcher(arg).matches()) {
                                    errMsgs.add(GETERRS[5].replace("%opt", name));
                                    continue;
                                }
                                val = Double.parseDouble(arg);
                                break;
                            case 'r':
                                if (!rp.matcher(arg).matches()) {
                                    errMsgs.add(err.replace("%mat", reg).replace("%opt", name));
                                    continue;
                                }
                            default:
                                val = arg;
                        }

                        if ('+' == sign || '*' == sign) {
                            vals.add(val);
                        } else {
                            if ('=' == sign && arg.isEmpty()) {
                                errMsgs.add(GETERRS[2].replace("%opt", name));
                            } else
                            if (newOpts.containsKey( name ) ) {
                                errMsgs.add(GETERRS[3].replace("%opt", name));
                            } else {
                                newOpts.put(name, val);
                            }
                            break;
                        }
                    }
                } else if (ub) {
                    // 未知参数
                    errMsgs.add(GETERRS[8].replace("%opt", name));
                } else {
                    newArgs.add(args[i]);
                }
            }
        } else if (vb) {
            // 匿名参数
            errMsgs.add(GETERRS[9]);
        } else {
            newArgs.add(args[i]);
        }
    }

    for (String name : reqOpts) {
      if (!newOpts.containsKey(name)) {
        Set<String> err = new LinkedHashSet();
        err.add(GETERRS[1].replace("%opt", name));
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

      throw  new HongsExemption(838, hlp, hlp);
    } else if (hlp != null && args.length == 0) {
      throw  new HongsExemption(839, hlp, hlp);
    }

    // 把剩余的参数放进去
    newOpts.put("", newArgs.toArray(new String[0]));

    return newOpts;
  }

  //** 输出相关 **/

  /**
   * 输出过程信息
   * 将输出到 OUT
   * 此方法用于显示条目、日志、名单等
   * @param text 提示文本
   */
  public static void paintln(String text)
  {
    OUT.get().println(text);
  }

  /**
   * 输出执行状态
   * 将输出到 ERR
   * 此方法用于显示错误、状态、进度等
   * @param text 提示文本
   */
  public static void println(String text)
  {
    ERR.get().println(text);
  }

  /**
   * 输出数据预览
   * 将输出到 OUT
   * 此方法会将对象(基础类型、集合框架)以 JSON 形式输出到终端.
   * @param data 预览数据
   */
  public static void preview(Object data)
  {
    PrintStream out = OUT.get ( );
    Dist.append(out, data, false);
    out.println();
  }

  /**
   * 输出执行进度
   * 将输出到 ERR
   * 由于大部分的终端(命令行)默认宽度普遍为 80 个字符,
   * 故请将 text 控制在 50 个字符以内, 一个中文占两位.
   * @param rate 完成比例, 0~1的浮点数
   * @param text 说明文本
   */
  public static void progres(float rate, String text)
  {
    StringBuilder sb = new StringBuilder();
    Formatter     ft = new Formatter( sb );

    if (text == null) text = "";
    if (rate  <  0  ) rate = 0 ;
    if (rate  >  1  ) rate = 1 ;

    rate = rate * 100; // 百分比

    sb.append("[");
    for(int i = 0 ; i < 100; i += 5)
    {
      if (rate < i + 5)
      {
        sb.append(' ');
      }
      else
      {
        sb.append('=');
      }
    }
    sb.append("]");

    ft.format(" %6.2f%% ", rate);
    sb.append(/* extra */  text);

    // 清除末尾多余字符
    // 并将光标移回行首
    // 无法获取宽度则为 80 (Windows 默认命令行窗口)
    int k = Synt.defxult(Synt.asInt(System.getenv("COLUMNS")), 80) - 1;
    int l =     sb.   length ( );
    if (l > k ) sb.setLength (k);
    for(int i = l ; i < k; i ++)
    {
      sb.append(" ");
    }
    sb.append( "\r");

    ERR.get().print(sb);
  }

  /**
   * 输出执行进度(按完成量计算)
   * @param n 总条目数
   * @param ok 完成条目数
   */
  public static void progres(int n, int ok)
  {
    if (0 == n) return;
    String notes = String.format("Ok(%d)", ok);
    float  scale = (float) ok / n;
    CombatHelper.progres(scale, notes);
  }

  /**
   * 输出执行进度(按完成失败量计算)
   * @param n 总条目数
   * @param ok 完成条目数
   * @param er 错误条目数
   */
  public static void progres(int n, int ok, int er)
  {
    if (0 == n) return;
    String notes = String.format("Ok(%d) Er(%d)", ok, er);
    float  scale = (float) (er + ok) / n;
    CombatHelper.progres(scale, notes);
  }

  /**
   * 输出剩余时间(按完成量计算, 此时间为估算)
   * @param t 开始时间(毫秒)
   * @param n 总条目数
   * @param ok 完成条目数
   */
  public static void progres(long t, int n, int ok)
  {
    if (0 == n ) {
        return ;
    }
    String notes ;
    float  scale = (float) ok / n;
    if (0 == ok) {
        notes = String.format("Ok(%d) ET: -" , ok);
    } else
    if (n == ok) {
        t = System.currentTimeMillis() - t;
        notes = String.format("Ok(%d) TT: %s", ok, Inst.phrase(t));
    } else {
        t = System.currentTimeMillis() - t;
        t = (long) (t / scale - t);
        notes = String.format("Ok(%d) ET: %s", ok, Inst.phrase(t));
    }
    CombatHelper.progres(scale, notes);
  }

  /**
   * 输出剩余时间(按完成失败量计算, 此时间为估算)
   * @param t 开始时间(毫秒)
   * @param n 总条目数
   * @param ok 完成条目数
   * @param er 错误条目数
   */
  public static void progres(long t, int n, int ok, int er)
  {
    if (0 == n ) {
        return ;
    }
    String notes ;
    float  scale = (float) (er + ok) / n;
    if (0 == ok + er) {
        notes = String.format("Ok(%d) Er(%d) ET: -" , ok, er);
    } else
    if (n == ok + er) {
        t = System.currentTimeMillis() - t;
        notes = String.format("Ok(%d) Er(%d) TT: %s", ok, er, Inst.phrase(t));
    } else {
        t = System.currentTimeMillis() - t;
        t = (long) (t / scale - t);
        notes = String.format("Ok(%d) Er(%d) ET: %s", ok, er, Inst.phrase(t));
    }
    CombatHelper.progres(scale, notes);
  }

  /**
   * 终止输出进度
   *
   * 请将执行块包裹在 try catch 中
   * 接获到异常或中止执行时
   * 使用本方法可安全的切行
   */
  public static void progres()
  {
    ERR.get().println();
  }

}
