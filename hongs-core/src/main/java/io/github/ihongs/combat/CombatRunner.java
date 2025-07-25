package io.github.ihongs.combat;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CoreRoster;
import io.github.ihongs.CruxCause;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.TimeZone;
import java.util.Locale;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 外壳程序启动器
 *
 * <h3>配置选项:</h3>
 * <pre>
 * server.id              服务ID
 * core.language.probing  探测语言
 * core.language.default  默认语言
 * core.timezone.probing  探测时区
 * core.timezone.default  默认时区
 * </pre>
 *
 * @author Hongs
 */
public class CombatRunner implements Runnable
{

  private final Method   met ;
  private final String   act ;
  private final String[] args;

  public CombatRunner(String[] args)
  {
    String  act = null ;
    int l = args.length;
    if (l == 1)
    {
      act  = args[0];
      args = new String[0];
    } else
    if (l  > 1)
    {
      act  = args[0];
      args = Arrays.copyOfRange(args, 1, l);
    }

    // 提取动作
    if (null == act || act.length() < 1)
    {
      throw new CruxExemption(835, "Combat name can not be empty.");
    }

    // 获取方法
    Method met = getCombats().get( act );
    if (null == met)
    {
      throw new CruxExemption(835, "Combat "+act+" is not exists.");
    }

    this.met  = met ;
    this.act  = act ;
    this.args = args;
  }

  /**
   * 后台任务
   */
  @Override
  public void run()
  {
    long now = System.currentTimeMillis( );
    Core.ACTION_TIME.set(now);
    Core.ACTION_NAME.set(act);
    try {
        met.invoke(null, new Object[]{ args });
    } catch (   IllegalAccessException ex) {
        CoreLogger.error("Illegal access for method "+met.getClass().getName()+"."+met.getName()+"(String[]).");
    } catch ( IllegalArgumentException ex) {
        CoreLogger.error("Illegal params for method "+met.getClass().getName()+"."+met.getName()+"(String[]).");
    } catch (InvocationTargetException ex) {
        CoreLogger.error( ex.getCause( ) );
    } finally {
        Core.getInstance( ).reset( );
    }
  }

  /**
   * 外部执行
   * @param args
   */
  public static void main(String[] args)
  {
    int c = 0;
    try
    {
      exec(init(args) );
    }
    catch (Throwable e)
    {
      if (e instanceof CruxCause) {
        CruxCause x = (CruxCause) e;
        int n = x.getErrno();

        DO: {
            if (n == 126 || n == 127) {
                c = n;   break DO;
            }

            e = e.getCause();
            if (e instanceof CruxCause) {
                x = (CruxCause) e;

                // Exit Code
                n = x.getErrno( );
                if (n == 256 || n == 257) {
                    c = 2;   break DO;
                }
                if (n == 0x0 || n == 0x1) {
                    n = 1;   break DO;
                }
                if (n >= 0x2 && n <= 255) {
                    c = n;   break DO;
                }

                // HTTP Code
                n = x.getState( );
                if (n >= 401 && n <= 403) {
                    c = 126; break DO;
                }
                if (n >= 404 && n <= 406) {
                    c = 127; break DO;
                }
                if (n >= 500) {
                    c = 128; break DO;
                }

                c = 2;
            } else {
                c = 1;
            }
        }
      } else {
        c = 1;
      }

      /**
       * 输入或缺失时输出错误消息即可,
       * 执行异常时需记录详细错误信息.
       */
      if (c == 2 || c == 126 || c == 127) {
        CoreLogger.error(e.getLocalizedMessage());
      } else {
        CoreLogger.error(e);
      }
    }
    finally
    {
      Core.getInstance().close();
      Core.getInterior().close();
      System.exit(c);
    }
  }

  /**
   * 内部执行
   * @param args
   */
  public static void exec(String[] args)
  {
    String  act = null ;
    int l = args.length;
    if (l == 1)
    {
      act  = args[0];
      args = new String[0];
    } else
    if (l  > 1)
    {
      act  = args[0];
      args = Arrays.copyOfRange(args, 1, l);
    }

    // 提取动作
    if (null == act || act.length() < 1)
    {
      throw new CruxExemption(127, "Combat name can not be empty.");
    }

    // 获取方法
    Method met = getCombats().get( act );
    if (null == met)
    {
      throw new CruxExemption(127, "Combat "+act+" is not exists.");
    }

    // 执行方法
    try
    {
      met.invoke(null, new Object[] {args});
    }
    catch (   IllegalAccessException ex)
    {
      throw new CruxExemption(ex, 126, "Illegal access for method "+met.getClass().getName()+"."+met.getName()+"(String[]).");
    }
    catch ( IllegalArgumentException ex)
    {
      throw new CruxExemption(ex, 126, "Illegal params for method "+met.getClass().getName()+"."+met.getName()+"(String[]).");
    }
    catch (InvocationTargetException ex)
    {
      throw new CruxExemption(ex.getCause(), 128);
    }
  }

  /**
   * 命令启动初始化
   * @param args
   * @return
   */
  public static String[] init(String[] args)
  {
    Map<String, Object> opts;
    opts = CombatHelper.getOpts(args,
           "DEBUG:i" , "COREPATH:s" ,
        "CONFPATH:s" , "DATAPATH:s" ,
        "BASEPATH:s" , "BASEHREF:s" ,
        "LANGUAGE:s" , "TIMEZONE:s" ,
        "!U", "!A"
    );
    args = ( String[] ) opts.get("");

    Core.THREAD_CORE.set(Core.GLOBAL_CORE);
    Core.ACTION_TIME.set(Core.STARTS_TIME);
    Core.ACTION_NAME.set(args.length != 0 ? args[0] : "");

    /** 核心属性配置 **/

    Core.ENVIR = 0;
    Core.DEBUG = Synt.declare(opts.get("DEBUG") , (byte) 0 );
    Core.SERVER_ID = Synt.declare(opts.get("SERVERID"), "0");
    Core.CORE_PATH = Synt.declare(opts.get("COREPATH"), System.getProperty("user.dir"));
    Core.CORE_PATH = new File(Core.CORE_PATH).getAbsolutePath();
    Core.CONF_PATH = Synt.declare(opts.get("CONFPATH"), Core.CORE_PATH + File.separator + "etc");
    Core.DATA_PATH = Synt.declare(opts.get("DATAPATH"), Core.CORE_PATH + File.separator + "var");
    Core.BASE_PATH = Synt.declare(opts.get("BASEPATH"), Core.CORE_PATH + File.separator + "web");

    // 如果 web 目录不存在, 则视为在 WEB-INF 下
    File bp = new File(Core.BASE_PATH);
    if (!bp.exists()) {
       Core.BASE_PATH = bp.getParent();
    }

    /** 系统属性配置 **/

    Map m = new HashMap(5);
    m.put("SERVER_ID", Core.SERVER_ID);
    m.put("BASE_PATH", Core.BASE_PATH);
    m.put("CORE_PATH", Core.CORE_PATH);
    m.put("CONF_PATH", Core.CONF_PATH);
    m.put("DATA_PATH", Core.DATA_PATH);

    CoreConfig cnf = CoreConfig.getInstance("defines");

    // 启动系统属性
    for(Map.Entry et : cnf.entrySet()) {
        String k = (String) et.getKey  ();
        String v = (String) et.getValue();
        if (k.startsWith("envir.")) {
            k = k.substring(6  );
            v = Syno.inject(v,m);
            System.setProperty(k,v);
        }
    }

    if (4 == (4 & Core.DEBUG)) {
    // 调试系统属性
    for(Map.Entry et : cnf.entrySet()) {
        String k = (String) et.getKey  ();
        String v = (String) et.getValue();
        if (k.startsWith("debug.")) {
            k = k.substring(6  );
            v = Syno.inject(v,m);
            System.setProperty(k,v);
        }
    }
    }

    /** 提取网址前缀 **/

    String su = Synt.defoult((String) opts.get( "BASEHREF" ), System.getProperty("serv.url"));
    if (null != su) {
        Pattern pattern = Pattern.compile( "^\\w+://[^/]+" );
        Matcher matcher = pattern.matcher( su );
        if (matcher.find()) {
            Core.SERV_PATH = su.substring(0 + matcher.end());
            Core.SERV_HREF = su.substring(0 , matcher.end());
        } else {
            Core.SERV_PATH = su;
            Core.SERV_HREF = "";
        }
    } else {
            Core.SERV_PATH = "";
            Core.SERV_HREF = "";
    }

    /** 实例属性配置 **/

    cnf = CoreConfig.getInstance("default");

    // 默认语言时区
    Core.ACTION_LANG.set(cnf.getProperty("core.language.default", Cnst.LANG_DEF));
    Core.ACTION_ZONE.set(cnf.getProperty("core.timezone.default", Cnst.ZONE_DEF));
    Locale  .setDefault(Core.getLocale  ());
    TimeZone.setDefault(Core.getTimeZone());

    // 当前语言时区
    String lang = (String) opts.get("LANGUAGE");
    if (lang != null && !lang.isEmpty()) {
        lang  = CoreLocale.getAcceptLanguage(lang);
        if (lang != null) {
            Core.ACTION_LANG.set(lang);
        }
    }
    String zone = (String) opts.get("TIMEZONE");
    if (zone != null && !zone.isEmpty()) {
        zone  = CoreLocale.getNormalTimeZone(zone);
        if (zone != null) {
            Core.ACTION_ZONE.set(zone);
        }
    }

    /** 初始化动作助手, 可复用动作组件 **/

    ActionHelper hlpr = new ActionHelper (new HashMap(), new HashMap(0), new HashMap(0), new HashMap(0));
    Core.getInstance().set(ActionHelper.class.getName(), hlpr);
    hlpr.updateOutput(System.out, new PrintWriter(System.out));

    return args;
  }

  public static Map<String, Method> getCombats()
  {
    return CoreRoster.getCombats();
  }

}
