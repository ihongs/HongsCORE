package app.hongs.cmdlet;

import app.hongs.CoreLogger;
import app.hongs.HongsError;
import app.hongs.util.Data;
import app.hongs.util.Tool;
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
 * @author Hongs
 */
public class CmdletHelper
{

  //** 参数相关 **/

  /**
   * 错误消息集合
   */
  private static final String[] getErrs = new String[]
  {
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

  /**
   * 解析参数
   * 本函数遵循 Perl 的 Getopt::Long 解析规则
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
    Pattern  p = Pattern.compile("^([\\w\\.\\-\\|]*)(=|:|\\+|\\*)([sifb]|\\/(.+)\\/(i)?( .*)?)$");
    Pattern bp = Pattern.compile("^(true|false|yes|no|y|n|1|0)$", Pattern.CASE_INSENSITIVE);
    Pattern tp = Pattern.compile("^(true|yes|y|1)$", Pattern.CASE_INSENSITIVE);
    Pattern fp = Pattern.compile("^\\d+(\\.\\d+)?$");
    Pattern ip = Pattern.compile("^\\d+$");
    boolean ub = false; // 禁止未知参数
    boolean vb = false; // 禁止匿名参数
    String hlp = null ; // 用法说明
    String pre = "\r\n\t";

    for (String chk : chks) {
      Matcher m = p.matcher(chk);

      if (!m.find()) {
        if (chk.equals("!U")) {
            ub  = true;
            continue;
        } else
        if (chk.equals("!A")) {
            vb  = true;
            continue;
        } else
        if (chk.startsWith("?")) {
            hlp = chk.substring(1);
            continue;
        }

        // 0号错误
        errMsgs.add(getErrs[0].replace("%chk", chk));
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
          err = getErrs[7];
        }
        reg = "/"+reg+"/"+mod;
        chkz.put(name, new Object[] {sign.charAt(0),'r',pat,reg,err});
      } else {
        chkz.put(name, new Object[] {sign.charAt(0), type.charAt(0)});
      }
    }

    F:for (int i = 0; i < args.length; i ++) {
      String name = args[i];

      if (name.startsWith("--")) {
        name = name.substring(2);

        if (chkz.containsKey(name)) {
          Object[] chk = chkz.get(name);
          char    sign = (Character)chk[0];
          char    type = (Character)chk[1];
          Pattern   rp = null;
          String   reg = null;
          String   err = null;
          Object   val = null;
          List    vals = null;

          if ('r' == type) {
            rp  = (Pattern)chk[3];
            reg = (String) chk[4];
            err = (String) chk[5];
          }

          if ('+' == sign || '*' == sign) {
            vals = (List)newOpts.get(name);
            if (vals == null) {
              vals = new ArrayList( );
              newOpts.put(name, vals);
            }
          }

          W:while (i < args.length-1) {
            String arg = args[i + 1];
            if (arg.startsWith("--")) {
              if (i == 0) break;
              else  continue  F;
            }
            if (arg.startsWith("\\")) {
              arg = arg.substring(1);
            }
            i ++;

            switch (type) {
              case 'i':
                if (!ip.matcher(arg).matches()) {
                  errMsgs.add(getErrs[4].replace("%opt", name));
                  continue W;
                }
                val = Long.parseLong(arg);
                break;
              case 'f':
                if (!fp.matcher(arg).matches()) {
                  errMsgs.add(getErrs[5].replace("%opt", name));
                  continue W;
                }
                val = Double.parseDouble(arg);
                break;
              case 'b':
                if (!bp.matcher(arg).matches()) {
                  errMsgs.add(getErrs[6].replace("%opt", name));
                  continue W;
                }
                val = tp.matcher(arg).matches();
                break;
              case 'r':
                if (!rp.matcher(arg).matches()) {
                  errMsgs.add(err.replace("%opt", name).replace("%mat", reg));
                  continue W;
                }
              default:
                val = arg;
            }

            if ('+' == sign || '*' == sign) {
              vals.add(val );
            } else {
              if (newOpts.containsKey(name)) {
                errMsgs.add(getErrs[3].replace("%opt", name));
              } else {
                newOpts.put(name, val );
              }
            }

            continue F;
          }

          if ('b' == type) {
            if ('+'== sign || '*' == sign) {
              vals.add(true);
            } else {
              if (newOpts.containsKey(name)) {
                errMsgs.add(getErrs[3].replace("%opt", name));
              } else {
                newOpts.put(name, true);
              }
            }
          } else {
            errMsgs.add(getErrs[2].replace("%opt", name));
          }
        }
        else if (ub) {
            // 7号错误
            errMsgs.add(getErrs[8].replace("%opt", name));
        }
        else {
            newArgs.add(   args[i]);
        }
      }
      else if (vb) {
        // 8号错误
        errMsgs.add(getErrs[9]);
      }
      else {
        newArgs.add(   args[i]);
      }
    }

    for (String name : reqOpts) {
      if (!newOpts.containsKey(name)) {
        Set<String> err = new LinkedHashSet();
        err.add(getErrs[1].replace("%opt", name));
        err.addAll(errMsgs);
        errMsgs  =  err;
      }
    }

    if (!errMsgs.isEmpty()) {
      StringBuilder err = new StringBuilder();
      for ( String  msg : errMsgs ) {
        err.append(pre).append(msg);
      }
      String msg = err.toString(  );
      String trs = msg;
      if (null  != hlp) {
        trs += pre + hlp.replaceAll("\\n", pre);
      }

      HongsError er = new HongsError(0x35, msg);
      er.setLocalizedOptions(trs);
      throw er;
    } else if (hlp != null && args.length == 0) {
      System.err.println(hlp.replaceAll("\\n",pre));
      System.exit(0);
    }

    // 把剩余的参数放进去
    newOpts.put("", newArgs.toArray(new String[0]));

    return newOpts;
  }

  //** 输出相关 **/

  /**
   * 输出数据预览
   * 此方法会将对象(基础类型、集合框架)以 JSON 形式输出到终端.
   * @param data 预览数据
   */
  public static void preview(Object data)
  {
    Data.append(System.out , data, false);
  }

  /**
   * 输出辅助信息
   * 此方法通过 CoreLogger 输出到 hongs.out, 仅被用作辅助输出;
   * 如果需要输出结构化的数据供其它程序处理, 请不要使用此方法.
   * @param text 提示文本
   */
  public static void println(String text)
  {
    CoreLogger.getLogger(CoreLogger.space("hongs.out"))
              .trace/**/(CoreLogger.envir( text /**/ ));
  }

  /**
   * 输出执行进度
   * 由于大部分的终端(命令行)默认宽度普遍为 80 个字符,
   * 故请将 text 控制在 50 个字符以内, 一个中文占两位.
   * @param text 说明文本
   * @param rate 完成比例, 0~100的浮点数
   */
  public static void progres(String text, float rate)
  {
    StringBuilder sb = new StringBuilder();
    Formatter     ft = new Formatter( sb );

    if (text == null) text = "" ;
    if (rate <    0 ) rate =   0;
    if (rate >  100 ) rate = 100;

    sb.append("|");
    for (int i = 0; i < 100; i += 5)
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
    sb.append("|");

    ft.format(" %6.2f%% ", rate);
    sb.append(/* extra */  text);

    // 清除末尾多余的字符, 并将光标移回行首
    // 每行按最少80个字符来计算
    for (int i = sb.length(); i < 79; i += 1)
    {
      sb.append(" ");
    }
    sb.append( "\r");

    if (rate == 100)
    {
      System.err.println(sb.toString());
    }
    else
    {
      System.err.print  (sb.toString());
    }
  }

  /**
   * 输出执行进度(按完成量计算)
   * @param n 总条目数
   * @param ok 完成条目数
   */
  public static void progres(int n, int ok)
  {
    String notes = String.format("Ok(%d)", ok);
    float  scale = (float)ok / n * 100;
    CmdletHelper.progres(notes, scale);
  }

  /**
   * 输出执行进度(按完成失败量计算)
   * @param n 总条目数
   * @param ok 完成条目数
   * @param er 错误条目数
   */
  public static void progres(int n, int ok, int er)
  {
    String notes = String.format("Ok(%d) Er(%d)", ok, er);
    float  scale = (float)(er + ok) / n * 100;
    CmdletHelper.progres(notes, scale);
  }

  /**
   * 输出剩余时间(按完成量计算, 此时间为估算)
   * @param t 开始时间(毫秒)
   * @param n 总条目数
   * @param ok 完成条目数
   */
  public static void progres(long t, int n, int ok)
  {
    float  scale = (float)ok / n * 100;
    t = System.currentTimeMillis() - t;
    float  left1 = t / scale * 100 - t;
    String left2 = Tool.humanTime((long) left1);
    String left3 = String.format("Ok(%d) TL: %s",
                                     ok, left2);
    CmdletHelper.progres(left3, scale);
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
    float  scale = (float)( er + ok ) / n * 100;
    t = System.currentTimeMillis() - t;
    float  left1 = t / scale * 100 - t;
    String left2 = Tool.humanTime((long) left1);
    String left3 = String.format("Ok(%d) Er(%d) TL: %s",
                                 ok, er, left2);
    CmdletHelper.progres(left3, scale);
  }

  /**
   * 终止输出进度
   *
   * 请将执行块包裹在 try catch 中
   * 接获到异常或中止执行时
   * 使用本方法可安全的切行
   */
  public static void progred()
  {
    System.err.println("");
  }

}
